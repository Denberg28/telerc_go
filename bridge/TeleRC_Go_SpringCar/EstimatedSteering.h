#pragma once
#include "RoverCore.h"
namespace car {
// All angles below are ESTIMATES. A two-wire motor supplies no measured angle.
struct SteeringTiming {
  uint16_t leftOut=350,rightOut=350,leftBack=180,rightBack=180;
  bool valid() const {
    return leftOut>=50&&leftOut<=1200&&rightOut>=50&&rightOut<=1200&&
           leftBack>=50&&leftBack<=1200&&rightBack>=50&&rightBack<=1200;
  }
};
struct EstimatedSteering {
  SteeringTiming timing;
  float position=0, previousGoal=0;
  int16_t previousPWM=0;
  uint32_t last=0,settleSince=0;
  bool clock=false,centering=true,ready=false,brake=false;
  void recenter(uint32_t now) {
    position=0; previousGoal=0; previousPWM=0; last=now; clock=true;
    settleSince=now; centering=true; ready=false; brake=false;
  }
  // Disarm/fault always coasts. Begin a fresh spring reference only once;
  // repeated stop frames must not indefinitely restart the settle clock.
  void stopped(uint32_t now) {
    if(!centering) recenter(now);
    previousPWM=0; brake=false; last=now; clock=true;
  }
  void disabledTick(uint32_t now,bool bootPressed) {
    stopped(now);
    if(bootPressed) { settleSince=now;ready=false; }
    else if(rover::age(now,settleSince)>=config::STEER_CENTER_SETTLE_MS) {
      ready=true; position=0;
    }
  }
  float segmentTime(int sign) const {
    if(sign>0) return position<0 ? timing.leftBack : timing.rightOut;
    return position>0 ? timing.rightBack : timing.leftOut;
  }
  void integrate(uint32_t now) {
    if(!clock) { last=now;clock=true;return; }
    uint32_t elapsed=rover::age(now,last);last=now;
    // Motor commands held through a delayed tick are included, but never replay
    // old motion beyond command expiry; the outer watchdog coasts/disarms first.
    if(elapsed>config::CONTROL_TIMEOUT_MS) elapsed=config::CONTROL_TIMEOUT_MS;
    float budget=float(elapsed)*fabsf(float(previousPWM))/config::STEER_MAX_PWM;
    const int sign=previousPWM>0?1:previousPWM<0?-1:0;
    for(int segment=0;segment<2 && sign && budget>0;++segment) {
      float boundary=sign>0 ? (position<0?0.f:1.f) : (position>0?0.f:-1.f);
      if(sign>0 && previousGoal<boundary) boundary=previousGoal;
      if(sign<0 && previousGoal>boundary) boundary=previousGoal;
      const float distance=(boundary-position)*sign;
      if(distance<=0) break;
      const float fullTime=segmentTime(sign), needed=distance*fullTime;
      if(budget>=needed) { position=boundary;budget-=needed; }
      else { position+=sign*budget/fullTime;budget=0; }
    }
    position=fmaxf(-1.f,fminf(1.f,position));
  }
  int16_t request(float target,uint32_t now) {
    integrate(now);brake=false;
    target=fmaxf(-1.f,fminf(1.f,target));
    if(centering) {
      if(rover::age(now,settleSince)<config::STEER_CENTER_SETTLE_MS) return 0;
      position=0;ready=true;
      if(target==0) return 0;
      centering=false;
    }
    const float error=target-position;
    if(fabsf(error)<=config::STEER_POSITION_TOLERANCE) {
      if(target==0) { recenter(now);return 0; } // spring finishes the center return
      previousGoal=target;brake=true;return 0;
    }
    previousGoal=target;
    return error>0 ? config::STEER_MAX_PWM : -config::STEER_MAX_PWM;
  }
  void applied(int16_t logicalPWM) { previousPWM=logicalPWM; }
};
}
