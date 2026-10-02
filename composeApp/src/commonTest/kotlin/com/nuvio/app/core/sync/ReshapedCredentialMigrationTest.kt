package com.nuvio.app.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReshapedCredentialMigrationTest {
    @Test fun migrationDeletesPlaintextOnlyAfterVerifiedSecureWrite() {
        var legacy: String? = "test-value"
        var secure: String? = null
        val result = migrateCredential({ secure }, { legacy }, { secure = it }, { legacy = null })
        assertEquals("test-value", result)
        assertEquals(null, legacy)
        legacy = "still-present"; secure = null
        assertFailsWith<IllegalStateException> { migrateCredential({ secure }, { legacy }, { }, { legacy = null }) }
        assertEquals("still-present", legacy)
    }
}
