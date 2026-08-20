package com.ai.mirror.data.streaming

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class StreamMetrics(
    val fps: Float = 0f,
    val bitrateKbps: Float = 0f,
    val latencyMs: Long = 0L,
    val totalFrames: Long = 0L,
    val droppedFrames: Long = 0L,
    val width: Int = 0,
    val height: Int = 0
)

class StreamStatsCalculator {
    private val frameCount = AtomicInteger(0)
    private val byteCount = AtomicLong(0L)
    private val totalFrameCounter = AtomicLong(0L)
    private val droppedFrameCounter = AtomicLong(0L)
    private val latestLatency = AtomicLong(0L)

    private var lastCalculationTime = System.currentTimeMillis()
    private var currentFps = 0f
    private var currentBitrateKbps = 0f
    private var lastWidth = 0
    private var lastHeight = 0

    @Synchronized
    fun onFrameProcessed(bytes: Int, width: Int = 0, height: Int = 0, latencyMs: Long = 0L) {
        frameCount.incrementAndGet()
        byteCount.addAndGet(bytes.toLong())
        totalFrameCounter.incrementAndGet()
        if (width > 0) lastWidth = width
        if (height > 0) lastHeight = height
        if (latencyMs > 0) latestLatency.set(latencyMs)

        checkAndCalculate()
    }

    @Synchronized
    fun onFrameDropped() {
        droppedFrameCounter.incrementAndGet()
    }

    @Synchronized
    private fun checkAndCalculate() {
        val now = System.currentTimeMillis()
        val deltaMs = now - lastCalculationTime
        if (deltaMs >= 1000) {
            val frames = frameCount.getAndSet(0)
            val bytes = byteCount.getAndSet(0L)

            currentFps = (frames * 1000f) / deltaMs
            currentBitrateKbps = (bytes * 8f) / deltaMs // in Kbps or KB/s -> (bytes / 1024f) / (deltaMs / 1000f)
            lastCalculationTime = now
        }
    }

    @Synchronized
    fun getMetrics(): StreamMetrics {
        checkAndCalculate()
        return StreamMetrics(
            fps = currentFps,
            bitrateKbps = currentBitrateKbps,
            latencyMs = latestLatency.get(),
            totalFrames = totalFrameCounter.get(),
            droppedFrames = droppedFrameCounter.get(),
            width = lastWidth,
            height = lastHeight
        )
    }

    @Synchronized
    fun reset() {
        frameCount.set(0)
        byteCount.set(0L)
        totalFrameCounter.set(0L)
        droppedFrameCounter.set(0L)
        latestLatency.set(0L)
        currentFps = 0f
        currentBitrateKbps = 0f
        lastCalculationTime = System.currentTimeMillis()
    }
}
