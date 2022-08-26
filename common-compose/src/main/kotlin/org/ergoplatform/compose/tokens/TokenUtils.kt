package org.ergoplatform.compose.tokens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector
import org.ergoplatform.persistance.GENUINE_SUSPICIOUS
import org.ergoplatform.persistance.GENUINE_VERIFIED

fun getGenuineImageVector(genuineFlag: Int): ImageVector? {
    return when (genuineFlag) {
        GENUINE_VERIFIED -> Icons.Default.Verified
        GENUINE_SUSPICIOUS -> Icons.Default.Report
        else -> null
    }
}