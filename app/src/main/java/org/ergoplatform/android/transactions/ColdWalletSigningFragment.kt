package org.ergoplatform.android.transactions

import StageConstants
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import org.ergoplatform.ErgoAmount
import org.ergoplatform.android.databinding.EntryOutboxBinding
import org.ergoplatform.android.databinding.FragmentColdWalletSigningBinding
import org.ergoplatform.appkit.Address
import scala.collection.JavaConversions

/**
 * Scans cold wallet signing request qr codes, signs the transaction, presents a qr code to go back
 */
class ColdWalletSigningFragment : Fragment() {

    var _binding: FragmentColdWalletSigningBinding? = null
    val binding get() = _binding!!

    private val args: ColdWalletSigningFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColdWalletSigningBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(this).get(ColdWalletSigningViewModel::class.java)

        args.qrCode?.let { viewModel.addQrCodeChunk(it) }
        viewModel.setWalletId(args.walletId, requireContext())

        viewModel.reducedTx.observe(viewLifecycleOwner, {
            it?.let {
                binding.transactionInfo.visibility = View.VISIBLE

                binding.layoutOutboxes.apply {
                    removeAllViews()

                    val outputs = JavaConversions.seqAsJavaList(it.outputCandidates())!!
                    outputs.forEach { ergoBoxCandidate ->
                        val outboxBinding = EntryOutboxBinding.inflate(layoutInflater, this, true)
                        outboxBinding.outboxErgAmount.amount =
                            ErgoAmount(ergoBoxCandidate.value()).toDouble()
                        outboxBinding.labelOutboxAddress.text =
                            Address.fromErgoTree(
                                ergoBoxCandidate.ergoTree(),
                                StageConstants.NETWORK_TYPE
                            ).toString()

                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}