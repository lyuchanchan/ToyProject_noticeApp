package com.example.toyproject_noticeapp.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.coordinatorlayout.widget.CoordinatorLayout
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
    private var isEditMode = false // 편집 모드 상태 변수

    private var visibleItemTouchHelper: ItemTouchHelper? = null
    private var hiddenItemTouchHelper: ItemTouchHelper? = null
    private lateinit var commonItemTouchCallback: ItemTouchHelper.SimpleCallback
    private var snapHelper: PagerSnapHelper? = null

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private lateinit var autoScrollRunnable: Runnable
    private val autoScrollDelay = 3000L
    private var currentPopularPosition = 0

    private var offsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null

    // 마스터 바로가기 목록 (원본 데이터)
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

    // 화면에 보여질 바로가기 목록과 숨겨진 바로가기 목록 (실제 데이터)
    private var visibleShortcutsData: MutableList<Shortcut> = mutableListOf()
    private var hiddenShortcutsData: MutableList<Shortcut> = mutableListOf()

    // 랜덤 메시지 목록
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

    // SharedPreferences 관련 상수
    companion object {
        private const val PREFS_NAME = "HomeShortcutPrefs"
        private const val KEY_VISIBLE_SHORTCUTS = "visible_shortcuts"
        private const val KEY_HIDDEN_SHORTCUTS = "hidden_shortcuts"
        private const val MIN_TARGET_HEIGHT_DP = 60 // 드래그 앤 드롭 영역 최소 높이
        private const val SCROLL_SPEED_MILLISECONDS_PER_INCH = 100f // 자동 스크롤 속도
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textviewRandomMessage.text = randomMessages.random() // 랜덤 메시지 설정
        loadShortcutPreferences() // 저장된 바로가기 순서 불러오기
        setupClickListeners() // 아이콘 및 버튼 클릭 리스너 설정
        setupRecyclerViews() // RecyclerView 어댑터 및 레이아웃 매니저 설정
        setupItemTouchHelpers() // 드래그 앤 드롭 기능 설정
        initializeAutoScroller() // 인기글 자동 스크롤 초기화
        fetchData() // Firestore 데이터 (인기글, 즐겨찾기) 불러오기
        setupAdvancedScrollAnimation() // 스크롤에 따른 애니메이션 설정
    }

    override fun onResume() {
        super.onResume()
        startAutoScroll() // 화면에 다시 보일 때 자동 스크롤 시작
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll() // 화면에서 벗어날 때 자동 스크롤 중지
    }

    // SharedPreferences에서 바로가기 순서 불러오기
    private fun loadShortcutPreferences() {
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val visibleNamesString = prefs.getString(KEY_VISIBLE_SHORTCUTS, null)
        val hiddenNamesString = prefs.getString(KEY_HIDDEN_SHORTCUTS, null)

        if (visibleNamesString != null && hiddenNamesString != null) {
            val visibleNames = visibleNamesString.split(',').filter { it.isNotEmpty() }
            val hiddenNames = hiddenNamesString.split(',').filter { it.isNotEmpty() }
            // 저장된 이름 순서대로 마스터 목록에서 찾아 데이터 생성
            visibleShortcutsData = visibleNames.mapNotNull { name -> masterShortcutList.find { it.name == name } }.toMutableList()
            hiddenShortcutsData = hiddenNames.mapNotNull { name -> masterShortcutList.find { it.name == name } }.toMutableList()

            // 마스터 목록에는 있지만 저장된 목록에는 없는 아이템 처리 (앱 업데이트 등으로 새 아이콘 추가 시)
            val accountedForNames = (visibleShortcutsData.map { it.name } + hiddenShortcutsData.map { it.name }).toSet()
            masterShortcutList.forEach { shortcut ->
                if (!accountedForNames.contains(shortcut.name)) {
                    // 기본적으로 숨겨진 목록에 추가
                    if (!hiddenShortcutsData.any { it.name == shortcut.name } && !visibleShortcutsData.any {it.name == shortcut.name }) {
                        hiddenShortcutsData.add(shortcut)
                    }
                }
            }
        } else {
            // 저장된 설정이 없으면 기본값 사용 (모든 아이콘 표시)
            visibleShortcutsData = masterShortcutList.toMutableList()
            hiddenShortcutsData = mutableListOf()
        }
    }

    // SharedPreferences에 현재 바로가기 순서 저장
    private fun saveShortcutPreferences() {
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            // 현재 어댑터의 아이템 순서대로 이름만 추출하여 저장
            val visibleNames = shortcutAdapter.items.joinToString(",") { it.name }
            val hiddenNames = hiddenShortcutAdapter.items.joinToString(",") { it.name }
            putString(KEY_VISIBLE_SHORTCUTS, visibleNames)
            putString(KEY_HIDDEN_SHORTCUTS, hiddenNames)
        }
    }

    // 클릭 리스너 설정
    private fun setupClickListeners() {
        binding.imageviewSearch.setOnClickListener { findNavController().navigate(R.id.action_home_to_search) }
        binding.imageviewNotifications.setOnClickListener { findNavController().navigate(R.id.action_home_to_notification_history) }
        binding.imageviewSettings.setOnClickListener { findNavController().navigate(R.id.action_home_to_settings) }
        binding.textviewHomeFavoriteMore.setOnClickListener { findNavController().navigate(R.id.action_home_to_favorites) }
        binding.textviewHomeShortcutEdit.setOnClickListener { toggleEditMode() } // 편집 모드 전환
    }

    // 편집 모드 전환 로직
    private fun toggleEditMode() {
        isEditMode = !isEditMode // 상태 반전
        binding.textviewHomeShortcutEdit.text = if (isEditMode) "완료" else "편집" // 버튼 텍스트 변경
        // 숨겨진 메뉴 관련 UI 표시/숨김 처리
        binding.dividerHiddenShortcuts.isVisible = isEditMode
        binding.textviewHiddenShortcutsTitle.isVisible = isEditMode
        binding.recyclerviewHiddenShortcuts.isVisible = isEditMode

        if (isEditMode) {
            // 편집 모드 시작 시: 드래그 영역 최소 높이 설정, 스크롤 비활성화, ItemTouchHelper 연결
            val minHeightPx = MIN_TARGET_HEIGHT_DP.dpToPx(requireContext())
            binding.recyclerviewHomeShortcuts.minimumHeight = minHeightPx
            binding.recyclerviewHiddenShortcuts.minimumHeight = minHeightPx
            binding.recyclerviewHomeShortcuts.isNestedScrollingEnabled = false // AppBarLayout 스크롤과 충돌 방지
            visibleItemTouchHelper?.attachToRecyclerView(binding.recyclerviewHomeShortcuts)
            hiddenItemTouchHelper?.attachToRecyclerView(binding.recyclerviewHiddenShortcuts)
        } else {
            // 편집 모드 종료 시: 최소 높이 제거, 스크롤 활성화, ItemTouchHelper 해제
            binding.recyclerviewHomeShortcuts.minimumHeight = 0
            binding.recyclerviewHiddenShortcuts.minimumHeight = 0
            binding.recyclerviewHomeShortcuts.isNestedScrollingEnabled = true
            visibleItemTouchHelper?.attachToRecyclerView(null)
            hiddenItemTouchHelper?.attachToRecyclerView(null)
            // ▼▼▼ [ 수정 ] 완료 시 저장 로직 호출 ▼▼▼
            saveShortcutPreferences() // 변경된 순서 저장
            // ▲▲▲ [ 수정 ] ▲▲▲
        }

        // ▼▼▼ [ 수정 ] 어댑터 갱신 로직 추가 ▼▼▼
        // 편집 모드 상태 변경 후 항상 어댑터 갱신하여 클릭 가능/불가능 상태 반영
        shortcutAdapter.notifyItemRangeChanged(0, shortcutAdapter.itemCount)
        hiddenShortcutAdapter.notifyItemRangeChanged(0, hiddenShortcutAdapter.itemCount)
        // ▲▲▲ [ 수정 ] ▲▲▲
    }

    // AppBarLayout 스크롤 기능 활성화/비활성화 (드래그 시 AppBar 고정용)
    private fun setAppBarScrollingEnabled(enabled: Boolean) {
        (binding.appBarLayout.layoutParams as? CoordinatorLayout.LayoutParams)?.let { params ->
            (params.behavior as? AppBarLayout.Behavior)?.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                    return enabled
                }
            })
        }
    }

    // RecyclerView 설정
    private fun setupRecyclerViews() {
        // 표시되는 바로가기 어댑터 설정
        shortcutAdapter = HomeShortcutAdapter(visibleShortcutsData) { shortcut ->
            if (!isEditMode) { // 편집 모드가 아닐 때만 클릭 동작
                if (shortcut.url == "BOARD") { // URL이 "BOARD"면 공지 목록 화면으로 이동
                    val bundle = bundleOf("categoryName" to shortcut.name)
                    findNavController().navigate(R.id.action_home_to_notice_list, bundle)
                } else { // 그 외에는 웹 브라우저로 URL 열기
                    openInAppBrowser(shortcut.url)
                }
            }
        }
        binding.recyclerviewHomeShortcuts.adapter = shortcutAdapter
        binding.recyclerviewHomeShortcuts.layoutManager = GridLayoutManager(requireContext(), 3) // 3열 그리드

        // 숨겨진 바로가기 어댑터 설정 (클릭 동작 없음)
        hiddenShortcutAdapter = HomeShortcutAdapter(hiddenShortcutsData) {}
        binding.recyclerviewHiddenShortcuts.adapter = hiddenShortcutAdapter
        binding.recyclerviewHiddenShortcuts.layoutManager = GridLayoutManager(requireContext(), 3)

        // 인기글 어댑터 설정
        popularAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice -> updateFavoriteStatus(notice) }
        )
        binding.recyclerviewHomePopular.adapter = popularAdapter
        binding.recyclerviewHomePopular.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false) // 가로 스크롤

        // PagerSnapHelper: 인기글 스크롤 시 페이지처럼 넘어가도록 함
        snapHelper = PagerSnapHelper()
        snapHelper?.attachToRecyclerView(binding.recyclerviewHomePopular)

        // 인기글 스크롤 리스너: 스크롤 멈출 때 현재 페이지 인디케이터 업데이트 및 자동 스크롤 재시작
        binding.recyclerviewHomePopular.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) { // 스크롤 멈췄을 때
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                    snapHelper?.let { pagerSnapHelper ->
                        val snapView = pagerSnapHelper.findSnapView(layoutManager) // 현재 보이는 아이템 찾기
                        snapView?.let {
                            val position = layoutManager?.getPosition(it) // 아이템 위치 확인
                            if (position != null && position != RecyclerView.NO_POSITION) {
                                binding.tabLayoutPopularIndicator.getTabAt(position)?.select() // 인디케이터 탭 선택
                                currentPopularPosition = position // 현재 위치 저장
                                startAutoScroll() // 자동 스크롤 다시 시작
                            }
                        }
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) { // 사용자가 직접 스크롤 시작하면
                    stopAutoScroll() // 자동 스크롤 중지
                }
            }
        })

        // 즐겨찾기 어댑터 설정
        favoriteAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = { notice -> updateFavoriteStatus(notice) }
        )
        binding.recyclerviewHomeFavorite.adapter = favoriteAdapter
        binding.recyclerviewHomeFavorite.layoutManager = LinearLayoutManager(requireContext())
    }

    // 드래그 앤 드롭 기능 설정
    private fun setupItemTouchHelpers() {
        commonItemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, // 상하좌우 드래그 허용
            0 // 스와이프는 사용 안 함
        ) {
            private var sourceAdapter: HomeShortcutAdapter? = null // 드래그 시작된 어댑터
            private var draggedItem: Shortcut? = null // 드래그 중인 아이템 데이터
            private var lastTargetRecyclerView: RecyclerView? = null // 드롭될 위치의 RecyclerView

            // 드래그 시 보여줄 그림자 효과용 변수들
            private var dragShadow: ImageView? = null
            private var dragShadowLP: FrameLayout.LayoutParams? = null
            private var draggedItemView: View? = null // 드래그 시작한 아이템의 View
            private var initialX: Float = 0f // 드래그 시작 X 좌표
            private var initialY: Float = 0f // 드래그 시작 Y 좌표

            // 현재 터치 좌표(x, y)가 어느 RecyclerView 위에 있는지 확인
            private fun findTargetRecyclerView(x: Int, y: Int): RecyclerView? {
                val hiddenRvRect = Rect()
                if (binding.recyclerviewHiddenShortcuts.isVisible) { // 숨겨진 목록이 보일 때만 체크
                    binding.recyclerviewHiddenShortcuts.getGlobalVisibleRect(hiddenRvRect)
                    if (hiddenRvRect.contains(x, y)) {
                        return binding.recyclerviewHiddenShortcuts
                    }
                }

                val visibleRvRect = Rect()
                binding.recyclerviewHomeShortcuts.getGlobalVisibleRect(visibleRvRect)
                if (visibleRvRect.contains(x, y)) {
                    return binding.recyclerviewHomeShortcuts
                }

                return null // 어느 RecyclerView 위에도 없을 때
            }

            // 아이템 순서 변경 시 호출 (같은 RecyclerView 내에서 이동)
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                val adapter = recyclerView.adapter as HomeShortcutAdapter
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION) {
                    adapter.moveItem(fromPos, toPos) // 어댑터에 순서 변경 알림
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {} // 스와이프 사용 안 함

            override fun isLongPressDragEnabled(): Boolean = isEditMode // 편집 모드일 때만 길게 눌러 드래그 가능

            // 드래그 시작 또는 종료 시 호출
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) { // 드래그 시작 시
                    setAppBarScrollingEnabled(false) // AppBar 스크롤 고정
                    if (viewHolder == null) return

                    draggedItemView = viewHolder.itemView // 드래그 시작한 아이템 View 저장
                    val position = viewHolder.adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        // 시작 어댑터와 아이템 데이터 저장
                        sourceAdapter = (viewHolder.itemView.parent as RecyclerView).adapter as? HomeShortcutAdapter
                        draggedItem = sourceAdapter?.items?.getOrNull(position)
                    }

                    // 아이템 View를 Bitmap 이미지로 변환 (그림자 효과용)
                    val bitmap = Bitmap.createBitmap(draggedItemView!!.width, draggedItemView!!.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    draggedItemView!!.draw(canvas)

                    // 그림자 ImageView 생성 및 설정
                    dragShadow = ImageView(requireContext()).apply {
                        setImageBitmap(bitmap)
                        elevation = requireContext().resources.getDimension(R.dimen.drag_elevation) // 그림자 높이 설정
                    }

                    // 드래그 시작 위치 저장
                    val location = IntArray(2)
                    draggedItemView!!.getLocationOnScreen(location)
                    initialX = location[0].toFloat()
                    initialY = location[1].toFloat()

                    // 그림자 ImageView의 레이아웃 파라미터 설정 (초기 위치)
                    dragShadowLP = FrameLayout.LayoutParams(draggedItemView!!.width, draggedItemView!!.height).apply {
                        leftMargin = initialX.toInt()
                        topMargin = initialY.toInt()
                    }

                    // 최상위 레이아웃에 그림자 추가하고 원본 아이템 반투명 처리
                    binding.rootFrameLayout.addView(dragShadow, dragShadowLP)
                    draggedItemView!!.alpha = 0.4f
                }
            }

            // 드래그 중 아이템이 그려질 때 호출 (그림자 이동 및 타겟 RecyclerView 강조)
            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive && dragShadow != null && draggedItemView != null) {
                    // RecyclerView의 현재 화면상 위치 계산
                    val rvLocation = IntArray(2)
                    recyclerView.getLocationOnScreen(rvLocation)
                    val rvLeft = rvLocation[0]
                    val rvTop = rvLocation[1]

                    // 아이템의 새 위치 계산 (RecyclerView 기준 + 드래그 이동량)
                    val newLeft = rvLeft + viewHolder.itemView.left + dX
                    val newTop = rvTop + viewHolder.itemView.top + dY

                    // 그림자 ImageView 위치 업데이트
                    dragShadowLP?.let {
                        it.leftMargin = newLeft.toInt()
                        it.topMargin = newTop.toInt()
                        dragShadow!!.layoutParams = it
                    }

                    // 그림자 중심 좌표로 타겟 RecyclerView 찾기
                    val shadowCenterX = (newLeft + draggedItemView!!.width / 2).toInt()
                    val shadowCenterY = (newTop + draggedItemView!!.height / 2).toInt()
                    val currentTarget = findTargetRecyclerView(shadowCenterX, shadowCenterY)
                    lastTargetRecyclerView = currentTarget // 마지막 타겟 저장

                    // 다른 RecyclerView 위에 있으면 배경색으로 강조 표시
                    binding.recyclerviewHomeShortcuts.setBackgroundColor(
                        if (currentTarget == binding.recyclerviewHomeShortcuts && currentTarget.adapter != sourceAdapter) "#E0E0E0".toColorInt() else Color.TRANSPARENT
                    )
                    binding.recyclerviewHiddenShortcuts.setBackgroundColor(
                        if (currentTarget == binding.recyclerviewHiddenShortcuts && currentTarget.adapter != sourceAdapter) "#E0E0E0".toColorInt() else Color.TRANSPARENT
                    )
                } else {
                    // 드래그 상태가 아니면 기본 그리기 동작 수행
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }

            // 드래그 종료 시 호출 (아이템 이동 처리, 그림자 제거 등)
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f // 원본 아이템 투명도 복구

                // 그림자 제거 및 관련 변수 초기화
                dragShadow?.let { binding.rootFrameLayout.removeView(it) }
                dragShadow = null

                // RecyclerView 배경색 복구
                binding.recyclerviewHomeShortcuts.setBackgroundColor(Color.TRANSPARENT)
                binding.recyclerviewHiddenShortcuts.setBackgroundColor(Color.TRANSPARENT)

                // 아이템 이동 로직 (다른 RecyclerView로 이동했을 경우)
                val targetRecyclerView = lastTargetRecyclerView
                val targetAdapter = targetRecyclerView?.adapter as? HomeShortcutAdapter

                if (sourceAdapter != null && targetAdapter != null && sourceAdapter != targetAdapter && draggedItem != null) {
                    // 시작 어댑터에서 아이템 제거 후 타겟 어댑터에 추가
                    val currentPosition = sourceAdapter!!.items.indexOf(draggedItem)
                    if (currentPosition != -1) {
                        sourceAdapter!!.removeItem(currentPosition)?.also { removedItem ->
                            targetAdapter.addItem(removedItem) // 타겟 어댑터의 맨 뒤에 추가
                            Toast.makeText(requireContext(), "'${removedItem.name}' 이동 완료", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // 상태 변수 초기화
                sourceAdapter = null
                draggedItem = null
                lastTargetRecyclerView = null
                draggedItemView = null

                setAppBarScrollingEnabled(true) // AppBar 스크롤 다시 활성화
            }
        }

        // 각 RecyclerView에 ItemTouchHelper 생성
        visibleItemTouchHelper = ItemTouchHelper(commonItemTouchCallback)
        hiddenItemTouchHelper = ItemTouchHelper(commonItemTouchCallback)
    }

    // Firestore 데이터 로딩 함수 호출
    private fun fetchData() {
        fetchPopularPosts() // 인기글 로딩
        fetchHomeFavorites() // 즐겨찾기 로딩 (홈 화면 미리보기용)
    }

    // 인기글 데이터 로딩 및 표시
    private fun fetchPopularPosts() {
        db.collection("popular_notices").limit(5).get() // 최대 5개 가져오기
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    // Firestore 문서를 DataNotificationItem 객체로 변환
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
                    // 로드된 목록에 대해 즐겨찾기 상태 업데이트 후 어댑터에 적용
                    updateFavoritesStateForList(popularList) { updatedList ->
                        popularAdapter.submitList(updatedList)
                        binding.tabLayoutPopularIndicator.removeAllTabs() // 기존 인디케이터 탭 제거
                        if (updatedList.isNotEmpty()) {
                            binding.tabLayoutPopularIndicator.isVisible = true // 인디케이터 표시
                            // 아이템 개수만큼 인디케이터 탭 추가
                            updatedList.forEach { _ ->
                                binding.tabLayoutPopularIndicator.addTab(binding.tabLayoutPopularIndicator.newTab())
                            }

                            // 인디케이터 탭 클릭 비활성화 (스크롤 연동용)
                            val tabStrip = binding.tabLayoutPopularIndicator.getChildAt(0) as ViewGroup
                            for (i in 0 until tabStrip.childCount) {
                                tabStrip.getChildAt(i).setOnTouchListener { _, _ -> true }
                            }

                            // 첫 번째 탭 선택 (기본값)
                            if (binding.tabLayoutPopularIndicator.selectedTabPosition == -1 || binding.tabLayoutPopularIndicator.selectedTabPosition >= updatedList.size) {
                                binding.tabLayoutPopularIndicator.getTabAt(0)?.select()
                            }
                            currentPopularPosition = 0 // 현재 위치 초기화
                            startAutoScroll() // 자동 스크롤 시작
                        } else {
                            binding.tabLayoutPopularIndicator.isVisible = false // 인기글 없으면 인디케이터 숨김
                            stopAutoScroll()
                        }
                    }
                } else {
                    // 인기글 없을 때 처리
                    popularAdapter.submitList(emptyList())
                    binding.tabLayoutPopularIndicator.isVisible = false
                    binding.tabLayoutPopularIndicator.removeAllTabs()
                    stopAutoScroll()
                }
            }
            .addOnFailureListener { e ->
                // 로딩 실패 시 처리
                Toast.makeText(requireContext(), "추천 글 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                popularAdapter.submitList(emptyList())
                binding.tabLayoutPopularIndicator.isVisible = false
                binding.tabLayoutPopularIndicator.removeAllTabs()
                stopAutoScroll()
            }
    }

    // 즐겨찾기 데이터 로딩 (홈 화면 미리보기용 - 최근 1개)
    private fun fetchHomeFavorites() {
        val userId = auth.currentUser?.uid
        if (userId == null) { // 로그인 안 했으면 빈 상태 표시
            showEmptyFavorites()
            return
        }
        db.collection("users").document(userId).get() // 사용자 정보 가져오기
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val favoriteIds = document.get("favorites") as? List<String> // 즐겨찾기 ID 목록 가져오기
                    if (!favoriteIds.isNullOrEmpty()) {
                        binding.recyclerviewHomeFavorite.isVisible = true // RecyclerView 표시
                        binding.textviewHomeFavoriteEmpty.isVisible = false // 빈 상태 메시지 숨김
                        val recentFavoriteIds = favoriteIds.reversed().take(1) // 최근 1개 ID만 가져오기
                        if (recentFavoriteIds.isNotEmpty()) {
                            // ID 목록으로 notices 컬렉션에서 실제 데이터 가져오기
                            db.collection("notices").whereIn(com.google.firebase.firestore.FieldPath.documentId(), recentFavoriteIds).get()
                                .addOnSuccessListener { noticeDocuments ->
                                    if (noticeDocuments != null && !noticeDocuments.isEmpty) {
                                        // Firestore 문서를 DataNotificationItem 객체로 변환
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
                                            notice.isFavorite = true // 즐겨찾기 목록에서 가져왔으므로 true 설정
                                            notice
                                        }
                                        favoriteAdapter.submitList(favoritePreviewList) // 어댑터에 데이터 적용
                                    } else {
                                        showEmptyFavorites() // 데이터 못 찾으면 빈 상태 표시
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "[즐겨찾기] 글 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                    showEmptyFavorites()
                                }
                        } else {
                            showEmptyFavorites() // ID 목록 비었으면 빈 상태 표시
                        }
                    } else {
                        showEmptyFavorites() // 즐겨찾기 ID 없으면 빈 상태 표시
                    }
                } else {
                    showEmptyFavorites() // 사용자 정보 없으면 빈 상태 표시
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "[즐겨찾기] 사용자 정보 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                showEmptyFavorites()
            }
    }

    // 즐겨찾기 목록이 비었을 때 UI 처리
    private fun showEmptyFavorites() {
        binding.recyclerviewHomeFavorite.isVisible = false // RecyclerView 숨김
        binding.textviewHomeFavoriteEmpty.isVisible = true // 빈 상태 메시지 표시
        favoriteAdapter.submitList(emptyList()) // 어댑터 데이터 비우기
    }

    // 즐겨찾기 상태 업데이트 로직 (인기글/즐겨찾기 아이템의 별 아이콘 클릭 시)
    private fun updateFavoriteStatus(notice: DataNotificationItem) {
        val userId = auth.currentUser?.uid ?: return // 로그인 상태 확인
        val userDocRef = db.collection("users").document(userId)
        val noticeDocId = "${notice.category}_${notice.id}" // Firestore 문서 ID 생성
        val newFavoriteState = !notice.isFavorite // 현재 상태 반전

        val message = if (newFavoriteState) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 삭제되었습니다."
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        // 인기글 목록(popularAdapter)에서 해당 아이템 찾아 상태 업데이트
        val popularList = popularAdapter.currentList.toMutableList()
        val popularIndex = popularList.indexOfFirst { it.id == notice.id && it.category == notice.category }
        if (popularIndex != -1) {
            popularList[popularIndex].isFavorite = newFavoriteState
            popularAdapter.submitList(popularList.toList()) // 변경된 리스트로 어댑터 갱신
        }

        // 홈 화면의 즐겨찾기 미리보기 갱신 (데이터 다시 로드)
        fetchHomeFavorites()

        // Firestore 사용자 문서의 'favorites' 필드 업데이트
        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId)) // 배열에 ID 추가
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId)) // 배열에서 ID 제거
        }
        updateTask.addOnFailureListener {
            // Firestore 업데이트 실패 시: UI 원상 복구 및 메시지 표시
            Toast.makeText(requireContext(), "즐겨찾기 상태 변경 실패 (DB)", Toast.LENGTH_SHORT).show()
            fetchHomeFavorites() // 즐겨찾기 미리보기 다시 로드
            // 인기글 목록 상태도 원상 복구
            val currentPopular = popularAdapter.currentList.toMutableList()
            val pIndex = currentPopular.indexOfFirst { it.id == notice.id && it.category == notice.category }
            if (pIndex != -1) {
                currentPopular[pIndex].isFavorite = !newFavoriteState // 원래 상태로 되돌림
                popularAdapter.submitList(currentPopular.toList())
            }
        }
    }

    // 주어진 공지 목록에 대해 현재 사용자의 즐겨찾기 상태를 업데이트하는 함수
    private fun updateFavoritesStateForList(list: List<DataNotificationItem>, onComplete: (List<DataNotificationItem>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) { // 로그인 안 했으면 그냥 원본 리스트 반환
            onComplete(list)
            return
        }
        db.collection("users").document(userId).get() // 사용자 정보 가져오기
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val favoriteIds = document.get("favorites") as? List<String> ?: emptyList() // 즐겨찾기 ID 목록
                    // 각 공지 아이템의 ID가 즐겨찾기 목록에 있는지 확인하여 isFavorite 상태 설정
                    list.forEach { notice ->
                        val noticeDocId = "${notice.category}_${notice.id}"
                        notice.isFavorite = favoriteIds.contains(noticeDocId)
                    }
                }
                onComplete(list) // 업데이트된 리스트 반환
            }.addOnFailureListener {
                onComplete(list) // 실패 시 원본 리스트 반환
            }
    }

    // 인앱 브라우저(CustomTabsIntent)로 URL 열기
    private fun openInAppBrowser(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(requireContext(), url.toUri()) // String을 Uri로 변환하여 사용
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "페이지를 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 인기글 자동 스크롤러 초기화
    private fun initializeAutoScroller() {
        autoScrollRunnable = Runnable {
            val popularItemsCount = popularAdapter.itemCount
            if (popularItemsCount > 0) { // 아이템이 있을 때만 실행
                // 부드러운 스크롤 효과 설정
                val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START // 수직 스크롤 아님 (가로 스크롤 설정 필요 시 수정)
                    }
                    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                        // 스크롤 속도 계산 (인치당 밀리초)
                        return SCROLL_SPEED_MILLISECONDS_PER_INCH / displayMetrics.densityDpi
                    }
                }
                currentPopularPosition = (currentPopularPosition + 1) % popularItemsCount // 다음 위치 계산 (순환)
                smoothScroller.targetPosition = currentPopularPosition // 스크롤 목표 위치 설정
                binding.recyclerviewHomePopular.layoutManager?.startSmoothScroll(smoothScroller) // 부드러운 스크롤 시작
                // 지정된 시간(autoScrollDelay) 후 다시 실행 예약
                autoScrollHandler.postDelayed(this.autoScrollRunnable, autoScrollDelay)
            }
        }
    }

    // 자동 스크롤 시작
    private fun startAutoScroll() {
        if (!this::autoScrollRunnable.isInitialized) {
            initializeAutoScroller() // 초기화 안 됐으면 초기화 먼저
        }
        stopAutoScroll() // 기존 예약된 작업 취소
        if (popularAdapter.itemCount > 0) { // 아이템 있을 때만 예약
            autoScrollHandler.postDelayed(autoScrollRunnable, autoScrollDelay)
        }
    }

    // 자동 스크롤 중지
    private fun stopAutoScroll() {
        if (this::autoScrollRunnable.isInitialized) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable) // 예약된 작업 취소
        }
    }

    // AppBar 스크롤 시 상단 메시지 위치 조절 애니메이션 설정
    private fun setupAdvancedScrollAnimation() {
        binding.textviewRandomMessage.post { // View가 그려진 후 실행
            val maxTopMargin = 100.dpToPx(requireContext()) // 최대 상단 여백
            val minTopMargin = 10.dpToPx(requireContext()) // 최소 상단 여백 (스크롤 최대로 올렸을 때)
            val maxBottomMargin = 80.dpToPx(requireContext()) // 최대 하단 여백 (실제로는 anchorView의 상단 여백)

            offsetChangedListener = AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val scrollRange = appBarLayout.totalScrollRange // AppBar 최대 스크롤 가능 범위
                if (scrollRange == 0) return@OnOffsetChangedListener

                val scrollRatio = abs(verticalOffset).toFloat() / scrollRange // 현재 스크롤 비율 (0.0 ~ 1.0)

                // 스크롤 비율에 따라 TextView의 상단 여백 계산 및 적용
                val textParams = binding.textviewRandomMessage.layoutParams as ViewGroup.MarginLayoutParams
                val newTopMargin = (minTopMargin + (maxTopMargin - minTopMargin) * (1 - scrollRatio)).toInt()
                if (textParams.topMargin != newTopMargin) {
                    textParams.topMargin = newTopMargin
                    binding.textviewRandomMessage.layoutParams = textParams
                }

                // 스크롤 비율에 따라 anchorView의 상단 여백(TextView의 하단 여백 역할) 계산 및 적용
                val anchorParams = binding.anchorView.layoutParams as ViewGroup.MarginLayoutParams
                val newBottomMargin = (maxBottomMargin * (1 - scrollRatio)).toInt()
                if (anchorParams.topMargin != newBottomMargin) {
                    anchorParams.topMargin = newBottomMargin
                    binding.anchorView.layoutParams = anchorParams
                }
            }
            binding.appBarLayout.addOnOffsetChangedListener(offsetChangedListener) // 리스너 등록
        }
    }

    // ▼▼▼ [ 수정된 함수 ] ▼▼▼
    override fun onDestroyView() {
        super.onDestroyView()
        // 화면 View가 파괴될 때 (다른 화면으로 이동, 앱 종료 등)
        if (isEditMode) {
            // 만약 편집 모드 상태였다면, 완료하지 않고 나간 것으로 간주
            isEditMode = false // 편집 모드 상태 초기화
            // saveShortcutPreferences() // 필요하다면 여기서 저장할 수도 있지만, 보통은 완료 시 저장
        }
        stopAutoScroll() // 자동 스크롤 중지
        snapHelper?.attachToRecyclerView(null) // SnapHelper 해제
        // AppBar 스크롤 리스너 해제 (_binding이 null일 수 있으므로 안전 호출 사용)
        offsetChangedListener?.let {
            _binding?.appBarLayout?.removeOnOffsetChangedListener(it)
        }
        _binding = null // _binding 참조 해제 (메모리 누수 방지)
    }
    // ▲▲▲ [ 수정된 함수 ] ▲▲▲
}