package org.ergoplatform.android.ui.wallet

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.android.ErgoFacade
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentWalletBinding
import java.io.InputStreamReader

class WalletFragment : Fragment() {

    private lateinit var walletViewModel: WalletViewModel

    private var _binding: FragmentWalletBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        walletViewModel =
            ViewModelProvider(this).get(WalletViewModel::class.java)
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        val root = binding.root
        binding.walletCard.walletSettings.setOnClickListener {
            binding.ergoLogo.visibility = View.GONE
        }
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_wallet, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_wallet -> {
                findNavController().navigate(R.id.action_to_addWalletChooserFragment)
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
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