package org.ergoplatform.utils

import junit.framework.TestCase
import org.junit.Assert

class HttpUtilsKtTest : TestCase() {

    fun testIsLocalOrIpAddress() {
        Assert.assertTrue(isLocalOrIpAddress("http://localhost/"))
        Assert.assertTrue(isLocalOrIpAddress("http://192.168.0.1:8080/"))
        Assert.assertTrue(isLocalOrIpAddress("http://127.0.0.1:8080/"))
        Assert.assertFalse(isLocalOrIpAddress("https://www.ergoplatform.com/"))
        Assert.assertFalse(isLocalOrIpAddress("https://www.101tips.com/"))
    }

    fun testGetHostname() {
        Assert.assertEquals("localhost", getHostname("http://localhost/"))
        Assert.assertEquals("localhost", getHostname("localhost"))
        Assert.assertEquals("localhost", getHostname("https://localhost"))
        Assert.assertEquals("localhost", getHostname("ergopay://localhost"))
        Assert.assertEquals("localhost", getHostname("https://localhost:8080/usw/uwf"))
    }
}