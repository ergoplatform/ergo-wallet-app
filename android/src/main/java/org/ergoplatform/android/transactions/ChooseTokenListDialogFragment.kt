package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.databinding.FragmentChooseTokenDialogBinding
import org.ergoplatform.android.databinding.FragmentChooseTokenDialogItemBinding
import org.ergoplatform.android.tokens.ChooseTokenEntryView
import org.ergoplatform.persistance.WalletToken

/**
 * Let the user choose one or more token(s) from the available tokens
 */
class ChooseTokenListDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentChooseTokenDialogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: SendFundsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChooseTokenDialogBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(parentFragment as ViewModelStoreOwner)
            .get(SendFundsViewModel::class.java)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager =
            LinearLayoutManager(context)

        val tokensToChooseFrom = viewModel.uiLogic.getTokensToChooseFrom()
        binding.list.adapter = DisplayTokenAdapter(tokensToChooseFrom)
    }

    private fun onChooseToken(tokenId: String) {
        viewModel.uiLogic.newTokenChosen(tokenId)
        dismiss()
    }

    private class ViewHolder(val binding: FragmentChooseTokenDialogItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var entryView: ChooseTokenEntryView? = null
    }

    private inner class DisplayTokenAdapter(private val items: List<WalletToken>) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            return ViewHolder(
                FragmentChooseTokenDialogItemBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val token = items.get(position)
            holder.entryView = ChooseTokenEntryView(holder.binding, token)
            holder.entryView?.bind(token.tokenId?.let { viewModel.uiLogic.tokensInfo.get(it) })
            holder.itemView.setOnClickListener { onChooseToken(token.tokenId!!) }
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}