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
    private var _binding: FragmentHomeMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoriteAdapter: AdapterNotificationList
    private lateinit var popularAdapter: AdapterNotificationList

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    // 랜덤 문구 리스트
    private val randomMessages = listOf(
        "오늘도 놓치지 말고 체크✔️",
        "캠퍼스 소식, 여기 다 있지",
        "공지 찾기? 이제 고생 끝🙌",
        "한신대 소식, 한신 나우에 다 모였다🙌",
        "공지 찾기 귀찮을 땐? 한신 나우!",
        "한신 나우 = 캠퍼스 정보 올인원 패키지🎁",
        "오늘도 소식 체크 완료",
        "공지부터 이벤트까지 풀세트 준비 완료⚡",
        "한눈에 보는 캠퍼스 라이프!",
        "오늘도 신선한 소식 배달왔습니다📦",
        "소식은 빠르게, 학교 생활은 여유롭게✨",
        "공지 확인은 쉽게, 스트레스는 노노✌️",
        "공지? 난 다 모아봤어😉",
        "놓치면 땅치고 후회할 소식들🔥",
        "캠퍼스 소식, 누구보다 빠르게 확인!",
        "여기만 보면 학사 인싸🧑‍🎓",
        "공지 놓쳤다고? 그건 전설일 뿐…",
        "모든 소식을 한눈에, 한신 나우!",
        "한신 나우와 학교생활을 더 똑똑하게",
        "하루를 바꾸는 작은 알림, 한신 나우!",
        "편리하게 모은 한신대 소식",
        "오늘의 공지, 지금 확인하세요",
        "공지 찾는 게 귀찮아? 여기 다 있어~",
        "학교 소식, 이제 헤매지 말고 직진👉",
        "놓치면 ‘나만 몰랐어?’ 소리 듣는다😂",
        "캠퍼스 인싸의 비밀: 공지 먼저 보기",
        "공지 덕후 모드 ON!",
        "교수님 말보다 빠른 공지 업데이트⚡",
        "시험 공지부터 동아리 소식까지 올인원📚",
        "중요한 건 공지 속에 다 있다😉",
        "캠퍼스 생활 치트키, 여기 맞습니다🎯",
        "공지? 그냥 여기 들르면 해결이지👌",
        "공지 놓치면 F각… 그건 막아야지✋",
        "또 나만 뒤늦게 알게 되는 건 이제 끝",
        "한신대 공식 스포일러📢",
        "공지 확인, 밥 먹듯이 하자🍚",
        "공지 싹 모아봤다🙌",
        "학교 소식 한 방 정리💡",
        "공지 맛집 오픈🍽️",
        "모든 공지, 원샷 원킬⚡",
        "공지=여기, 검색=끝",
        "류찬, 박소영, 서성민이 만든 앱😎"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 랜덤 문구를 설정
        binding.textviewRandomMessage.text = randomMessages.random()

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

        val message = if (newFavoriteState) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 삭제되었습니다."
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        val popularList = popularAdapter.currentList.toMutableList()
        val popularIndex = popularList.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (popularIndex != -1) {
            popularList[popularIndex].isFavorite = newFavoriteState
            popularAdapter.submitList(popularList)
            popularAdapter.notifyItemChanged(popularIndex)
        }

        fetchHomeFavorites()

        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }
        updateTask.addOnFailureListener {
            Toast.makeText(context, "즐겨찾기 상태 변경 실패", Toast.LENGTH_SHORT).show()
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