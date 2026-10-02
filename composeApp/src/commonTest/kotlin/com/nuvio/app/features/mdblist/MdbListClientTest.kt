package com.nuvio.app.features.mdblist

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MdbListClientTest {
    @Test fun outOfRangeProviderRatingIsNotDisplayed() = runBlocking {
        val client = MdbListClient(clock = { 0 }, transport = { _, _, _ ->
            MdbListResponse(200, """{"ratings":[{"id":"tt123","rating":11.0}]}""")
        })
        assertEquals(emptyMap(), client.ratings(listOf("tt123"), "movie", "imdb", "key"))
    }

    @Test fun cacheInvalidationCancelsStaleRatingsPublication() = runBlocking {
        lateinit var client: MdbListClient
        client = MdbListClient(clock = { 0 }, transport = { _, _, _ ->
            client.clearCache()
            MdbListResponse(200, """{"ratings":[{"id":"tt123","rating":8.5}]}""")
        })
        assertFailsWith<kotlinx.coroutines.CancellationException> { client.ratings(listOf("tt123"), "movie", "imdb", "key") }
        Unit
    }

    @Test fun listPaginationHonorsResponseHeaders() = runBlocking {
        var count = 0
        val client = MdbListClient(clock = { 0 }, transport = { _, url, _ ->
            count++
            if ("cursor=next%26page" in url) MdbListResponse(200, "[]")
            else MdbListResponse(200, "[]", mapOf("X-Next-Cursor" to "next&page"))
        })
        assertEquals(emptyList(), client.listItems(12, "key"))
        assertEquals(2, count)
    }

    @Test fun batchUsesReturnedIdsNotResponseOrder() = runBlocking {
        var requests = 0
        val client = MdbListClient(clock = { 0 }, transport = { _, url, body ->
            requests++
            assertEquals(true, url.endsWith("apikey=a%26b"))
            assertEquals(true, body.contains("tt123"))
            MdbListResponse(200, """{"ratings":[{"id":"tt456","rating":7.0},{"id":"tt123","rating":8.5}]}""")
        })
        assertEquals(mapOf("tt123" to 8.5, "tt456" to 7.0), client.ratings(listOf("tt123", "tt456", "bad-tt123"), "show", "mal", "a&b"))
        assertEquals(1, requests)
        client.clearCache()
        client.ratings(listOf("tt123"), "show", "mal", "a&b")
        assertEquals(2, requests)
    }

    @Test fun personalListsFetchAllPagesAndDeduplicateIds() = runBlocking {
        val client = MdbListClient(clock = { 0 }, transport = { _, url, _ ->
            if ("offset=2" in url) MdbListResponse(200, """{"items":[{"id":42,"mediatype":"movie","imdb_id":"tt123","title":"Movie"}]}""")
            else MdbListResponse(200, """{"items":[{"id":42,"mediatype":"movie","imdb_id":"tt123","title":"Movie"}],"next_offset":2}""")
        })
        val items = client.listItems(12, "key")
        assertEquals(1, items.size)
        assertEquals("tt123", items.single().imdbId)
    }

    @Test fun cacheExpiresAndErrorsAreNotCached() = runBlocking {
        var now = 0L
        var requests = 0
        var status = 200
        val client = MdbListClient(clock = { now }, transport = { _, _, _ ->
            requests++
            MdbListResponse(status, """{"ratings":[{"id":"tt123","rating":8.5}]}""")
        })
        assertEquals(8.5, client.ratings(listOf("tt123"), "movie", "imdb", "key")["tt123"])
        client.ratings(listOf("tt123"), "movie", "imdb", "key")
        assertEquals(1, requests)
        now = 1_800_000
        status = 429
        assertFailsWith<MdbListApiException> { client.ratings(listOf("tt123"), "movie", "imdb", "key") }
        status = 200
        client.ratings(listOf("tt123"), "movie", "imdb", "key")
        assertEquals(3, requests)
        client.ratings(listOf("tt123"), "movie", "imdb", "other-key")
        assertEquals(4, requests)
    }
}
