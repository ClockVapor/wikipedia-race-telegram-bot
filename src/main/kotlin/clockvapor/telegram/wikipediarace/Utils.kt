package clockvapor.telegram.wikipediarace

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

fun encodeUrlArgument(value: String): String = URLEncoder.encode(value, "UTF-8")

fun wget(url: URL): String = wget(url, getConnection(url))

fun wget(url: URL, connection: HttpURLConnection): String = tryUrlAction(url) {
    connection.inputStream.bufferedReader().use { it.lineSequence().toList().joinToString("\n") }
}

fun getConnection(url: URL): HttpURLConnection = (url.openConnection() as HttpURLConnection).apply {
    requestMethod = "GET"
    setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0"
    )
    connectTimeout = 20000
    readTimeout = connectTimeout
}

fun tryCreateUrl(urlString: String): URL = try {
    URL(urlString)
} catch (e: Exception) {
    throw RuntimeException("Failed to create URL: $urlString", e)
}

inline fun <T> tryUrlAction(url: URL, action: () -> T): T = try {
    action()
} catch (e: Exception) {
    throw RuntimeException("Failed to access URL: $url", e)
}
