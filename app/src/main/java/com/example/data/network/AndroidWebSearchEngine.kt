package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SearchResultItem(
    val title: String,
    val snippet: String,
    val url: String
)

class AndroidWebSearchEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Executes web search directly from the Android device using DuckDuckGo HTML / Instant Answer API
     * and Wikipedia API as high-reliability native fallback endpoints.
     * Doesn't require any LM Studio MCP or PC configuration!
     */
    suspend fun searchWeb(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResultItem>()

        // 1. DuckDuckGo Instant Answer / Zero-Click API
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ClawAssistant-Android/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)

                    val heading = json.optString("Heading")
                    val abstractText = json.optString("AbstractText")
                    val abstractUrl = json.optString("AbstractURL")

                    if (abstractText.isNotBlank()) {
                        results.add(
                            SearchResultItem(
                                title = heading.ifBlank { cleanQuery },
                                snippet = abstractText,
                                url = abstractUrl.ifBlank { "https://duckduckgo.com/?q=$encoded" }
                            )
                        )
                    }

                    // Related topics
                    val related = json.optJSONArray("RelatedTopics")
                    if (related != null) {
                        for (i in 0 until minOf(related.length(), 4)) {
                            val item = related.optJSONObject(i) ?: continue
                            val text = item.optString("Text")
                            val firstUrl = item.optString("FirstURL")
                            if (text.isNotBlank()) {
                                results.add(
                                    SearchResultItem(
                                        title = text.take(60),
                                        snippet = text,
                                        url = firstUrl
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to Wikipedia search API
        }

        // 2. Wikipedia Search API as rich factual content source
        if (results.size < 2) {
            try {
                val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
                val wikiUrl = "https://de.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&utf8=&format=json&srlimit=4"
                val request = Request.Builder()
                    .url(wikiUrl)
                    .header("User-Agent", "ClawAssistant-Android/1.0 (contact@example.com)")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val queryObj = json.optJSONObject("query")
                        val searchArr = queryObj?.optJSONArray("search")
                        if (searchArr != null) {
                            for (i in 0 until searchArr.length()) {
                                val item = searchArr.optJSONObject(i) ?: continue
                                val title = item.optString("title")
                                val snippet = item.optString("snippet")
                                    .replace(Regex("<.*?>"), "") // Strip HTML tags
                                val pageUrl = "https://de.wikipedia.org/wiki/${URLEncoder.encode(title, "UTF-8")}"
                                results.add(
                                    SearchResultItem(
                                        title = title,
                                        snippet = snippet,
                                        url = pageUrl
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore and proceed
            }
        }

        results.distinctBy { it.title }
    }
}
