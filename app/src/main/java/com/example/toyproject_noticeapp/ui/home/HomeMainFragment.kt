
package com.example.toyproject_noticeapp.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.AdapterNotificationList
import com.example.toyproject_noticeapp.adapter.HomeShortcutAdapter
import com.example.toyproject_noticeapp.data.DataNotificationItem
import com.example.toyproject_noticeapp.data.Shortcut
import com.example.toyproject_noticeapp.databinding.FragmentHomeMainBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.abs

fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

class HomeMainFragment : Fragment() {
    private var _binding: FragmentHomeMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoriteAdapter: AdapterNotificationList
    private lateinit var popularAdapter: AdapterNotificationList
    private lateinit var shortcutAdapter: HomeShortcutAdapter
    private lateinit var hiddenShortcutAdapter: HomeShortcutAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private var isEditMode = false

    private var visibleItemTouchHelper: ItemTouchHelper? = null
    private var hiddenItemTouchHelper: ItemTouchHelper? = null
    private lateinit var commonItemTouchCallback: ItemTouchHelper.SimpleCallback
    private var snapHelper: PagerSnapHelper? = null

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private lateinit var autoScrollRunnable: Runnable
    private val autoScrollDelay = 3000L
    private var currentPopularPosition = 0

    private var offsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null

    private val masterShortcutList by lazy {
        listOf(
            Shortcut(R.drawable.home_icon_check, "공지사항", "BOARD"),
            Shortcut(R.drawable.home_icon_haksa, "학사공지", "BOARD"),
            Shortcut(R.drawable.home_icon_festival2, "행사공지", "BOARD"),
            Shortcut(R.drawable.home_icon_scholarship, "장학공지", "BOARD"),
            Shortcut(R.drawable.home_icon_job, "취업공지", "BOARD"),
            Shortcut(R.drawable.home_icon_chuiup, "홈페이지", "https://www.hs.ac.kr/kor/index.do"),
            Shortcut(R.drawable.home_icon_calendar2, "학사일정", "https://www.hs.ac.kr/kor/4837/subview.do"),
            Shortcut(R.drawable.home_icon_food, "식단표", "https://www.hs.ac.kr/kor/8398/subview.do"),
            Shortcut(R.drawable.home_icon_library2, "도서관", "https://hslib.hs.ac.kr/main_main.mir")
        )
    }

    private var visibleShortcutsData: MutableList<Shortcut> = mutableListOf()
    private var hiddenShortcutsData: MutableList<Shortcut> = mutableListOf()

    private val randomMessages = listOf(
        "오늘도 놓치지 말고 체크✔️",
        "캠퍼스 소식, 여기 다 있지",
        "공지 찾기? 이제 고생 끝🙌",
        "한신대 소식, 다 모였다🙌",
        "공지는 한신 나우!",
        "한신 나우 = 올인원 패키지",
        "오늘도 소식 체크 완료",
        "공지부터 이벤트까지⚡",
        "한눈에 보는 캠퍼스 라이프!",
        "오늘도 신선한 소식 배달왔습니다📦",
        "소식은 빠르게, 학교 생활은 여유롭게✨",
        "공지 확인은 쉽게, 스트레스는 노노",
        "공지? 난 다 모아봤어😉",
        "놓치면 땅치고 후회할 소식들🔥",
        "캠퍼스 소식, 빠르게 확인!",
        "여기만 보면 학사 인싸🧑‍🎓",
        "공지 놓쳤다고? 그건 전설일 뿐…",
        "모든 소식을 한눈에, 한신 나우!",
        "학교생활을 더 똑똑하게",
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
        "캠퍼스 치트키, 여기 맞습니다🎯",
        "공지? 나우면 해결이지👌",
        "공지 놓치면 F각… 그건 막아야지✋",
        "또 나만 뒤늦게 알게 되는 건 이제 끝",
        "한신대 공식 스포일러📢",
        "공지 확인, 밥 먹듯이 하자🍚",
        "공지 싹 모아봤다🙌",
        "학교 소식 한 방 정리💡",
        "공지 맛집 오픈🍽️",
        "모든 공지, 원샷 원킬⚡",
        "공지 = 한신 나우",
        "입벌려, 공지사항 들어간다",
        "떠먹여주는 학교소식",
        "식단표도 한신 나우!",
        "개쩌는 3인방이 만든 앱😎"
    )

