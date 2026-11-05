#pragma once
#include <stdint.h>
#include "HardwareProfile.h"
namespace config {
constexpr char VERSION[]="0.2.0-estimated-steering";
constexpr char WIFI_SSID[]="TeleRC-MiniRover";
constexpr char WIFI_PASSWORD[]="telerc123";
constexpr uint16_t UDP_PORT=14550;
constexpr uint8_t SYSTEM_ID=1, COMPONENT_ID=1;
constexpr uint8_t GCS_SYSTEM_ID=255, GCS_COMPONENT_ID=190;
constexpr uint32_t CONTROL_TIMEOUT_MS=500, PEER_TIMEOUT_MS=3000;
constexpr uint32_t NEUTRAL_HOLD_MS=250, MOTOR_TICK_MS=5, REVERSAL_COAST_MS=30;
constexpr uint32_t LOOP_WATCHDOG_MS=1000;
constexpr uint8_t MAX_PWM=242; // Drive ceiling 95%; motor rail must suit motors.
constexpr uint8_t STEER_MAX_PWM=160; // Initial bench setting, tune to the spring.
constexpr uint8_t DRIVE_SLEW_PER_TICK=4, STEER_SLEW_PER_TICK=10;
// Timed travel then electrical brake; no measured/static position lock.
constexpr uint32_t STEER_CENTER_SETTLE_MS=200;
constexpr float STEER_POSITION_TOLERANCE=0.015f;
constexpr int RC_DEADBAND_US=25, RC_FULL_SCALE_US=400;
constexpr int BOOT_STOP_PIN=0, STATUS_LED_PIN=-1; // No assumption about board LED.
constexpr bool DRIVE_INVERT=false, STEER_INVERT=false;
constexpr bool MOTOR_INVERT[4]={DRIVE_INVERT,false,STEER_INVERT,false};
constexpr uint8_t MOTOR_TRIM_PERCENT[4]={100,0,100,0};
constexpr uint8_t MOTOR_DUTY_CAP[4]={MAX_PWM,0,STEER_MAX_PWM,0};
static_assert(sizeof(WIFI_PASSWORD)>=9 && sizeof(WIFI_PASSWORD)<=64,"Wi-Fi password: 8..63 characters");
static_assert(RC_DEADBAND_US>=0 && RC_DEADBAND_US<RC_FULL_SCALE_US && RC_FULL_SCALE_US<=500,"Invalid scaling");
static_assert(MAX_PWM>0 && MAX_PWM<=242 && STEER_MAX_PWM>0 && STEER_MAX_PWM<=242,"Invalid PWM cap");
static_assert(DRIVE_SLEW_PER_TICK>0 && STEER_SLEW_PER_TICK>0,"Invalid slew");
}
