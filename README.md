# Finance — Android-ready offline personal finance app

This is a bilingual English/বাংলা, offline-first personal finance project packaged with Capacitor for Android. It is inspired by the broad layout and workflows in the Pawlet screenshots supplied by the user; it is not Pawlet and does not reproduce Pawlet's proprietary code or online services.

## Included in this source package

- Dashboard with total balance, monthly income/expense, recent activity and category spend
- Transactions: income, expense and transfers between two distinct accounts; add/edit/delete, search and filters
- Accounts: cash, bank, card, wallet and debt; bank name, account holder, account number, branch and routing code
- Budgets and custom categories
- Reports: six-month cash-flow chart, category summary and CSV export
- Recurring payment schedules, threshold rules and local flags
- Messages page: paste a bank SMS, detect a likely amount/type, review, create a transaction, ignore or delete
- English and Bengali interface, BDT/USD/EUR/GBP/INR, dark/light themes
- JSON backup/restore and local audit log
- Offline static assets via service worker
- GitHub Actions workflow to build a debug APK

## Important limitations — read before using

This package is a functional offline finance starter, **not yet a complete Pawlet-equivalent production app**. In particular:

- It does not automatically read the phone's SMS inbox. Messages must be pasted/imported by the user. Android SMS permissions are highly restricted by Google Play policy and require an eligible/default-SMS-handler design or an approved exception.
- It does not yet include native biometric authentication or a hardened app PIN gate. Do not treat the app as a secure vault.
- Local IndexedDB data is not encrypted at rest. Device lock and encrypted device storage are recommended; never enter card PINs, OTPs, passwords or bank login credentials.
- Recurring payments are schedules only; there is no guaranteed background posting or notification engine.
- No bank API, Gmail, cloud sync, AI message service, automatic bank reconciliation or remote notifications are connected.
- This is a debug APK build workflow, not a signed Play Store release.

Do not use this starter as the sole record of financial activity. Export backups regularly and verify all imported SMS transactions before saving.

## Build APK with GitHub Actions

1. Create/open your GitHub repository. Upload **all files and folders in this ZIP**, including the hidden `.github` folder, to the repository root (not the ZIP file itself).
2. Commit the files to the `main` branch. The workflow `.github/workflows/main.yml` runs automatically on push.
3. Open the repository's **Actions** tab and select the latest **Build Finance Android APK** run. Wait for a green Success status.
4. Under **Artifacts**, download `Finance-debug-apk`. Extract the downloaded ZIP and install `app-debug.apk` on Android.
5. If Android blocks installation, allow your browser/file manager to install unknown apps.

Requirements for a manual build: Node.js 20+, Java 21, Android SDK and Capacitor-compatible Gradle.

## Local build commands

```bash
npm install
npm run build
npx cap add android       # first time only
npx cap sync android
cd android && ./gradlew assembleDebug
```

APK output: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Android native security and SMS integration (v2)

The Android workflow runs `scripts/prepare-android.mjs` after `npx cap add android`. It copies and registers a custom Capacitor plugin and adds the Android SMS receiver.

- First launch: create a 4-digit PIN. The PIN is stored as a salted PBKDF2-HMAC-SHA256 hash (150,000 iterations), not as plaintext.
- App lock: PIN is required at launch and after the WebView is backgrounded and resumed. Android `FLAG_SECURE` prevents screenshots and task-switcher previews.
- Biometrics: optional AndroidX BiometricPrompt unlock; enabled after a successful biometric/device-credential prompt.
- Local data encryption: the IndexedDB app-state value is encrypted as AES-256-GCM using a non-exportable Android Keystore key. The PIN gates app access; it is not itself the encryption key.
- SMS: the user explicitly grants READ_SMS and RECEIVE_SMS. The Messages screen can import recent inbox messages for review. A background SMS_RECEIVED receiver encrypts transaction-like incoming messages into a pending queue and posts a generic notification. On next unlock, Finance imports them into Messages for review; it does not silently create a financial transaction.
- Android 13+ notification permission is requested alongside SMS permission.

### Important limitations and safety notes

SMS permissions are highly restricted by Google Play policy; this project is intended for personal sideloaded testing, and permission availability depends on Android version/device policy. Some OEMs may restrict background delivery. SMS parsing is heuristic and must be reviewed before creating a transaction. Notification content is deliberately generic to avoid exposing amounts on the lock screen. JSON/CSV exports are plaintext; protect exported files. The custom native integration must be built and tested on a real Android device before relying on it. This source has not been independently security-audited.


## Automatic bank profile detection + backup/restore (v4)

- When a bank SMS is imported or pasted, Finance checks known Bangladesh bank/MFS sender and message patterns, detects the institution when identifiable, and stores only the masked account suffix when present. A matching bank account profile is created/updated automatically and linked to the queued SMS transaction. No bank-detail form entry is required.
- SMS usually does not contain the account holder, full account number, branch or routing number. Finance deliberately does not fabricate these values; those fields cannot be reliably auto-filled without a supported bank API or statement import.
- Settings includes JSON backup export and restore. Backup contains the complete app state, including account profiles, transactions, categories, budgets, recurring rules and message history. Restore validates the JSON structure and asks for confirmation before replacing local data. Exported backup files are plaintext; keep them private.
- Footer attribution: Development by ©Tanber Ahammed.
