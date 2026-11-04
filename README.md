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

## Current implementation status (v8)

The source includes the listed app workflows plus Android-native PIN, biometric, encrypted local storage, SMS inbox/receiver integration, and generic notification support. These native capabilities are implemented in source but **are not confirmed working on a real device until the APK is built and tested**. This is a personal offline finance app starter, not a certified banking product or a security-audited vault.

Known limits:
- SMS parsing is heuristic. It can only use details actually present in the message; review every imported/pasted message before creating a transaction. Duplicate inbox imports are filtered by native message ID where available.
- Android SMS permissions are sensitive/restricted by Google Play policy and may be unavailable or blocked on some devices. This workflow is for personal debug-APK sideload testing, not Play Store approval.
- Full account number, account holder, branch and routing number cannot generally be inferred from ordinary bank SMS. Manual correction remains available.
- Recurring payments are stored schedules; automatic background transaction posting is not guaranteed. Generic SMS notifications depend on Android permission and OEM background behavior.
- JSON/CSV exports are plaintext. Keep exported files private and delete them securely when no longer needed.
- The workflow creates a debug APK, not a signed Play Store release. No independent penetration test, financial audit or Android-device compatibility test has been completed.

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

## v7 mobile UI and SMS permission improvements
- Refined mobile spacing, typography, navigation touch targets, transaction rows, forms, dialogs and SMS inbox layout.
- SMS import now clearly distinguishes the installed Android APK from browser preview. Manual SMS paste remains available without SMS permission.
- If permission is denied, the app can open Android App info so SMS access can be enabled manually.
- SMS parsing uses only fields present in the message; incomplete details remain incomplete and should be reviewed before saving a transaction.

Note: SMS permissions are sensitive Android permissions and may be restricted by Google Play policy. This source workflow builds a debug APK for personal sideloading; native permission behavior must be tested on the target Android device. The web build check does not compile the Android Java project.

## v8 pre-release verification checklist

After installing the APK on the target phone, test each item before relying on it:

- [ ] First launch PIN creation; wrong PIN rejected; correct PIN unlocks.
- [ ] Background/resume lock and Android screenshot/task-switcher protection.
- [ ] Biometric setup and unlock on a device with biometrics enrolled.
- [ ] Create/edit/delete income, expense and transfer; confirm balances for both accounts.
- [ ] Manual bank/account detail entry and auto-detected SMS bank profile correction.
- [ ] Paste SMS without permission; import inbox with permission; deny permission and reopen app settings.
- [ ] Review queued SMS; ensure no transaction is created until user taps Create transaction.
- [ ] Import the same SMS twice; verify it is not duplicated.
- [ ] Budget, recurring schedule, rules, reports, CSV export and offline restart.
- [ ] Export JSON backup, clear app data in a test install, restore backup and verify totals.
- [ ] Switch Bengali/English and dark/light themes; inspect small-screen alignment and touch targets.

This checklist is intentionally not marked complete by a web build. A successful Vite build does not prove that Android Java compilation, SMS permission prompts, biometrics, notifications or encryption work on a physical device.


## v9 review fixes

- Fixed: "today" used UTC, so between 00:00–06:00 in Bangladesh (UTC+6) new entries were dated yesterday. Dates now use the device's local time.
- Fixed: Reports six-month chart could show wrong months on the 29th–31st.
- Fixed: Payee and Note were mandatory when adding a transaction; they are now optional.
- Fixed: Category list now follows the selected type (income/expense/transfer); "To account" shows only for transfers; validation errors now show a message instead of failing silently.
- Fixed: Currency/Language/Theme dropdowns in Settings now show the saved value. Added missing "Categories" and "Data safety" labels (English + বাংলা).
- Fixed (Android): Biometric prompt is now created on the UI thread (previously could crash/fail on real devices).
- Fixed (Android): JSON backup and CSV export use the Android share sheet (Capacitor Filesystem + Share); blob downloads do not work inside the Android WebView.
- Fixed (Android): the service worker is no longer used inside the APK (it could keep serving stale files after an app update); old ones are cleaned up.
- Fixed (Android): the app no longer re-locks while the SMS permission or biometric dialog is open.
- Security: Android Auto Backup disabled (the Keystore key is not backed up, so restored data would be unreadable); PIN entry is throttled after 5 wrong attempts; biometric library pinned to stable 1.1.0.
- SMS: inbox import now keeps only transaction-like messages (OTP/personal SMS are skipped), duplicates between the live receiver and inbox import are detected, and credit/debit detection uses whichever keyword appears first. Receiver also recognises bKash/Nagad-style "Cash In / Cash Out / received / Tk" messages.
- New: "Post due payments" button for recurring items (with account/category), delete for budgets and unused accounts, recovery button if the local database cannot be decrypted.
- Build: added `vite` dev dependency for `npm start`, removed obsolete `bundledWebRuntime`, extra syntax checks in the workflow.

Still true: the Android Java code and native plugins could not be compiled or run in the review environment. Build the APK via GitHub Actions and run the v8 checklist above on a real phone.
