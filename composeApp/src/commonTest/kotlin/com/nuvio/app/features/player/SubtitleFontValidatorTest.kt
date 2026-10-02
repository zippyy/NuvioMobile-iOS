package com.nuvio.app.features.player
import kotlin.test.*

class SubtitleFontValidatorTest {
    @Test fun acceptsBoundedUnicodeFamilyName() {
        assertEquals("Nuvio Test", validateSubtitleFont(fontFixture())?.familyName)
    }
    @Test fun rejectsOutOfBoundsNameTable() {
        val bytes = fontFixture(); bytes[20] = 0x7f
        assertNull(validateSubtitleFont(bytes))
    }
    @Test fun rejectsTruncatedAndCollections() {
        assertNull(validateSubtitleFont(byteArrayOf(0, 1, 0, 0)))
        assertNull(validateSubtitleFont("ttcfNotAFont".encodeToByteArray()))
    }
    @Test fun rejectsFontOverLimit() { assertNull(validateSubtitleFont(ByteArray(MAX_SUBTITLE_FONT_BYTES + 1))) }
}

internal fun fontFixture(): ByteArray {
    val name = "Nuvio Test".flatMap { listOf((it.code shr 8).toByte(), it.code.toByte()) }.toByteArray()
    val bytes = ByteArray(46 + name.size)
    fun u16(at: Int, value: Int) { bytes[at] = (value shr 8).toByte(); bytes[at + 1] = value.toByte() }
    fun u32(at: Int, value: Int) { u16(at, value shr 16); u16(at + 2, value) }
    u32(0, 0x00010000); u16(4, 1)
    "name".encodeToByteArray().copyInto(bytes, 12)
    u32(20, 28); u32(24, 18 + name.size)
    u16(30, 1); u16(32, 18)
    u16(34, 3); u16(36, 1); u16(38, 0x0409); u16(40, 1); u16(42, name.size); u16(44, 0)
    name.copyInto(bytes, 46)
    return bytes
}
