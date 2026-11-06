package io.github.denberg28.telerc

import org.junit.Assert.*
import org.junit.Test

/** Measure magnitude and first-frame response; merely nonzero motion misses lag. */
class GoProportionalResponseTest {
    private val deadband = ControlResponse.GO_RECEIVER_DEADBAND_US
    private fun command(gain: Int, axis: Float, vertical: Boolean, expo: Int = 100): Int =
        ControlResponse(gain, gain, expo).proportionalChannel(
            if (vertical) StickPosition(0f, -axis) else StickPosition(axis, 0f), vertical, deadband)
    private fun practice(): MazePractice {
        val rows = listOf("#######", "#.....#", "#.....#", "#.....#", "#.....#", "#.....#", "#######")
        return MazePractice(MazeLayout(42, 7, 7, rows.joinToString("").map { it == '#' }.toBooleanArray()),
            inputDeadbandUs = deadband, immediateResponse = true).apply { start() }
    }
    @Test fun lineFractionsProduceSelectedReceiverDemandOnFirstFrame() {
        for (gain in listOf(25, 50, 75, 100)) for (fraction in listOf(.09f, .12f, .25f, .5f, .75f, 1f))
            for (direction in listOf(-1f, 1f)) for (vertical in listOf(false, true)) {
                val p = practice()
                val value = command(gain, fraction * direction, vertical)
                if (vertical) p.drive(value) else p.steering(value)
                p.advance(16)
                val expected = fraction * direction * gain / 100f
                assertEquals("Line $fraction gain $gain axis $vertical", expected, p.pose.leftTrack, 1f / 365f)
                assertEquals(if (vertical) expected else -expected, p.pose.rightTrack, 1f / 365f)
            }
    }
    @Test fun holdingAndChangingInputDoNotAddAccelerationOrReversalLag() {
        val p = practice()
        p.steering(command(50, .5f, false)); p.advance(16)
        val first = p.pose.leftTrack
        repeat(3) { p.advance(16); assertEquals(first, p.pose.leftTrack, 0f) }
        p.steering(command(50, -.5f, false)); p.advance(16)
        assertEquals(-first, p.pose.leftTrack, 0f)
        p.steering(1500); p.advance(16)
        assertEquals(0f, p.pose.leftTrack, 0f)
        assertEquals(0f, p.pose.rightTrack, 0f)
    }
    @Test fun fixedNeutralGuardAndIndependentSensitivityRemain() {
        val s = ControlResponse(25, 75, 100)
        for (axis in listOf(-.08f, -.04f, 0f, .04f, .08f)) {
            assertEquals(1500, command(100, axis, false))
            assertEquals(1500, command(100, axis, true))
        }
        assertEquals(1626, s.proportionalChannel(StickPosition(1f, 0f), false, deadband))
        assertEquals(1809, s.proportionalChannel(StickPosition(0f, -1f), true, deadband))
        for (gain in 25..100) {
            var previous = 1500
            for (i in 0..1000) {
                val axis = i / 1000f
                val value = command(gain, axis, false)
                assertTrue(value >= previous && value in 1500..1900)
                assertEquals(3000 - value, command(gain, -axis, false))
                assertEquals(value, command(gain, axis, false, 0)) // Hidden legacy expo cannot soften Go.
                previous = value
            }
        }
    }
    @Test fun invalidInputAndResetStayNeutral() {
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(1500, command(100, bad, false))
            assertEquals(1500, proportionalStickChannel(StickPosition(1f, 0f), false, bad, deadband))
        }
        assertEquals(1500, proportionalStickChannel(StickPosition(1f, 0f), false, 1f, -1))
        assertEquals(1500, proportionalStickChannel(StickPosition(1f, 0f), false, 1f, 100))
        val p = practice(); p.drive(1900); p.advance(16); p.pause()
        assertEquals(0f, p.pose.leftTrack, 0f)
        p.start(); p.advance(16); assertEquals(0f, p.pose.speed, 0f)
    }
}
