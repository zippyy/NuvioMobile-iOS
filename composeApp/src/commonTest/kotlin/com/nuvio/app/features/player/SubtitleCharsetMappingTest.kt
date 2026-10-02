package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals

/** Fixed code-page bytes independent of the decoder's lookup tables. */
class SubtitleCharsetMappingTest {
    @Test fun decodesCentralEuropeanLetters() {
        assertEquals("ąćřů", SubtitleCharsetDetector.decode(byteArrayOf(-71, -26, -8, -7), languageHint = "pol"))
    }
    @Test fun decodesArabicFaAndMeem() {
        assertEquals("فم", SubtitleCharsetDetector.decode(byteArrayOf(-35, -29), languageHint = "ara"))
    }
    @Test fun preservesVietnameseCombiningToneMark() {
        assertEquals("ê\u0323", SubtitleCharsetDetector.decode(byteArrayOf(-22, -14), languageHint = "vie"))
    }
    @Test fun replacesUndefinedWesternByte() {
        assertEquals("\uFFFD", SubtitleCharsetDetector.decode(byteArrayOf(-127), languageHint = "eng"))
    }
}
