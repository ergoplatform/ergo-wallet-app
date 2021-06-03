package org.ergoplatform.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.android.ErgoFacade
import org.ergoplatform.android.databinding.FragmentWalletBinding
import java.io.InputStreamReader

class WalletFragment : Fragment() {

    private lateinit var walletViewModel: WalletViewModel

    private var _binding: FragmentWalletBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        walletViewModel =
            ViewModelProvider(this).get(WalletViewModel::class.java)
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        val root = binding.root
        binding.walletCard.walletSettings.setOnClickListener {
            binding.ergoLogo.visibility = View.GONE
        }
        return root
    }


    private fun runOnErgo() {
        val configPath = "config/freeze_coin_config.json"
        GlobalScope.launch(Dispatchers.Main) {
            val tx = withContext(Dispatchers.IO) {
                val inputStream = requireContext().assets.open(configPath)
                val configReader = InputStreamReader(inputStream)
                ErgoFacade.sendTx(1000000000, configReader)
            }
            System.out.println(tx)
            Snackbar.make(requireView(), "Tx: $tx", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }
}