package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentChooseTokenDialogBinding
import org.ergoplatform.android.databinding.FragmentChooseTokenDialogItemBinding
import org.ergoplatform.persistance.WalletToken

/**
 * Let the user choose one or more token(s) from the available tokens
 */
class ChooseTokenListDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentChooseTokenDialogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChooseTokenDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager =
            LinearLayoutManager(context)

        val viewModel =
            ViewModelProvider(parentFragment as ViewModelStoreOwner)
                .get(SendFundsViewModel::class.java)
        val tokensToChooseFrom = viewModel.uiLogic.getTokensToChooseFrom()
        binding.list.adapter = DisplayTokenAdapter(tokensToChooseFrom)
    }

    private fun onChooseToken(tokenId: String) {
        ViewModelProvider(parentFragment as ViewModelStoreOwner).get(SendFundsViewModel::class.java)
            .uiLogic.newTokenChosen(tokenId)
        dismiss()
    }

    private inner class ViewHolder(binding: FragmentChooseTokenDialogItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val tokenName: TextView = binding.labelTokenName
        val tokenId: TextView = binding.labelTokenId
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
            holder.tokenName.text = token.name ?: getString(R.string.label_unnamed_token)
            holder.tokenId.text = token.tokenId
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