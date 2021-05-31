package org.ergoplatform.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.android.ErgoFacade
import org.ergoplatform.android.R
import java.io.InputStreamReader

class WalletFragment : Fragment() {

    private lateinit var walletViewModel: WalletViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        walletViewModel =
                ViewModelProvider(this).get(WalletViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val textView: TextView = root.findViewById(R.id.text_dashboard)
        walletViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
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