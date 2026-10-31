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

*How it applies here (added 2026-10-31; the text above is verbatim).* The app has no MRN field, by a rule
that stays: bed only, no names or MRN. The one way an MRN can get in is by mistake, typed as a bed label under
"+ Other bed" or in the ward's bed list. So a run of 5 or more digits in a bed label counts as an MRN, and is
masked in those three places. No MRN field was added and no new data is stored. There is no CSV export (removed
on 2026-10-28, Cerner is the record), so that item holds by having none. Sample screens use short bed numbers,
which are never masked, so no screenshot differs.
