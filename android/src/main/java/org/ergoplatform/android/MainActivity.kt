package org.ergoplatform.android

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.ergoplatform.android.transactions.ChooseSpendingWalletFragmentDialog
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.postDelayed
import org.ergoplatform.android.wallet.WalletFragmentDirections
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
                R.id.navigation_wallet,
                R.id.navigation_mosaik,
                R.id.navigation_settings
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

        findViewById<Button>(R.id.button_unlock).setOnClickListener { showBiometricPrompt() }

        if (walletApp?.shouldLockApp == true)
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (isActive) {
                        delay(1000 * 60)
                        if (walletApp?.isAppLocked() == true)
                            lockApp()
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()

        // removes pending notifications from system bar
        NotificationManagerCompat.from(this).cancel(BackgroundSync.NOTIF_ID_SYNC)

        if (walletApp?.isAppLocked() == false)
            unlockApp()
        else
            lockApp()
    }

    private fun unlockApp() {
        findViewById<View>(R.id.layout_app_locked).visibility = View.GONE
    }

    private fun lockApp() {
        findViewById<View>(R.id.layout_app_locked).visibility = View.VISIBLE
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

        MainAppUiLogic.handleRequests(data, fromQrCode, AndroidStringProvider(this),
            navigateToChooseWalletDialog = {
                navController.navigate(
                    R.id.chooseSpendingWalletFragmentDialog,
                    ChooseSpendingWalletFragmentDialog.buildArgs(it)
                )
            },
            navigateToErgoPay = {
                navController.navigate(
                    WalletFragmentDirections.actionNavigationWalletToErgoPaySigningFragment(
                        it
                    ).setCloseApp(!fromQrCode)
                )
            },
            navigateToAuthentication = {
                navController.navigate(
                    WalletFragmentDirections.actionNavigationWalletToErgoAuthFragment(
                        it
                    )
                )
            },
            navigateToMosaikApp = { url ->
                navController.navigate(
                    WalletFragmentDirections.actionNavigationWalletToMosaikFragment(url, null)
                )
            },
            presentUserMessage = {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        walletApp?.userInteracted()
    }

    private fun showBiometricPrompt() {

        // setDeviceCredentialAllowed is deprecated, but needed for older SDK level
        @Suppress("DEPRECATION") val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.title_authenticate))
            .setConfirmationRequired(false)
            .setDeviceCredentialAllowed(true)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                walletApp?.appUnlocked()
                unlockApp()
            }
        }

        try {
            BiometricPrompt(this, callback).authenticate(promptInfo)
        } catch (t: Throwable) {

        }
    }

    private val walletApp get() = (application as? App)
}