// Compile the actual sample/control path with the reference-style mounting.
#include <cassert>
#include "../TT_Balancer_ESP32S3.ino"
int main() {
  setup(); loop();
  assert(mode==Mode::DISARMED && calibrated && calDiag.count==600);
  assert(!directionsVerified && motorCommand==0);
  auto gx=[](int16_t value) {
    Wire.regs[0x43]=uint16_t(value)>>8;
    Wire.regs[0x44]=uint16_t(value)&255;
  };
  Wire.frame(0,-0.1f,0.995f,0);gx(-1310);
  Sample s; assert(readSample(s)==1);
  assert(s.ax<0 && s.ay==0 && s.gy==10 && s.gx==0);
  assert(accelAngle(s)>5 && accelAngle(s)<6);
  updateControl(s,0.004f); assert(rate==10 && angle>0 && motorCommand==0);
  mode=Mode::BALANCING; angle=0; pitchControl.reset();
  updateControl(s,0.004f);
  assert(motorCommand>0 && duty[LEFT_IN1]>0 && duty[RIGHT_IN2]>0);
  Wire.frame(0,0.1f,0.995f,0);gx(1310);
  assert(readSample(s)==1 && accelAngle(s)<-5 && s.gy==-10);
  // This is the mirrored independent mounting case. Reset the rate filter,
  // as the real arm/disarm transition does, rather than carrying a positive
  // derivative history into a manually forced negative-angle initial state.
  angle=0; pitchControl.reset(); updateControl(s,0.004f);
  assert(motorCommand<0 && duty[LEFT_IN2]>0 && duty[RIGHT_IN1]>0);
  disarm("mount test complete");
  Wire.frame(0.2f,0,0.98f,0);gx(0);
  assert(readSample(s)==1 && s.ay<0 && s.ax==0);
  assert(!strcmp(calibrationSampleBlock(s,1,0),"CAL_SIDE_TILT"));
  Wire.busOK=false; assert(readSample(s)==-1);
  std::puts("Reference mounting: sample, angle/rate signs, side tilt and bus fault PASS");
}
