package com.example.toyproject_noticeapp.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.toyproject_noticeapp.databinding.FragmentCommonToolbarBinding

class FragmentCommonToolbar : Fragment() {

    private var _binding: FragmentCommonToolbarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommonToolbarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기 버튼 클릭 시, 현재 액티비티를 종료
        binding.buttonToolbarBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    // 다른 프래그먼트에서 타이틀을 설정할 수 있는 함수
    fun setToolbarTitle(title: String) {
        binding.textviewToolbarTitle.text = title
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}