package com.example.toyproject_noticeapp.ui.notification

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.FragmentNotificationMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FragmentNotificationMain : Fragment() {

    private var _binding: FragmentNotificationMainBinding? = null
    private val binding get() = _binding!!
    private val args: FragmentNotificationMainArgs by navArgs()

    private lateinit var notificationAdapter: AdapterNotificationList
    private var allNotifications = mutableListOf<DataNotificationItem>()
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    private enum class SortOrder {
        VIEWS, LATEST, OLDEST
    }
    private var currentSortOrder = SortOrder.LATEST

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationMainBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupNotificationRecyclerView()
        fetchNoticesFromFirebase()
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

    private fun fetchNoticesFromFirebase() {
        val categoryToShow = args.categoryName
        val collectionPath = if (categoryToShow == "인기글") "popular_notices" else "notices"
        var query: Query = db.collection(collectionPath)

        if (categoryToShow != "인기글") {
            if (categoryToShow != "전체") {
                query = query.whereEqualTo("category", categoryToShow)
            }
            // 데이터를 ID 순서대로 가져옵니다.
            query = query.orderBy("id", Query.Direction.DESCENDING)
        }

        query.get()
            .addOnSuccessListener { result ->
                allNotifications = result.toObjects(DataNotificationItem::class.java).toMutableList()
                updateFavoritesStateAndApplySort()
            }
            .addOnFailureListener {
                Toast.makeText(context, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupToolbar() {
        val toolbarTitle = if (args.categoryName == "인기글") "Now! 인기글" else args.categoryName
        binding.toolbarNotificationMain.toolbar.title = toolbarTitle

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(binding.toolbarNotificationMain.toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        }
    }

    private fun setupNotificationRecyclerView() {
        notificationAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice -> updateFavoriteStatus(notice) }
        )
        binding.recyclerviewNotificationList.adapter = notificationAdapter
        binding.recyclerviewNotificationList.layoutManager = LinearLayoutManager(context)
    }

    private fun updateFavoritesStateAndApplySort() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            sortAndDisplayList()
            return
        }

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val favoriteIds = document.get("favorites") as? List<String> ?: emptyList()
                allNotifications.forEach { notice ->
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
            SortOrder.VIEWS -> allNotifications.sortedByDescending { it.viewCount }
            // ID를 기준으로 최신/오래된 순 정렬
            SortOrder.LATEST -> allNotifications.sortedByDescending { it.id }
            SortOrder.OLDEST -> allNotifications.sortedBy { it.id }
        }

        notificationAdapter.submitList(sortedList) {
            binding.recyclerviewNotificationList.scrollToPosition(0)
        }
    }

    private fun openInAppBrowser(url: String) {
        if (url.isNotEmpty()) {
            try {
                val customTabsIntent = CustomTabsIntent.Builder().build()
                customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
            } catch (e: Exception) {
                Toast.makeText(context, "페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "연결된 링크가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFavoriteStatus(notice: DataNotificationItem) {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)
        val noticeDocId = "${notice.category}_${notice.id}"
        val newFavoriteState = !notice.isFavorite

        val message = if (newFavoriteState) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 삭제되었습니다."
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        val indexInAllList = allNotifications.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (indexInAllList != -1) {
            allNotifications[indexInAllList].isFavorite = newFavoriteState
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
            Toast.makeText(context, "즐겨찾기 상태 변경 실패", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.setSupportActionBar(null)
        _binding = null
    }
}