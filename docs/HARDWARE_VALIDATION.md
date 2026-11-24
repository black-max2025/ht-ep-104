# Hardware validation checklist (Milestone 2)

- [ ] Pair HT-EP104; confirm bond state and SDP UUIDs (A2DP sink 0x110B, AVRCP, HFP 0x111E) in Diagnostics.
- [ ] Note whether Android reports a battery level (Diagnostics -> Android-reported battery).
- [ ] BLE scan: is the headset advertising? Record service UUIDs, manufacturer data, connectable flag.
- [ ] GATT connect: record state, errors (e.g. 133), services, characteristics, readable values.
- [ ] Place each earbud in the case / on the charger and re-export to see whether any value changes.
- [ ] Event log: watch for `HFP-VENDOR` events while charging or changing state.
- [ ] Physical button probe during music: single, double, long press, volume buttons.
- [ ] Incoming call: press the button once and record by hand what happens.
- [ ] Immediate Alert (only if present): send High Alert, then Stop; confirm by ear.
- [ ] Disconnect from the phone's Bluetooth settings and confirm the last-seen record updates.
