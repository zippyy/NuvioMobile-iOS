package com.nuvio.app.features.player

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

internal actual fun decodeSubtitleCharset(bytes: ByteArray, offset: Int, length: Int, charset: String): String? {
    require(offset >= 0 && length >= 0 && offset <= bytes.size - length)
    return runCatching {
        Charset.forName(charset).newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes, offset, length)).toString()
    }.getOrNull()
}
