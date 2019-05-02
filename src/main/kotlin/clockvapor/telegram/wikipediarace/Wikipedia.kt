package clockvapor.telegram.wikipediarace

import com.fasterxml.jackson.databind.ObjectMapper
import org.jsoup.Jsoup

object Wikipedia {
    fun fetchRandomPageTitle(): String {
        val url = tryCreateUrl("https://en.wikipedia.org/wiki/Special:Random")
        val htmlString = wget(url)
        val document = Jsoup.parse(htmlString)
        return document.getElementById("firstHeading")!!.text()
    }

    fun fetchPageTitle(query: String): String? {
        val url = tryCreateUrl("https://en.wikipedia.org/w/api.php?action=query&list=search&utf8=&format=json" +
            "&srlimit=1&srsearch=${encodeUrlArgument(query)}")
        val jsonString = wget(url)
        val json = ObjectMapper().readTree(jsonString)
        return json["query"]?.get("search")?.firstOrNull()?.get("title")?.asText()
    }

    /**
     * Returns a list of links on the page, and the plcontinue string if there are more links than those returned.
     */
    fun fetchPageLinks(pageTitle: String, plcontinue: String? = null): Pair<List<String>, String?> {
        val url = tryCreateUrl("https://en.wikipedia.org/w/api.php?action=query&prop=links&format=json" +
            "&pllimit=20&titles=${encodeUrlArgument(pageTitle)}" +
            if (plcontinue != null) "&plcontinue=${encodeUrlArgument(plcontinue)}" else ""
        )
        val jsonString = wget(url)
        val json = ObjectMapper().readTree(jsonString)
        return Pair(
            json["query"]?.get("pages")?.firstOrNull()?.get("links")?.mapNotNull { it["title"]?.asText() }.orEmpty(),
            json["continue"]?.get("plcontinue")?.asText()
        )
    }

    fun containsLinkToPage(pageTitle: String, targetPageTitle: String): Boolean {
        val url = tryCreateUrl("https://en.wikipedia.org/w/api.php?action=query&prop=links&format=json" +
            "&titles=${encodeUrlArgument(pageTitle)}&pltitles=${encodeUrlArgument(targetPageTitle)}"
        )
        val jsonString = wget(url)
        val json = ObjectMapper().readTree(jsonString)
        return !json["query"]?.get("pages")?.firstOrNull()?.get("links")
            ?.mapNotNull { it["title"]?.asText() }
            .isNullOrEmpty()
    }
}
