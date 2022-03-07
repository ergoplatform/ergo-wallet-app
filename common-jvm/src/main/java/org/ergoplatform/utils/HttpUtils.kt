package org.ergoplatform.utils

import okhttp3.*
import okio.*
import java.io.IOException

private const val IPV4_PATTERN =
    "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$"

fun isLocalOrIpAddress(url: String): Boolean {
    val hostname = getHostname(url)
    return hostname.equals("localhost", false) || hostname.matches(Regex(IPV4_PATTERN))
}

fun getHostname(url: String): String {
    return url.substringAfter("://").substringBefore('/').substringBefore(':')
}

fun fetchHttpGetStringSync(httpUrl: String): String {
    val request = Request.Builder().url(httpUrl).build()
    val stringResponse = OkHttpClient().newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Unexpected response code $response")
        }

        response.body()!!.string()

    }
    return stringResponse
}

fun httpPostStringSync(httpUrl: String, body: String, mediaType: String) {
    val request = Request.Builder()
        .url(httpUrl)
        .post(RequestBody.create(MediaType.parse(mediaType), body))
        .build()

    OkHttpClient().newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
    }
}

fun fetchHttpGetWithListener(url: String, progressListener: ProgressListener): ByteArray {
    val request = Request.Builder()
        .url(url)
        .build()
    val client = OkHttpClient.Builder()
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