    companion object {
        private const val PREFS_NAME = "HomeShortcutPrefs"
        private const val KEY_VISIBLE_SHORTCUTS = "visible_shortcuts"
        private const val KEY_HIDDEN_SHORTCUTS = "hidden_shortcuts"
        private const val MIN_TARGET_HEIGHT_DP = 60
        private const val SCROLL_SPEED_MILLISECONDS_PER_INCH = 100f
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textviewRandomMessage.text = randomMessages.random()
        loadShortcutPreferences()
        setupClickListeners()
        setupRecyclerViews()
        setupItemTouchHelpers()
        initializeAutoScroller()
        fetchData()
        setupAdvancedScrollAnimation()
    }

    override fun onResume() {
        super.onResume()
        startAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    private fun loadShortcutPreferences() {
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val visibleNamesString = prefs.getString(KEY_VISIBLE_SHORTCUTS, null)
        val hiddenNamesString = prefs.getString(KEY_HIDDEN_SHORTCUTS, null)

        if (visibleNamesString != null && hiddenNamesString != null) {
            val visibleNames = visibleNamesString.split(',').filter { it.isNotEmpty() }
            val hiddenNames = hiddenNamesString.split(',').filter { it.isNotEmpty() }
            visibleShortcutsData = visibleNames.mapNotNull { name -> masterShortcutList.find { it.name == name } }.toMutableList()
            hiddenShortcutsData = hiddenNames.mapNotNull { name -> masterShortcutList.find { it.name == name } }.toMutableList()

            val accountedForNames = (visibleShortcutsData.map { it.name } + hiddenShortcutsData.map { it.name }).toSet()
            masterShortcutList.forEach { shortcut ->
                if (!accountedForNames.contains(shortcut.name)) {
                    if (!hiddenShortcutsData.any { it.name == shortcut.name } && !visibleShortcutsData.any {it.name == shortcut.name }) {
                        hiddenShortcutsData.add(shortcut)
                    }
                }
            }
        } else {
            visibleShortcutsData = masterShortcutList.toMutableList()
            hiddenShortcutsData = mutableListOf()
        }
    }

    private fun saveShortcutPreferences() {
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            val visibleNames = shortcutAdapter.items.joinToString(",") { it.name }
            val hiddenNames = hiddenShortcutAdapter.items.joinToString(",") { it.name }
            putString(KEY_VISIBLE_SHORTCUTS, visibleNames)
            putString(KEY_HIDDEN_SHORTCUTS, hiddenNames)
        }
    }

    private fun setupClickListeners() {
        binding.imageviewSearch.setOnClickListener { findNavController().navigate(R.id.action_home_to_search) }
        binding.imageviewNotifications.setOnClickListener { findNavController().navigate(R.id.action_home_to_notification_history) }
        binding.imageviewSettings.setOnClickListener { findNavController().navigate(R.id.action_home_to_settings) }
        binding.textviewHomeFavoriteMore.setOnClickListener { findNavController().navigate(R.id.action_home_to_favorites) }
        binding.textviewHomeShortcutEdit.setOnClickListener { toggleEditMode() }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        binding.textviewHomeShortcutEdit.text = if (isEditMode) "완료" else "편집"
        binding.dividerHiddenShortcuts.isVisible = isEditMode
        binding.textviewHiddenShortcutsTitle.isVisible = isEditMode
        binding.recyclerviewHiddenShortcuts.isVisible = isEditMode

        if (isEditMode) {
            val minHeightPx = MIN_TARGET_HEIGHT_DP.dpToPx(requireContext())
            binding.recyclerviewHomeShortcuts.minimumHeight = minHeightPx
            binding.recyclerviewHiddenShortcuts.minimumHeight = minHeightPx
            binding.recyclerviewHomeShortcuts.isNestedScrollingEnabled = false
            visibleItemTouchHelper?.attachToRecyclerView(binding.recyclerviewHomeShortcuts)
            hiddenItemTouchHelper?.attachToRecyclerView(binding.recyclerviewHiddenShortcuts)
        } else {
            binding.recyclerviewHomeShortcuts.minimumHeight = 0
            binding.recyclerviewHiddenShortcuts.minimumHeight = 0
            binding.recyclerviewHomeShortcuts.isNestedScrollingEnabled = true
            visibleItemTouchHelper?.attachToRecyclerView(null)
            hiddenItemTouchHelper?.attachToRecyclerView(null)
            saveShortcutPreferences()
            shortcutAdapter.notifyItemRangeChanged(0, shortcutAdapter.itemCount)
            hiddenShortcutAdapter.notifyItemRangeChanged(0, hiddenShortcutAdapter.itemCount)
        }
    }

