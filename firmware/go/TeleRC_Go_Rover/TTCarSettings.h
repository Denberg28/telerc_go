#pragma once
#include <stdint.h>

// Bare TT steering motor: these bound energization time, NOT steering angle.
// Begin with the linkage disconnected; tune for the actual gearbox and travel.
namespace tt_car {
constexpr uint32_t STEER_PULSE_MS=50;
constexpr uint32_t STEER_NEUTRAL_REARM_MS=200;
constexpr uint8_t STEER_MIN_PWM=160;
constexpr uint8_t STEER_MAX_PWM=200;
constexpr bool INVERT_STEERING=false;
constexpr bool INVERT_DRIVE=false;
static_assert(STEER_PULSE_MS>=20 && STEER_PULSE_MS<=250,"Steering pulse must be 20..250 ms");
static_assert(STEER_NEUTRAL_REARM_MS>=200,"Require a deliberate neutral gap between jogs");
static_assert(STEER_MIN_PWM>0 && STEER_MIN_PWM<=STEER_MAX_PWM && STEER_MAX_PWM<=242,"Invalid steering duty range");
}
