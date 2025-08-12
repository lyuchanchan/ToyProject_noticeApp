package com.example.toyproject_noticeapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toyproject_noticeapp.adapter.HomeNoticeAdapter
import com.example.toyproject_noticeapp.data.Notice
import com.example.toyproject_noticeapp.databinding.FragmentHomeMainBinding

class HomeMainFragment : Fragment() {

    private var _binding: FragmentHomeMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 툴바 설정 (필요 시)
        // binding.toolbarHomeMain. ...

        // 2. 바로가기 버튼 리스너 (필요 시)
        // 예: binding.gridlayoutHomeShortcuts.getChildAt(0).setOnClickListener { ... }

        // 3. '최근 알림' 헤더 클릭 리스너 설정
        binding.layoutHomeRecentHeader.setOnClickListener {
            // TODO: 알림 페이지로 넘어가는 로직 구현
            Toast.makeText(requireContext(), "'더보기' 클릭됨. 알림 페이지로 이동합니다.", Toast.LENGTH_SHORT).show()
        }

        // 4. RecyclerView 설정
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // 임시 데이터 생성 (나중에 실제 데이터로 교체)
        val noticeList = listOf(
            Notice("학사공지", "2024학년도 2학기 기말고사 일정 안내", "2024-12-15", true),
            Notice("시설공지", "겨울방학 도서관 운영시간 변경 안내", "2024-12-14", true),
            Notice("장학공지", "2025년 1학기 국가장학금 신청 안내", "2024-12-11", false)
        )

        val noticeAdapter = HomeNoticeAdapter(noticeList)
        binding.recyclerviewHomeNotifications.apply {
            // 리사이클러뷰 성능 최적화
            setHasFixedSize(true)
            // 레이아웃 매니저 설정
            layoutManager = LinearLayoutManager(requireContext())
            // 어댑터 연결
            adapter = noticeAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}