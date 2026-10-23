#include <cassert>
#include <cstdlib>
#include <iostream>
#include <string>
#define TELERC_HOST_TEST 1
#include "../TeleRC_MiniRover_WiFi/TeleRC_MiniRover_WiFi.ino"

const IPAddress phone(192,168,4,2);
static void zero() { for(unsigned pin=4;pin<=11;++pin)assert(duties[pin]==0); }
static void tick(uint32_t ms=5) {fakeTime+=ms;watchdog(fakeTime);updateMotors(fakeTime);}
static void send(uint8_t id,const uint8_t *payload,uint8_t len,uint8_t extra) {
  uint8_t bytes[80];const auto n=mini_mav::encode(bytes,0,255,190,id,payload,len,extra);
  udp.incoming.push_back({phone,14550,{bytes,bytes+n}});
  watchdog(fakeTime);receiveOne();watchdog(fakeTime);updateMotors(fakeTime);
}
static void rc(uint16_t s=1500,uint16_t d=1500) {
  uint8_t p[18]={};mini_mav::put16(p,s);mini_mav::put16(p+2,d);p[16]=p[17]=1;send(70,p,18,124);
}
static void arm() {
  for(unsigned i=0;i<4;++i){rc();tick(100);}
  uint8_t p[33]={};float one=1;memcpy(p,&one,4);mini_mav::put16(p+28,400);p[30]=p[31]=1;send(76,p,33,152);
  assert(state.armed);
}
int main(int argc,char **argv) {
  assert(argc>=2);const std::string mode=argv[1];const unsigned pin=argc==3?unsigned(std::atoi(argv[2])):4;
  if(mode=="attach") {
    motor_output::Bank b;failAttachChannel=int(pin-4);fakeTime=100;
    assert(b.prepare());assert(!b.begin());assert(!b.ready && b.fault && b.attached==0);zero();
    std::cout<<"PASS: failed PWM channel "<<pin-4<<" cannot leave a partial motor bank ready\n";return 0;
  }
  fakeTime=100;setup();assert(motorBank.attached==0xff && pwmReady);zero();
  for(unsigned ch=0;ch<8;++ch)assert(channelPins[ch]==int(ch+4));
  if(mode=="idle") {
    // Unexpected energization while disarmed is still checked; no drive packet is needed.
    duties[pin]=17;tick();assert(motorBank.fault && !pwmReady && !state.armed);zero();return 0;
  }
  udp.incoming.push_back({phone,14550,{DISCOVERY,DISCOVERY+sizeof(DISCOVERY)-1}});receiveOne();arm();
  rc(1500,1900);tick();
  for(unsigned p:{4,6,8,10})assert(duties[p]==242);
  for(unsigned p:{5,7,9,11})assert(duties[p]==0);
  // Repeated unchanged input must keep all four channels active beyond one tick.
  for(unsigned i=0;i<10;++i){tick(20);rc(1500,1900);assert(state.armed);}
  if(mode=="mapping") {
    rc(1500,1100);tick();zero();tick(30);
    for(unsigned p:{5,7,9,11})assert(duties[p]==242);
    for(unsigned p:{4,6,8,10})assert(duties[p]==0);
    rc(1900,1500);tick();tick(30);
    for(unsigned p:{4,6,9,11})assert(duties[p]==242);
    for(unsigned p:{5,7,8,10})assert(duties[p]==0);
    tick(500);assert(!state.armed);zero();return 0;
  }
  assert(mode=="drift");
  // Simulate a PWM leg silently losing duty or energizing its inactive leg.
  // Cached unchanged commands previously never detected this peripheral state.
  duties[pin]=duties[pin]?0:17;tick();
  assert(motorBank.fault && motorBank.readbackErrors==1 && motorBank.attached==0);
  assert(!state.armed && !pwmReady && std::string(lastStop)=="PWM_READBACK");zero();
  for(unsigned p=4;p<=11;++p)assert(pins[p]==LOW);
  for(unsigned i=0;i<4;++i){rc();tick(100);}
  uint8_t a[33]={};float one=1;memcpy(a,&one,4);mini_mav::put16(a+28,400);a[30]=a[31]=1;send(76,a,33,152);
  assert(!state.armed);zero();
}
