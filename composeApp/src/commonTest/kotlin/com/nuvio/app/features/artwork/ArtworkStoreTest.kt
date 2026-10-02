package com.nuvio.app.features.artwork

import kotlin.test.*

class ArtworkStoreTest {
    @Test fun overridesPersistWithoutChangingOtherProfilesOrOriginalIdentity() {
        val disk = mutableMapOf<Int, String>()
        val storage = object : ArtworkStorage {
            override fun load(profileId: Int) = disk[profileId]
            override fun save(profileId: Int, payload: String) { disk[profileId] = payload }
        }
        val store = ArtworkStore(storage, { 1 })
        store.saveOverride("series", "tt123", ArtworkOverride(poster = "https://art.example/poster.jpg"))
        val restored = ArtworkStore(storage, { 1 })
        assertEquals("https://art.example/poster.jpg", restored.resolve("series", "tt123", ArtworkScreen.HOME, ArtworkKind.POSTER, "https://original"))
        assertEquals("https://original", ArtworkStore(storage, { 2 }).resolve("series", "tt123", ArtworkScreen.HOME, ArtworkKind.POSTER, "https://original"))
        restored.saveOverride("series", "tt123", ArtworkOverride())
        assertEquals("https://original", restored.resolve("series", "tt123", ArtworkScreen.HOME, ArtworkKind.POSTER, "https://original"))
    }
    @Test fun scopesAndRequiredIdentifiersFallBackToOriginal() {
        val storage = object : ArtworkStorage {
            var payload: String? = null
            override fun load(profileId: Int) = payload
            override fun save(profileId: Int, payload: String) { this.payload = payload }
        }
        val store = ArtworkStore(storage, { 1 })
        store.saveSettings(ArtworkSettings("https://art.example/{imdb_id}.jpg", setOf(ArtworkScreen.HOME)))
        assertEquals("https://art.example/tt123.jpg", store.resolve("series", "tt123", ArtworkScreen.HOME, ArtworkKind.POSTER, "original"))
        assertEquals("original", store.resolve("series", "tt123", ArtworkScreen.DETAIL, ArtworkKind.POSTER, "original"))
        assertEquals("original", store.resolve("series", "tmdb:1", ArtworkScreen.HOME, ArtworkKind.POSTER, "original"))
    }
}
