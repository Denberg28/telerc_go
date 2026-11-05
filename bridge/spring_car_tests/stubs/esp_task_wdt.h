#pragma once
#include <stdint.h>
using esp_err_t=int;
constexpr esp_err_t ESP_OK=0,ESP_ERR_NOT_FOUND=1,ESP_ERR_INVALID_STATE=2,ESP_FAIL=3;
struct esp_task_wdt_config_t { uint32_t timeout_ms,idle_core_mask; bool trigger_panic; };
inline esp_err_t fakeWdtInit=ESP_OK,fakeWdtConfigure=ESP_OK,fakeWdtStatus=ESP_ERR_NOT_FOUND,fakeWdtAdd=ESP_OK,fakeWdtReset=ESP_OK;
inline esp_err_t esp_task_wdt_init(const esp_task_wdt_config_t *c) { return c->timeout_ms==1000&&c->trigger_panic ? fakeWdtInit : ESP_FAIL; }
inline esp_err_t esp_task_wdt_reconfigure(const esp_task_wdt_config_t *) { return fakeWdtConfigure; }
inline esp_err_t esp_task_wdt_status(void *) { return fakeWdtStatus; }
inline esp_err_t esp_task_wdt_add(void *) { return fakeWdtAdd; }
inline esp_err_t esp_task_wdt_reset() { return fakeWdtReset; }
