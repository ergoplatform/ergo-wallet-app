package org.ergoplatform.ios.api

import org.ergoplatform.utils.LogUtils
import org.robovm.apple.corefoundation.CFData
import org.robovm.apple.corefoundation.OSStatusException
import org.robovm.apple.foundation.NSString
import org.robovm.apple.foundation.NSStringEncoding
import org.robovm.apple.localauthentication.LAContext
import org.robovm.apple.security.*

object IosKeychainAccess {
    // https://developer.apple.com/documentation/localauthentication/accessing_keychain_items_with_face_id_or_touch_id
    // https://medium.com/@alx.gridnev/biometry-protected-entries-in-ios-keychain-6125e130e0d5

    private const val ERR_KEY_NOT_EXISTING = -25300
    private const val ERR_USER_CANCELLED = -128
    // returned when key is rendered inaccessible because user deactivated device authentication
    private const val ERR_NOT_AVAILABLE = -25291
    private const val ERR_AUTH_FAILED = -25293

    fun savePassword(service: String, name: String, value: String) {
        val attributes = SecAttributes().apply {
            set(SecQuery.Keys.Class(), SecClass.Values.GenericPassword())
            setAccount(name)
            setService(service)
            set(SecValue.Keys.Data(), NSString(value).toData(NSStringEncoding.UTF8))
            setAccessControl(
                SecAccessControl.create(
                    SecAttrAccessible.WhenPasscodeSetThisDeviceOnly,
                    SecAccessControlCreateFlags.UserPresence
                )
            )
        }
        try {
            SecItem.add(attributes)
        } catch (t: Throwable) {
            LogUtils.logDebug("IosKeychainAccess", "Could not add key", t)
            // throw this so user knows this wasn't saved
            throw t
        }
    }

    fun deletePassword(service: String, name: String) {
        val query = SecQuery().apply {
            set(SecQuery.Keys.Class(), SecClass.Values.GenericPassword())
            attributes = SecAttributes().apply {
                setAccount(name)
                setService(service)
            }
        }

        try {
            SecItem.delete(query)
        } catch (t: Throwable) {
            // ignore when key could not be deleted because not present
            if (!(isKeyNotExistingError(t))) {
                LogUtils.logDebug("IosKeychainAccess", "Could not delete key", t)
                throw t
            }
        }
    }

    /**
     * @return password from keychain, or null if not existing
     */
    fun queryPassword(service: String, name: String, context: LAContext? = null): String? {
        val query = SecQuery().apply {
            set(SecQuery.Keys.Class(), SecClass.Values.GenericPassword())
            attributes = SecAttributes().apply {
                setAccount(name)
                setService(service)
                context?.let {
                    setUseAuthenticationContext(context)
                    setUsesAuthenticationUI(false)
                }
            }
            `return`.setShouldReturnData(true)
            match.setLimit(SecMatchLimit.One)
        }

        return try {
            val item = SecItem.getMatching(query)
            String((item as CFData).bytes, Charsets.UTF_8)
        } catch (t: Throwable) {
            if (isKeyNotExistingError(t)) {
                // key not existing
                null
            } else {
                LogUtils.logDebug("IosKeychainAccess", "Could not query key", t)
                throw t
            }
        }
    }

    private fun isKeyNotExistingError(t: Throwable): Boolean {
        return t is OSStatusException && t.status.statusCode == ERR_KEY_NOT_EXISTING
    }
}