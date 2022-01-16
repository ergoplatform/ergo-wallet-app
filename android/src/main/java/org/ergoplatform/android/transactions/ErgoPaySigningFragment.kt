package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import org.ergoplatform.MessageSeverity
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.databinding.FragmentErgoPaySigningBinding
import org.ergoplatform.android.ui.AbstractAuthenticationFragment
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic

class ErgoPaySigningFragment : SubmitTransactionFragment() {
    private var _binding: FragmentErgoPaySigningBinding? = null
    private val binding get() = _binding!!

    private val args: ErgoPaySigningFragmentArgs by navArgs()

    override val viewModel: ErgoPaySigningViewModel
        get() = ViewModelProvider(this).get(ErgoPaySigningViewModel::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErgoPaySigningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewModel = this.viewModel
        val context = requireContext()
        viewModel.uiLogic.init(
            args.request,
            args.walletId,
            args.derivationIdx,
            RoomWalletDbProvider(AppDatabase.getInstance(context)),
            Preferences(context)
        )

        viewModel.uiStateRefresh.observe(viewLifecycleOwner, { state ->
            binding.transactionInfo.root.visibility =
                visibleWhen(state, ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION)
            binding.layoutProgress.visibility =
                visibleWhen(state, ErgoPaySigningUiLogic.State.FETCH_DATA)
            binding.layoutDoneInfo.visibility =
                visibleWhen(state, ErgoPaySigningUiLogic.State.DONE)

            when (state) {
                ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS -> TODO()
                ErgoPaySigningUiLogic.State.FETCH_DATA -> showFetchData()
                ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION -> showTransactionInfo()
                ErgoPaySigningUiLogic.State.DONE -> showDoneInfo()
                null -> throw IllegalStateException("Not allowed")
            }
        })

        binding.transactionInfo.buttonSignTx.setOnClickListener {
            startAuthFlow(viewModel.uiLogic.wallet!!.walletConfig)
        }
    }

    private fun visibleWhen(
        state: ErgoPaySigningUiLogic.State?,
        visibleWhen: ErgoPaySigningUiLogic.State
    ) =
        if (state == visibleWhen) View.VISIBLE else View.GONE

    private fun showFetchData() {
        // nothing special to do yet
    }

    private fun showDoneInfo() {
        // TODO Ergo Pay use normal tx done message in case of success
        binding.tvMessage.text = viewModel.uiLogic.lastMessage
        binding.tvMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            when (viewModel.uiLogic.lastMessageSeverity) {
                MessageSeverity.NONE -> 0
                MessageSeverity.INFORMATION -> R.drawable.ic_info_24
                MessageSeverity.WARNING -> R.drawable.ic_warning_amber_24
                MessageSeverity.ERROR -> R.drawable.ic_error_outline_24
            },
            0, 0
        )
    }

    private fun showTransactionInfo() {
        binding.transactionInfo.bindTransactionInfo(
            viewModel.uiLogic.transactionInfo!!.reduceBoxes(),
            layoutInflater
        )
    }

    override fun proceedAuthFlowFromBiometrics() {
        TODO("Not yet implemented")
    }

    override fun proceedAuthFlowWithPassword(password: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}