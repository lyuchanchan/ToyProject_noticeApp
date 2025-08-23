package com.example.toyproject_noticeapp.ui.favorite

import android.net.Uri
import android.os.Bundle
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
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.databinding.FragmentFavoriteListBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FavoriteListFragment : Fragment() {

    private var _binding: FragmentFavoriteListBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoriteAdapter: AdapterNotificationList

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        fetchFavoriteNotices()
    }

    private fun setupToolbar() {
        binding.toolbarFavoriteList.toolbar.title = "즐겨찾기"
        binding.toolbarFavoriteList.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarFavoriteList.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        favoriteAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice ->
                removeFavorite(notice)
            }
        )
        binding.recyclerviewFavoriteList.adapter = favoriteAdapter
        binding.recyclerviewFavoriteList.layoutManager = LinearLayoutManager(context)
    }

    // ##### 이 함수가 수정되었습니다! #####
    private fun fetchFavoriteNotices() {
        val db = Firebase.firestore
        val userId = "test" // TODO: 실제 로그인된 사용자 ID로 교체해야 함
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val favoriteIds = document.get("favorites") as? List<String>

                    // 비어있는 문자열을 제거하여 유효한 ID 목록만 필터링합니다.
                    val validFavoriteIds = favoriteIds?.filter { it.isNotBlank() }

                    if (validFavoriteIds != null && validFavoriteIds.isNotEmpty()) {
                        db.collection("notices").whereIn(com.google.firebase.firestore.FieldPath.documentId(), validFavoriteIds).get()
                            .addOnSuccessListener { noticeDocuments ->
                                val favoriteList = mutableListOf<DataNotificationItem>()
                                for (noticeDoc in noticeDocuments) {
                                    val notice = noticeDoc.toObject(DataNotificationItem::class.java)
                                    notice.isFavorite = true
                                    favoriteList.add(notice)
                                }
                                val sortedList = favoriteList.sortedByDescending { notice ->
                                    validFavoriteIds.indexOf("${notice.category}_${notice.id}")
                                }
                                favoriteAdapter.submitList(sortedList)
                            }
                    } else {
                        favoriteAdapter.submitList(emptyList())
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "즐겨찾기 목록을 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeFavorite(notice: DataNotificationItem) {
        val db = Firebase.firestore
        val userId = "test" // TODO: 실제 로그인된 사용자 ID로 교체
        val userDocRef = db.collection("users").document(userId)
        val noticeDocId = "${notice.category}_${notice.id}"

        userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
            .addOnSuccessListener {
                Toast.makeText(context, "즐겨찾기에서 삭제했습니다.", Toast.LENGTH_SHORT).show()
                fetchFavoriteNotices()
            }
            .addOnFailureListener {
                Toast.makeText(context, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openInAppBrowser(url: String) {
        if (url.isNotEmpty()) {
            try {
                val customTabsIntent = CustomTabsIntent.Builder().build()
                customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
            } catch (e: Exception) {
                // ...
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}