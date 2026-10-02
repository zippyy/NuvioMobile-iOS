import Foundation

/// Pure decisions used by the native MPV session (clock injected by its caller).
enum MPVPlaybackPolicy {
    static func cacheOptions(live: Bool) -> [String: String] {
        ["cache": "yes", "cache-pause": "yes", "cache-pause-wait": live ? "1" : "3",
         "cache-secs": live ? "10" : "120",
         "demuxer-max-bytes": live ? "33554432" : "134217728",
         "demuxer-max-back-bytes": live ? "0" : "33554432"]
    }
    static func watchdogFailure(rendered: Bool, loadingFor: Double, stalledFor: Double,
                                playing: Bool, paused: Bool, seeking: Bool, ended: Bool, foreground: Bool) -> String? {
        guard playing, !paused, !seeking, !ended, foreground else { return nil }
        if !rendered && loadingFor >= 20 { return "First frame timed out" }
        if rendered && stalledFor >= 15 { return "Playback stalled" }
        return nil
    }

    static func normalizedFPS(_ fps: Double) -> Double {
        guard fps.isFinite, fps >= 1, fps <= 240 else { return 0 }
        let standards = [24000.0 / 1001, 24, 25, 30000.0 / 1001, 30, 48, 50, 60000.0 / 1001, 60, 120000.0 / 1001, 120]
        let closest = standards.min { abs($0 - fps) < abs($1 - fps) }!
        return abs(closest - fps) <= 0.03 ? closest : fps
    }
    static func audioFilter(percent: Int) -> String {
        let gain = Double(max(0, min(200, percent))) / 100
        guard gain > 1 else { return "" }
        // aeval processes each channel in float precision, gain BEFORE the tanh knee.
        let x = "val(ch)*\(gain)"
        return "aeval=exprs='sgn(\(x))*if(lte(abs(\(x)),0.8),abs(\(x)),0.8+0.2*tanh((abs(\(x))-0.8)/0.2))':c=same"
    }
    static func bufferedEnd(position: Double, cacheEnd: Double, duration: Double) -> Double {
        let end = max(position, cacheEnd)
        return duration > 0 ? min(duration, end) : end
    }
}

struct MPVRecoveryBudget {
    private(set) var attempts = 0
    mutating func nextDelay(error: String, hasRendered: Bool) -> Double? {
        // These errors cannot heal by reconnecting; 429 must not hammer the host.
        let permanent = [400, 401, 403, 404, 410, 429].contains { status in
            error.range(of: "\\b\(status)\\b", options: .regularExpression) != nil
        }
        guard !permanent, attempts < (hasRendered ? 2 : 3) else { return nil }
        attempts += 1
        return pow(2, Double(attempts - 1))
    }
}
