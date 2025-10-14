package com.example.toyproject_noticeapp.ui.notification

import android.net.Uri
import android.os.Bundle
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class NotificationHistoryFragment : Fragment() {

    private var _binding: FragmentNotificationHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationAdapter: AdapterNotificationList
    private var historyNotifications = mutableListOf<DataNotificationItem>()

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
        loadNotificationHistory()
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
            sortAndDisplayList()
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
                updateFavoriteStatus(notice)
            }
        )
        binding.recyclerviewNotificationHistoryList.adapter = notificationAdapter
        binding.recyclerviewNotificationHistoryList.layoutManager = LinearLayoutManager(context)
    }

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
                historyNotifications = documents.toObjects(DataNotificationItem::class.java).toMutableList()
                updateFavoritesStateAndUpdateList()
            }
            .addOnFailureListener {
                Toast.makeText(context, "알림 내역을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFavoritesStateAndUpdateList() {
        val userId = auth.currentUser?.uid ?: run {
            sortAndDisplayList()
            return
        }

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val favoriteIds = document.get("favorites") as? List<String> ?: emptyList()
                historyNotifications.forEach { notice ->
                    val noticeDocId = "${notice.category}_${notice.id}"
                    notice.isFavorite = favoriteIds.contains(noticeDocId)
                }
            }
            sortAndDisplayList()
        }.addOnFailureListener {
            sortAndDisplayList()
        }
    }

    private fun sortAndDisplayList() {
        val sortedList = when (currentSortOrder) {
            // 알림 내역은 timestamp를 기준으로 정렬
            SortOrder.LATEST -> historyNotifications.sortedByDescending { it.timestamp }
            SortOrder.OLDEST -> historyNotifications.sortedBy { it.timestamp }
            SortOrder.VIEWS -> historyNotifications.sortedByDescending { it.viewCount }
        }
        notificationAdapter.submitList(sortedList) {
            binding.recyclerviewNotificationHistoryList.scrollToPosition(0)
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

        val indexInAllList = historyNotifications.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (indexInAllList != -1) {
            historyNotifications[indexInAllList].isFavorite = newFavoriteState
            val currentAdapterList = notificationAdapter.currentList
            val indexInAdapter = currentAdapterList.indexOfFirst { it.id == notice.id && it.category == notice.category }
            if (indexInAdapter != -1) {
                notificationAdapter.notifyItemChanged(indexInAdapter)
            }
        }

        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }

        updateTask.addOnFailureListener {
            Toast.makeText(context, "즐겨찾기 상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            if (indexInAllList != -1) {
                historyNotifications[indexInAllList].isFavorite = !newFavoriteState
                notificationAdapter.notifyItemChanged(
                    notificationAdapter.currentList.indexOfFirst { it.id == notice.id && it.category == notice.category }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.setSupportActionBar(null)
        _binding = null
    }
}