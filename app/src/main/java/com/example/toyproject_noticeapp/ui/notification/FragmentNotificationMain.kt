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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FragmentNotificationMain : Fragment() {

    private var _binding: FragmentNotificationMainBinding? = null
    private val binding get() = _binding!!
    private val args: FragmentNotificationMainArgs by navArgs()

    private lateinit var notificationAdapter: AdapterNotificationList
    private val allNotifications = mutableListOf<DataNotificationItem>()
    private val filterList = mutableListOf<FilterItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationMainBinding.inflate(inflater, container, false)
        return binding.root
    }

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
        filterList.addAll(listOf(
            FilterItem("전체선택", FilterType.COMMAND),
            FilterItem("전체해제", FilterType.COMMAND),
            FilterItem("공지사항", FilterType.CATEGORY, isSelected = true),
            FilterItem("학사공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("행사공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("장학공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("취업공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("AISW계열 공지사항", FilterType.CATEGORY, isSelected = true)
        ))
    }

    private fun fetchNoticesFromFirebase() {
        val db = Firebase.firestore
        val categoryToShow = args.categoryName

        val collectionPath = if (categoryToShow == "인기글") "popular_notices" else "notices"
        var query: Query = db.collection(collectionPath)

        if (categoryToShow == "인기글") {
            query = query.orderBy("category").orderBy("viewCount", Query.Direction.DESCENDING)
        } else if (categoryToShow != "전체") {
            query = query.whereEqualTo("category", categoryToShow)
                .orderBy("id", Query.Direction.DESCENDING)
        } else {
            query = query.orderBy("id", Query.Direction.DESCENDING)
        }

        query.get()
            .addOnSuccessListener { result ->
                allNotifications.clear()
                for (document in result) {
                    val notice = document.toObject(DataNotificationItem::class.java)
                    allNotifications.add(notice)
                }

                updateFavoritesState {
                    if (categoryToShow == "인기글" || categoryToShow == "전체") {
                        applyFilters()
                    } else {
                        notificationAdapter.submitList(allNotifications.toList())
                    }
                }
                Log.d("Firestore", "Successfully fetched ${result.size()} documents.")
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents.", exception)
                Toast.makeText(context, "데이터를 불러오는 데 실패했습니다. Firestore 규칙 및 색인을 확인하세요.", Toast.LENGTH_LONG).show()
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
        if (args.categoryName != "전체" && args.categoryName != "인기글") {
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
            }
            (binding.recyclerviewNotificationFilters.adapter as FilterAdapter).notifyDataSetChanged()
            applyFilters()
        }
        binding.recyclerviewNotificationFilters.adapter = filterAdapter
    }

    private fun setupNotificationRecyclerView() {
        notificationAdapter = AdapterNotificationList(
            onItemClick = { notice ->
                openInAppBrowser(notice.url)
            },
            // ##### 이 부분이 수정되었습니다! #####
            onFavoriteClick = { notice ->
                // 1. UI를 먼저 업데이트 (낙관적 업데이트)
                val index = allNotifications.indexOfFirst { it.id == notice.id && it.category == notice.category }
                if (index != -1) {
                    val newFavoriteState = !allNotifications[index].isFavorite
                    allNotifications[index].isFavorite = newFavoriteState

                    // 현재 화면에 보이는 목록을 즉시 새로고침하여 별 아이콘 변경
                    val currentList = notificationAdapter.currentList.toMutableList()
                    val displayIndex = currentList.indexOfFirst { it.id == notice.id && it.category == notice.category }
                    if (displayIndex != -1) {
                        notificationAdapter.notifyItemChanged(displayIndex)
                    }

                    // 2. 백그라운드에서 Firestore DB 업데이트
                    updateFavoriteStatusInFirestore(notice, newFavoriteState)
                }
            }
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
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "연결된 링크가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFavoriteStatusInFirestore(notice: DataNotificationItem, newFavoriteState: Boolean) {
        val db = Firebase.firestore
        val userId = "test" // TODO: 실제 로그인된 사용자 ID로 교체
        val userDocRef = db.collection("users").document(userId)
        val noticeDocId = "${notice.category}_${notice.id}"

        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }

        updateTask.addOnSuccessListener {
            // 성공 메시지 표시
            val message = if (newFavoriteState) "즐겨찾기에 추가했습니다." else "즐겨찾기에서 삭제했습니다."
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            // 실패 시, UI를 원래 상태로 되돌림
            Toast.makeText(context, "즐겨찾기 상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            val index = allNotifications.indexOfFirst { it.id == notice.id && it.category == notice.category }
            if (index != -1) {
                allNotifications[index].isFavorite = !newFavoriteState // 상태 원복
                val currentList = notificationAdapter.currentList.toMutableList()
                val displayIndex = currentList.indexOfFirst { it.id == notice.id && it.category == notice.category }
                if (displayIndex != -1) {
                    notificationAdapter.notifyItemChanged(displayIndex)
                }
            }
        }
    }

    private fun updateFavoritesState(onComplete: () -> Unit) {
        val db = Firebase.firestore
        val userId = "test" // TODO: 실제 로그인된 사용자 ID로 교체

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val favoriteIds = document.get("favorites") as? List<String> ?: emptyList()
                allNotifications.forEach { notice ->
                    val noticeDocId = "${notice.category}_${notice.id}"
                    notice.isFavorite = favoriteIds.contains(noticeDocId)
                }
            }
            onComplete()
        }.addOnFailureListener {
            // 즐겨찾기 상태를 못불러와도 앱이 죽지 않도록 onComplete() 호출
            onComplete()
        }
    }

    private fun applyFilters() {
        val selectedCategories = filterList.filter { it.type == FilterType.CATEGORY && it.isSelected }.map { it.name }

        var filteredList = allNotifications.toList()

        if (selectedCategories.isNotEmpty()) {
            filteredList = filteredList.filter { notice -> selectedCategories.contains(notice.category) }
        } else {
            filteredList = emptyList()
        }

        notificationAdapter.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}