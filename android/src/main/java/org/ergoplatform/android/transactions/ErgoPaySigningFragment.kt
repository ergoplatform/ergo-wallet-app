package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.databinding.FragmentErgoPaySigningBinding
import org.ergoplatform.android.ui.AbstractAuthenticationFragment
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic
import java.lang.IllegalStateException

class ErgoPaySigningFragment : AbstractAuthenticationFragment() {
    private var _binding: FragmentErgoPaySigningBinding? = null
    private val binding get() = _binding!!

    private val args: ErgoPaySigningFragmentArgs by navArgs()

    private val viewModel: ErgoPaySigningViewModel
        get() {
            return ViewModelProvider(this).get(ErgoPaySigningViewModel::class.java)
        }

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
        viewModel.uiLogic.init(args.request, args.address, Preferences(requireContext()))

        viewModel.uiStateRefresh.observe(viewLifecycleOwner, { state ->
            binding.transactionInfo.root.visibility =
                visibleWhen(state, ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION)
            binding.layoutProgress.visibility =
                visibleWhen(state, ErgoPaySigningUiLogic.State.FETCH_DATA)

            when (state) {
                ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS -> TODO()
                ErgoPaySigningUiLogic.State.FETCH_DATA -> showFetchData()
                ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION -> showTransactionInfo()
                ErgoPaySigningUiLogic.State.DONE -> showDoneInfo()
                null -> throw IllegalStateException("Not allowed")
            }
        })
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
        // TODO Ergo Pay Show done info and/or error messages
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