package org.ergoplatform.ios.ui

import org.robovm.apple.audiotoolbox.AudioServices
import org.robovm.apple.corehaptic.CHHapticEngine
import org.robovm.apple.uikit.UIImpactFeedbackGenerator
import org.robovm.apple.uikit.UIImpactFeedbackStyle

class HapticFeedback {
    fun perform(type: UIImpactFeedbackStyle) {
        try {
            if (CHHapticEngine.capabilitiesForHardware().supportsHaptics())
                UIImpactFeedbackGenerator(type).impactOccurred()
            else
                AudioServices.playSystemSound(4095)
        } catch (_: Throwable) {
        }
    }
}