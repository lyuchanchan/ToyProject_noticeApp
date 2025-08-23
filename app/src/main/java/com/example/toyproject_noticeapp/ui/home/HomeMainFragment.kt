package com.example.toyproject_noticeapp.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList
import com.example.toyproject_noticeapp.adapter.HomeShortcutAdapter
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.data.Shortcut
import com.example.toyproject_noticeapp.databinding.FragmentHomeMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeMainFragment : Fragment() {
    // ... (변수 선언 및 onCreateView는 동일)
    private var _binding: FragmentHomeMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoriteAdapter: AdapterNotificationList
    private lateinit var popularAdapter: AdapterNotificationList

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ⬇️ onViewCreated 이하 모든 함수를 아래 코드로 교체 ⬇️
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupRecyclerViews()
        fetchData()
    }

    private fun setupClickListeners() {
        binding.imageviewSearch.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_search)
        }
        binding.imageviewSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
        binding.textviewHomePopularMore.setOnClickListener {
            val bundle = bundleOf("categoryName" to "인기글")
            findNavController().navigate(R.id.action_home_to_notice_list, bundle)
        }
        binding.textviewHomeFavoriteMore.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_favorites)
        }
    }

    private fun setupRecyclerViews() {
        val shortcutList = listOf(
            Shortcut(R.drawable.home_icon_1, "홈페이지", "https://www.hs.ac.kr/kor/index.do"),
            Shortcut(R.drawable.home_icon_calendar, "학사일정", "https://www.hs.ac.kr/kor/80/subview.do"),
            Shortcut(R.drawable.home_icon_haksa, "학사공지", "BOARD"),
            Shortcut(R.drawable.home_icon_chuiup, "공지사항", "BOARD"),
            Shortcut(R.drawable.home_icon_festival, "행사공지", "BOARD"),
            Shortcut(R.drawable.home_icon_scholarship, "장학공지", "BOARD"),
            Shortcut(R.drawable.home_icon_check, "취업공지", "BOARD"),
            Shortcut(R.drawable.home_icon_food, "식단표", "https://www.hs.ac.kr/kor/70/subview.do"),
            Shortcut(R.drawable.home_icon_bus, "셔틀버스", "https://www.hs.ac.kr/kor/69/subview.do"),
            Shortcut(R.drawable.home_icon_check, "AISW계열", "BOARD")
        )
        val shortcutAdapter = HomeShortcutAdapter(shortcutList) { shortcut ->
            if (shortcut.url == "BOARD") {
                val bundle = bundleOf("categoryName" to shortcut.name)
                findNavController().navigate(R.id.action_home_to_notice_list, bundle)
            } else {
                openInAppBrowser(shortcut.url)
            }
        }
        binding.recyclerviewHomeShortcuts.adapter = shortcutAdapter
        binding.recyclerviewHomeShortcuts.layoutManager = GridLayoutManager(context, 5)

        popularAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice -> updateFavoriteStatus(notice) }
        )
        binding.recyclerviewHomePopular.adapter = popularAdapter
        binding.recyclerviewHomePopular.layoutManager = LinearLayoutManager(context)

        favoriteAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice -> updateFavoriteStatus(notice) }
        )
        binding.recyclerviewHomeFavorite.adapter = favoriteAdapter
        binding.recyclerviewHomeFavorite.layoutManager = LinearLayoutManager(context)
    }

    private fun fetchData() {
        fetchPopularPosts()
        fetchHomeFavorites()
    }

    private fun fetchPopularPosts() {
        db.collection("popular_notices").limit(1).get()
            .addOnSuccessListener { documents ->
                val popularList = documents.mapNotNull { doc ->
                    DataNotificationItem(
                        id = (doc.getLong("id") ?: 0L).toInt(),
                        category = doc.getString("category") ?: "",
                        date = doc.getString("date") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        url = doc.getString("url") ?: "",
                        viewCount = (doc.getLong("viewCount") ?: 0L).toInt()
                    )
                }
                updateFavoritesStateForList(popularList) { updatedList ->
                    popularAdapter.submitList(updatedList)
                }
            }
    }

    private fun fetchHomeFavorites() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val favoriteIds = document.get("favorites") as? List<String>
                    val validFavoriteIds = favoriteIds?.filter { it.isNotBlank() }
                    if (!validFavoriteIds.isNullOrEmpty()) {
                        binding.recyclerviewHomeFavorite.visibility = View.VISIBLE
                        binding.textviewHomeFavoriteEmpty.visibility = View.GONE
                        val recentFavoriteIds = validFavoriteIds.reversed().take(1)
                        if (recentFavoriteIds.isNotEmpty()) {
                            db.collection("notices").whereIn(com.google.firebase.firestore.FieldPath.documentId(), recentFavoriteIds).get()
                                .addOnSuccessListener { noticeDocuments ->
                                    val favoritePreviewList = noticeDocuments.mapNotNull { doc ->
                                        val notice = DataNotificationItem(
                                            id = (doc.getLong("id") ?: 0L).toInt(),
                                            category = doc.getString("category") ?: "",
                                            date = doc.getString("date") ?: "",
                                            title = doc.getString("title") ?: "",
                                            description = doc.getString("description") ?: "",
                                            url = doc.getString("url") ?: "",
                                            viewCount = (doc.getLong("viewCount") ?: 0L).toInt()
                                        )
                                        notice.isFavorite = true
                                        notice
                                    }
                                    favoriteAdapter.submitList(favoritePreviewList)
                                }
                        } else {
                            showEmptyFavorites()
                        }
                    } else {
                        showEmptyFavorites()
                    }
                } else {
                    showEmptyFavorites()
                }
            }
    }

    private fun showEmptyFavorites() {
        binding.recyclerviewHomeFavorite.visibility = View.GONE
        binding.textviewHomeFavoriteEmpty.visibility = View.VISIBLE
        favoriteAdapter.submitList(emptyList())
    }

    private fun updateFavoriteStatus(notice: DataNotificationItem) {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)
        val noticeDocId = "${notice.category}_${notice.id}"
        val newFavoriteState = !notice.isFavorite

        // 1. UI를 즉시 업데이트
        val message = if (newFavoriteState) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 삭제되었습니다."
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        // 인기글 목록에서 아이템 상태 변경
        val popularList = popularAdapter.currentList.toMutableList()
        val popularIndex = popularList.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (popularIndex != -1) {
            popularList[popularIndex].isFavorite = newFavoriteState
            popularAdapter.submitList(popularList)
            popularAdapter.notifyItemChanged(popularIndex)
        }

        // 즐겨찾기 미리보기 목록 업데이트
        fetchHomeFavorites()

        // 2. Firestore 데이터베이스를 백그라운드에서 업데이트
        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }
        updateTask.addOnFailureListener {
            Toast.makeText(context, "즐겨찾기 상태 변경 실패", Toast.LENGTH_SHORT).show()
            // 실패 시 UI 롤백 (선택 사항)
        }
    }

    private fun updateFavoritesStateForList(list: List<DataNotificationItem>, onComplete: (List<DataNotificationItem>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onComplete(list)
            return
        }
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val favoriteIds = document.get("favorites") as? List<String> ?: emptyList()
                list.forEach { notice ->
                    val noticeDocId = "${notice.category}_${notice.id}"
                    notice.isFavorite = favoriteIds.contains(noticeDocId)
                }
            }
            onComplete(list)
        }.addOnFailureListener { onComplete(list) }
    }

    private fun openInAppBrowser(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            Toast.makeText(context, "페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}