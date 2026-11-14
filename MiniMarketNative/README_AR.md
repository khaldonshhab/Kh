# ميني ماركت – تطبيق Android أصلي

## التقنية
- Kotlin + AndroidX
- واجهة Android أصلية (XML Views)
- SQLite محلية لحفظ المواد
- CameraX للكاميرا الخلفية
- Google ML Kit لقراءة الباركود
- Storage Access Framework لاستيراد وتصدير CSV

## فتح المشروع
1. فك ضغط المشروع.
2. افتح Android Studio.
3. اختر Open ثم اختر مجلد `MiniMarketNative`.
4. انتظر Gradle Sync حتى ينتهي، مع اتصال إنترنت لتنزيل الاعتماديات.
5. من القائمة Build اختر Build Bundle(s) / APK(s) ثم Build APK(s).
6. ستجد APK عادة في:
   `app/build/outputs/apk/debug/app-debug.apk`

## ملاحظات
- أول تشغيل للماسح يطلب إذن الكاميرا من Android.
- لا يحتاج التطبيق إلى اتصال إنترنت أثناء الاستخدام العادي بعد تثبيت التطبيق وتنزيل نموذج ML Kit والاعتماديات؛ إعداد ML Kit هنا يستخدم نموذج Barcode Scanning المضمّن عبر dependency.
- البيانات محفوظة في قاعدة SQLite داخل التطبيق ولا تُحذف عند إغلاقه.
- CSV يدعم الحقول المقتبسة والفواصل داخل اسم المادة.
- هذا مشروع مصدر قابل للبناء في Android Studio؛ لم يتم إنشاء APK أو اختباره على جهاز فعلي ضمن بيئة الإنشاء الحالية.
