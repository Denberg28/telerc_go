#pragma once
#include <stdint.h>
namespace control {
struct Sequence {
 bool seen=false;uint8_t last=0;
 bool accept(uint8_t next) {
  const uint8_t distance=uint8_t(next-last);
  if(seen&&(distance==0||distance>=128))return false;
  seen=true;last=next;return true;
 }
};
}
