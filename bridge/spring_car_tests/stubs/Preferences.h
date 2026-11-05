#pragma once
#include <vector>
#include <cstring>
inline bool failPreferenceWrite=false;
inline std::vector<unsigned char> preferenceData;
struct Preferences {
 bool begin(const char *,bool) { return true; }
 size_t getBytesLength(const char *) { return preferenceData.size(); }
 size_t getBytes(const char *,void *p,size_t n) { if(n>preferenceData.size())return 0;memcpy(p,preferenceData.data(),n);return n; }
 size_t putBytes(const char *,const void *p,size_t n) { if(failPreferenceWrite)return 0;const auto *b=static_cast<const unsigned char *>(p);preferenceData.assign(b,b+n);return n; }
};
