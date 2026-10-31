# Add-on (optional): protect the MRN — only this, nothing else
Apply ONLY the items below. The same rules as the change request apply: plan first,
tests, screenshot regression, scope audit.

## Changes
- **Masking:** notifications, the alarm screen over the lock screen, and the widget show a
  masked MRN such as "•••4821".
  - Use the shortest suffix that is unique among active patients, minimum 4 digits.
  - The full MRN appears only inside the app, on an unlocked phone.
- **CSV export:** contains no MRN.
- **Logs:** MRNs never appear in logcat.

## Tests
- `dumpsys notification --noredact` shows only masked values.
- The widget and the alarm over the keyguard are masked.
- The CSV export and logcat contain zero test MRNs.
- These masked locations are allowed differences in the screenshot regression.

---

*How it applies here (updated 2026-10-31; the text above is verbatim).* The maintainer replaced the bed number with
the MRN, because some places in the hospital have no beds. So the mask now covers every patient: outside the app
(notifications, the alarm over the lock screen, the widget) an MRN shows as "•••4821". There is no CSV export (removed
on 2026-10-28, Cerner is the record), so that item holds by having none.
