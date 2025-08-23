package com.example.toyproject_noticeapp.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SettingMainFragment : Fragment() {

    private var _binding: FragmentSettingMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var includeKeywordAdapter: SettingKeywordAdapter
    private lateinit var excludeKeywordAdapter: SettingKeywordAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    // 임시 데이터
    private val subscriptionList = mutableListOf(
        Subscription("공지사항", true),
        Subscription("학사공지", true),
        Subscription("행사공지", false),
        Subscription("장학공지", true),
        Subscription("취업공지", false),
        Subscription("AISW계열 공지사항", true),
        Subscription("SW중심대학 공지사항", true)
    )
    private val includeKeywordList = mutableListOf(
        Keyword("장학금"),
        Keyword("기숙사"),
        Keyword("수강신청")
    )
    private val excludeKeywordList = mutableListOf(
        Keyword("연장"),
        Keyword("마감")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserInfo()
        setupToolbar()
        setupSubscriptionRecyclerView()
        setupIncludeKeywordRecyclerView()
        setupExcludeKeywordRecyclerView()
        setupClickListeners()
    }

    private fun setupUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Firestore에서 사용자 이름 가져오기
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userName = document.getString("name")
                        if (!userName.isNullOrEmpty()) {
                            binding.textviewSettingName.text = "${userName}님"
                        } else {
                            binding.textviewSettingName.text = "이름 없음"
                        }
                    } else {
                        binding.textviewSettingName.text = "이름 없음"
                    }
                }
                .addOnFailureListener {
                    binding.textviewSettingName.text = "이름을 불러올 수 없음"
                }

            binding.textviewSettingEmail.text = currentUser.email ?: "이메일 정보 없음"
        } else {
            startActivity(Intent(requireActivity(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun setupToolbar() {
        binding.toolbarSettingMain.toolbar.title = "설정"
        binding.toolbarSettingMain.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbarSettingMain.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSubscriptionRecyclerView() {
        val subscriptionAdapter = SettingSubscriptionAdapter { subscription, isChecked ->
            Toast.makeText(
                requireContext(),
                "${subscription.name} 알림 ${if (isChecked) "ON" else "OFF"}",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.recyclerviewSettingSubscriptions.adapter = subscriptionAdapter
        subscriptionAdapter.submitList(subscriptionList)
    }

    private fun setupIncludeKeywordRecyclerView() {
        includeKeywordAdapter = SettingKeywordAdapter { keyword ->
            includeKeywordList.remove(keyword)
            includeKeywordAdapter.submitList(includeKeywordList.toList())
            Toast.makeText(requireContext(), "${keyword.text} 삭제됨", Toast.LENGTH_SHORT).show()
        }
        val layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }
        binding.recyclerviewSettingIncludeKeywords.layoutManager = layoutManager
        binding.recyclerviewSettingIncludeKeywords.adapter = includeKeywordAdapter
        includeKeywordAdapter.submitList(includeKeywordList.toList())
    }

    private fun setupExcludeKeywordRecyclerView() {
        excludeKeywordAdapter = SettingKeywordAdapter { keyword ->
            excludeKeywordList.remove(keyword)
            excludeKeywordAdapter.submitList(excludeKeywordList.toList())
            Toast.makeText(
                requireContext(),
                "제외 키워드 '${keyword.text}'가 삭제되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
        val layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }
        binding.recyclerviewSettingExcludeKeywords.layoutManager = layoutManager
        binding.recyclerviewSettingExcludeKeywords.adapter = excludeKeywordAdapter
        excludeKeywordAdapter.submitList(excludeKeywordList.toList())
    }

    private fun setupClickListeners() {
        binding.buttonSettingLogout.setOnClickListener {
            val sharedPreferences =
                requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            FirebaseAuth.getInstance().signOut()

            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            requireActivity().finish()
        }

        binding.buttonAddIncludeKeyword.setOnClickListener {
            val keywordText = binding.edittextIncludeKeyword.text.toString().trim()
            if (keywordText.isNotEmpty()) {
                includeKeywordList.add(Keyword(keywordText))
                includeKeywordAdapter.submitList(includeKeywordList.toList())
                binding.edittextIncludeKeyword.text.clear()
                hideKeyboard()
            } else {
                Toast.makeText(requireContext(), "키워드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonAddExcludeKeyword.setOnClickListener {
            val keywordText = binding.edittextExcludeKeyword.text.toString().trim()
            if (keywordText.isNotEmpty()) {
                excludeKeywordList.add(Keyword(keywordText))
                excludeKeywordAdapter.submitList(excludeKeywordList.toList())
                binding.edittextExcludeKeyword.text.clear()
                hideKeyboard()
            } else {
                Toast.makeText(requireContext(), "제외할 키워드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
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