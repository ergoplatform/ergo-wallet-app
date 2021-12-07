package org.ergoplatform.ios.api

import org.ergoplatform.utils.LogUtils
import org.robovm.apple.corefoundation.CFData
import org.robovm.apple.corefoundation.OSStatusException
import org.robovm.apple.foundation.NSString
import org.robovm.apple.foundation.NSStringEncoding
import org.robovm.apple.security.*

object IosKeychainAccess {
    // TODO https://medium.com/@alx.gridnev/biometry-protected-entries-in-ios-keychain-6125e130e0d5

    fun savePassword(service: String, name: String, value: String) {
        val attributes = SecAttributes().apply {
            set(SecQuery.Keys.Class(), SecClass.Values.GenericPassword())
            setAccount(name)
            setService(service)
            set(SecValue.Keys.Data(), NSString(value).toData(NSStringEncoding.UTF8))
            setAccessible(SecAttrAccessible.WhenPasscodeSetThisDeviceOnly)
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
            // ignore -25300 errKCItemNotFound
            if (!(t is OSStatusException && t.status.statusCode == -25300)) {
                LogUtils.logDebug("IosKeychainAccess", "Could not delete key", t)
            }
        }
    }

    fun queryPassword(service: String, name: String): String? {
        val query = SecQuery().apply {
            set(SecQuery.Keys.Class(), SecClass.Values.GenericPassword())
            attributes = SecAttributes().apply {
                setAccount(name)
                setService(service)
                setOperationPrompt("Test")
            }
            `return`.setShouldReturnData(true)
            match.setLimit(SecMatchLimit.One)
        }

        try {
            val item = SecItem.getMatching(query)
            return String((item as CFData).bytes, Charsets.UTF_8)
        } catch (t: Throwable) {
            LogUtils.logDebug("IosKeychainAccess", "Could not query key", t)
            return null
        }
    }
}