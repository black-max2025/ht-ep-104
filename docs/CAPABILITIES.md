# Capability matrix (HT-EP104)

Status legend: **Supported** = implemented and backed by evidence at runtime; **Unsupported** = ruled out by evidence;
**Unverified** = no evidence yet (needs hardware validation). The app computes this at runtime from what the
headset actually exposes (`protocol/StandardGattProtocol.kt`) and exports it in the diagnostics report.

| Feature | Mechanism | Status before hardware testing |
| --- | --- | --- |
| Battery level (aggregate) | GATT 0x180F/0x2A19, or Android HFP/AVRCP battery | Unverified until the headset is tested |
| Charging state | GATT 0x2BED (Battery Level Status) only | Unverified; never inferred from percentage |
| Left / right / case battery | needs a source that labels components | Unverified; shown Unsupported if none found |
| Find alert | Immediate Alert 0x1802 / Alert Level 0x2A06 | Unverified |
| Find left / right separately | no standard mechanism | Unsupported after discovery |
| Last connected / last seen | ACL connection events (best effort in background) | Supported (phone-side) |
| Last-known phone location | Android location, user permission | Supported (phone location, not earbud tracking) |
| Connection-loss notification | ACL disconnect event | Supported (best effort) |
| Play / pause / next / previous, volume | Android media keys + AudioManager (AVRCP handled by OS) | Supported; headset behaviour to validate |
| Answer / reject / end call, mic mute | Android Telecom API (`ANSWER_PHONE_CALLS`) | Supported on the phone; end/reject needs Android 9+ |
| GATT disconnect / reconnect | BluetoothGatt | Supported when GATT connects |
| Classic audio/call link connect/disconnect | no public Android API | Unsupported in-app (system settings shortcut) |
| Physical power off / on | none known | Unverified |
| Button remapping, ANC, transparency, EQ, mic control, rename | vendor protocol needed | Unverified |
| Firmware info | Device Information 0x180A | Unverified |
| Firmware update | no documented safe mechanism | Unsupported (intentionally not implemented) |
| Signal strength | RSSI from scan / GATT | Supported when sampled |
