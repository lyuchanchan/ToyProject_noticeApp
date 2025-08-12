package com.example.toyproject_noticeapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.databinding.FragmentHomeScreenBinding

// 1. HomeFragment: 홈 화면의 메인 로직 담당
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupShortcutsRecyclerView()
        setupNotificationsRecyclerView()

        // '최근 알림' 헤더 클릭 시 알림 페이지로 이동 (임시 토스트 메시지)
        binding.layoutHomeNotificationHeader.setOnClickListener {
            Toast.makeText(context, "알림 페이지로 이동", Toast.LENGTH_SHORT).show()
            // 추후 Navigation Component 등으로 페이지 이동 로직 구현
        }
    }

    // 툴바 버튼 클릭 리스너 설정
    private fun setupToolbar() {
        binding.toolbarHomeTop.setNavigationOnClickListener {
            Toast.makeText(context, "로그인 페이지로 이동", Toast.LENGTH_SHORT).show()
        }
        binding.buttonHomeSearch.setOnClickListener {
            Toast.makeText(context, "검색 페이지로 이동", Toast.LENGTH_SHORT).show()
        }
        binding.buttonHomeSetting.setOnClickListener {
            Toast.makeText(context, "설정 페이지로 이동", Toast.LENGTH_SHORT).show()
        }
    }

// HomeFragment.kt 파일의 일부입니다.

    // '바로가기' RecyclerView 설정
    private fun setupShortcutsRecyclerView() {
        // 표시할 데이터 생성 (Lucide 아이콘으로 교체)
        val shortcutItems = listOf(
            ShortcutItem(R.drawable.ic_lucide_home, "홈페이지"),
            ShortcutItem(R.drawable.ic_lucide_calendar_days, "학사일정"),
            ShortcutItem(R.drawable.ic_lucide_book_marked, "학사공지"),
            ShortcutItem(R.drawable.ic_lucide_megaphone, "공지사항"),
            ShortcutItem(R.drawable.ic_lucide_party_popper, "행사공지"),
            ShortcutItem(R.drawable.ic_lucide_graduation_cap, "장학일정"),
            ShortcutItem(R.drawable.ic_lucide_briefcase, "취업공지"),
            ShortcutItem(R.drawable.ic_lucide_utensils, "식단표"),
            ShortcutItem(R.drawable.ic_lucide_bus, "셔틀버스"),
            ShortcutItem(R.drawable.ic_lucide_cpu, "AISW공지"),
            ShortcutItem(R.drawable.ic_lucide_laptop, "SW중심대학")
        )

        val shortcutAdapter = ShortcutAdapter(shortcutItems) { shortcut ->
            // 바로가기 아이템 클릭 시 동작 (URL 연결 등)
            Toast.makeText(context, "${shortcut.title} 클릭!", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerHomeShortcuts.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = shortcutAdapter
        }
    }

    // '최근 알림' RecyclerView 설정
    private fun setupNotificationsRecyclerView() {
        // 표시할 데이터 생성 (임시 데이터)
        // 추후 파이썬으로 크롤링한 실제 데이터로 교체
        val notificationItems = listOf(
            NotificationItem("학사공지", true, "2024학년도 2학기 기말고사 일정 안내", "2024-12-15"),
            NotificationItem("시설공지", true, "겨울방학 도서관 운영시간 변경 안내", "2024-12-14"),
            NotificationItem("장학공지", false, "2025학년도 1학기 국가장학금 신청 안내", "2024-12-11")
        )
        val notificationAdapter = NotificationAdapter(notificationItems)
        binding.recyclerHomeNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


// 2. 바로가기 메뉴 데이터 클래스
data class ShortcutItem(val iconResId: Int, val title: String)

// 3. 바로가기 메뉴 어댑터
class ShortcutAdapter(
    private val items: List<ShortcutItem>,
    private val onItemClick: (ShortcutItem) -> Unit
) : RecyclerView.Adapter<ShortcutAdapter.ShortcutViewHolder>() {

    class ShortcutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.image_item_shortcut_icon)
        val title: TextView = view.findViewById(R.id.text_item_shortcut_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_shortcut, parent, false)
        return ShortcutViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconResId)
        holder.title.text = item.title
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size
}


// 4. 최근 알림 데이터 클래스
data class NotificationItem(
    val category: String,
    val isNew: Boolean,
    val title: String,
    val date: String
)

// 5. 최근 알림 어댑터
class NotificationAdapter(private val items: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val category: TextView = view.findViewById(R.id.text_item_notification_category)
        val newTag: TextView = view.findViewById(R.id.text_item_notification_new)
        val title: TextView = view.findViewById(R.id.text_item_notification_title)
        val date: TextView = view.findViewById(R.id.text_item_notification_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = items[position]
        holder.category.text = item.category
        holder.title.text = item.title
        holder.date.text = item.date
        holder.newTag.isVisible = item.isNew // 'NEW' 태그 보임/숨김 처리
    }

    override fun getItemCount() = items.size
}