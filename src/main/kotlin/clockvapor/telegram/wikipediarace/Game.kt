package clockvapor.telegram.wikipediarace

class Game(startPageTitle: String, val targetPageTitle: String) {
    private var currentPageTitle = startPageTitle
    private val currentLinks = arrayListOf<String>()
    private val plcontinueList = arrayListOf<String>()
    private var linkPageNumber = 0
    private var steps = 0
    private val path = arrayListOf(startPageTitle)
    val state: State
        get() = when (currentPageTitle) {
            targetPageTitle -> State.WIN
            else -> State.CONTINUE
        }

    init {
        updateLinksForNewPage()
    }

    override fun toString(): String =
        if (state == State.WIN)
            "You win! You took $steps steps to reach the destination. Here is the path you took:\n\n" +
                path
                    .mapIndexed { i, pageTitle -> "${i + 1}. $pageTitle" }
                    .joinToString("\n")
        else
            "Destination: $targetPageTitle\n\n" +
                "Links taken: $steps\n\n" +
                "Current: $currentPageTitle\n\n" +
                "Links (pg ${linkPageNumber + 1}):\n" +
                currentLinks
                    .mapIndexed { i, link -> "${i + 1}. $link" }
                    .joinToString("\n")

    fun update(i: Int): Boolean {
        if (i < 1 || i > currentLinks.size) {
            return false
        }
        currentPageTitle = currentLinks[i - 1]
        path.add(currentPageTitle)
        updateLinksForNewPage()
        steps++
        return true
    }

    fun getNextPageOfLinks(): Boolean {
        val plcont = plcontinueList.getOrNull(linkPageNumber)
        if (plcont != null) {
            val (links, plcontinue) = Wikipedia.fetchPageLinks(currentPageTitle, plcont)
            currentLinks.clear()
            currentLinks.addAll(links)
            if (plcont != plcontinue) {
                linkPageNumber++
                if (plcontinue != null && plcontinue.isNotBlank()) {
                    plcontinueList.add(plcontinue)
                }
                return true
            }
        }
        return false
    }

    fun getPreviousPageOfLinks(): Boolean {
        if (linkPageNumber > 0) {
            val plcont = plcontinueList.getOrNull(linkPageNumber - 2)
            val (links, _) = Wikipedia.fetchPageLinks(currentPageTitle, plcont)
            currentLinks.clear()
            currentLinks.addAll(links)
            linkPageNumber--
            return true
        }
        return false
    }

    fun goBack() {
        path.getOrNull(path.size - 2)?.let { back ->
            currentPageTitle = back
            path.removeAt(path.indices.last)
            updateLinksForNewPage()
        }
    }

    fun skipTo(skipSubject: String): Boolean {
        var i = 0

        while (maxLink()!!.goodCompare(skipSubject) < 0 && getNextPageOfLinks()) {
            i++
        }
        while (minLink()!!.goodCompare(skipSubject) > 0 && getPreviousPageOfLinks()) {
            i--
        }

        if (!atSubject(skipSubject)) {
            while (i < 0) {
                getNextPageOfLinks()
                i++
            }
            while (i > 0) {
                getPreviousPageOfLinks()
                i--
            }
            return false
        }
        return true
    }

    private fun maxLink(): String? =
        currentLinks.lastOrNull()

    private fun minLink(): String? =
        currentLinks.firstOrNull()

    private fun atSubject(subject: String): Boolean =
        currentLinks.any { it.startsWith(subject, ignoreCase = true) }

    private fun updateLinksForNewPage() {
        linkPageNumber = 0
        plcontinueList.clear()
        currentLinks.clear()
        if (Wikipedia.containsLinkToPage(currentPageTitle, targetPageTitle)) {
            currentLinks.add(targetPageTitle)
        } else {
            val (links, plcontinue) = Wikipedia.fetchPageLinks(currentPageTitle, null)
            currentLinks.addAll(links)
            if (plcontinue != null && plcontinue.isNotBlank()) {
                plcontinueList.add(plcontinue)
            }
        }
    }

    enum class State {
        CONTINUE,
        WIN
    }
}
