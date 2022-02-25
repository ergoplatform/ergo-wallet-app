package org.ergoplatform.android.tokens

import org.ergoplatform.android.R
import org.ergoplatform.persistance.GENUINE_SUSPICIOUS
import org.ergoplatform.persistance.GENUINE_VERIFIED
import org.ergoplatform.persistance.TokenInformation

fun TokenInformation.getGenuineDrawableId() = getGenuineDrawableId(genuineFlag)

fun getGenuineDrawableId(genuineFlag: Int): Int {
    return when (genuineFlag) {
        GENUINE_VERIFIED -> R.drawable.ic_verified_18
        GENUINE_SUSPICIOUS -> R.drawable.ic_suspicious_18
        else -> 0
    }
}
