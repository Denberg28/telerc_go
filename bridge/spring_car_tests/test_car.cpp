#include <cassert>
#include <iostream>
#include <random>
#include <utility>
#define TELERC_HOST_TEST 1
#include "../TeleRC_Go_SpringCar/TeleRC_Go_SpringCar.ino"
const IPAddress phone(192,168,4,2), other(192,168,4,3);
uint8_t sequence=0;
std::vector<uint8_t> frame(uint8_t id,const uint8_t *p,uint8_t len,uint8_t extra) {
  uint8_t b[80]; const size_t n=mini_mav::encode(b,sequence++,255,190,id,p,len,extra); return {b,b+n};
}
void advance(uint32_t ms) {
  // Run the same timeout/output ordering as the real loop at every motor tick.
  while(ms) { const auto n=ms>5?5:ms; fakeTime+=n; ms-=n; watchdog(fakeTime);updateMotors(fakeTime); }
}
void receive(const std::vector<uint8_t> &bytes,IPAddress source=phone,uint16_t port=49152) {
  udp.incoming.push_back({source,port,bytes});watchdog(fakeTime);receiveOne();watchdog(fakeTime);updateMotors(fakeTime);
}
void discover() { receive({DISCOVERY,DISCOVERY+sizeof(DISCOVERY)-1}); }
std::vector<uint8_t> rcFrame(uint16_t s=1500,uint16_t d=1500) {
  uint8_t p[18]={};mini_mav::put16(p,s);mini_mav::put16(p+2,d);p[16]=p[17]=1;return frame(70,p,18,124);
}
void rc(uint16_t s=1500,uint16_t d=1500) { receive(rcFrame(s,d)); }
std::vector<uint8_t> cmdFrame(float value,uint16_t command=400,bool force=false) {
  uint8_t p[33]={};memcpy(p,&value,4);mini_mav::put16(p+28,command);p[30]=p[31]=1;if(force)p[4]=1;return frame(76,p,33,152);
}
void command(bool arm) { receive(cmdFrame(arm?1.f:0.f)); }
void dwell(uint16_t s=1500,uint16_t d=1500,unsigned ms=350) {
  while(ms) { rc(s,d); const auto n=ms>25?25:ms;advance(n);ms-=n; }
}
void zero() {for(unsigned p:{4,5,8,9})assert(duties[p]==0);}
void arm() { dwell();command(true);assert(state.armed); }
uint32_t u32(const uint8_t *p) {return uint32_t(p[0])|uint32_t(p[1])<<8|uint32_t(p[2])<<16|uint32_t(p[3])<<24;}
void telemetry() {
  heartbeat();const auto &hb=udp.outgoing[udp.outgoing.size()-2].data;const auto &own=udp.outgoing.back().data;
  assert(hb.size()==17 && hb[5]==0 && u32(hb.data()+6)==bootNonce && bootNonce);
  assert(hb[10]==10 && hb[11]==0 && bool(hb[12]&128)==state.armed);
  assert(own.size()==26 && own[0]==254 && own[1]==18 && own[3]==1 && own[4]==1 && own[5]==252);
  assert(!memcmp(own.data()+14,"TELERC_OWN",10));
  const uint32_t bits=uint32_t(phoneOwner)|2048u|((bootHeld||!pwmReady||motorBank.fault)?512u:0u);
  assert(u32(own.data()+10)==bits);
  // Independent X.25 CRC check, not the firmware encoder/decoder helper.
  uint16_t crc=65535;
  for(size_t i=1;i<=24;++i) {
    uint8_t t=uint8_t((i==24?44:own[i])^uint8_t(crc));t=uint8_t(t^(t<<4));
    crc=uint16_t((crc>>8)^(uint16_t(t)<<8)^(uint16_t(t)<<3)^(t>>4));
  }
  assert(own[24]==uint8_t(crc) && own[25]==uint8_t(crc>>8));
}
int main(int argc,char **argv) {
  assert(startLoopWatchdog());fakeWdtInit=ESP_ERR_INVALID_STATE;assert(startLoopWatchdog());
  fakeWdtStatus=ESP_OK;fakeWdtAdd=ESP_FAIL;assert(startLoopWatchdog());
  fakeWdtStatus=ESP_ERR_NOT_FOUND;assert(!startLoopWatchdog());fakeWdtAdd=ESP_OK;
  fakeWdtConfigure=ESP_FAIL;assert(!startLoopWatchdog());fakeWdtInit=ESP_OK;fakeWdtConfigure=ESP_OK;
  fakeTime=100;setup();assert(pwmReady&&wifiReady&&!state.armed);zero();telemetry();
  assert(hardware::MOTOR_COUNT==2&&motorBank.attached==15&&hardware::PWM_HZ==20000);
  command(true);assert(!state.armed);discover();assert(peerPort==49152);telemetry();
  if(argc==2) {
    arm();dwell(1500,1900,400);
    if(!strcmp(argv[1],"watchdog")) { fakeWdtReset=ESP_FAIL;loop();assert(!strcmp(lastStop,"WDT_RESET_ERROR")); }
    else {
#if ESP_ARDUINO_VERSION_MAJOR >= 3
      failWritePin=4;rc(1500,1800);advance(5);assert(motorBank.fault&&motorBank.attached==0);
#else
      motorBank.attached=0;advance(5);assert(motorBank.fault);
#endif
    }
    assert(!state.armed&&!pwmReady);zero();telemetry();dwell();command(true);assert(!state.armed);
    std::cout<<"PASS fault injection: "<<argv[1]<<"\n";return 0;
  }
  command(true);assert(!state.armed);rc(1900,1900);command(true);assert(!state.armed);zero();
  arm();dwell(1500,1900,400);assert(duties[4]==242&&duties[5]==0&&duties[8]==0&&duties[9]==0);
  rc();advance(5);zero(); // Neutral bypasses acceleration ramp.
  dwell(1900,1500,120);assert(duties[8]==160&&duties[9]==0&&duties[4]==0&&duties[5]==0);
  dwell(1900,1900,500);assert(duties[4]==242&&steering.brake&&duties[8]==255&&duties[9]==255);
  assert(fabsf(steering.position-1.f)<0.02f); // Model only, never physical-angle evidence.
  dwell(1900,1900,10000);assert(state.armed&&motors[2].output==0&&steering.brake);
  rc(1100,1900);advance(5);assert(duties[8]==0&&duties[9]==0&&duties[4]>0);
  advance(25);assert(duties[8]==0&&duties[9]==0);advance(5);assert(duties[9]>0);
  dwell(1100,1900,800);assert(steering.brake&&fabsf(steering.position+1.f)<0.02f&&state.armed);
  rc(1500,1900);advance(5);assert(duties[8]==0&&duties[9]==0&&duties[4]>0);
  advance(30);assert(duties[8]>0); // Bounded active track-back, then spring finishing coast.
  dwell(1500,1900,600);assert(state.armed&&duties[8]==0&&duties[9]==0&&steering.position==0);
  dwell();
  dwell(1500,1100,400);assert(duties[5]==242&&duties[4]==0);rc();advance(5);zero();
  // Full RC range maps monotonically, symmetrically, and the existing app's
  // 1100/1900 endpoints hit full demand. Low inputs have no boost discontinuity.
  int previousD=-242,previousS=-160;
  for(int us=1000;us<=2000;++us) {
    rover::State st;st.armed=st.haveRC=true;st.inhibited=false;st.steer=st.drive=us;
    int16_t d,s;st.demands(d,s);assert(d>=previousD&&s>=previousS&&abs(d)<=242&&abs(s)<=160);
    previousD=d;previousS=s;
  }
  assert(rover::axis(1100)==-1&&rover::axis(1900)==1&&rover::axis(1475)==0&&rover::axis(1525)==0);
  rc(1500,1600);advance(5);assert(duties[4]<=4);dwell(1500,1600,100);assert(duties[4]==48);
  rc();advance(5);zero();
  // Six estimated target levels: finite movement, then brake with no continued
  // rotation command. Repeating a target for minutes cannot restart its pulse.
  for(const auto &item:{std::pair<uint16_t,float>{1900,1.f},{1713,0.501333f},{1600,0.2f},
                      {1100,-1.f},{1287,-0.501333f},{1400,-0.2f}}) {
    dwell(item.first,1900,1500);assert(state.armed&&phoneOwner==1&&steering.brake);
    assert(fabsf(steering.position-item.second)<=config::STEER_POSITION_TOLERANCE);
    assert(motors[2].output==0&&duties[4]==242&&duties[8]==255&&duties[9]==255);
  }
  dwell(1900,1900,120000);assert(state.armed&&steering.brake&&motors[2].output==0);
  dwell(1500,1900,700);assert(state.armed&&duties[8]==0&&duties[9]==0&&duties[4]==242);
  rc();advance(5);zero();
  // Faults/disarm must release the electrical brake, not leave both inputs high.
  dwell(1900,1900,1000);assert(steering.brake);command(false);zero();arm();
  dwell(1900,1900,1000);assert(steering.brake);advance(500);assert(!state.armed);zero();arm();
  // Exact duplicate RC, backward RC and duplicate ARM have no side effects.
  auto b=rcFrame(1500,1900);receive(b);advance(10);const auto at=state.lastRC;
  receive(b);assert(state.lastRC==at);
  uint8_t oldBytes[80];mini_mav::encode(oldBytes,uint8_t(b[2]-1),255,190,70,b.data()+6,18,124);
  receive({oldBytes,oldBytes+b.size()});assert(state.lastRC==at); // Valid CRC, backward sequence.
  advance(490);assert(!state.armed);zero(); // Duplicate never feeds the timeout.
  arm();const auto replayArm=cmdFrame(1);receive(replayArm);command(false);receive(replayArm);assert(!state.armed);
  arm();const auto driveFrame=rcFrame(1500,1900);receive(driveFrame);advance(200);discover();
  const auto last=state.lastRC;advance(300);assert(!state.armed&&state.lastRC==last);zero();
  command(true);assert(!state.armed);rc(1900,1900);command(true);assert(!state.armed);arm();
  command(false);assert(!state.armed&&phoneOwner==0);zero();
  arm();rc(0,0);assert(!state.armed&&phoneOwner==0);zero();arm();
  receive({DISCONNECT,DISCONNECT+sizeof(DISCONNECT)-1},other);assert(state.armed);
  receive(cmdFrame(0),phone,49153);assert(state.armed); // Source port cannot steal control.
  receive({DISCONNECT,DISCONNECT+sizeof(DISCONNECT)-1});assert(!state.armed&&peer==ZERO_IP);zero();
  discover();arm();WiFi.stations=0;watchdog(fakeTime);assert(!state.armed&&peer==ZERO_IP);zero();WiFi.stations=1;
  discover();arm();pins[0]=LOW;watchdog(fakeTime);assert(!state.armed);zero();telemetry();
  dwell();command(true);assert(!state.armed);pins[0]=HIGH;watchdog(fakeTime);arm();
  // Complete datagrams are validated atomically; a good disarm plus bad tail
  // must not partially execute or advance the per-peer sequence window.
  const auto seq=peerSequence.last;auto invalid=cmdFrame(0);invalid.push_back(254);receive(invalid);
  assert(state.armed&&peerSequence.last==seq);
  auto corrupt=rcFrame(1500,1900);corrupt.back()^=1;const auto old=state.lastRC;receive(corrupt);assert(state.lastRC==old);
  receive(std::vector<uint8_t>(300,254));assert(state.lastRC==old);
  command(false);dwell();receive(cmdFrame(1,400,true));assert(!state.armed);
  receive(cmdFrame(NAN));assert(!state.armed);receive(cmdFrame(1,31010,true));assert(!state.armed);
  receive(cmdFrame(1,31010));assert(phoneOwner==1&&!state.armed&&!state.haveRC);arm();
  // Disarmed calibration parsing/save/load, bounds, corruption and atomic
  // activation on storage success. Commands never initiate a motor sweep.
  command(false);dwell();
  Serial.input="CAL 400 420 210 220\n";for(int i=0;i<4;++i)loop();
  assert(steering.timing.leftOut==400&&steering.timing.rightBack==220);zero();
  car::SteeringTiming loaded;car::CalibrationStore restored;restored.begin(loaded);assert(loaded.rightOut==420);
  Serial.input="CAL 0 400 200 200\n";for(int i=0;i<4;++i)loop();assert(steering.timing.leftOut==400);
  failPreferenceWrite=true;calibrationCommand("CAL 350 350 180 180");assert(steering.timing.leftOut==400);
  failPreferenceWrite=false;calibrationCommand("CAL 350 350 180 180");assert(steering.timing.leftOut==350);
  auto record=preferenceData;preferenceData[0]^=1;car::SteeringTiming defaults;restored.begin(defaults);assert(defaults.leftOut==350);preferenceData=record;
  car::SteeringTiming parsed;assert(!car::parseTiming("CAL 350 350 180 180 tail",parsed));
  assert(!car::parseTiming("CAL 350 350 180",parsed));assert(!car::parseTiming("CAL 350x 350 180 180",parsed));
  assert(!car::parseTiming("CAL -350 350 180 180",parsed));assert(!car::parseTiming("CAL 99999999999999999 350 180 180",parsed));
  arm();calibrationCommand("CAL 500 500 250 250");assert(state.armed&&steering.timing.leftOut==350);
  command(false);calibrationCommand("CENTER");assert(!steering.ready);advance(250);assert(steering.ready);zero();arm();
  command(false);Serial.input=std::string(100,'X')+"\nCENTER\n";
  for(int i=0;i<12;++i)loop();
  assert(!state.armed&&!steering.ready);zero();advance(250);assert(steering.ready);arm();
  car::EstimatedSteering wrapped;wrapped.recenter(0xfffffe00u);wrapped.disabledTick(0xffffff00u,false);
  assert(wrapped.ready);wrapped.request(0.5f,0xfffffff0u);wrapped.applied(160);
  wrapped.request(0.5f,0x10u);assert(wrapped.position>0&&wrapped.position<0.5f);
  // Verify monotonic modulo sequence acceptance and millis-wrap behavior.
  car::Sequence order;assert(order.accept(254)&&order.accept(255)&&order.accept(0)&&!order.accept(255)&&!order.accept(0)&&order.accept(1));
  car::Motor reversal;assert(reversal.update(100,0xffffff00u,4)==4);assert(reversal.update(-100,0xfffffff0u,4)==0);
  assert(reversal.update(-100,0x0du,4)==0);assert(reversal.update(-100,0x0eu,4)==-4);
  rover::State expired;expired.rc(1500,1500,0xffffff00u);expired.rc(1500,1500,0xfffffffau);
  assert(expired.arm(0xfffffffau));expired.tick(0x200u);assert(!expired.armed);
  // Parser robustness: signed/truncated/random packets never overrun buffers.
  std::mt19937 rng(0x53505249);mini_mav::Message msg;size_t consumed;
  for(int i=0;i<20000;++i) {
    std::vector<uint8_t> data(rng()%300);for(auto &v:data)v=uint8_t(rng());
    if(!data.empty())data[0]=i%2?254:253;
    mini_mav::decode(data.data(),data.size(),msg,consumed);assert(consumed<=data.size());
  }
  // Serial backpressure never prevents command expiry.
  Serial.txSpace=0;advance(500);serviceDiagnostics(fakeTime);assert(!state.armed);zero();Serial.txSpace=64;
  for(int i=0;i<30;++i)serviceDiagnostics(fakeTime);
  assert(Serial.written.find("CH1=")!=std::string::npos);
  arm();dwell(1500,1900,400);duties[4]=17;advance(5);
  assert(motorBank.fault&&!state.armed&&!pwmReady&&motorBank.attached==0);zero();telemetry();
  // All later ARM/take requests stay rejected until reboot after a PWM fault.
  receive(cmdFrame(1,31010));dwell();command(true);assert(!state.armed);zero();
  // Failed attach must detach channels already attached and return GPIO-low.
  motor_output::Bank partial;failAttachChannel=2;assert(partial.prepare());assert(!partial.begin());
  assert(partial.fault&&partial.attached==0);zero();failAttachChannel=-1;
  std::cout<<"PASS spring car: independent motors, coast/ramp/reversal, estimated timed travel/brake/return and saved calibration, Go ownership, sequence/fault/timeout and 20000 parser probes\n";
}
