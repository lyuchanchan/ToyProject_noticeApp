package com.example.toyproject_noticeapp.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList // 추가
import com.example.toyproject_noticeapp.data.DataNotificationItem     // 추가
import com.example.toyproject_noticeapp.databinding.FragmentNotificationMainBinding
import com.example.toyproject_noticeapp.ui.common.FragmentCommonToolbar

class FragmentNotificationMain : Fragment() {

    private var _binding: FragmentNotificationMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationAdapter: AdapterNotificationList

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        loadNotifications()
    }

    private fun setupToolbar() {
        // FragmentContainerView에서 Toolbar Fragment의 인스턴스를 찾아 제목을 설정
        val toolbarFragment = childFragmentManager.findFragmentById(R.id.fragment_container_toolbar) as? FragmentCommonToolbar
        toolbarFragment?.setToolbarTitle("알림 내역")
    }

    private fun setupRecyclerView() {
        notificationAdapter = AdapterNotificationList()
        binding.recyclerviewNotificationList.adapter = notificationAdapter
        // (선택) 아이템 사이에 구분선을 추가하고 싶다면 아래 코드 활성화
        // val decoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        // binding.recyclerviewNotificationList.addItemDecoration(decoration)
    }

    private fun loadNotifications() {
        // TODO: 실제로는 서버나 데이터베이스에서 공지사항 데이터를 가져와야 함
        // 여기서는 UI 확인을 위한 임시 데이터(Dummy Data)를 사용
        val dummyNotifications = listOf(
            DataNotificationItem("학사공지", "2024.03.20", "2024학년도 1학기 중간고사 일정 안내", "중간고사가 4월 15일부터 19일까지 실시됩니다. 시험 시간표를 확인하시기 바랍니다.", true),
            DataNotificationItem("급식공지", "2024.03.19", "학교 급식 메뉴 변경 안내", "3월 26일 급식 메뉴가 변경되었습니다. 자세한 내용은 공지사항을 확인해주세요.", false),
            DataNotificationItem("학사공지", "2024.03.18", "2024년 체육대회 개최 안내", "5월 10일 체육대회가 개최됩니다. 참가 신청서를 제출해주시기 바랍니다.", false),
            DataNotificationItem("도서관공지", "2024.03.17", "도서관 이용시간 변경 안내", "4월부터 도서관 이용시간이 변경됩니다. 평일 9시-18시, 토요일 9시-15시로 운영됩니다.", true),
            DataNotificationItem("시스템공지", "2024.03.16", "학교 홈페이지 시스템 점검 안내", "3월 22일 오후 6시부터 8시까지 시스템 점검으로 인해 홈페이지 이용이 제한됩니다.", false)
        )
        notificationAdapter.submitList(dummyNotifications)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}