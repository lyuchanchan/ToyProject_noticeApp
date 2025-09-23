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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchMainFragment : Fragment() {

    private var _binding: FragmentSearchMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchAdapter: AdapterNotificationList
    private val allNotifications = mutableListOf<DataNotificationItem>()
    private val db = Firebase.firestore
    private val auth = Firebase.auth

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
                updateFavoritesState() // 즐겨찾기 상태 업데이트
            }
            .addOnFailureListener {
                Toast.makeText(context, "공지 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // --- 👇 *** 여기가 핵심 수정 사항입니다! *** 👇 ---
    private fun updateFavoritesState() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            filter(binding.edittextSearchQuery.text.toString())
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
            filter(binding.edittextSearchQuery.text.toString())
        }.addOnFailureListener {
            filter(binding.edittextSearchQuery.text.toString())
        }
    }
    // --- 👆 *** 여기가 핵심 수정 사항입니다! *** 👆 ---

    private fun setupToolbar() {
        binding.toolbarSearchMain.toolbar.title = "게시글 검색"
        binding.toolbarSearchMain.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarSearchMain.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    // --- 👇 *** 여기가 핵심 수정 사항입니다! *** 👇 ---
    private fun setupRecyclerView() {
        searchAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice ->
                // Toast 메시지 대신 즐겨찾기 업데이트 함수 호출
                updateFavoriteStatus(notice)
            }
        )
        binding.recyclerviewSearchResults.adapter = searchAdapter
    }
    // --- 👆 *** 여기가 핵심 수정 사항입니다! *** 👆 ---

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

    // --- 👇 *** 여기가 핵심 수정 사항입니다! (새로운 함수 추가) *** 👇 ---
    private fun updateFavoriteStatus(notice: DataNotificationItem) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val userDocRef = db.collection("users").document(userId)
        val noticeDocId = "${notice.category}_${notice.id}"
        val newFavoriteState = !notice.isFavorite

        val message = if (newFavoriteState) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 삭제되었습니다."
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        // UI 즉시 업데이트
        val currentList = searchAdapter.currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (index != -1) {
            currentList[index].isFavorite = newFavoriteState
            searchAdapter.notifyItemChanged(index)
        }

        // 원본 데이터도 업데이트
        val indexInAll = allNotifications.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if(indexInAll != -1) {
            allNotifications[indexInAll].isFavorite = newFavoriteState
        }

        // Firestore 데이터 업데이트
        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }

        updateTask.addOnFailureListener {
            Toast.makeText(context, "즐겨찾기 상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            // 실패 시 UI 원상 복구
            if (index != -1) {
                currentList[index].isFavorite = !newFavoriteState
                searchAdapter.notifyItemChanged(index)
            }
            if (indexInAll != -1){
                allNotifications[indexInAll].isFavorite = !newFavoriteState
            }
        }
    }
    // --- 👆 *** 여기가 핵심 수정 사항입니다! *** 👆 ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}