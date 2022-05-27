package org.ergoplatform.android.mosaik

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.ergoplatform.android.databinding.FragmentMosaikBinding
import org.ergoplatform.mosaik.MosaikViewTree

class MosaikFragment : Fragment() {
    private var _binding: FragmentMosaikBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MosaikViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMosaikBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.setContent {
            // TODO set correct colors
            // FIXME images not loading
            MaterialTheme {
                MosaikViewTree(viewModel.uiLogic.viewTree)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}