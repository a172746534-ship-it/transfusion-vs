# A S — Transfusion VS & repositioning

منبّه لقياس العلامات الحيوية أثناء نقل الدم وتسجيلها في Cerner في ميعادها، ومنبّه لتقليب المرضى على مواعيد ثابتة (كل ساعتين: 08:00، 10:00، 12:00…). اسمه على الموبايل **A S**.
التطبيق نسخة من الكارت الورق، بس بيرن.

- **مش بيتصل بالنت خالص.** مفيه أسماء مرضى ولا رقم ملف، وأي بيانات بتفضل على الموبايل بس.
- مجاني بالكامل، ومش موجود على Play Store: بيتسطّب من ملف APK.

---

## التسطيب على موبايلك (أول مرة)

1. افتح الصفحة دي **من الموبايل**: <https://github.com/a172746534-ship-it/transfusion-vs/releases/latest>
2. تحت كلمة **Assets** دوس على الملف اللي آخره **`.apk`** عشان يتحمّل.
3. افتح الملف من الإشعار أو من تطبيق **الملفات / Downloads**.
4. لو ظهرت رسالة إن تسطيب التطبيقات غير مسموح: دوس **الإعدادات** ← فعّل **السماح من هذا المصدر**، وارجع.
5. دوس **تثبيت / Install**، وبعدها **فتح / Open**.
6. هتفتح شاشة **Setup check**. أي سطر **أحمر** دوس جنبه **Fix** وفعّل اللي يطلبه، لحد ما كل السطور تبقى **خضرا**.
7. تحت هتلاقي **خطوات خاصة بنوع موبايلك** (شاومي، سامسونج، أوبو…). اعملها كلها بالترتيب. زرار **Open** بيفتحلك الصفحة الصح.
8. دوس **Test alarm in 1 minute**، واقفل الشاشة، وحط الموبايل على **صامت**. لازم يرن بصوت وتظهر شاشة كاملة خلال دقيقة. دوس **Measured & documented ✓**.
9. لو مارنّش: ارجع للخطوة 6 و7. لو لسه مش بيرن، ابعت نوع الموبايل ونسخة الأندرويد.

## التحديث لنسخة أجدد

1. نزّل الـ APK الجديد من نفس اللينك.
2. سطّبه **فوق** القديم على طول. **ماتمسحش التطبيق القديم الأول**، عشان سجلّك يفضل والمنبّهات تفضل شغالة.

## الساعة أو الباند الذكي

عشان الساعة/الباند تهتز مع الجرس: افتح تطبيقها (Zepp أو Mi Fitness أو Galaxy Wearable…) ← الإشعارات ← فعّل **Transfusion VS**.

## مهم

- **ماتعملش Force stop** للتطبيق. أندرويد بيلغي منبّهاته لحد ما تفتحه تاني.
- لو صوت المنبّه (Alarm volume) على صفر، مش هيرن. الـ Setup check بيوضّح ده.

---

## For developers

- Spec and decisions: [SPEC.md](SPEC.md). Card designs: [design/](design/).
- Built only by GitHub Actions ([.github/workflows/android.yml](.github/workflows/android.yml)):
  unit tests → signed `assembleRelease` → APK artifact. Tags `v*` also publish a GitHub Release.
- The release signing key is **not** in this repo. It lives offline with the maintainer, and CI
  reads it from GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
