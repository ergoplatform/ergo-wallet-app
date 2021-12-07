package org.ergoplatform.uilogic.wallet

import org.ergoplatform.MNEMONIC_WORDS_COUNT
import org.ergoplatform.appkit.SecretString
import kotlin.random.Random

class ConfirmCreateWalletUiLogic() {
    var firstWord: Int = 0
        private set
    var secondWord: Int = 0
        private set
    var firstWordCorrect: Boolean = false
        private set
    var secondWordCorrect: Boolean = false
        private set

    var mnemonic: SecretString? = null

    init {
        firstWord = Random.nextInt(1, MNEMONIC_WORDS_COUNT)
        while (secondWord == 0 || secondWord == firstWord)
            secondWord = Random.nextInt(1, MNEMONIC_WORDS_COUNT)
    }

    fun checkUserEnteredCorrectWordsAndConfirmedObligations(
        inputWord1: String,
        inputWord2: String,
        obligationsConfirmed: Boolean
    ): Boolean {

        val words = mnemonic!!.toStringUnsecure().split(" ")

        firstWordCorrect = inputWord1.trim().equals(words[firstWord - 1], false)
        secondWordCorrect = inputWord2.trim().equals(words[secondWord - 1], false)

        return !firstWordCorrect || !secondWordCorrect || !obligationsConfirmed
    }
}