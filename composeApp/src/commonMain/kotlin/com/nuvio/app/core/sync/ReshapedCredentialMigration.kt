package com.nuvio.app.core.sync

/** Never discard the legacy credential until secure storage confirms the same value. */
internal fun migrateCredential(readSecure: () -> String?, readLegacy: () -> String?, writeSecure: (String) -> Unit, removeLegacy: () -> Unit): String? {
    val secure = readSecure()
    if (secure != null) { removeLegacy(); return secure }
    val legacy = readLegacy() ?: return null
    writeSecure(legacy)
    check(readSecure() == legacy) { "Secure credential migration failed" }
    removeLegacy()
    return legacy
}
