#pragma once
#include "RoverCore.h"

namespace car {
// PWM slew and reversal dead time only. EstimatedSteering and MotorOutputs
// own the timed movement, electrical brake and center-return policy.
struct Motor {
  int16_t output=0;
  int8_t lastDirection=0;
  uint32_t zeroSince=0;
  bool zeroTracking=false;
  void coast(uint32_t now) {
    // Keep the real coast start through disarm/rearm and zero commands.
    if(!zeroTracking) { zeroSince=now; zeroTracking=true; }
    output=0;
  }
  int16_t update(int16_t target,uint32_t now,uint8_t step) {
    const int8_t direction=target>0 ? 1 : target<0 ? -1 : 0;
    if(!direction) { coast(now); return 0; }
    if(lastDirection && direction!=lastDirection) {
      coast(now);
      if(rover::age(now,zeroSince)<config::REVERSAL_COAST_MS) return 0;
    }
    lastDirection=direction; zeroTracking=false;
    // Bounded acceleration AND reductions; neutral/failsafe bypass ramping.
    const int delta=int(target)-output;
    output=int16_t(output+(delta>step ? step : delta<-int(step) ? -int(step) : delta));
    return output;
  }
};

// Per-peer sequence window. An exact duplicate or backward frame cannot feed
// the command watchdog or replay ARM. This is ordering, not authentication.
struct Sequence {
  bool seen=false;
  uint8_t last=0;
  bool accept(uint8_t next) {
    const uint8_t distance=uint8_t(next-last);
    if(seen && (distance==0 || distance>=128)) return false;
    seen=true; last=next; return true;
  }
};
}
