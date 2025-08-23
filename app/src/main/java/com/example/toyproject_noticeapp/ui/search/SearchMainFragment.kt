package com.example.toyproject_noticeapp.ui.search

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.FragmentSearchMainBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchMainFragment : Fragment() {

    private var _binding: FragmentSearchMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchAdapter: AdapterNotificationList
    private val allNotifications = mutableListOf<DataNotificationItem>()
    private val db = Firebase.firestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearchEditText()
        fetchAllNotices()
    }

    private fun fetchAllNotices() {
        db.collection("notices").get()
            .addOnSuccessListener { documents ->
                allNotifications.clear()
                val noticeList = documents.toObjects(DataNotificationItem::class.java)
                allNotifications.addAll(noticeList)
            }
            .addOnFailureListener {
                Toast.makeText(context, "공지 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupToolbar() {
        binding.toolbarSearchMain.toolbar.title = "게시글 검색"
        binding.toolbarSearchMain.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarSearchMain.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = {
                Toast.makeText(context, "이 화면에서는 즐겨찾기를 변경할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        )
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
            emptyList()
        } else {
            allNotifications.filter {
                it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }
        }
        searchAdapter.submitList(filteredList)
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}