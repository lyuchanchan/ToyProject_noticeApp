package com.example.toyproject_noticeapp.ui.notification

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList
import com.example.toyproject_noticeapp.adapter.FilterAdapter
import com.example.toyproject_noticeapp.adapter.FilterItem
import com.example.toyproject_noticeapp.adapter.FilterType
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.FragmentNotificationMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FragmentNotificationMain : Fragment() {
    // ... (변수 선언 및 onCreateView는 동일)
    private var _binding: FragmentNotificationMainBinding? = null
    private val binding get() = _binding!!
    private val args: FragmentNotificationMainArgs by navArgs()

    private lateinit var notificationAdapter: AdapterNotificationList
    private var allNotifications = mutableListOf<DataNotificationItem>()
    private val filterList = mutableListOf<FilterItem>()
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ⬇️ onViewCreated 이하 모든 함수를 아래 코드로 교체 ⬇️
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (filterList.isEmpty()){
            loadFilterData()
        }
        setupToolbar()
        setupFilterRecyclerView()
        setupNotificationRecyclerView()
        fetchNoticesFromFirebase()
    }

    private fun loadFilterData() {
        filterList.clear()
        val initialFilters = listOf(
            FilterItem("전체선택", FilterType.COMMAND),
            FilterItem("전체해제", FilterType.COMMAND),
            FilterItem("공지사항", FilterType.CATEGORY, isSelected = true),
            FilterItem("학사공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("행사공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("장학공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("취업공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("AISW계열", FilterType.CATEGORY, isSelected = true)
        )

        if (args.categoryName != "인기글" && args.categoryName != "전체") {
            initialFilters.filter { it.type == FilterType.CATEGORY }.forEach {
                it.isSelected = it.name == args.categoryName
            }
        }
        filterList.addAll(initialFilters)
    }

    private fun fetchNoticesFromFirebase() {
        val categoryToShow = args.categoryName
        val collectionPath = if (categoryToShow == "인기글") "popular_notices" else "notices"
        var query: Query = db.collection(collectionPath)

        if (categoryToShow != "인기글" && categoryToShow != "전체") {
            query = query.whereEqualTo("category", categoryToShow)
        }
        query = query.orderBy("id", Query.Direction.DESCENDING)

        query.get()
            .addOnSuccessListener { result ->
                allNotifications = result.mapNotNull { doc ->
                    DataNotificationItem(
                        id = (doc.getLong("id") ?: 0L).toInt(),
                        category = doc.getString("category") ?: "",
                        date = doc.getString("date") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        url = doc.getString("url") ?: "",
                        viewCount = (doc.getLong("viewCount") ?: 0L).toInt()
                    )
                }.toMutableList()

                updateFavoritesStateAndApplyFilters()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupToolbar() {
        binding.toolbarNotificationMain.toolbar.title = if (args.categoryName == "인기글") "이 달의 인기글" else args.categoryName
        binding.toolbarNotificationMain.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarNotificationMain.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupFilterRecyclerView() {
        val categoryToShow = args.categoryName
        if (categoryToShow != "전체" && categoryToShow != "인기글") {
            binding.recyclerviewNotificationFilters.visibility = View.GONE
        } else {
            binding.recyclerviewNotificationFilters.visibility = View.VISIBLE
        }

        val filterAdapter = FilterAdapter(filterList) { selectedFilter ->
            when (selectedFilter.type) {
                FilterType.COMMAND -> {
                    val shouldSelectAll = selectedFilter.name == "전체선택"
                    filterList.filter { it.type == FilterType.CATEGORY }.forEach { it.isSelected = shouldSelectAll }
                }
                FilterType.CATEGORY -> {
                    selectedFilter.isSelected = !selectedFilter.isSelected
                }
                else -> {}
            }
            (binding.recyclerviewNotificationFilters.adapter as FilterAdapter).notifyDataSetChanged()
            applyFilters()
        }
        binding.recyclerviewNotificationFilters.adapter = filterAdapter
    }

    private fun setupNotificationRecyclerView() {
        notificationAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice -> updateFavoriteStatus(notice) }
        )
        binding.recyclerviewNotificationList.adapter = notificationAdapter
        binding.recyclerviewNotificationList.layoutManager = LinearLayoutManager(context)
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

        // 1. UI를 즉시 업데이트
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

        // 2. Firestore 데이터베이스를 백그라운드에서 업데이트
        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }
        updateTask.addOnFailureListener {
            Toast.makeText(context, "즐겨찾기 상태 변경 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFavoritesStateAndApplyFilters() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            applyFilters()
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
            applyFilters()
        }.addOnFailureListener {
            applyFilters()
        }
    }

    private fun applyFilters() {
        val selectedCategories = filterList.filter { it.type == FilterType.CATEGORY && it.isSelected }.map { it.name }
        var filteredList = allNotifications.toList()

        if (args.categoryName == "전체" || args.categoryName == "인기글") {
            if (selectedCategories.isNotEmpty()) {
                filteredList = filteredList.filter { notice -> selectedCategories.contains(notice.category) }
            } else {
                filteredList = emptyList()
            }
        }
        notificationAdapter.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}