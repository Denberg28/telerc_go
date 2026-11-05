#pragma once
#include <stdint.h>
#include <math.h>
#include "Config.h"

namespace rover {
inline uint32_t age(uint32_t now, uint32_t then) { return now - then; }
inline float axis(uint16_t us) {
  int delta = int(us) - 1500;
  if (delta >= -config::RC_DEADBAND_US && delta <= config::RC_DEADBAND_US) return 0;
  const float value = delta > 0 ? float(delta-config::RC_DEADBAND_US) / (config::RC_FULL_SCALE_US-config::RC_DEADBAND_US)
                                : float(delta+config::RC_DEADBAND_US) / (config::RC_FULL_SCALE_US-config::RC_DEADBAND_US);
  // Preserve valid wider MAVLink inputs without exceeding the existing PWM cap.
  return fmaxf(-1.0f,fminf(1.0f,value));
}
struct State {
  bool armed = false, inhibited = true, haveRC = false, trackingNeutral = false;
  uint16_t steer = 1500, drive = 1500;
  uint32_t lastRC = 0, neutralSince = 0;
  void stop(bool disarm) {
    if (disarm) armed = false;
    inhibited = true; haveRC = false; trackingNeutral = false;
    steer = drive = 1500;
  }
  void tick(uint32_t now) {
    if (haveRC && age(now,lastRC) >= config::CONTROL_TIMEOUT_MS) stop(true);
  }
  bool neutralReady(uint32_t now) const {
    return haveRC && trackingNeutral && age(now,lastRC) < config::CONTROL_TIMEOUT_MS &&
           age(now,neutralSince) >= config::NEUTRAL_HOLD_MS;
  }
  // Both motion channels must be explicit and fresh in the same frame.
  // 0 releases; 65535 never reuses a possibly stale motion command.
  bool rc(uint16_t s, uint16_t d, uint32_t now) {
    tick(now);
    if (s == 0 || d == 0) { stop(true); return true; }
    if (s < 1000 || s > 2000 || d < 1000 || d > 2000) return false;
    steer=s; drive=d; lastRC=now; haveRC=true;
    if (axis(s)==0 && axis(d)==0) {
      if (!trackingNeutral) { neutralSince=now; trackingNeutral=true; }
      if (neutralReady(now)) inhibited=false;
    } else {
      trackingNeutral=false;
    }
    return true;
  }
  bool arm(uint32_t now) {
    tick(now);
    if (!neutralReady(now)) return false;
    armed=true; inhibited=false; return true;
  }
  void demands(int16_t &driveOut,int16_t &steerOut) const {
    driveOut=steerOut=0;
    if(!armed || inhibited || !haveRC) return;
    driveOut=int16_t(lroundf(axis(drive)*config::MAX_PWM));
    steerOut=int16_t(lroundf(axis(steer)*config::STEER_MAX_PWM));
  }
};
}
