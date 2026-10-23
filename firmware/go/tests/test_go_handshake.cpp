#include <cassert>
#include <cstring>
#include <iostream>
#include "../TeleRC_MiniRover_WiFi/TeleRC_MiniRover_WiFi.ino"

static uint32_t u32(const uint8_t *p) {
  return uint32_t(p[0]) | uint32_t(p[1])<<8 | uint32_t(p[2])<<16 | uint32_t(p[3])<<24;
}
// Independent check of the exact MAVLink 1 wire shape consumed by GoElrs.status.
static void status(uint8_t owner) {
  heartbeat();
  const auto &hb=udp.outgoing[udp.outgoing.size()-2].data;
  assert(hb.size()==17 && hb[5]==0 && u32(hb.data()+6)==bootNonce && bootNonce!=0);
  assert(hb[10]==10 && hb[11]==0 && (hb[12]&128)==(state.armed?128:0));
  const auto &b=udp.outgoing.back().data;
  assert(b.size()==26 && b[0]==254 && b[1]==18 && b[3]==1 && b[4]==1 && b[5]==252);
  assert(!std::memcmp(b.data()+14,"TELERC_OWN",10));
  assert(u32(b.data()+10)==uint32_t(owner|2048));
  uint16_t crc=65535;
  for(size_t i=1;i<24;++i) crc=mini_mav::accumulate(crc,b[i]);
  assert(mini_mav::accumulate(crc,44)==mini_mav::u16(b.data()+24));
}
static void command(float value,uint16_t cmd=31010,bool extra=false,IPAddress from=IPAddress(192,168,4,2),uint16_t port=49152) {
  uint8_t p[33]={},b[80]; std::memcpy(p,&value,4);
  mini_mav::put16(p+28,cmd);p[30]=p[31]=1;if(extra)p[4]=1;
  const auto n=mini_mav::encode(b,0,255,190,76,p,33,152);
  udp.incoming.push_back({from,port,{b,b+n}});receiveOne();
}
int main() {
  fakeTime=100;setup();assert(phoneOwner==0 && !state.armed);status(0);
  const auto nonce=bootNonce;
  const IPAddress phone(192,168,4,2);
  udp.incoming.push_back({phone,49152,{DISCOVERY,DISCOVERY+sizeof(DISCOVERY)-1}});receiveOne();
  assert(peer==phone && peerPort==49152);status(1);
  command(1,400);assert(!state.armed); // Acquisition/discovery cannot bypass neutral ARM.
  stopRover(true,"TEST");status(0);
  command(1,31010,true);assert(phoneOwner==0);
  command(0);assert(phoneOwner==0);
  command(1,31010,false,IPAddress(192,168,4,3));assert(phoneOwner==0);
  command(1,31010,false,phone,49153);assert(phoneOwner==0); // No peer-port stealing.
  command(1);assert(phoneOwner==1 && !state.armed && !state.haveRC);status(1);
  pins[0]=LOW;watchdog(fakeTime);command(1);assert(phoneOwner==0 && !state.armed);status(0);
  pins[0]=HIGH;watchdog(fakeTime);command(1);status(1);
  udp.incoming.push_back({phone,49152,{DISCONNECT,DISCONNECT+sizeof(DISCONNECT)-1}});receiveOne();
  assert(peer==ZERO_IP && !state.armed && phoneOwner==0);status(0);
  setup();assert(bootNonce!=nonce && bootNonce!=0);status(0);
  std::cout<<"PASS: Go boot/session, owner/capability, source-port lock and non-arming acquisition\n";
}
