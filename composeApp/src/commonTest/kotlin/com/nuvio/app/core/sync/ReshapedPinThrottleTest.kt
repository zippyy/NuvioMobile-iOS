package com.nuvio.app.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class ReshapedPinThrottleTest {
    @Test fun repeatedFailuresLockOnlyThatAccountProfileAndSuccessResets() {
        val throttle = ProfilePinThrottle()
        repeat(5) { throttle.failed("account:1", 100) }
        assertEquals(false, throttle.allowed("account:1", 101))
        assertEquals(true, throttle.allowed("account:2", 101))
        assertEquals(true, throttle.allowed("other:1", 101))
        assertEquals(true, throttle.allowed("account:1", 30_100))
        throttle.succeeded("account:1")
        assertEquals(true, throttle.allowed("account:1", 102))
    }
}
