#pragma once
// SBRB-inspired derivative-on-measurement pitch control.
// Independent implementation for TeleRC's encoderless MX1508 hardware.
// Reference: https://github.com/jaguilar/sbrb (MIT, James Aguilar, 2026).
#include <math.h>
struct SbrbPitchController {
  float filteredRate=0, integral=0;
  bool initialized=false;
  void reset(){filteredRate=integral=0;initialized=false;}
  float step(float error,float gyroRate,float dt,float kp,float ki,float kd,
             float integralLimit,float outputLimit,float rateTau) {
    if(!isfinite(error)||!isfinite(gyroRate)||!isfinite(dt)||dt<=0 ||
       !isfinite(kp)||!isfinite(ki)||!isfinite(kd)||!isfinite(outputLimit)||
       !isfinite(integralLimit)||!isfinite(rateTau)||
       outputLimit<=0||integralLimit<0||rateTau<0) return NAN;
    if(!initialized){filteredRate=gyroRate;initialized=true;}
    else {
      const float alpha=dt/(rateTau+dt);
      filteredRate+=alpha*(gyroRate-filteredRate);
    }
    const float pd=kp*error+kd*filteredRate;
    const float proposed=fmaxf(-integralLimit,fminf(integralLimit,integral+ki*error*dt));
    const float candidate=pd+proposed;
    // Reject integral growth when already pushing farther into an output rail.
    if(fabsf(candidate)<=outputLimit ||
       (candidate>outputLimit && ki*error<0) ||
       (candidate< -outputLimit && ki*error>0)) integral=proposed;
    return pd+integral;
  }
};
