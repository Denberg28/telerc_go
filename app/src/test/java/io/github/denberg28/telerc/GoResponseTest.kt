package io.github.denberg28.telerc

import org.junit.Assert.*
import org.junit.Test

class GoResponseTest {
    private fun command(settings: ControlResponse, axis: Float, vertical: Boolean = false): Int =
        settings.channel(if (vertical) StickPosition(0f, -axis) else StickPosition(axis, 0f),
            vertical, ControlResponse.GO_RECEIVER_DEADBAND_US)

    private fun practice(): MazePractice {
        val rows = listOf("#######", "#.....#", "#.....#", "#.....#", "#.....#", "#.....#", "#######")
        return MazePractice(MazeLayout(42, 7, 7, rows.joinToString("").map { it == '#' }.toBooleanArray()))
            .apply { start() }
    }

    @Test fun assignedAxisKeepsFullRangeWhenFingerDrifts() {
        assertEquals(StickPosition(1f, 0f), limitStick(1f, -5f, false))
        assertEquals(StickPosition(0f, -1f), limitStick(5f, -1f, true))
        assertEquals(StickPosition(.12f, 0f), limitStick(.12f, -5f, false))
        assertEquals(StickPosition(0f, 0f), limitStick(1f, 0f, true))
    }

    @Test fun smallTravelClearsReceiverWindowAtEverySetting() {
        for (gain in listOf(25, 50, 100)) for (expo in listOf(0, 50, 100)) {
            val settings = ControlResponse(gain, gain, expo)
            for (travel in listOf(.09f, .12f, .2f)) for (vertical in listOf(false, true)) {
                val delta = command(settings, travel, vertical) - 1500
                assertTrue("Low travel lost at $gain% / expo $expo", delta > 35)
                assertTrue(delta <= gain * 4)
                assertEquals(1500 - delta, command(settings, -travel, vertical))
            }
        }
    }

    @Test fun neutralStaysFixedAndAxisGainsStayIndependent() {
        val settings = ControlResponse(25, 75, 100)
        for (axis in listOf(-.08f, -.02f, 0f, .02f, .08f)) {
            assertEquals(1500, command(settings, axis))
            assertEquals(1500, command(settings, axis, true))
        }
        assertEquals(1600, command(settings, 1f))
        assertEquals(1400, command(settings, -1f))
        assertEquals(1800, command(settings, 1f, true))
        assertEquals(1200, command(settings, -1f, true))
    }

    @Test fun responseIsSymmetricMonotonicAndCapped() {
        for (gain in listOf(25, 50, 100)) for (expo in listOf(0, 50, 100)) {
            val settings = ControlResponse(gain, gain, expo)
            var previous = 1500
            for (step in 0..1000) {
                val axis = step / 1000f
                val value = command(settings, axis)
                assertTrue(value >= previous)
                assertTrue(value <= 1500 + gain * 4)
                assertEquals(3000 - value, command(settings, -axis))
                assertEquals(value, command(settings, axis, true))
                previous = value
            }
            assertEquals(1500 + gain * 4, previous)
        }
    }

    @Test fun testRoverRespondsToSmallCommandsInBothDirections() {
        for (gain in listOf(25, 50, 100)) for (expo in listOf(0, 100)) for (direction in listOf(-1f, 1f)) {
            val settings = ControlResponse(gain, gain, expo)
            val drive = practice()
            drive.drive(command(settings, .12f * direction, true))
            drive.advance(20)
            assertTrue(drive.pose.speed * direction > 0f)
            val steer = practice()
            val heading = steer.pose.heading
            steer.steering(command(settings, .12f * direction))
            steer.advance(20)
            assertTrue((steer.pose.heading - heading) * direction > 0f)
            assertEquals(0f, steer.pose.speed, .00001f)
        }
    }

    @Test fun minimumReceiverCommandMovesWithoutSecondDeadZone() {
        val drive = practice()
        drive.drive(1526)
        drive.advance(20)
        assertTrue(drive.pose.speed > 0f)
        drive.pause()
        assertEquals(0f, drive.pose.speed, 0f)
        val neutral = practice()
        neutral.drive(1525)
        neutral.steering(1475)
        neutral.advance(20)
        assertEquals(0f, neutral.pose.speed, 0f)
        assertEquals(0f, neutral.pose.leftTrack, 0f)
        assertEquals(0f, neutral.pose.rightTrack, 0f)
    }

    @Test fun invalidValuesRemainNeutral() {
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(1500, command(ControlResponse(), bad))
            assertEquals(StickPosition(0f, 0f), limitStick(bad, .5f, true))
        }
        assertEquals(1500, stickChannel(StickPosition(1f, 0f), false, receiverDeadbandUs = -1))
        assertEquals(1500, stickChannel(StickPosition(1f, 0f), false, receiverDeadbandUs = 100))
    }
}
