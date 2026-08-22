# Overnight soak baseline (restarted after v0.2.1 install)

Captured: 2026-08-21 19:31:56 CDT
Tablet:   Fri Aug 21 20:31:56 EDT 2026
Build:    v0.2.1 release

## Process
    15038       00:40 110632 com.kindcode.alarmhub

## Power
      AC powered: true
      status: 2
      level: 92
      temperature: 200

## Armed
          type=RTC_WAKEUP origWhen=2026-08-22 07:30:00.000 window=0 exactAllowReason=policy_permission repeatInterval=0 count=0 flags=0x3
          type=RTC_WAKEUP origWhen=2026-08-22 07:45:00.000 window=0 exactAllowReason=policy_permission repeatInterval=0 count=0 flags=0x3

## Known open item
    Timezone is still America/New_York. Auto-detection is off, so once
    it is set to Central on the tablet, TIMEZONE_CHANGED will re-arm
    the alarms to 07:45 Central automatically.

## Pass criteria in the morning
    1. Same PID (process never restarted)
    2. mWakefulness=Awake throughout
    3. The 07:45 alarm fired
    4. Battery at/near 100, temperature not climbing
    5. Next alarm re-armed for Sunday 07:45
