package org.ergoplatform.android

import StageConstants
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.wallet.WalletStateDbEntity
import org.ergoplatform.api.ErgoApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class NodeConnector() {

    val isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val refreshNum: MutableLiveData<Int> = MutableLiveData()
    private var lastRefresMs: Long = 0
    val service: ErgoApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(StageConstants.EXPLORER_API_ADDRESS)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(ErgoApi::class.java)
    }

    fun invalidateCache() {
        lastRefresMs = 0
    }

    fun refreshByUser(context: Context): Boolean {
        if (System.currentTimeMillis() - lastRefresMs > 1000L * 10) {
            refreshNow(context)
            return true
        } else
            return false
    }

    fun refreshWhenNeeded(context: Context) {
        if (System.currentTimeMillis() - lastRefresMs > 1000L * 60) {
            refreshNow(context)
        }
    }

    private fun refreshNow(context: Context) {
        if (!(isRefreshing.value ?: false)) {
            isRefreshing.postValue(true)
            GlobalScope.launch(Dispatchers.IO) {
                // Refresh Ergo fiat value


                // Refresh wallet states
                try {
                    val statesToSave = mutableListOf<WalletStateDbEntity>()
                    val walletDao = AppDatabase.getInstance(context).walletDao()
                    walletDao.getAllSync().forEach { walletConfig ->
                        val transactionsInfo =
                            service.addressesIdGet(walletConfig.publicAddress).execute()
                                .body()?.transactions

                        val newState = WalletStateDbEntity(
                            walletConfig.id, transactionsInfo?.confirmed,
                            transactionsInfo?.confirmedBalance, transactionsInfo?.totalBalance
                        )

                        statesToSave.add(newState)
                    }

                    walletDao.insertWalletStates(*statesToSave.toTypedArray())
                } catch (t: Throwable) {
                    Log.e("Nodeconnector", "Error", t)
                    // TODO report to user
                }

                lastRefresMs = System.currentTimeMillis()
                isRefreshing.postValue(false)
            }
        }
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: NodeConnector? = null

        fun getInstance(): NodeConnector {
            return instance ?: synchronized(this) {
                instance ?: NodeConnector().also { instance = it }
            }
        }

    }
}