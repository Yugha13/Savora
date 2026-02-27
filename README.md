# Savora

**Your intelligent, privacy-first companion for tracking personal expenses.**

Savora takes the hassle out of managing your money by securely processing your bank SMS messages entirely on your device, extracting your expenses, and providing you with actionable insights—all without your financial data ever leaving your phone.

---

## 🌟 Why Savora?

- **100% Private & Secure:** All parsing and analytics are powered by on-device AI. We don't use cloud servers to read your data.
- **Automated Expense Tracking:** Say goodbye to manual entry. Simply receive a bank SMS, and Savora logs it.
- **Beautiful, Intuitive Analytics:** Our newly redesigned dynamic analytics dashboard gives you a clear view of your spending, making financial goals easier to reach.
- **Smart Categorization:** Automatically assigns logical categories to your transactions using intelligent local models.
- **Multi-Currency Support:** Traveling or receiving international alerts? We handle multiple currencies effortlessly.
- **Subscriptions & Recurring Payments:** Savora automatically detects recurring charges so you're never caught off-guard.

## 🚀 Getting Started

To experience financial clarity with Savora:

1. Clone this repository to your local machine:
   ```bash
   git clone https://github.com/your-username/savora.git
   cd savora
   ```

2. Build and run the app using Android Studio (Ladybug or newer) or run the Gradle command directly:
   ```bash
   ./gradlew assembleDebug
   ```

3. Install the APK on your Android 12+ device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 🛠️ Built With

Savora is built using modern Android development tools and libraries:
- **Kotlin 2.0+**
- **Jetpack Compose** (for a gorgeous, modern UI)
- **Room Database** 
- **Coroutines & Flow**
- **Hilt**
- **MediaPipe on-device LLM**

## 🤝 Contributing

We welcome contributions from the community! From fixing bugs to adding support for new banks, your help is appreciated. 
Please refer to our `CONTRIBUTING.md` (coming soon) and `CODE_OF_CONDUCT.md` for more details. 
To run local checks:
```bash
./gradlew test
./gradlew lint
```

## 🛡️ Privacy & Security

We believe your finances are your business alone. Savora respects your privacy by processing everything locally. If you discover a security vulnerability, please refer to `SECURITY.md`.

## 📄 License

Savora is open-source software licensed under the AGPL v3 License. See the `LICENSE` file for more details.

---
*Elevate your financial awareness with Savora.*
