# Plumber - تطبيق سباكة وتجهيزات وورشات العمل

تطبيق Android Native حديث لإدارة مشاريع السباكة، حساب تكاليف المواد، إدارة طاقم العمل، وإصدار وتصدير الفواتير ملفات PDF وواتساب.

## 🛠️ التقنيات المستخدمة

- **اللغة**: Kotlin
- **واجهة المستخدم**: Jetpack Compose (Material Design 3)
- **قاعدة البيانات المحلّية**: Room Database
- **نظام البناء**: Gradle (Kotlin DSL)
- **التوافق**: Android Native (Android 7.0 / SDK 24+)

---

## 🚀 كيفية بناء ملف الـ APK (Build Instructions)

المشروع مجهز بالكامل بملفات **Gradle Wrapper** لتشغيل البناء التلقائي على أجهزة الكمبيوتر وسيرفرات CI/CD مثل **Codemagic** و **GitHub Actions**.

### 1. منح صلاحية التنفيذ للسكربت:

```bash
chmod +x gradlew
```

### 2. تنظيف وبناء ملف APK للـ Debug:

```bash
./gradlew clean
./gradlew assembleDebug
```

### 3. مسار ملف الـ APK الناتج:

بعد اكتمال عملية البناء بنجاح، ستجد ملف الـ APK جاهزاً للتثبيت في المسار التالي:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚡ البناء على سيرفرات CI/CD (Codemagic & GitHub Actions)

المشروع يحتوي على ملف `codemagic.yaml` مهيأ بالكامل. عند رفع الكود على Codemagic سيتم بناء ملف الـ APK وتوفيره للتحميل تلقائياً بدون الحاجة لأي إعدادات إضافية.
