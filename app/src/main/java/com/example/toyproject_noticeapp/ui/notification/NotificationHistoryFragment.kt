package com.example.toyproject_noticeapp.ui.notification

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
import com.example.toyproject_noticeapp.databinding.FragmentNotificationHistoryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class NotificationHistoryFragment : Fragment() {

    private var _binding: FragmentNotificationHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationAdapter: AdapterNotificationList

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        loadNotificationHistory()
    }

    private fun setupToolbar() {
        binding.toolbarNotificationHistory.toolbar.title = "알림 내역"
        binding.toolbarNotificationHistory.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarNotificationHistory.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = AdapterNotificationList(
            onItemClick = { notice -> openInAppBrowser(notice.url) },
            onFavoriteClick = {
                Toast.makeText(context, "이 화면에서는 즐겨찾기를 변경할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerviewNotificationHistoryList.adapter = notificationAdapter
        binding.recyclerviewNotificationHistoryList.layoutManager = LinearLayoutManager(context)
    }

    private fun loadNotificationHistory() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).collection("notification_history")
            .orderBy("timestamp", Query.Direction.DESCENDING) // "DESCENDING"이 최신순 정렬입니다.
            .get()
            .addOnSuccessListener { documents ->
                // ❗️ 핵심: Firestore가 정렬해준 순서를 100% 보장하기 위해
                // documents를 직접 map으로 순회하여 새 리스트를 만듭니다.
                val historyList = documents.map { doc ->
                    doc.toObject(DataNotificationItem::class.java)
                }
                notificationAdapter.submitList(historyList)
            }
            .addOnFailureListener {
                Toast.makeText(context, "알림 내역을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
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