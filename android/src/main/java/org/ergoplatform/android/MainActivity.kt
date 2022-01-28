package org.ergoplatform.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.integration.android.IntentIntegrator
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.ergoplatform.android.transactions.ChooseSpendingWalletFragmentDialog
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.postDelayed
import org.ergoplatform.uilogic.MainAppUiLogic

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        setSupportActionBar(findViewById(R.id.toolbar))

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_wallet, R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        KeyboardVisibilityEvent.registerEventListener(this, { isOpen ->
            navView.visibility = if (isOpen) View.GONE else View.VISIBLE
        })

        if (savedInstanceState == null) {
            handleIntent(navController)
        }
    }

    fun scanQrCode() {
        IntentIntegrator(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                // post on Main thread, otherwise navigate() not working
                postDelayed(0) { handleRequests(it, true) }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleRequests(
        data: String,
        fromQrCode: Boolean,
        optNavController: NavController? = null
    ) {
        val navController = optNavController ?: findNavController(R.id.nav_host_fragment)

        MainAppUiLogic.handleRequests(data, fromQrCode, AndroidStringProvider(this), {
            navController.navigate(
                R.id.chooseSpendingWalletFragmentDialog,
                ChooseSpendingWalletFragmentDialog.buildArgs(it)
            )
        }, {
            MaterialAlertDialogBuilder(this)
                .setMessage(it)
                .setPositiveButton(R.string.zxing_button_ok, null)
                .show()
        })
    }

    private fun handleIntent(navController: NavController? = null) {
        intent.dataString?.let {
            handleRequests(it, false, navController)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp() || super.onSupportNavigateUp()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent()
    }
}