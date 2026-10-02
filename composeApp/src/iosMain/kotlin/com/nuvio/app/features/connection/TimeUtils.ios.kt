package com.nuvio.app.features.connection

import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSDate

internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()
