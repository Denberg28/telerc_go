#pragma once
#include <stdint.h>
#include <math.h>
#include "TTCarSettings.h"

namespace tt_car {
// No position estimate, homing, auto-centering or powered holding at neutral.
// A direction flip cannot bypass the exhausted pulse or neutral dwell.
struct SteeringJog {
  bool ready=true, active=false, trackingNeutral=false;
  int8_t direction=0;
  uint32_t started=0, neutralSince=0;
  void reset() { *this=SteeringJog(); }
  int16_t update(float axis,uint32_t now) {
    const int8_t requested=axis>0 ? 1 : axis<0 ? -1 : 0;
    if(!requested) {
      active=false;
      if(!trackingNeutral) {trackingNeutral=true;neutralSince=now;}
      if(uint32_t(now-neutralSince)>=STEER_NEUTRAL_REARM_MS) ready=true;
      return 0;
    }
    trackingNeutral=false;
    if(active && (requested!=direction || uint32_t(now-started)>=STEER_PULSE_MS)) active=false;
    if(!active) {
      if(!ready) return 0;
      ready=false;active=true;direction=requested;started=now;
    }
    const float magnitude=fminf(1.f,fabsf(axis));
    const int duty=STEER_MIN_PWM+int(lroundf(magnitude*(STEER_MAX_PWM-STEER_MIN_PWM)));
    return int16_t(requested*duty);
  }
};
}
