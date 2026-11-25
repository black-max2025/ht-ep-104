# HT-EP104 protocol report

**Status: protocol not yet determined.** No hardware run has been recorded, so no vendor-specific UUID,
command or response is documented. Nothing in the app depends on an unverified protocol.

## How to fill this in (Milestone 2)

1. Install the debug APK, pair the HT-EP104, select it in the app.
2. Diagnostics -> *Start scan* -> connect GATT on the HT-EP104 entry (starred), wait for enumeration.
3. *Export diagnostics* (TXT and JSON) and attach them to the repository or an issue.
4. Record below, per discovered vendor UUID: service/characteristic UUID, properties, observed read values,
   and the evidence for any proposed command.

## Findings

Source: diagnostics export from a Xiaomi Redmi 8 (Android 10, API 29), 2026-11-25, app 0.1.0-diagnostic.

| Item | Result |
| --- | --- |
| Transport | **Bluetooth Classic** (Android reports device type "Classic", class major 0x0400 / minor 0x0404 = wearable headset) |
| Classic profiles (SDP) | HFP 0x111E, A2DP sink 0x110B, AVRCP 0x110E; plus two entries with UUID `00000000-0000-1000-8000-00805f9b34fb` (unnamed, purpose unknown) |
| Profile state | A2DP CONNECTED, HFP CONNECTED (AVRCP has no public Android API) |
| GATT | Connection to the bonded address failed 3 times with status 133 (GATT_ERROR). No GATT services were enumerated. No GATT service UUID (e.g. 0x1801) appears in SDP. |
| BLE advertisements | None recorded in this export (a scan may not have been run; not yet conclusive) |
| Standard services (0x180F, 0x180A, 0x1802, 0x1803) | Not reachable: GATT never connected |
| Vendor-specific services | None found |
| Battery source and granularity | Android reports **one aggregate value, 100%** (HFP/AVRCP path). No per-earbud or case value. Whether 100% is live or a default is unverified; watch it change over time. |
| Charging information source | None found |
| HFP vendor events (`HFP-VENDOR`) | None in the event log of this export |
| Physical button media events | Probe not run yet |

Conclusion so far: the HT-EP104 behaves as a Classic-only headset. Standard GATT features (separate battery,
charging state, Immediate Alert, remote power) are not available through it. Remaining unexplored paths: HFP
vendor-specific AT events, AVRCP, and the physical button probe.

## Rules for adding a vendor command

Read-only first; document UUID, payload and response; record the evidence; no firmware writes;
add a `DeviceProtocol` implementation and register it in `ProtocolRegistry`.