    private fun setupRecyclerViews() {
        shortcutAdapter = HomeShortcutAdapter(visibleShortcutsData) { shortcut ->
            if (!isEditMode) {
                if (shortcut.url == "BOARD") {
                    val bundle = bundleOf("categoryName" to shortcut.name)
                    findNavController().navigate(R.id.action_home_to_notice_list, bundle)
                } else {
                    openInAppBrowser(shortcut.url)
                }
            }
        }
        binding.recyclerviewHomeShortcuts.adapter = shortcutAdapter
        binding.recyclerviewHomeShortcuts.layoutManager = GridLayoutManager(requireContext(), 3)

        hiddenShortcutAdapter = HomeShortcutAdapter(hiddenShortcutsData) {}
        binding.recyclerviewHiddenShortcuts.adapter = hiddenShortcutAdapter
        binding.recyclerviewHiddenShortcuts.layoutManager = GridLayoutManager(requireContext(), 3)

        popularAdapter = AdapterNotificationList(onItemClick = { notice -> openInAppBrowser(notice.url) }, onFavoriteClick = { notice -> updateFavoriteStatus(notice) })
        binding.recyclerviewHomePopular.adapter = popularAdapter
        binding.recyclerviewHomePopular.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        
        snapHelper = PagerSnapHelper()
        snapHelper?.attachToRecyclerView(binding.recyclerviewHomePopular)

        binding.tabLayoutPopularIndicator.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.recyclerviewHomePopular.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                    snapHelper?.let { pagerSnapHelper ->
                        val snapView = pagerSnapHelper.findSnapView(layoutManager)
                        snapView?.let {
                            val position = layoutManager?.getPosition(it)
                            if (position != null && position != RecyclerView.NO_POSITION) {
                                binding.tabLayoutPopularIndicator.getTabAt(position)?.select()
                                currentPopularPosition = position
                                startAutoScroll()
                            }
                        }
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    stopAutoScroll()
                }
            }
        })

        favoriteAdapter = AdapterNotificationList(onItemClick = { notice -> openInAppBrowser(notice.url) }, onFavoriteClick = { notice -> updateFavoriteStatus(notice) })
        binding.recyclerviewHomeFavorite.adapter = favoriteAdapter
        binding.recyclerviewHomeFavorite.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupItemTouchHelpers() {
        commonItemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            private var dragInProgressViewHolder: RecyclerView.ViewHolder? = null
            private var dragInProgressSourceRecyclerView: RecyclerView? = null
            private var isOverTargetForDrop: Boolean = false
            private var draggedItemData: Shortcut? = null
            private var originalDragPosition: Int = RecyclerView.NO_POSITION

            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                val adapter = recyclerView.adapter as? HomeShortcutAdapter ?: return false
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
                    adapter.moveItem(fromPosition, toPosition)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = isEditMode

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                when (actionState) {
                    ItemTouchHelper.ACTION_STATE_DRAG -> {
                        if (viewHolder != null && isEditMode) {
                            viewHolder.itemView.alpha = 0.7f
                            viewHolder.itemView.elevation = 16f
                            viewHolder.itemView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()

                            dragInProgressViewHolder = viewHolder
                            dragInProgressSourceRecyclerView = viewHolder.itemView.parent as? RecyclerView
                            binding.nestedScrollView.requestDisallowInterceptTouchEvent(true)
                            isOverTargetForDrop = false
                            originalDragPosition = viewHolder.adapterPosition

                            when (dragInProgressSourceRecyclerView?.id) {
                                binding.recyclerviewHomeShortcuts.id -> {
                                    binding.recyclerviewHomeShortcuts.elevation = 20f
                                    binding.recyclerviewHiddenShortcuts.elevation = 10f
                                }
                                binding.recyclerviewHiddenShortcuts.id -> {
                                    binding.recyclerviewHiddenShortcuts.elevation = 20f
                                    binding.recyclerviewHomeShortcuts.elevation = 10f
                                }
                            }

                            if (originalDragPosition != RecyclerView.NO_POSITION && dragInProgressSourceRecyclerView != null) {
                                val sourceAdapterCurrent = when (dragInProgressSourceRecyclerView?.id) {
                                    binding.recyclerviewHomeShortcuts.id -> shortcutAdapter
                                    binding.recyclerviewHiddenShortcuts.id -> hiddenShortcutAdapter
                                    else -> null
                                }
                                draggedItemData = sourceAdapterCurrent?.items?.getOrNull(originalDragPosition)
                            } else {
                                draggedItemData = null
                            }
                            if (draggedItemData == null && originalDragPosition != RecyclerView.NO_POSITION) {
                                Toast.makeText(requireContext(), "드래그 시작 오류: 아이템 데이터 못찾음", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    ItemTouchHelper.ACTION_STATE_IDLE -> {
                        binding.nestedScrollView.requestDisallowInterceptTouchEvent(false)
                        dragInProgressViewHolder?.itemView?.alpha = 1.0f

                        if (isOverTargetForDrop && draggedItemData != null && dragInProgressSourceRecyclerView != null && originalDragPosition != RecyclerView.NO_POSITION) {
                            val (fromAdapter, targetAdapter) = when (dragInProgressSourceRecyclerView?.id) {
                                binding.recyclerviewHomeShortcuts.id -> shortcutAdapter to hiddenShortcutAdapter
                                binding.recyclerviewHiddenShortcuts.id -> hiddenShortcutAdapter to shortcutAdapter
                                else -> null to null
                            }

                            if (fromAdapter != null && targetAdapter != null) {
                                val currentPositionInDrag = dragInProgressViewHolder?.adapterPosition ?: originalDragPosition
                                if (currentPositionInDrag in 0 until fromAdapter.itemCount) {
                                    val itemAtOriginalPos = fromAdapter.items.getOrNull(currentPositionInDrag)

                                    if (itemAtOriginalPos == draggedItemData) {
                                        fromAdapter.removeItem(currentPositionInDrag)
                                        targetAdapter.addItem(draggedItemData!!)

                                        fromAdapter.notifyItemRemoved(currentPositionInDrag)
                                        targetAdapter.notifyItemInserted(targetAdapter.itemCount - 1)

                                        Toast.makeText(requireContext(), "'${draggedItemData!!.name}' 이동 완료", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                        binding.recyclerviewHomeShortcuts.setBackgroundColor(Color.TRANSPARENT)
                        binding.recyclerviewHiddenShortcuts.setBackgroundColor(Color.TRANSPARENT)
                        dragInProgressViewHolder = null
                        dragInProgressSourceRecyclerView = null
                        draggedItemData = null
                        originalDragPosition = RecyclerView.NO_POSITION
                        isOverTargetForDrop = false
                    }
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                    val itemView = viewHolder.itemView
                    val rvScreenLocation = IntArray(2)
                    recyclerView.getLocationOnScreen(rvScreenLocation)

                    val draggedItemScreenRect = Rect(
                        (rvScreenLocation[0] + itemView.left + dX).toInt(),
                        (rvScreenLocation[1] + itemView.top + dY).toInt(),
                        (rvScreenLocation[0] + itemView.left + itemView.width + dX).toInt(),
                        (rvScreenLocation[1] + itemView.top + itemView.height + dY).toInt()
                    )

                    val otherRecyclerView = if (recyclerView.id == binding.recyclerviewHomeShortcuts.id) {
                        binding.recyclerviewHiddenShortcuts
                    } else {
                        binding.recyclerviewHomeShortcuts
                    }
                    val otherRvRect = Rect()
                    otherRecyclerView.getGlobalVisibleRect(otherRvRect)

                    if (otherRecyclerView.isVisible && Rect.intersects(draggedItemScreenRect, otherRvRect)) {
                        otherRecyclerView.setBackgroundColor("#E0E0E0".toColorInt())
                        isOverTargetForDrop = true
                    } else {
                        otherRecyclerView.setBackgroundColor(Color.TRANSPARENT)
                        isOverTargetForDrop = false
                    }
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                viewHolder.itemView.elevation = 0f
                viewHolder.itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()

                binding.recyclerviewHomeShortcuts.setBackgroundColor(Color.TRANSPARENT)
                binding.recyclerviewHiddenShortcuts.setBackgroundColor(Color.TRANSPARENT)

                binding.recyclerviewHomeShortcuts.elevation = 0f
                binding.recyclerviewHiddenShortcuts.elevation = 0f
            }
        }
        visibleItemTouchHelper = ItemTouchHelper(commonItemTouchCallback)
        hiddenItemTouchHelper = ItemTouchHelper(commonItemTouchCallback)
    }

    private fun fetchData() {
        fetchPopularPosts()
        fetchHomeFavorites()
    }

    private fun fetchPopularPosts() {
        db.collection("popular_notices").limit(5).get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
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
                        binding.tabLayoutPopularIndicator.removeAllTabs()
                        if (updatedList.isNotEmpty()) {
                            binding.tabLayoutPopularIndicator.isVisible = true
                            updatedList.forEach { _ ->
                                binding.tabLayoutPopularIndicator.addTab(binding.tabLayoutPopularIndicator.newTab())
                            }
                            if (binding.tabLayoutPopularIndicator.selectedTabPosition == -1 || binding.tabLayoutPopularIndicator.selectedTabPosition >= updatedList.size) {
                                 binding.tabLayoutPopularIndicator.getTabAt(0)?.select()
                            }
                            currentPopularPosition = 0
                            startAutoScroll()
                        } else {
                            binding.tabLayoutPopularIndicator.isVisible = false
                            stopAutoScroll()
                        }
                    }
                } else {
                    popularAdapter.submitList(emptyList())
                    binding.tabLayoutPopularIndicator.isVisible = false
                    binding.tabLayoutPopularIndicator.removeAllTabs()
                    stopAutoScroll()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "추천 글 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                popularAdapter.submitList(emptyList())
                binding.tabLayoutPopularIndicator.isVisible = false
                binding.tabLayoutPopularIndicator.removeAllTabs()
                stopAutoScroll()
            }
    }

    private fun fetchHomeFavorites() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showEmptyFavorites()
            return
        }
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val favoriteIds = document.get("favorites") as? List<String>
                    if (!favoriteIds.isNullOrEmpty()) {
                        binding.recyclerviewHomeFavorite.isVisible = true
                        binding.textviewHomeFavoriteEmpty.isVisible = false
                        val recentFavoriteIds = favoriteIds.reversed().take(1)
                        if (recentFavoriteIds.isNotEmpty()) {
                            db.collection("notices").whereIn(com.google.firebase.firestore.FieldPath.documentId(), recentFavoriteIds).get()
                                .addOnSuccessListener { noticeDocuments ->
                                    if (noticeDocuments != null && !noticeDocuments.isEmpty) {
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
                                    } else {
                                        showEmptyFavorites()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "[즐겨찾기] 글 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                    showEmptyFavorites()
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
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "[즐겨찾기] 사용자 정보 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                showEmptyFavorites()
            }
    }

    private fun showEmptyFavorites() {
        binding.recyclerviewHomeFavorite.isVisible = false
        binding.textviewHomeFavoriteEmpty.isVisible = true
        favoriteAdapter.submitList(emptyList())
    }

    private fun updateFavoriteStatus(notice: DataNotificationItem) {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)
        val noticeDocId = "${notice.category}_${notice.id}"
        val newFavoriteState = !notice.isFavorite

        val message = if (newFavoriteState) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 삭제되었습니다."
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        val popularList = popularAdapter.currentList.toMutableList()
        val popularIndex = popularList.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (popularIndex != -1) {
            popularList[popularIndex].isFavorite = newFavoriteState
            popularAdapter.submitList(popularList.toList())
        }

        fetchHomeFavorites()

        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }
        updateTask.addOnFailureListener {
            Toast.makeText(requireContext(), "즐겨찾기 상태 변경 실패 (DB)", Toast.LENGTH_SHORT).show()
            fetchHomeFavorites()
            val currentPopular = popularAdapter.currentList.toMutableList()
            val pIndex = currentPopular.indexOfFirst { it.id == notice.id && it.category == notice.category }
            if (pIndex != -1) {
                currentPopular[pIndex].isFavorite = !newFavoriteState
                popularAdapter.submitList(currentPopular.toList())
            }
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
                @Suppress("UNCHECKED_CAST")
                val favoriteIds = document.get("favorites") as? List<String> ?: emptyList()
                list.forEach { notice ->
                    val noticeDocId = "${notice.category}_${notice.id}"
                    notice.isFavorite = favoriteIds.contains(noticeDocId)
                }
            }
            onComplete(list)
        }.addOnFailureListener {
            onComplete(list)
        }
    }

    private fun openInAppBrowser(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(requireContext(), url.toUri())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "페이지를 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeAutoScroller() {
        autoScrollRunnable = Runnable {
            val popularItemsCount = popularAdapter.itemCount
            if (popularItemsCount > 0) {
                val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }

                    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                        return SCROLL_SPEED_MILLISECONDS_PER_INCH / displayMetrics.densityDpi
                    }
                }
                currentPopularPosition = (currentPopularPosition + 1) % popularItemsCount
                smoothScroller.targetPosition = currentPopularPosition
                binding.recyclerviewHomePopular.layoutManager?.startSmoothScroll(smoothScroller)
                autoScrollHandler.postDelayed(this.autoScrollRunnable, autoScrollDelay)
            }
        }
    }

    private fun startAutoScroll() {
        if (!this::autoScrollRunnable.isInitialized) {
            initializeAutoScroller()
        }
        stopAutoScroll()
        if (popularAdapter.itemCount > 0) {
            autoScrollHandler.postDelayed(autoScrollRunnable, autoScrollDelay)
        }
    }

    private fun stopAutoScroll() {
        if (this::autoScrollRunnable.isInitialized) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable)
        }
    }

    private fun setupAdvancedScrollAnimation() {
        binding.textviewRandomMessage.post {
            val maxTopMargin = 100.dpToPx(requireContext())
            val collapsedToolbarHeight = 56.dpToPx(requireContext())
            val minTopMargin = collapsedToolbarHeight - binding.textviewRandomMessage.height
            val maxBottomMargin = 80.dpToPx(requireContext())

            offsetChangedListener = AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val scrollRange = appBarLayout.totalScrollRange
                if (scrollRange == 0) return@OnOffsetChangedListener

                val scrollRatio = abs(verticalOffset).toFloat() / scrollRange

                // Update TextView's top margin
                val textParams = binding.textviewRandomMessage.layoutParams as ViewGroup.MarginLayoutParams
                val newTopMargin = (minTopMargin + (maxTopMargin - minTopMargin) * (1 - scrollRatio)).toInt()
                if (textParams.topMargin != newTopMargin) {
                    textParams.topMargin = newTopMargin
                    binding.textviewRandomMessage.layoutParams = textParams
                }

                // Update Anchor View's top margin (which acts as TextView's bottom margin)
                val anchorParams = binding.anchorView.layoutParams as ViewGroup.MarginLayoutParams
                val newBottomMargin = (maxBottomMargin * (1 - scrollRatio)).toInt()
                if (anchorParams.topMargin != newBottomMargin) {
                    anchorParams.topMargin = newBottomMargin
                    binding.anchorView.layoutParams = anchorParams
                }
            }
            binding.appBarLayout.addOnOffsetChangedListener(offsetChangedListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll()
        snapHelper?.attachToRecyclerView(null)
        offsetChangedListener?.let {
            binding.appBarLayout.removeOnOffsetChangedListener(it)
        }
        _binding = null
    }
}
