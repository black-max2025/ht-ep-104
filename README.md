# HT-EP104 Control

A native Android (Kotlin + Jetpack Compose) companion app for the **HOOTOO HT-EP104** Bluetooth headset.

The HT-EP104 has no documented API or SDK. This project therefore follows a *standard-first, evidence-based* approach:
the app discovers what the headset really exposes and only shows controls for capabilities that are verified.
Everything else is labelled **Unsupported** or **Not verified**.

- Arabic and English (with RTL), light and dark themes
- No account, no cloud, no telemetry, **no INTERNET permission**
- Minimum Android 8.0 (API 26)

## Current milestone: 1 - Diagnostic APK

| Area | State |
| --- | --- |
| Bluetooth Classic discovery (bond state, SDP UUIDs, A2DP/HFP state, connection events) | implemented |
| BLE scan (service UUIDs, manufacturer/service data, RSSI) | implemented |
| GATT connect, full service/characteristic/descriptor enumeration, read-only value reads | implemented |
| Standard services 0x180F / 0x2A19 / 0x2BED / 0x180A / 0x1802 / 0x2A06 / 0x1803 | inspected |
| Diagnostics page + TXT/JSON export | implemented |
| Battery, audio, call, find, settings screens | implemented, gated by real evidence |
| Vendor protocol | **not identified yet** - see [docs/PROTOCOL_REPORT.md](docs/PROTOCOL_REPORT.md) |

See [docs/CAPABILITIES.md](docs/CAPABILITIES.md) for the supported / unsupported / unverified matrix and
[docs/HARDWARE_VALIDATION.md](docs/HARDWARE_VALIDATION.md) for the Milestone 2 test procedure.

## Download and install the APK

1. Open the **Actions** tab of this repository and select the latest successful **Android Build** run
   (or run it manually with **Run workflow**).
2. Under **Artifacts**, download **ht-ep104-control-debug-apk** and unzip it. It contains `app-debug.apk`.
3. Copy the APK to your phone and open it. Allow **Install unknown apps** for your file manager/browser when asked.
4. Pair the HT-EP104 in Android Bluetooth settings first, then open the app and choose **Select headset**.
5. Open **Diagnostics**, start a scan, connect GATT, then use **Export diagnostics** and share the report.

The debug build is signed with the standard Android debug key; no signing secrets are needed.

## Build locally

```
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Requires JDK 17 and the Android SDK (API 35). The APK is written to `app/build/outputs/apk/debug/`.

## Project layout

```
app/src/main/java/app/ht104/control/
  core/         UUID names, hex, JSON, helpers
  bluetooth/    ClassicInspector, BleScanner, GattInspector (read-only discovery)
  diagnostics/  models, DiagnosticEngine, report export
  protocol/     DeviceProtocol abstraction, StandardGattProtocol, capability evaluation
  control/      media, call and physical-button probe controllers
  data/         settings, permissions, notifications, connection events
  ui/           Compose screens and theme
```

New hardware discoveries plug in by implementing `DeviceProtocol` and registering it in `ProtocolRegistry`
after the evidence is recorded in `docs/PROTOCOL_REPORT.md`.

## Safety rules enforced in code

- GATT writes are limited to the standard Alert Level characteristic (0x2A06); there is no generic write path.
- Battery percentages are never invented, never copied between components, and never used to infer charging.
- A Bluetooth/GATT disconnect is never labelled as physical power-off.
- No firmware writes.
