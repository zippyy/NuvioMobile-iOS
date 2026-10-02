import Foundation

func expect(_ condition: @autoclosure () -> Bool, _ message: String) {
    if !condition() { fatalError(message) }
}
var startup = MPVRecoveryBudget()
expect(startup.nextDelay(error: "network timeout", hasRendered: false) == 1, "startup first retry delayed")
expect(startup.nextDelay(error: "network timeout", hasRendered: false) == 2, "startup backoff")
expect(startup.nextDelay(error: "network timeout", hasRendered: false) == 4, "startup third retry")
expect(startup.nextDelay(error: "network timeout", hasRendered: false) == nil, "startup bounded")
var playback = MPVRecoveryBudget()
expect(playback.nextDelay(error: "stall", hasRendered: true) == 1, "playback retry")
expect(playback.nextDelay(error: "stall", hasRendered: true) == 2, "playback retry two")
expect(playback.nextDelay(error: "stall", hasRendered: true) == nil, "playback bounded")
for status in [400, 401, 403, 404, 410, 429] {
    var rejected = MPVRecoveryBudget()
    expect(rejected.nextDelay(error: "HTTP error \(status)", hasRendered: false) == nil, "permanent HTTP error")
}
expect(MPVPlaybackPolicy.cacheOptions(live: true)["cache-secs"] == "10", "live latency bounded")
expect(MPVPlaybackPolicy.cacheOptions(live: false)["demuxer-max-back-bytes"] == "33554432", "VOD back buffer")
expect(MPVPlaybackPolicy.bufferedEnd(position: 20, cacheEnd: 40, duration: 100) == 40, "cache timestamp is absolute not duration")
expect(MPVPlaybackPolicy.bufferedEnd(position: 20, cacheEnd: 140, duration: 100) == 100, "VOD clamp")
expect(abs(MPVPlaybackPolicy.normalizedFPS(23.98) - 24000.0 / 1001) < 0.0001, "NTSC snap")
expect(MPVPlaybackPolicy.normalizedFPS(.nan) == 0, "invalid FPS")
expect(MPVPlaybackPolicy.normalizedFPS(25) == 25, "PAL remains PAL")
expect(MPVPlaybackPolicy.normalizedFPS(47.1) == 47.1, "nonstandard remains unsnapped")
expect(MPVPlaybackPolicy.audioFilter(percent: 200).contains("tanh"), "actual DSP filter not linear mpv volume")
expect(MPVPlaybackPolicy.audioFilter(percent: 100) == "", "unity bypass")
expect(MPVPlaybackPolicy.watchdogFailure(rendered: false, loadingFor: 20, stalledFor: 20,
    playing: true, paused: false, seeking: false, ended: false, foreground: true) == "First frame timed out", "startup watchdog")
expect(MPVPlaybackPolicy.watchdogFailure(rendered: true, loadingFor: 200, stalledFor: 15,
    playing: true, paused: false, seeking: false, ended: false, foreground: true) == "Playback stalled", "stall watchdog")
for state in 0..<4 {
    expect(MPVPlaybackPolicy.watchdogFailure(rendered: true, loadingFor: 200, stalledFor: 30,
        playing: true, paused: state == 0, seeking: state == 1, ended: state == 2, foreground: state != 3) == nil,
        "pause/seek/end/background never recover")
}
if CommandLine.arguments.contains("--filter") { print(MPVPlaybackPolicy.audioFilter(percent: 200)) }
print("MPV recovery/cache/audio/display policies passed")
