package com.example.toyproject_noticeapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.HomeNoticeAdapter
import com.example.toyproject_noticeapp.adapter.HomeShortcutAdapter
import com.example.toyproject_noticeapp.data.Notice
import com.example.toyproject_noticeapp.data.Shortcut
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

        setupToolbar()
        setupShortcutRecyclerView()
        setupRecentNoticeRecyclerView()

        // '최근 알림' 헤더 클릭 리스너
        binding.layoutHomeRecentHeader.setOnClickListener {
            // 기존 Toast 메시지 대신 아래 코드로 변경
            findNavController().navigate(R.id.action_home_to_notification)
        }
    }


    private fun setupToolbar() {
        binding.toolbarHomeMain.toolbar.title = "홈"
        binding.toolbarHomeMain.toolbar.inflateMenu(R.menu.toolbar_home_menu)
        binding.toolbarHomeMain.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // ⬇️ 검색 아이콘 클릭 케이스 추가
                R.id.action_search -> {
                    findNavController().navigate(R.id.action_home_to_search)
                    true
                }
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_home_to_settings)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupShortcutRecyclerView() {
        val shortcutList = listOf(
            Shortcut("홈페이지"), Shortcut("학사일정"), Shortcut("학사공지"),
            Shortcut("공지사항"), Shortcut("행사공지"), Shortcut("장학일정"),
            Shortcut("취업공지"), Shortcut("식단표"), Shortcut("셔틀버스"),
            Shortcut("AISW공지"), Shortcut("SW중심대학")
        )
        val shortcutAdapter = HomeShortcutAdapter(shortcutList) { shortcut ->
            Toast.makeText(requireContext(), "${shortcut.name} 클릭됨", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerviewHomeShortcuts.adapter = shortcutAdapter
        binding.recyclerviewHomeShortcuts.layoutManager = GridLayoutManager(context, 3)
    }

    // ### 바로 이 함수입니다! ###
    // '최근 알림' 목록에 임시 데이터를 넣고 RecyclerView에 연결합니다.
    private fun setupRecentNoticeRecyclerView() {
        // 임시 데이터 생성
        val noticeList = listOf(
            Notice("학사공지", "2024학년도 2학기 기말고사 일정 안내", "2024-12-15", true),
            Notice("시설공지", "겨울방학 도서관 운영시간 변경 안내", "2024-12-14", true),
            Notice("장학공지", "2025년 1학기 국가장학금 신청 안내", "2024-12-11", false)
        )

        val noticeAdapter = HomeNoticeAdapter(noticeList)
        binding.recyclerviewHomeNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = noticeAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}