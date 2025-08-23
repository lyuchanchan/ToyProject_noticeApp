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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FavoriteListFragment : Fragment() {

    private var _binding: FragmentFavoriteListBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoriteAdapter: AdapterNotificationList

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoriteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        fetchFavorites()
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
                updateFavoriteStatus(notice)
            }
        )
        binding.recyclerviewFavoriteList.adapter = favoriteAdapter
        binding.recyclerviewFavoriteList.layoutManager = LinearLayoutManager(context)
    }

    private fun fetchFavorites() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val favoriteIds = document.get("favorites") as? List<String>
                    if (!favoriteIds.isNullOrEmpty()) {
                        db.collection("notices").whereIn(com.google.firebase.firestore.FieldPath.documentId(), favoriteIds).get()
                            .addOnSuccessListener { noticeDocuments ->
                                val favoriteList = noticeDocuments.mapNotNull { doc ->
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
                                val sortedList = favoriteList.sortedByDescending { notice ->
                                    favoriteIds.indexOf("${notice.category}_${notice.id}")
                                }
                                favoriteAdapter.submitList(sortedList)
                            }
                    } else {
                        favoriteAdapter.submitList(emptyList())
                    }
                }
            }
    }

    private fun updateFavoriteStatus(notice: DataNotificationItem) {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)
        val noticeDocId = "${notice.category}_${notice.id}"

        userDocRef.update("favorites", FieldValue.arrayRemove(noticeDocId))
            .addOnSuccessListener {
                val currentList = favoriteAdapter.currentList.toMutableList()
                currentList.removeAll { it.id == notice.id && it.category == notice.category }
                favoriteAdapter.submitList(currentList)
            }
            .addOnFailureListener {
                Toast.makeText(context, "즐겨찾기 해제 실패", Toast.LENGTH_SHORT).show()
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