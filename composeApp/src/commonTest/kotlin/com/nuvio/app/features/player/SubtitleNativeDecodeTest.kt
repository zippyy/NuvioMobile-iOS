package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleNativeDecodeTest {
    @Test fun shiftJisLanguageHintDecodesJapanese() {
        assertEquals("日本語", SubtitleCharsetDetector.decode(byteArrayOf(0x93.toByte(), 0xfa.toByte(), 0x96.toByte(), 0x7b, 0x8c.toByte(), 0xea.toByte()), languageHint = "ja"))
    }
    @Test fun koreanLanguageHintDecodesEucKr() {
        assertEquals("한국어", SubtitleCharsetDetector.decode(byteArrayOf(0xc7.toByte(), 0xd1.toByte(), 0xb1.toByte(), 0xb9.toByte(), 0xbe.toByte(), 0xee.toByte()), languageHint = "ko"))
    }
    @Test fun big5LanguageHintDecodesTraditionalChinese() {
        assertEquals("中文", SubtitleCharsetDetector.decode(byteArrayOf(0xa4.toByte(), 0xa4.toByte(), 0xa4.toByte(), 0xe5.toByte()), languageHint = "zh-tw"))
    }
}
