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

| Item | Result |
| --- | --- |
| Transport (Classic only / BLE GATT / dual) | _unknown_ |
| Advertised service UUIDs | _unknown_ |
| Standard services present (0x180F, 0x180A, 0x1802, 0x1803) | _unknown_ |
| Vendor-specific services | _unknown_ |
| Battery source and granularity | _unknown_ |
| Charging information source | _unknown_ |
| HFP vendor events observed (Event log, `HFP-VENDOR`) | _unknown_ |
| Physical button media events (Calls -> Physical button test) | _unknown_ |

## Rules for adding a vendor command

Read-only first; document UUID, payload and response; record the evidence; no firmware writes;
add a `DeviceProtocol` implementation and register it in `ProtocolRegistry`.
