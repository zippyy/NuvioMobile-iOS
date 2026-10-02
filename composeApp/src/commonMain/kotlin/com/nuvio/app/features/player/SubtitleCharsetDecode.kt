package com.nuvio.app.features.player

/** Strict decoder: null means unsupported or malformed; never guesses UTF-8. */
internal expect fun decodeSubtitleCharset(bytes: ByteArray, offset: Int, length: Int, charset: String): String?
