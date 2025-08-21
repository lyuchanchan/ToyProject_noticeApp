package com.example.toyproject_noticeapp.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList
import com.example.toyproject_noticeapp.adapter.FilterAdapter
import com.example.toyproject_noticeapp.adapter.FilterItem
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.FragmentNotificationMainBinding

class FragmentNotificationMain : Fragment() {

    private var _binding: FragmentNotificationMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationAdapter: AdapterNotificationList
    private val allNotifications = mutableListOf<DataNotificationItem>() // 필터링을 위한 전체 공지 원본 리스트
    private val filterList = mutableListOf<FilterItem>() // 필터 아이템 리스트

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 데이터가 비어있을 때만 임시 데이터를 로드하여 중복 로드를 방지
        if (allNotifications.isEmpty()){
            loadDummyData()
        }
        setupToolbar()
        setupFilterRecyclerView()
        setupNotificationRecyclerView()
        applyFilters() // 초기 필터 적용으로 전체 목록 표시
    }

    // 임시 데이터 생성
    private fun loadDummyData() {
        allNotifications.addAll(listOf(
            DataNotificationItem(1, "학사공지", "2025-08-20", "2학기 수강신청 최종 정정 안내", "수강신청 최종 정정 기간은...", false),
            DataNotificationItem(2, "장학공지", "2025-08-19", "국가장학금 2차 신청 안내", "2차 신청 기간은...", true),
            DataNotificationItem(3, "공지사항", "2025-08-18", "도서관 운영시간 변경 안내", "시험 기간 도서관 운영시간이...", false),
            DataNotificationItem(4, "취업공지", "2025-08-17", "2025년 하반기 IT기업 채용설명회", "자세한 내용은...", true),
            DataNotificationItem(5, "학사공지", "2025-08-16", "2025학년도 2학기 재입학 신청", "신청 기간 및 방법을 확인하세요.", false),
            DataNotificationItem(6, "AISW", "2025-08-15", "AISW 트랙 설명회 개최 안내", "트랙 설명회가 개최됩니다.", false)
        ))

        filterList.addAll(listOf(
            FilterItem("전체", isSelected = true),
            FilterItem("즐겨찾기", isFavoriteFilter = true),
            FilterItem("학사공지"),
            FilterItem("공지사항"),
            FilterItem("행사공지"),
            FilterItem("장학공지"),
            FilterItem("취업공지"),
            FilterItem("AISW"),
            FilterItem("SW중심대학")
        ))
    }

    private fun setupToolbar() {
        binding.toolbarNotificationMain.toolbar.title = "알림 내역"
        binding.toolbarNotificationMain.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarNotificationMain.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    // 필터 RecyclerView 설정
    private fun setupFilterRecyclerView() {
        val filterAdapter = FilterAdapter(filterList) { selectedFilter ->
            if (selectedFilter.name == "전체") {
                filterList.forEach { it.isSelected = false }
                selectedFilter.isSelected = true
            } else {
                filterList.find { it.name == "전체" }?.isSelected = false
                selectedFilter.isSelected = !selectedFilter.isSelected
            }

            if (filterList.none { it.isSelected }) {
                filterList.find { it.name == "전체" }?.isSelected = true
            }
            (binding.recyclerviewNotificationFilters.adapter as FilterAdapter).notifyDataSetChanged()
            applyFilters()
        }
        binding.recyclerviewNotificationFilters.adapter = filterAdapter
    }

    // 알림 목록 RecyclerView 설정
    private fun setupNotificationRecyclerView() {
        notificationAdapter = AdapterNotificationList(
            onItemClick = { notice ->
                Toast.makeText(context, notice.title, Toast.LENGTH_SHORT).show()
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

    // 선택된 필터를 기반으로 목록을 필터링하여 보여주는 함수
    private fun applyFilters() {
        val selectedFilters = filterList.filter { it.isSelected && it.name != "전체" }

        if (selectedFilters.isEmpty()) {
            notificationAdapter.submitList(allNotifications.toList())
            return
        }

        var filteredList = allNotifications.toList()

        val isFavoriteFilterOn = selectedFilters.any { it.isFavoriteFilter }
        val categoryFilters = selectedFilters.filter { !it.isFavoriteFilter }.map { it.name }

        filteredList = filteredList.filter { notice ->
            val favoriteMatch = if (isFavoriteFilterOn) notice.isFavorite else true
            val categoryMatch = if (categoryFilters.isNotEmpty()) categoryFilters.contains(notice.category) else true
            favoriteMatch && categoryMatch
        }

        notificationAdapter.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}