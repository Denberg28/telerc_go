#pragma once
#include <stdint.h>

// Two MX1508 boards, one channel per board. Unused outputs stay disconnected.
#define TELERC_DRIVER 1
namespace hardware {
constexpr uint8_t MOTOR_COUNT=2, PWM_CHANNELS=4;
constexpr uint8_t DRIVE=0, STEER=2;
struct Pins { uint8_t a,b,extra; };
constexpr Pins MOTORS[4]={{4,5,0},{0,0,0},{8,9,0},{0,0,0}};
constexpr bool active(uint8_t i) { return i==DRIVE || i==STEER; }
constexpr uint8_t ordinal(uint8_t i) { return i/2; }
constexpr uint8_t channelsPerMotor() { return 2; }
constexpr bool dualPWM() { return true; }
constexpr int GLOBAL_ENABLE=-1;
constexpr uint32_t WAKE_MS=0, PWM_HZ=20000;
// These are declarations, not voltage measurements. This build expects a
// 2S battery feeding a regulated 5 V MOTOR rail, NOT raw battery voltage.
constexpr uint32_t SUPPLY_MAX_MV=5000;
inline const char *driverName() { return "MX1508-spring-car"; }
}
