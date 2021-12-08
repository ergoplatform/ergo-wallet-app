package org.ergoplatform.ios.api

import org.ergoplatform.utils.LogUtils
import org.robovm.apple.localauthentication.LAContext
import org.robovm.apple.localauthentication.LAError
import org.robovm.apple.localauthentication.LAPolicy

object IosAuthentication {
    fun canAuthenticate(): Boolean {
        try {
            return LAContext().canEvaluatePolicy(LAPolicy.DeviceOwnerAuthentication)
        } catch (t: Throwable) {
            LogUtils.logDebug("IosAuthentication", "Error on canEvaluatePolicy", t)
            return false
        }
    }

    fun authenticate(reason: String, callback: IosAuthCallback) {
        val context = LAContext()
        context.evaluatePolicy(LAPolicy.DeviceOwnerAuthentication, reason) { success, error ->
            if (success)
                callback.onAuthenticationSucceeded(context)
            else {
                if (error.code == LAError.UserCancel.value()) {
                    callback.onAuthenticationCancelled()
                } else {
                    LogUtils.logDebug(
                        "IosAuthentication",
                        "Error on evaluatePolicy: ${error.code} (${error.localizedDescription})"
                    )
                    callback.onAuthenticationError("Authentication error: ${error.localizedDescription}")
                }
            }
        }
    }

    interface IosAuthCallback {
        fun onAuthenticationSucceeded(context: LAContext)
        fun onAuthenticationError(error: String)
        fun onAuthenticationCancelled()
    }
}