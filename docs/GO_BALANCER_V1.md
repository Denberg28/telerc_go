# Go / balancer paired v1.0 candidate

Use the balancer source from this branch with Go 1.0.4/code1000. The prior Go
consumer handshake rejected the older balancer's zero custom-mode heartbeat
and absent ownership capability. That pairing was not compatible.

The candidate adds a nonzero boot identity and TELERC_OWN telemetry with Go
capability bit 11 and balancing profile bit 12. Owner 1 means the paired phone;
receiver-present remains false. Older Go parsers reject the new profile flag,
so this is a paired upgrade. It is not an ArduPilot mode or an ELRS claim.

Go accepts IN_PROGRESS ARM acknowledgments and waits up to a fixed three-second
response deadline without retrying ARM. Local calibration, verified directions,
upright/still checks, sensor freshness and the firmware's own arming window
remain mandatory. Use TeleRC Base's pre-test tools before Go driving.

For this profile, link/lifecycle loss cancels travel and releases remote axes;
the local upright balance loop remains active. Actual fall, IMU, power and
controller faults retain their firmware behavior. An explicit DISARM still
removes motor torque. After pause, Resume control requires operator intent,
fresh identity/ownership/heartbeat and a new neutral dwell before sticks enable.
No recovery heartbeat automatically resumes travel or re-arms.

The SBRB filter mounting regression now tests independent initial states with
the same filter reset as actual arming; a separate test bounds sign reversal.
Non-finite integral/filter limits reject output. This is software evidence only:
support the chassis for installation, direction checks, disarm and recovery tests.
Actual balancing, stall/power behavior and physical acceptance remain unverified.
