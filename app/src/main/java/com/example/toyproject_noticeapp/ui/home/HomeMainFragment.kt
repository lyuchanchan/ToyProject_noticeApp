package com.example.toyproject_noticeapp.ui.home

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
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
import com.example.toyproject_noticeapp.databinding.ItemHomeNotificationBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeMainFragment : Fragment() {

    private var _binding: FragmentHomeMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoriteAdapter: AdapterNotificationList

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupShortcutRecyclerView()
        setupPopularPost()
        setupFavoriteRecyclerView()

        binding.layoutHomeRecentHeader.setOnClickListener {
            val bundle = bundleOf("categoryName" to "인기글")
            findNavController().navigate(R.id.action_home_to_notice_list, bundle)
        }

        binding.layoutHomeFavoriteHeader.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_favorites)
        }
    }

    private fun setupToolbar() {
        binding.toolbarHomeMain.toolbar.title = "홈"
        binding.toolbarHomeMain.toolbar.inflateMenu(R.menu.toolbar_home_menu)
        binding.toolbarHomeMain.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
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
        // 아이콘, 이름, URL/타입을 모두 포함하는 데이터 리스트
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
            Shortcut(R.drawable.home_icon_check, "AISW계열 공지사항", "BOARD")
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
        binding.recyclerviewHomeShortcuts.layoutManager = GridLayoutManager(context, 3)
    }

    private fun openInAppBrowser(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            Toast.makeText(context, "페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun setupPopularPost() {
        val prefs = requireActivity().getSharedPreferences("popular_post_prefs", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastShownDate = prefs.getString("date", null)
        val lastShownDocId = prefs.getString("docId", null)

        if (lastShownDate == today && lastShownDocId != null) {
            fetchSpecificPopularPost(lastShownDocId)
        } else {
            fetchRandomPopularPost()
        }
    }

    private fun fetchSpecificPopularPost(docId: String) {
        Firebase.firestore.collection("popular_notices").document(docId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val notice = document.toObject(DataNotificationItem::class.java)
                    if (notice != null) {
                        displayPopularPost(notice)
                    }
                } else {
                    fetchRandomPopularPost()
                }
            }
            .addOnFailureListener { fetchRandomPopularPost() }
    }

    private fun fetchRandomPopularPost() {
        Firebase.firestore.collection("popular_notices").get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val randomPostDocument = documents.shuffled().first()
                    val notice = randomPostDocument.toObject(DataNotificationItem::class.java)
                    if(notice != null) displayPopularPost(notice)

                    val prefs = requireActivity().getSharedPreferences("popular_post_prefs", Context.MODE_PRIVATE)
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    prefs.edit()
                        .putString("date", today)
                        .putString("docId", randomPostDocument.id)
                        .apply()
                }
            }
    }

    private fun displayPopularPost(notice: DataNotificationItem) {
        val popularPostBinding = binding.popularPostItem
        popularPostBinding.root.visibility = View.VISIBLE
        popularPostBinding.textviewItemCategory.text = notice.category
        popularPostBinding.textviewItemTitle.text = notice.title
        popularPostBinding.textviewItemDate.text = notice.date
        popularPostBinding.textviewItemNew.isVisible = false
    }

    private fun setupFavoriteRecyclerView() {
        favoriteAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice ->
                // TODO: 홈 화면에서도 즐겨찾기 상태 변경 로직 구현
                Toast.makeText(context, "즐겨찾기 상태 변경", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerviewHomeFavorites.adapter = favoriteAdapter
        binding.recyclerviewHomeFavorites.layoutManager = LinearLayoutManager(context)

        fetchHomeFavorites()
    }

    private fun fetchHomeFavorites() {
        val db = Firebase.firestore
        val userId = "test" // TODO: 실제 로그인된 사용자 ID로 교체

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val favoriteIds = document.get("favorites") as? List<String>

                    val validFavoriteIds = favoriteIds?.filter { it.isNotBlank() }

                    if (validFavoriteIds != null && validFavoriteIds.isNotEmpty()) {
                        val recentFavoriteIds = validFavoriteIds.reversed().take(3)

                        if (recentFavoriteIds.isNotEmpty()) {
                            db.collection("notices").whereIn(com.google.firebase.firestore.FieldPath.documentId(), recentFavoriteIds).get()
                                .addOnSuccessListener { noticeDocuments ->
                                    val favoritePreviewList = mutableListOf<DataNotificationItem>()
                                    for (noticeDoc in noticeDocuments) {
                                        val notice = noticeDoc.toObject(DataNotificationItem::class.java)
                                        notice.isFavorite = true
                                        favoritePreviewList.add(notice)
                                    }
                                    val sortedList = favoritePreviewList.sortedByDescending { notice ->
                                        recentFavoriteIds.indexOf("${notice.category}_${notice.id}")
                                    }
                                    favoriteAdapter.submitList(sortedList)
                                }
                        } else {
                            favoriteAdapter.submitList(emptyList())
                        }
                    } else {
                        favoriteAdapter.submitList(emptyList())
                    }
                }
            }
            .addOnFailureListener {
                favoriteAdapter.submitList(emptyList())
                Toast.makeText(context, "즐겨찾기 목록을 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}