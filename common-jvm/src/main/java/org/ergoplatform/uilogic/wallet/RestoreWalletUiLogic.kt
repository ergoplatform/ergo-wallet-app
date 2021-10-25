package org.ergoplatform.uilogic.wallet

import org.ergoplatform.android.MNEMONIC_MIN_WORDS_COUNT
import org.ergoplatform.android.loadAppKitMnemonicWordList
import org.ergoplatform.appkit.Mnemonic
import org.ergoplatform.appkit.MnemonicValidationException
import org.ergoplatform.uilogic.STRING_MNEMONIC_INVALID
import org.ergoplatform.uilogic.STRING_MNEMONIC_LENGTH_NOT_ENOUGH
import org.ergoplatform.uilogic.STRING_MNEMONIC_UNKNOWN_WORDS
import org.ergoplatform.uilogic.StringProvider
import java.util.*

/**
 * UI Logic Restores a formerly generated wallet from mnemonic
 */
abstract class RestoreWalletUiLogic(private val stringProvider: StringProvider) {
    private val wordList = ArrayList(loadAppKitMnemonicWordList())
    private var isSecondButtonClick = false

    fun userChangedMnemonic() {
        isSecondButtonClick = false
        val words = countMnemonicWords()

        // check for invalid words, but skip the last one when it is not completed yet
        val hasInvalidWords = words > 0 && mnemonicToWords(getMnemonic())
            .dropLast(if (getEnteredMnemonic()!!.endsWith(" ")) 0 else 1)
            .filter { word -> Collections.binarySearch(wordList, word) < 0 }
            .isNotEmpty()

        if (hasInvalidWords) {
            setErrorLabel(stringProvider.getString(STRING_MNEMONIC_UNKNOWN_WORDS))
        } else if (words in 1 until MNEMONIC_MIN_WORDS_COUNT) {
            setErrorLabel(
                stringProvider.getString(
                    STRING_MNEMONIC_LENGTH_NOT_ENOUGH,
                    (MNEMONIC_MIN_WORDS_COUNT - words).toString()
                )
            )
        } else
            setErrorLabel(null)
    }

    private fun countMnemonicWords(): Int {
        val mnemonic = getMnemonic()
        val words = if (mnemonic.isEmpty()) 0 else mnemonicToWords(mnemonic).size
        return words
    }

    private fun mnemonicToWords(mnemonic: String): List<String> {
        return mnemonic.split(" ")
    }

    fun doRestore() {
        val wordsCount = countMnemonicWords()
        if (wordsCount >= MNEMONIC_MIN_WORDS_COUNT) {
            hideForcedSoftKeyboard()

            val mnemonic = getMnemonic()
            var mnemonicIsValid = true

            try {
                Mnemonic.checkEnglishMnemonic(mnemonicToWords(mnemonic))
            } catch (e: MnemonicValidationException) {
                mnemonicIsValid = false
            }

            if (!isSecondButtonClick && !mnemonicIsValid) {
                isSecondButtonClick = true
                setErrorLabel(stringProvider.getString(STRING_MNEMONIC_INVALID))
            } else {
                navigateToSaveWalletDialog(mnemonic)
            }
        } else {
            setErrorLabel(
                stringProvider.getString(
                    STRING_MNEMONIC_LENGTH_NOT_ENOUGH,
                    (MNEMONIC_MIN_WORDS_COUNT - wordsCount).toString()
                )
            )
        }
    }

    private fun getMnemonic(): String {
        return getEnteredMnemonic()?.trim()?.replace("\\s+".toRegex(), " ") ?: ""
    }

    protected abstract fun getEnteredMnemonic(): CharSequence?

    protected abstract fun setErrorLabel(error: String?)

    protected abstract fun navigateToSaveWalletDialog(mnemonic: String)

    protected abstract fun hideForcedSoftKeyboard()
}