package com.ai.mirror

import com.ai.mirror.data.streaming.StreamStatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamStatsTest {

    @Test
    fun testMetricsInitialization() {
        val stats = StreamStatsCalculator()
        val metrics = stats.getMetrics()
        assertEquals(0f, metrics.fps, 0.001f)
        assertEquals(0f, metrics.bitrateKbps, 0.001f)
        assertEquals(0L, metrics.latencyMs)
        assertEquals(0L, metrics.totalFrames)
        assertEquals(0L, metrics.droppedFrames)
    }

    @Test
    fun testFrameProcessingAndDroppedTracking() {
        val stats = StreamStatsCalculator()

        stats.onFrameProcessed(bytes = 1024, width = 1280, height = 720, latencyMs = 35)
        stats.onFrameProcessed(bytes = 2048, width = 1280, height = 720, latencyMs = 40)
        stats.onFrameDropped()

        val metrics = stats.getMetrics()
        assertEquals(2L, metrics.totalFrames)
        assertEquals(1L, metrics.droppedFrames)
        assertEquals(1280, metrics.width)
        assertEquals(720, metrics.height)
        assertEquals(40L, metrics.latencyMs)
    }

    @Test
    fun testReset() {
        val stats = StreamStatsCalculator()
        stats.onFrameProcessed(bytes = 5000, width = 640, height = 480, latencyMs = 20)
        stats.onFrameDropped()

        stats.reset()

        val metrics = stats.getMetrics()
        assertEquals(0L, metrics.totalFrames)
        assertEquals(0L, metrics.droppedFrames)
        assertEquals(0L, metrics.latencyMs)
    }
}
