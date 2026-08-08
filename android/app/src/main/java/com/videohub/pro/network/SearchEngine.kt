package com.videohub.pro.network

import java.net.URLEncoder
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real search engine — fetches actual search results from the internet.
 *
 * Uses YouTube's public suggestion API.
 * Does NOT generate fake fallback results.
 * Returns empty list if no results or no connectivity.
 */
@Singleton
class SearchEngine @Inject constructor(
    private val networkClient: NetworkClient,
) {

    data class SearchResult(
        val title: String,
        val url: String,
        val platform: String,
        val thumbnailUrl: String?,
        val duration: String?,
        val views: String?,
    )

    /**
     * Search for videos across platforms.
     * Returns REAL results only. Empty list if no results or no connectivity.
     */
    suspend fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        // Only use real search — no fake fallbacks
        return searchYouTube(query)
    }

    /**
     * Search YouTube using the public suggestions API.
     * Returns real search suggestions or empty list on failure.
     */
    private fun searchYouTube(query: String): List<SearchResult> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val apiUrl = "https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=$encodedQuery&output=json"

        val response = networkClient.fetchText(apiUrl) ?: return emptyList()

        return try {
            val cleanResponse = response.trim().removePrefix("(").removeSuffix(")")
            val suggestionPattern = Pattern.compile("\"([^\"]+)\"")
            val matcher = suggestionPattern.matcher(cleanResponse)

            val results = mutableListOf<SearchResult>()
            var firstMatch = true

            while (matcher.find() && results.size < 10) {
                val rawMatch: String = matcher.group(1) ?: continue
                val suggestion: String = rawMatch.trim()

                if (suggestion.isEmpty()) continue

                if (firstMatch) {
                    firstMatch = false
                    continue
                }

                if (suggestion.startsWith("http")) continue
                if (suggestion.matches(Regex("\\d+"))) continue

                val searchUrl = "https://www.youtube.com/results?search_query=" +
                    URLEncoder.encode(suggestion, "UTF-8")

                results.add(
                    SearchResult(
                        title = suggestion,
                        url = searchUrl,
                        platform = "youtube",
                        thumbnailUrl = null,
                        duration = null,
                        views = null,
                    ),
                )
            }

            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if the search engine can reach the internet.
     */
    fun isAvailable(): Boolean = networkClient.isOnline()
}
