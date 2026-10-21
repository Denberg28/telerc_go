#include <cassert>
#include <cmath>
#include <iostream>
#include <vector>
#define TELERC_HOST_TEST 1
#include "../TeleRC_Go_TT_Car/TeleRC_Go_TT_Car.ino"

const IPAddress phone(192,168,4,2);
static void zero() {for(unsigned p:{4,5,6,7})assert(duties[p]==0);}
static void steerZero() {assert(duties[4]==0 && duties[5]==0);}
static void driveZero() {assert(duties[6]==0 && duties[7]==0);}
static void advance(uint32_t ms) {fakeTime+=ms;watchdog(fakeTime);updateMotors(fakeTime);}
static void receive(uint8_t id,const uint8_t *p,uint8_t len,uint8_t extra) {
  uint8_t bytes[80];static uint8_t seq=0;
  const auto n=mini_mav::encode(bytes,seq++,255,190,id,p,len,extra);
  udp.incoming.push_back({phone,14550,{bytes,bytes+n}});
  watchdog(fakeTime);receiveOne();watchdog(fakeTime);updateMotors(fakeTime);
}
static void rc(uint16_t steer=1500,uint16_t drive=1500) {
  uint8_t p[18]={};mini_mav::put16(p,steer);mini_mav::put16(p+2,drive);p[16]=p[17]=1;
  receive(70,p,18,124);
}
static void command(uint16_t cmd,float value) {
  uint8_t p[33]={};memcpy(p,&value,4);mini_mav::put16(p+28,cmd);p[30]=p[31]=1;
  receive(76,p,33,152);
}
static void armPhone() {
  command(31010,1);
  for(unsigned i=0;i<4;++i){rc();advance(100);}
  command(400,1);assert(state.armed);
}
#if TELERC_GO_ELRS
static void rf(uint16_t s=992,uint16_t d=992,uint16_t arm=172,uint16_t source=1811,uint16_t stop=172) {
  uint8_t b[26]={0xc8,24,0x16};
  for(unsigned c=0;c<16;++c){
    unsigned raw=c==0?s:c==1?d:c==4?arm:c==5?stop:c==6?source:992;
    for(unsigned bit=0;bit<11;++bit)if(raw&(1u<<bit))b[3+(c*11+bit)/8]|=1u<<((c*11+bit)%8);
  }
  b[25]=go_rf::crc8(b+2,23);
  for(auto v:b)rfSerial.incoming.push_back(v);
  serviceReceiver(fakeTime);watchdog(fakeTime);updateMotors(fakeTime);
}
#endif
int main() {
  // Continuous commands, direction flips and sub-dwell neutral cannot extend a jog.
  tt_car::SteeringJog jog;
  assert(jog.update(1,100)==200);
  assert(jog.update(1,249)==200);
  assert(jog.update(1,250)==0);
  assert(jog.update(-1,251)==0);
  assert(jog.update(0,260)==0);
  assert(jog.update(0,459)==0 && !jog.ready);
  assert(jog.update(1,460)==0); // Interrupting neutral one ms early resets dwell.
  assert(jog.update(0,470)==0);
  assert(jog.update(0,670)==0 && jog.ready);
  assert(jog.update(-1,675)==-200);
  assert(jog.update(1,680)==0); // A direct direction flip cancels, never reverses.
  assert(jog.update(-1,685)==0);
  jog.reset();assert(jog.update(0.01f,10)>=tt_car::STEER_MIN_PWM);
  assert(jog.update(0,11)==0); // Releasing is immediate even before pulse expires.
  jog.reset();assert(jog.update(1,0xffffffd0u)==200);
  assert(jog.update(1,uint32_t(0xffffffd0u+150u))==0); // millis rollover.

  fakeTime=100;setup();assert(pwmReady && wifiReady);zero();
  assert(hardware::MOTOR_COUNT==2 && hardware::PWM_CHANNELS==4);
  assert(hardware::MOTORS[0].a==4 && hardware::MOTORS[2].a==6);
  assert(!hardware::active(1) && !hardware::active(3));
  for(unsigned ch=0;ch<4;++ch)assert(channelPins[ch]==int(4+ch));
  udp.incoming.push_back({phone,14550,{DISCOVERY,DISCOVERY+sizeof(DISCOVERY)-1}});receiveOne();
#if TELERC_GO_ELRS
  rf(); // SA high allows the phone; no automatic arming.
#endif
  command(400,1);assert(!state.armed);armPhone();
  rc(1500,1900);advance(5);assert(duties[6]==242 && duties[7]==0);steerZero();
  rc(1500,1700);advance(5);assert(duties[6]==113); // Independent proportional rear drive.
  rc(1900,1700);advance(5);assert(duties[4]==200 && duties[6]==113);
  for(unsigned i=0;i<30;++i){advance(5);rc(1900,1700);}
  steerZero();assert(duties[6]==113 && state.armed); // Held steer times out, drive continues.
  rc(1100,1700);advance(5);steerZero();assert(duties[6]==113);
  rc(1500,1700);advance(5);advance(100);rc(1500,1700);advance(100);rc(1500,1700);advance(5);
  rc(1100,1500);advance(5);driveZero();advance(30);assert(duties[5]==200);
  rc();advance(5);zero(); // Stops steering motor; does not command a return to center.
  rc(1500,1100);advance(5);driveZero();advance(30);assert(duties[7]==242);
  command(400,0);assert(!state.armed);zero();
  armPhone();rc(1900,1900);advance(5);advance(500);assert(!state.armed);zero();
  rc(1900,1900);command(400,1);assert(!state.armed);zero(); // No auto recovery.
  armPhone();rc(1900,1900);advance(35);pins[0]=LOW;watchdog(fakeTime);assert(!state.armed);zero();
  pins[0]=HIGH;watchdog(fakeTime);armPhone();rc(1500,1900);advance(35);
  WiFi.stations=0;watchdog(fakeTime);assert(!state.armed && peer==ZERO_IP);zero();WiFi.stations=1;
  udp.incoming.push_back({phone,14550,{DISCOVERY,DISCOVERY+sizeof(DISCOVERY)-1}});receiveOne();
  armPhone();rc(1900,1900);advance(35);rc(0,0);assert(!state.armed);zero();
#if TELERC_GO_ELRS
  // Receiver-side SA takeover stops both mechanical functions and requires a fresh ARM edge.
  rf();armPhone();rc(1900,1900);advance(35);
  rf(992,992,1811,172);assert(!state.armed);zero();
  for(unsigned i=0;i<5;++i){advance(100);rf(992,992,172,172);}
  advance(5);rf(992,992,1811,172);assert(state.armed && authority.owner==go_rf::Authority::TRANSMITTER);
  advance(5);rf(1811,1811,1811,172);advance(35);rf(1811,1811,1811,172);
  assert(duties[4]>0 && duties[6]>0); // Allow the retained reversal-coast guard.
  for(unsigned i=0;i<4;++i){advance(50);rf(1811,1811,1811,172);}
  steerZero();assert(duties[6]>0);
  advance(300);serviceReceiver(fakeTime);assert(!state.armed);zero();
  rf(1811,1811,1811,172);assert(!state.armed);zero();
  rf();armPhone();rc(1500,1900);advance(35);rf(992,992,1811,1811,1811);assert(!state.armed);zero();
  rf();
#endif
  armPhone();rc(1500,1900);advance(35);
#if ESP_ARDUINO_VERSION_MAJOR >= 3
  failWritePin=6;rc(1500,1800);advance(5);
  assert(motorBank.fault && !pwmReady && !state.armed && motorBank.attached==0);zero();
#else
  command(400,0);zero();
#endif
  for(unsigned pin:{8,9,10,11})assert(duties[pin]==0); // No phantom motor channels.
  std::cout<<"PASS: TT car independent steering/drive, bounded jog/rearm, failsafes, RF exclusion and PWM faults\n";
}
