package com.example.toyproject_noticeapp.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
// import android.util.Log // Logcat을 위해 필요할 수 있으나, Gemini 환경에서는 print 사용
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

// Helper function for dp to px conversion
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

    private val masterShortcutList by lazy {
        listOf(
            Shortcut(R.drawable.home_icon_chuiup, "공지사항", "BOARD"),
            Shortcut(R.drawable.home_icon_haksa, "학사공지", "BOARD"),
            Shortcut(R.drawable.home_icon_festival, "행사공지", "BOARD"),
            Shortcut(R.drawable.home_icon_scholarship, "장학공지", "BOARD"),
            Shortcut(R.drawable.home_icon_check, "취업공지", "BOARD"),
            Shortcut(R.drawable.home_icon_1, "홈페이지", "https://www.hs.ac.kr/kor/index.do"),
            Shortcut(R.drawable.home_icon_calendar, "학사일정", "https://www.hs.ac.kr/kor/4837/subview.do"),
            Shortcut(R.drawable.home_icon_food, "식단표", "https://www.hs.ac.kr/kor/8398/subview.do"),
            Shortcut(R.drawable.home_icon_bus, "셔틀버스", "https://www.hs.ac.kr/kor/4984/subview.do"),
            Shortcut(R.drawable.home_icon_check, "AISW계열", "BOARD")
        )
    }
    private var visibleShortcutsData: MutableList<Shortcut> = mutableListOf()
    private var hiddenShortcutsData: MutableList<Shortcut> = mutableListOf()

    private val randomMessages = listOf(
        "오늘도 놓치지 말고 체크✔️", "캠퍼스 소식, 여기 다 있지", "공지 찾기? 이제 고생 끝🙌", "개쩌는 3인방이 만든 앱😎"
    )

    companion object {
        private const val PREFS_NAME = "HomeShortcutPrefs"
        private const val KEY_VISIBLE_SHORTCUTS = "visible_shortcuts"
        private const val KEY_HIDDEN_SHORTCUTS = "hidden_shortcuts"
        private const val MIN_TARGET_HEIGHT_DP = 60 // Minimum height for empty drop target in DP
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
        fetchData()
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
        val editor = prefs.edit()
        val visibleNames = shortcutAdapter.items.map { it.name }.joinToString(",")
        val hiddenNames = hiddenShortcutAdapter.items.map { it.name }.joinToString(",")
        editor.putString(KEY_VISIBLE_SHORTCUTS, visibleNames)
        editor.putString(KEY_HIDDEN_SHORTCUTS, hiddenNames)
        editor.apply()
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
        if (isEditMode) {
            binding.textviewHomeShortcutEdit.text = "완료"
            binding.dividerHiddenShortcuts.visibility = View.VISIBLE
            binding.textviewHiddenShortcutsTitle.visibility = View.VISIBLE
            binding.recyclerviewHiddenShortcuts.visibility = View.VISIBLE

            val minHeightPx = MIN_TARGET_HEIGHT_DP.dpToPx(requireContext())
            binding.recyclerviewHomeShortcuts.minimumHeight = minHeightPx
            binding.recyclerviewHiddenShortcuts.minimumHeight = minHeightPx

            binding.recyclerviewHomeShortcuts.isNestedScrollingEnabled = false
            visibleItemTouchHelper?.attachToRecyclerView(binding.recyclerviewHomeShortcuts)
            hiddenItemTouchHelper?.attachToRecyclerView(binding.recyclerviewHiddenShortcuts)
        } else {
            binding.textviewHomeShortcutEdit.text = "편집"
            binding.dividerHiddenShortcuts.visibility = View.GONE
            binding.textviewHiddenShortcutsTitle.visibility = View.GONE
            binding.recyclerviewHiddenShortcuts.visibility = View.GONE

            binding.recyclerviewHomeShortcuts.minimumHeight = 0
            binding.recyclerviewHiddenShortcuts.minimumHeight = 0

            binding.recyclerviewHomeShortcuts.isNestedScrollingEnabled = true
            visibleItemTouchHelper?.attachToRecyclerView(null)
            hiddenItemTouchHelper?.attachToRecyclerView(null)
            saveShortcutPreferences()

            shortcutAdapter.notifyDataSetChanged()
            hiddenShortcutAdapter.notifyDataSetChanged()
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
        binding.recyclerviewHomeShortcuts.layoutManager = GridLayoutManager(context, 5)

        hiddenShortcutAdapter = HomeShortcutAdapter(hiddenShortcutsData) {}
        binding.recyclerviewHiddenShortcuts.adapter = hiddenShortcutAdapter
        binding.recyclerviewHiddenShortcuts.layoutManager = GridLayoutManager(context, 5)

        popularAdapter = AdapterNotificationList(onItemClick = { notice -> openInAppBrowser(notice.url) }, onFavoriteClick = { notice -> updateFavoriteStatus(notice) })
        binding.recyclerviewHomePopular.adapter = popularAdapter
        binding.recyclerviewHomePopular.layoutManager = LinearLayoutManager(context)

        favoriteAdapter = AdapterNotificationList(onItemClick = { notice -> openInAppBrowser(notice.url) }, onFavoriteClick = { notice -> updateFavoriteStatus(notice) })
        binding.recyclerviewHomeFavorite.adapter = favoriteAdapter
        binding.recyclerviewHomeFavorite.layoutManager = LinearLayoutManager(context)
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

                            if (dragInProgressSourceRecyclerView?.id == binding.recyclerviewHomeShortcuts.id) {
                                binding.recyclerviewHomeShortcuts.elevation = 20f
                                binding.recyclerviewHiddenShortcuts.elevation = 10f
                            } else {
                                binding.recyclerviewHiddenShortcuts.elevation = 20f
                                binding.recyclerviewHomeShortcuts.elevation = 10f
                            }

                            if (originalDragPosition != RecyclerView.NO_POSITION && dragInProgressSourceRecyclerView != null) {
                                val sourceAdapterCurrent = if (dragInProgressSourceRecyclerView!!.id == binding.recyclerviewHomeShortcuts.id) {
                                    shortcutAdapter
                                } else if (dragInProgressSourceRecyclerView!!.id == binding.recyclerviewHiddenShortcuts.id) {
                                    hiddenShortcutAdapter
                                } else {
                                    null
                                }
                                draggedItemData = sourceAdapterCurrent?.items?.getOrNull(originalDragPosition)
                            } else {
                                draggedItemData = null
                            }
                            if (draggedItemData == null && originalDragPosition != RecyclerView.NO_POSITION) {
                                Toast.makeText(context, "드래그 시작 오류: 아이템 데이터 못찾음", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    ItemTouchHelper.ACTION_STATE_IDLE -> {
                        binding.nestedScrollView.requestDisallowInterceptTouchEvent(false)
                        dragInProgressViewHolder?.itemView?.alpha = 1.0f

                        if (isOverTargetForDrop && draggedItemData != null && dragInProgressSourceRecyclerView != null && originalDragPosition != RecyclerView.NO_POSITION) {
                            val fromAdapter: HomeShortcutAdapter?
                            val targetAdapter: HomeShortcutAdapter?

                            if (dragInProgressSourceRecyclerView!!.id == binding.recyclerviewHomeShortcuts.id) {
                                fromAdapter = shortcutAdapter
                                targetAdapter = hiddenShortcutAdapter
                            } else if (dragInProgressSourceRecyclerView!!.id == binding.recyclerviewHiddenShortcuts.id) {
                                fromAdapter = hiddenShortcutAdapter
                                targetAdapter = shortcutAdapter
                            } else {
                                Toast.makeText(context, "드롭 오류: 알 수 없는 시작 목록", Toast.LENGTH_SHORT).show()
                                fromAdapter = null
                                targetAdapter = null
                            }

                            if (fromAdapter != null && targetAdapter != null && fromAdapter != targetAdapter) {
                                val currentPosition = dragInProgressViewHolder?.adapterPosition ?: originalDragPosition
                                if (currentPosition >= 0 && currentPosition < fromAdapter.itemCount) {
                                    val itemAtOriginalPos = fromAdapter.items.getOrNull(currentPosition)

                                    val areItemsLogicallyEqual = if (itemAtOriginalPos != null && draggedItemData != null) {
                                        itemAtOriginalPos.name == draggedItemData!!.name &&
                                                itemAtOriginalPos.url == draggedItemData!!.url &&
                                                itemAtOriginalPos.iconResId == draggedItemData!!.iconResId
                                    } else {
                                        itemAtOriginalPos == draggedItemData
                                    }

                                    if (areItemsLogicallyEqual) {
                                        fromAdapter.removeItem(currentPosition)
                                        targetAdapter.addItem(draggedItemData!!)

                                        fromAdapter.notifyDataSetChanged()
                                        targetAdapter.notifyDataSetChanged()

                                        Toast.makeText(context, "'${draggedItemData!!.name}' 이동 완료", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val logMessage = """
                                            드롭 오류: 아이템 불일치 (명시적 비교 후)
                                            draggedItemData: name='${draggedItemData?.name}', url='${draggedItemData?.url}', iconResId=${draggedItemData?.iconResId} (Instance: ${System.identityHashCode(draggedItemData)})
                                            itemAtOriginalPos: name='${itemAtOriginalPos?.name}', url='${itemAtOriginalPos?.url}', iconResId=${itemAtOriginalPos?.iconResId} (Instance: ${System.identityHashCode(itemAtOriginalPos)})
                                            originalDragPosition: $originalDragPosition
                                            fromAdapter.itemCount: ${fromAdapter.itemCount}
                                        """.trimIndent()
                                        print("########## DRAG DROP DEBUG - EXPLICIT ITEM MISMATCH ##########")
                                        print(logMessage)
                                        print("############################################################")
                                        Toast.makeText(context, "드롭 오류: 아이템 불일치 (명시적 비교, 로그 확인)", Toast.LENGTH_LONG).show()
                                        fromAdapter.notifyDataSetChanged()
                                    }
                                } else {
                                    val logMessage = """
                                        드롭 오류: 원래 위치가 잘못됨 ($originalDragPosition)
                                        draggedItemData: name='${draggedItemData?.name}', url='${draggedItemData?.url}', iconResId=${draggedItemData?.iconResId}
                                        fromAdapter.itemCount: ${fromAdapter?.itemCount}
                                    """.trimIndent()
                                    print("########## DRAG DROP DEBUG - INVALID POSITION ##########")
                                    print(logMessage)
                                    print("#######################################################")
                                    Toast.makeText(context, "드롭 오류: 원래 위치가 잘못됨 (로그 확인)", Toast.LENGTH_SHORT).show()
                                    fromAdapter?.notifyDataSetChanged()
                                }
                            }
                        } else if (isOverTargetForDrop) {
                            val logMessage = """
                                드롭 실패: 드롭 대상 위였으나, 주요 데이터 누락
                                draggedItemData is null: ${draggedItemData == null}
                                dragInProgressSourceRecyclerView is null: ${dragInProgressSourceRecyclerView == null}
                                originalDragPosition: $originalDragPosition
                            """.trimIndent()
                            print("########## DRAG DROP DEBUG - DROP OVER TARGET, DATA MISSING ##########")
                            print(logMessage)
                            print("#####################################################################")
                            if (draggedItemData == null) Toast.makeText(context, "드롭 실패: 드래그된 아이템 데이터 없음 (로그 확인)", Toast.LENGTH_SHORT).show()
                            if (originalDragPosition == RecyclerView.NO_POSITION) Toast.makeText(context, "드롭 실패: 원래 위치 정보 없음 (로그 확인)", Toast.LENGTH_SHORT).show()
                            val sourceAdapterToNotify = if (dragInProgressSourceRecyclerView?.id == binding.recyclerviewHomeShortcuts.id) shortcutAdapter else if (dragInProgressSourceRecyclerView?.id == binding.recyclerviewHiddenShortcuts.id) hiddenShortcutAdapter else null
                            sourceAdapterToNotify?.notifyDataSetChanged()
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

                    if (otherRecyclerView.visibility == View.VISIBLE &&
                        !otherRvRect.isEmpty &&
                        !draggedItemScreenRect.isEmpty &&
                        Rect.intersects(draggedItemScreenRect, otherRvRect)) {
                        otherRecyclerView.setBackgroundColor(Color.parseColor("#E0E0E0"))
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
        db.collection("popular_notices").limit(1).get()
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
                    }
                } else {
                    popularAdapter.submitList(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "추천 글 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
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
                    val favoriteIds = document.get("favorites") as? List<String>
                    val validFavoriteIds = favoriteIds?.filter { it.isNotBlank() }
                    if (!validFavoriteIds.isNullOrEmpty()) {
                        binding.recyclerviewHomeShortcuts.visibility = View.VISIBLE
                        binding.textviewHomeFavoriteEmpty.visibility = View.GONE
                        val recentFavoriteIds = validFavoriteIds.reversed().take(1)
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
                                    Toast.makeText(context, "[즐겨찾기] 글 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(context, "[즐겨찾기] 사용자 정보 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                showEmptyFavorites()
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
            popularAdapter.submitList(popularList.toList())
        }

        fetchHomeFavorites()

        val updateTask = if (newFavoriteState) {
            userDocRef.update("favorites", FieldValue.arrayUnion(noticeDocId))
        } else {
            userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
        }
        updateTask.addOnFailureListener {
            Toast.makeText(context, "즐겨찾기 상태 변경 실패 (DB)", Toast.LENGTH_SHORT).show()
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
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            Toast.makeText(context, "페이지를 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}