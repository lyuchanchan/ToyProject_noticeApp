// app/src/main/java/com/example/toyproject_noticeapp/ui/setting/SettingMainFragment.kt
package com.example.toyproject_noticeapp.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.toyproject_noticeapp.R
import com.example.toyproject_noticeapp.adapter.SettingKeywordAdapter
import com.example.toyproject_noticeapp.adapter.SettingSubscriptionAdapter
import com.example.toyproject_noticeapp.data.Keyword
import com.example.toyproject_noticeapp.data.Subscription
import com.example.toyproject_noticeapp.databinding.FragmentSettingMainBinding
import com.example.toyproject_noticeapp.ui.login.LoginActivity
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SettingMainFragment : Fragment() {

    private var _binding: FragmentSettingMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var includeKeywordAdapter: SettingKeywordAdapter
    private lateinit var excludeKeywordAdapter: SettingKeywordAdapter
    private lateinit var subscriptionAdapter: SettingSubscriptionAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private val userDocRef by lazy { db.collection("users").document(auth.currentUser!!.uid) }

    // --- 👇 *** 여기가 핵심 수정 사항입니다! *** 👇 ---
    private val allSubscriptionNames = listOf(
        "공지사항", "학사공지", "행사공지", "장학공지", "취업공지" // "도서관" 제거
    )
    // ---------------------------------------------

    private val subscriptionList = mutableListOf<Subscription>()
    private val includeKeywordList = mutableListOf<Keyword>()
    private val excludeKeywordList = mutableListOf<Keyword>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        loadUserSettings()
    }

    private fun loadUserSettings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        binding.textviewSettingEmail.text = currentUser.email ?: "이메일 정보 없음"

        userDocRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val userName = document.getString("name")
                binding.textviewSettingName.text = if (!userName.isNullOrEmpty()) "${userName}님" else "이름 없음"

                val subscribed = document.get("subscriptions") as? List<String> ?: allSubscriptionNames
                subscriptionList.clear()
                subscriptionList.addAll(allSubscriptionNames.map { Subscription(it, subscribed.contains(it)) })
                subscriptionAdapter.submitList(subscriptionList.toList())

                val includeKeywords = document.get("includeKeywords") as? List<String> ?: emptyList()
                includeKeywordList.clear()
                includeKeywordList.addAll(includeKeywords.map { Keyword(it) })
                includeKeywordAdapter.submitList(includeKeywordList.toList())

                val excludeKeywords = document.get("excludeKeywords") as? List<String> ?: emptyList()
                excludeKeywordList.clear()
                excludeKeywordList.addAll(excludeKeywords.map { Keyword(it) })
                excludeKeywordAdapter.submitList(excludeKeywordList.toList())
            } else {
                val defaultSubscriptions = allSubscriptionNames
                val initialSettings = hashMapOf(
                    "name" to (currentUser.displayName ?: ""),
                    "email" to currentUser.email,
                    "subscriptions" to defaultSubscriptions,
                    "includeKeywords" to emptyList<String>(),
                    "excludeKeywords" to emptyList<String>()
                )
                userDocRef.set(initialSettings).addOnSuccessListener {
                    loadUserSettings()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "설정 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        binding.toolbarSettingMain.toolbar.title = "설정"
        binding.toolbarSettingMain.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarSettingMain.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerViews() {
        subscriptionAdapter = SettingSubscriptionAdapter { subscription, isChecked ->
            val updateData: Map<String, Any> = hashMapOf(
                "subscriptions" to if (isChecked) {
                    FieldValue.arrayUnion(subscription.name)
                } else {
                    FieldValue.arrayRemove(subscription.name)
                }
            )
            userDocRef.update(updateData)
        }
        binding.recyclerviewSettingSubscriptions.adapter = subscriptionAdapter

        includeKeywordAdapter = SettingKeywordAdapter { keyword ->
            userDocRef.update("includeKeywords", FieldValue.arrayRemove(keyword.text))
            includeKeywordList.remove(keyword)
            includeKeywordAdapter.submitList(includeKeywordList.toList())
        }
        binding.recyclerviewSettingIncludeKeywords.layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }
        binding.recyclerviewSettingIncludeKeywords.adapter = includeKeywordAdapter

        excludeKeywordAdapter = SettingKeywordAdapter { keyword ->
            userDocRef.update("excludeKeywords", FieldValue.arrayRemove(keyword.text))
            excludeKeywordList.remove(keyword)
            excludeKeywordAdapter.submitList(excludeKeywordList.toList())
        }
        binding.recyclerviewSettingExcludeKeywords.layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }
        binding.recyclerviewSettingExcludeKeywords.adapter = excludeKeywordAdapter
    }

    private fun setupClickListeners() {
        binding.buttonSettingLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        binding.buttonAddIncludeKeyword.setOnClickListener {
            val keywordText = binding.edittextIncludeKeyword.text.toString().trim()
            if (keywordText.isNotEmpty()) {
                userDocRef.update("includeKeywords", FieldValue.arrayUnion(keywordText))
                includeKeywordList.add(Keyword(keywordText))
                includeKeywordAdapter.submitList(includeKeywordList.toList())
                binding.edittextIncludeKeyword.text.clear()
                hideKeyboard()
            }
        }

        binding.buttonAddExcludeKeyword.setOnClickListener {
            val keywordText = binding.edittextExcludeKeyword.text.toString().trim()
            if (keywordText.isNotEmpty()) {
                userDocRef.update("excludeKeywords", FieldValue.arrayUnion(keywordText))
                excludeKeywordList.add(Keyword(keywordText))
                excludeKeywordAdapter.submitList(excludeKeywordList.toList())
                binding.edittextExcludeKeyword.text.clear()
                hideKeyboard()
            }
        }
    }

    private fun goToLogin() {
        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}