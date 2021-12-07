package org.ergoplatform.ios.api

import org.robovm.apple.localauthentication.LAContext
import org.robovm.apple.localauthentication.LAPolicy

object IosAuthentication {
    fun canAuthenticate(): Boolean {
        return LAContext().canEvaluatePolicy(LAPolicy.DeviceOwnerAuthentication)
    }

    fun authenticate(reason: String, callback: IosAuthCallback) {
        val context = LAContext()
        context.evaluatePolicy(LAPolicy.DeviceOwnerAuthentication, reason) { success, error ->
            if (success)
                callback.onAuthenticationSucceeded(context)
            else
                callback.onAuthenticationError("Authentication error ${error.errorCode}")
        }
    }

    interface IosAuthCallback {
        fun onAuthenticationSucceeded(context: LAContext)
        fun onAuthenticationError(error: String)
    }
}