package com.example.toyproject_noticeapp.ui.notification

import android.net.Uri
import android.os.Bundle
import android.util.Log // 로그 추가
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.FragmentNotificationHistoryBinding
import com.google.firebase.Timestamp // Timestamp 추가
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


// ▼▼▼ [ 수정된 클래스 ] ▼▼▼
// notification_history 컬렉션의 데이터 구조에 맞는 데이터 클래스 정의
data class NotificationHistoryEntry(
    val noticeDocId: String = "",
    val timestamp: Timestamp = Timestamp.now() // 기본값 설정
)
// ▲▲▲ [ 수정된 클래스 ] ▲▲▲

class NotificationHistoryFragment : Fragment() {

    private var _binding: FragmentNotificationHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationAdapter: AdapterNotificationList
    // ▼▼▼ [ 수정된 변수 ] ▼▼▼
    // 최종적으로 화면에 표시될 공지 데이터 목록
    private var historyNotifications = mutableListOf<DataNotificationItem>()
    // ▲▲▲ [ 수정된 변수 ] ▲▲▲

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private enum class SortOrder {
        VIEWS, LATEST, OLDEST
    }
    private var currentSortOrder = SortOrder.LATEST

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationHistoryBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        // ▼▼▼ [ 수정된 호출 ] ▼▼▼
        // 코루틴을 사용하여 비동기 데이터 로딩 처리
        CoroutineScope(Dispatchers.Main).launch {
            loadNotificationHistoryOptimized()
        }
        // ▲▲▲ [ 수정된 호출 ] ▲▲▲
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_sort, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            findNavController().navigateUp()
            return true
        }

        val newSortOrder = when (item.itemId) {
            R.id.sort_by_views -> SortOrder.VIEWS
            R.id.sort_by_latest -> SortOrder.LATEST
            R.id.sort_by_oldest -> SortOrder.OLDEST
            else -> null
        }

        if (newSortOrder != null) {
            currentSortOrder = newSortOrder
            sortAndDisplayList() // 정렬 및 표시 함수는 동일하게 사용
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        binding.toolbarNotificationHistory.toolbar.title = "알림 내역"
        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(binding.toolbarNotificationHistory.toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice ->
                updateFavoriteStatus(notice) // 즐겨찾기 업데이트 함수는 동일하게 사용
            }
        )
        binding.recyclerviewNotificationHistoryList.adapter = notificationAdapter
        binding.recyclerviewNotificationHistoryList.layoutManager = LinearLayoutManager(context)
    }

    // ▼▼▼ [ 완전히 새로 작성된 함수 ] ▼▼▼
    private suspend fun loadNotificationHistoryOptimized() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 1. notification_history에서 ID와 timestamp 목록 가져오기 (시간 내림차순)
            val historyEntries = withContext(Dispatchers.IO) {
                db.collection("users").document(uid).collection("notification_history")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects(NotificationHistoryEntry::class.java)
            }

            if (historyEntries.isEmpty()) {
                Log.d("History", "알림 내역이 비어 있습니다.")
                historyNotifications.clear()
                updateFavoritesStateAndUpdateList() // 빈 목록으로 UI 업데이트
                return
            }

            // 2. historyEntries에서 noticeDocId 목록 추출
            val noticeDocIds = historyEntries.map { it.noticeDocId }.distinct()

            // Firestore 'in' 쿼리는 최대 30개의 ID만 지원하므로, 나누어서 처리
            val noticeDetailsMap = mutableMapOf<String, DataNotificationItem>()
            val chunks = noticeDocIds.chunked(30)

            withContext(Dispatchers.IO) {
                chunks.forEach { chunk ->
                    if (chunk.isNotEmpty()) {
                        val noticeDocs = db.collection("notices")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                            .get()
                            .await()

                        noticeDocs.documents.forEach { doc ->
                            // DataNotificationItem으로 변환 (이때 timestamp는 notices 컬렉션의 것임)
                            val noticeItem = doc.toObject(DataNotificationItem::class.java)
                            if (noticeItem != null) {
                                noticeDetailsMap[doc.id] = noticeItem
                            }
                        }
                    }
                }
            }


            // 3. historyEntries와 noticeDetailsMap을 조합하여 최종 historyNotifications 목록 생성
            historyNotifications = historyEntries.mapNotNull { entry ->
                noticeDetailsMap[entry.noticeDocId]?.copy(
                    // ★★★ 중요: timestamp를 notification_history의 것으로 교체 ★★★
                    timestamp = entry.timestamp
                )
            }.toMutableList()

            Log.d("History", "최종 로드된 알림 개수: ${historyNotifications.size}")

            // 4. 즐겨찾기 상태 업데이트 및 목록 표시
            updateFavoritesStateAndUpdateList()

        } catch (e: Exception) {
            Log.e("History", "알림 내역 로딩 실패", e)
            Toast.makeText(context, "알림 내역을 불러오는데 실패했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            historyNotifications.clear() // 실패 시 목록 비우기
            updateFavoritesStateAndUpdateList() // UI 업데이트
        }
    }
    // ▲▲▲ [ 완전히 새로 작성된 함수 ] ▲▲▲

    // 즐겨찾기 상태 업데이트 함수는 기존 로직 유지 가능 (historyNotifications 사용)
    private fun updateFavoritesStateAndUpdateList() {
        val userId = auth.currentUser?.uid ?: run {
            sortAndDisplayList() // 로그아웃 상태면 그냥 현재 목록 정렬
            return
        }

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val favoriteIds = document.get("favorites") as? List<String> ?: emptyList()
                historyNotifications.forEach { notice ->
                    // DataNotificationItem에 id, category가 있으므로 noticeDocId 재생성 가능
                    val noticeDocId = "${notice.category}_${notice.id}"
                    notice.isFavorite = favoriteIds.contains(noticeDocId)
                }
            }
            Log.d("History", "즐겨찾기 상태 업데이트 완료, 정렬 시작")
            sortAndDisplayList() // 즐겨찾기 상태 반영 후 정렬 및 표시
        }.addOnFailureListener {
            Log.e("History", "즐겨찾기 정보 로드 실패", it)
            sortAndDisplayList() // 실패해도 일단 정렬 및 표시는 진행
        }
    }

    // 정렬 및 표시 함수는 기존 로직 유지 가능 (historyNotifications 사용)
    private fun sortAndDisplayList() {
        Log.d("History", "정렬 방식: $currentSortOrder, 정렬 대상 개수: ${historyNotifications.size}")
        val sortedList = when (currentSortOrder) {
            // ★★★ 중요: timestamp는 이미 notification_history의 것이므로 그대로 사용 ★★★
            SortOrder.LATEST -> historyNotifications.sortedByDescending { it.timestamp }
            SortOrder.OLDEST -> historyNotifications.sortedBy { it.timestamp }
            SortOrder.VIEWS -> historyNotifications.sortedByDescending { it.viewCount }
        }
        Log.d("History", "정렬 완료, 어댑터 업데이트 시작")
        notificationAdapter.submitList(sortedList.toList()) { // toList()로 불변 리스트 전달
            if (sortedList.isNotEmpty()) {
                binding.recyclerviewNotificationHistoryList.scrollToPosition(0)
            }
            Log.d("History", "어댑터 업데이트 완료 및 스크롤 이동")
        }
    }

    private fun openInAppBrowser(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            Toast.makeText(context, "페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 즐겨찾기 상태 변경 함수는 기존 로직 유지 가능 (historyNotifications 사용)
    private fun updateFavoriteStatus(notice: DataNotificationItem) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val userDocRef = db.collection("users").document(userId)
        val noticeDocId = "${notice.category}_${notice.id}"
        val newFavoriteState = !notice.isFavorite

        val message = if (newFavoriteState) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 삭제되었습니다."
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        // historyNotifications (원본 데이터) 업데이트
        val indexInAllList = historyNotifications.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (indexInAllList != -1) {
            historyNotifications[indexInAllList].isFavorite = newFavoriteState
        }

        // 어댑터에 현재 표시된 목록 가져와서 업데이트 및 UI 갱신
        // submitList는 비동기로 동작하므로 currentList를 직접 수정하는 것보다 안전
        val currentAdapterList = notificationAdapter.currentList.toMutableList()
        val indexInAdapter = currentAdapterList.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (indexInAdapter != -1) {
            // 어댑터 아이템의 isFavorite 상태 변경 (복사본을 만들어서 변경)
            val updatedItem = currentAdapterList[indexInAdapter].copy(isFavorite = newFavoriteState)
            currentAdapterList[indexInAdapter] = updatedItem
            // 변경된 리스트를 다시 어댑터에 제출 (DiffUtil이 변경 감지 후 효율적으로 업데이트)
            notificationAdapter.submitList(currentAdapterList.toList()) {
                // 필요하다면 업데이트 완료 후 특정 작업 수행
            }
        }


        // Firestore 업데이트
        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }

        updateTask.addOnFailureListener {
            Toast.makeText(context, "즐겨찾기 상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            // 실패 시 UI 원상 복구
            if (indexInAllList != -1) {
                historyNotifications[indexInAllList].isFavorite = !newFavoriteState // 원본 데이터 복구
            }
            if (indexInAdapter != -1) {
                // 어댑터에도 원상 복구된 상태를 반영하여 다시 제출
                val revertedItem = currentAdapterList[indexInAdapter].copy(isFavorite = !newFavoriteState)
                currentAdapterList[indexInAdapter] = revertedItem
                notificationAdapter.submitList(currentAdapterList.toList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.setSupportActionBar(null)
        _binding = null
    }
}