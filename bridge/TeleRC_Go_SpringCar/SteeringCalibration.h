#pragma once
#include <Preferences.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "EstimatedSteering.h"
namespace car {
struct CalibrationStore {
  Preferences prefs;
  bool opened=false;
  // One bounded record with a checksum prevents partially written timings from
  // becoming live. It contains durations, never a claim of encoder calibration.
  struct Record { uint32_t version;uint16_t lo,ro,lb,rb;uint32_t checksum; };
  static uint32_t checksum(const Record &r) {
    return 0x53434152u^r.version^(uint32_t(r.lo)<<16|r.ro)^(uint32_t(r.lb)<<16|r.rb);
  }
  void begin(SteeringTiming &timing) {
    opened=prefs.begin("springcar",false);
    if(!opened || prefs.getBytesLength("timing")!=sizeof(Record)) return;
    Record r{};if(prefs.getBytes("timing",&r,sizeof(r))!=sizeof(r) || r.version!=1 || r.checksum!=checksum(r)) return;
    const SteeringTiming loaded{r.lo,r.ro,r.lb,r.rb};if(loaded.valid())timing=loaded;
  }
  bool save(const SteeringTiming &timing) {
    if(!opened || !timing.valid()) return false;
    Record r{1,timing.leftOut,timing.rightOut,timing.leftBack,timing.rightBack,0};r.checksum=checksum(r);
    if(prefs.putBytes("timing",&r,sizeof(r))!=sizeof(r)) return false;
    Record verify{};
    return prefs.getBytes("timing",&verify,sizeof(verify))==sizeof(verify) && !memcmp(&r,&verify,sizeof(r));
  }
};
inline bool parseTiming(const char *line,SteeringTiming &timing) {
  if(strncmp(line,"CAL ",4)) return false;
  const char *p=line+4;long values[4];
  for(auto &v:values) {
    while(*p==' ')++p;
    if(*p<'0'||*p>'9')return false;
    char *end=nullptr;v=strtol(p,&end,10);
    if(end==p||v<50||v>1200)return false;
    p=end;
    if(&v!=&values[3] && *p!=' ') return false;
  }
  while(*p==' ')++p;
  if(*p)return false;
  const SteeringTiming parsed{uint16_t(values[0]),uint16_t(values[1]),uint16_t(values[2]),uint16_t(values[3])};
  if(!parsed.valid())return false;
  timing=parsed;return true;
}
}
