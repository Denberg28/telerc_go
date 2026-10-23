#include <cassert>
#include <cmath>
#include <cstdio>
#include "../SbrbPitchController.h"
int main() {
 SbrbPitchController c;
 assert(std::isfinite(c.step(1,0,0.004f,18,0,0.7f,25,160,0.012f)));
 assert(c.filteredRate==0);
 const float r=c.step(1,100,0.004f,18,0,0.7f,25,160,0.012f);
 assert(r>18 && r<88); // D measurement filtering limits a sudden gyro spike.
 c.reset();
 assert(!c.initialized && c.integral==0);
 for(int i=0;i<200;++i)c.step(100,0,0.004f,18,10,0,25,160,0);
 assert(c.integral==0); // Windup is blocked while saturated.
 c.reset();
 for(int i=0;i<100;++i)c.step(1,0,0.004f,18,10,0,25,160,0);
 assert(c.integral>0 && c.integral<=25);
 assert(std::isnan(c.step(NAN,0,0.004f,18,0,0.7f,25,160,0.012f)));
 assert(std::isnan(c.step(0,0,0.004f,18,0,0.7f,NAN,160,0.012f)));
 assert(std::isnan(c.step(0,0,0.004f,18,0,0.7f,25,160,NAN)));
 c.reset();
 c.step(0,10,0.004f,18,0,0.7f,25,160,0.012f);
 float reverse=0;
 for(int i=0;i<4;++i)reverse=c.step(0,-10,0.004f,18,0,0.7f,25,160,0.012f);
 assert(reverse<0); // A sustained reverse rate reverses D torque within 16 ms.
 std::puts("SBRB-inspired controller unit tests PASS");
}
