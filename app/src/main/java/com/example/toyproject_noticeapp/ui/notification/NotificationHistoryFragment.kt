package com.example.toyproject_noticeapp.ui.notification

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.FragmentNotificationHistoryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class NotificationHistoryFragment : Fragment() {

    private var _binding: FragmentNotificationHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationAdapter: AdapterNotificationList

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        loadNotificationHistory()
    }

    private fun setupToolbar() {
        binding.toolbarNotificationHistory.toolbar.title = "알림 내역"
        binding.toolbarNotificationHistory.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarNotificationHistory.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    // --- 👇 *** 여기가 핵심 수정 사항입니다! *** 👇 ---
    private fun setupRecyclerView() {
        notificationAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice ->
                // Toast 메시지 대신 즐겨찾기 업데이트 함수 호출
                updateFavoriteStatus(notice)
            }
        )
        binding.recyclerviewNotificationHistoryList.adapter = notificationAdapter
        binding.recyclerviewNotificationHistoryList.layoutManager = LinearLayoutManager(context)
    }
    // --- 👆 *** 여기가 핵심 수정 사항입니다! *** 👆 ---

    private fun loadNotificationHistory() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).collection("notification_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val historyList = documents.map { doc ->
                    doc.toObject(DataNotificationItem::class.java)
                }
                updateFavoritesState(historyList) // 즐겨찾기 상태 업데이트
            }
            .addOnFailureListener {
                Toast.makeText(context, "알림 내역을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // --- 👇 *** 여기가 핵심 수정 사항입니다! *** 👇 ---
    private fun updateFavoritesState(historyList: List<DataNotificationItem>) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val favoriteIds = document.get("favorites") as? List<String> ?: emptyList()
                historyList.forEach { notice ->
                    val noticeDocId = "${notice.category}_${notice.id}"
                    notice.isFavorite = favoriteIds.contains(noticeDocId)
                }
            }
            notificationAdapter.submitList(historyList)
        }.addOnFailureListener {
            notificationAdapter.submitList(historyList)
        }
    }
    // --- 👆 *** 여기가 핵심 수정 사항입니다! *** 👆 ---

    private fun openInAppBrowser(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            Toast.makeText(context, "페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 👇 *** 여기가 핵심 수정 사항입니다! (새로운 함수 추가) *** 👇 ---
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

        // UI 즉시 업데이트
        val currentList = notificationAdapter.currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (index != -1) {
            currentList[index].isFavorite = newFavoriteState
            notificationAdapter.notifyItemChanged(index)
        }

        // Firestore 데이터 업데이트
        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }

        updateTask.addOnFailureListener {
            Toast.makeText(context, "즐겨찾기 상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            // 실패 시 UI 원상 복구
            if (index != -1) {
                currentList[index].isFavorite = !newFavoriteState
                notificationAdapter.notifyItemChanged(index)
            }
        }
    }
    // --- 👆 *** 여기가 핵심 수정 사항입니다! *** 👆 ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}