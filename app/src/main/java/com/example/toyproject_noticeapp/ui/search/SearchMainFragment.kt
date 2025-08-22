package com.example.toyproject_noticeapp.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.SearchResultAdapter
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.FragmentSearchMainBinding

class SearchMainFragment : Fragment() {

    private var _binding: FragmentSearchMainBinding? = null
    private val binding get() = _binding!!

    private val searchAdapter = SearchResultAdapter()
    private val allNotifications = mutableListOf<DataNotificationItem>() // 전체 공지 원본

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadDummyData() // 임시로 전체 데이터 로드
        setupToolbar()
        setupRecyclerView()
        setupSearchEditText()
    }

    // ##### 이 부분이 수정되었습니다! #####
    // 실제 앱에서는 모든 공지사항 목록을 불러와야 함
    private fun loadDummyData() {
        allNotifications.addAll(listOf(
            // 각 항목의 끝에 url 값을 추가합니다.
            DataNotificationItem(1, "학사공지", "2025-08-20", "2학기 수강신청 최종 정정 안내", "수강신청 최종 정정 기간은...", false, "https://www.hs.ac.kr/kor/4953/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGa29yJTJGMjQlMkYxNTA2NTUlMkZhcnRjbFZpZXcuZG8lM0ZwYWdlJTNEMSUyNnNyY2hDb2x1bW4lM0QlMjZzcmNoV3JkJTNEJTI2YmJzQ2xTZXElM0QlMjZiYnNPcGVuV3JkU2VxJTNEJTI2cmdzQmduZGVTdHIlM0QlMjZyZ3NFbmRkZVN0ciUzRCUyNmlzVmlld01pbmUlM0RmYWxzZSUyNnBhc3N3b3JkJTNEJTI2"),
            DataNotificationItem(2, "장학공지", "2025-08-19", "국가장학금 2차 신청 안내", "2차 신청 기간은...", true, ""),
            DataNotificationItem(3, "공지사항", "2025-08-18", "도서관 운영시간 변경 안내", "시험 기간 도서관 운영시간이...", false, "")
        ))
    }

    private fun setupToolbar() {
        binding.toolbarSearchMain.toolbar.title = "게시글 검색"
        binding.toolbarSearchMain.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarSearchMain.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerviewSearchResults.adapter = searchAdapter
    }

    private fun setupSearchEditText() {
        binding.edittextSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            emptyList() // 검색어가 없으면 빈 목록
        } else {
            allNotifications.filter {
                it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }
        }
        searchAdapter.updateQuery(query) // 하이라이트를 위해 어댑터에 현재 검색어 전달
        searchAdapter.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}