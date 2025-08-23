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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList
import com.example.toyproject_noticeapp.adapter.FilterAdapter
import com.example.toyproject_noticeapp.adapter.FilterItem
import com.example.toyproject_noticeapp.adapter.FilterType
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.FragmentNotificationMainBinding
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FragmentNotificationMain : Fragment() {

    private var _binding: FragmentNotificationMainBinding? = null
    private val binding get() = _binding!!

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

    // ##### 이 부분이 수정되었습니다! #####
    private fun loadFilterData() {
        filterList.clear()
        filterList.addAll(listOf(
            FilterItem("전체선택", FilterType.COMMAND),
            FilterItem("전체해제", FilterType.COMMAND),
            FilterItem("즐겨찾기", FilterType.FAVORITE),
            FilterItem("공지사항", FilterType.CATEGORY, isSelected = true),
            FilterItem("학사공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("행사공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("장학공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("취업공지", FilterType.CATEGORY, isSelected = true),
            FilterItem("AISW계열 공지사항", FilterType.CATEGORY, isSelected = true),
            FilterItem("SW중심대학 공지사항", FilterType.CATEGORY, isSelected = true)
        ))
    }

    private fun fetchNoticesFromFirebase() {
        val db = Firebase.firestore
        db.collection("notices")
            .orderBy("id", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                allNotifications.clear()
                for (document in result) {
                    val notice = document.toObject(DataNotificationItem::class.java)
                    allNotifications.add(notice)
                }
                applyFilters()
                Log.d("Firestore", "Successfully fetched ${result.size()} documents.")
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents.", exception)
                Toast.makeText(context, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupToolbar() {
        binding.toolbarNotificationMain.toolbar.title = "알림 내역"
        binding.toolbarNotificationMain.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarNotificationMain.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupFilterRecyclerView() {
        val filterAdapter = FilterAdapter(filterList) { selectedFilter ->
            when (selectedFilter.type) {
                FilterType.COMMAND -> {
                    val shouldSelectAll = selectedFilter.name == "전체선택"
                    filterList.filter { it.type == FilterType.CATEGORY }.forEach { it.isSelected = shouldSelectAll }
                }
                FilterType.FAVORITE, FilterType.CATEGORY -> {
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
            onFavoriteClick = { notice ->
                val index = allNotifications.indexOfFirst { it.id == notice.id }
                if (index != -1) {
                    val updatedItem = allNotifications[index].copy(isFavorite = !allNotifications[index].isFavorite)
                    allNotifications[index] = updatedItem
                    applyFilters()
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

    private fun applyFilters() {
        val isFavoriteFilterOn = filterList.find { it.type == FilterType.FAVORITE }?.isSelected ?: false
        val selectedCategories = filterList.filter { it.type == FilterType.CATEGORY && it.isSelected }.map { it.name }

        var filteredList = allNotifications.toList()

        if (isFavoriteFilterOn) {
            filteredList = filteredList.filter { it.isFavorite }
        }

        if (selectedCategories.isNotEmpty()) {
            filteredList = filteredList.filter { notice -> selectedCategories.contains(notice.category) }
        } else {
            if (!isFavoriteFilterOn) {
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