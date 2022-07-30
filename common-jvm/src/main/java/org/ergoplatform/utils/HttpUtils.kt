package org.ergoplatform.utils

import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.*
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.ergoplatform.api.OkHttpSingleton
import java.io.IOException
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit


private const val IPV4_PATTERN =
    "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$"

fun isLocalOrIpAddress(url: String): Boolean {
    val hostname = getHostname(url)
    return hostname.equals("localhost", false) || hostname.matches(Regex(IPV4_PATTERN))
}

fun getHostname(url: String): String {
    return url.substringAfter("://").substringBefore('/').substringBefore(':')
}

/**
 * returns lower case host and protocol name, as it is case insensitive
 * removes trailing slash if no uri is set
 */
fun normalizeUrl(url: String): String {
    val hostNameStarts = url.indexOf("://") + 3
    val hostNameEnds = url.indexOf('/', hostNameStarts)

    val lcHostname = if (hostNameEnds < 0)
        url.lowercase()
    else url.substring(0, hostNameEnds).lowercase() + url.substring(hostNameEnds)

    return if (hostNameEnds < 0 || hostNameEnds == url.length - 1)
        lcHostname.trimEnd('/') else lcHostname
}

fun fetchHttpGetStringSync(httpUrl: String, timeout: Long = 10): String =
    fetchHttpsGetStringSync(httpUrl, timeout).first

fun fetchHttpsGetStringSync(httpUrl: String, timeout: Long = 10): Pair<String, List<Certificate>?> {
    val request = Request.Builder().url(httpUrl).build()
    val response =
        OkHttpSingleton.getInstance().newBuilder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS).build().newCall(request).execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code $response")
                }

                Pair(response.body()!!.string(), response.handshake()?.peerCertificates())
            }
    return response
}

fun Certificate.getIssuerOrg(): String? {
    return try {
        val x509cert = this as X509Certificate
        val x500name = X500Name(x509cert.issuerX500Principal.name)
        val cn = x500name.getRDNs(BCStyle.O)[0]

        IETFUtils.valueToString(cn.first.value)
    } catch (t: Throwable) {
        null
    }
}

fun httpPostStringSync(httpUrl: String, body: String, mediaType: String): String? {
    val request = Request.Builder()
        .url(httpUrl)
        .post(RequestBody.create(MediaType.parse(mediaType), body))
        .build()

    return OkHttpSingleton.getInstance().newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("$httpUrl returned $response")

        response.body()?.string()
    }
}

fun fetchHttpGetWithListener(url: String, progressListener: ProgressListener): ByteArray {
    val request = Request.Builder()
        .url(url)
        .build()
    val client = OkHttpSingleton.getInstance().newBuilder()
        .addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body(), progressListener))
                .build()
        }
        .build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        return response.body()!!.bytes()
    }
}

private class ProgressResponseBody(
    private val responseBody: ResponseBody?,
    private val progressListener: ProgressListener
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null
    override fun contentType(): MediaType? {
        return responseBody!!.contentType()
    }

    override fun contentLength(): Long {
        return responseBody!!.contentLength()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody!!.source()))
        }
        return (bufferedSource)!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                progressListener.update(
                    totalBytesRead,
                    responseBody!!.contentLength(),
                    bytesRead == -1L
                )
                return bytesRead
            }
        }
    }
}

/**
 * Progress listener for downloads started with [fetchHttpGetWithListener]
 */
interface ProgressListener {
    /**
     * @param bytesRead         number of bytes downloaded so far
     * @param contentLength     number of bytes to download in total, or <= 0 if not known
     * @param done              download is not in progress any more
     */
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}

const val MEDIA_TYPE_JSON = "application/json; charset=utf-8"