# Finance — offline personal finance app

A bilingual (English / বাংলা), offline-first personal finance manager. The UI is a Vite web app packaged for Android with Capacitor. It stores data in the device/browser's IndexedDB; there is no cloud backend and no analytics SDK.

## Features in this starter release

- Dashboard: net account balance, current-month income/expenses, recent activity, category spend and budget progress.
- Transactions: add/edit income, expenses and transfers; search and filter; date, category, account, payee and notes.
- Accounts: cash, bank, card, wallet and debt account types; opening balance and calculated running balance.
- Manual bank details: bank name, account holder, account number, branch and routing/SWIFT code can be entered and edited locally for each bank account. No bank login, PIN, password or OTP is requested.
- Budgets: monthly category limits and remaining/over-budget indicators.
- Categories: custom name, type and color.
- Reports: six-month income/expense chart, category breakdown and CSV export.
- Tools: recurring-payment definitions, threshold-based smart rules and local flags.
- Settings: English/বাংলা, BDT/USD/EUR/GBP/INR, dark/light theme, JSON backup/restore, local audit log.
- Installable web app with a service worker for offline static assets.

## Important scope notes

This release is a useful offline personal-finance foundation, not a complete implementation of every Pawlet online service. Since the selected mode is offline-only, it does **not** connect to banks, Gmail, Discord, Slack, cloud sync, online subscription billing, or remote notifications. Recurring items are stored as schedules; automatic posting/notifications are not yet implemented. Transfers are recorded as transaction entries but this starter does not model paired source/destination accounts. Account balances are calculated from opening balances plus income minus expenses.

No financial app can guarantee security simply by being offline. The app currently does not encrypt the IndexedDB database. Protect the phone with device encryption/lock, export backups carefully, and do not store bank credentials, card PINs, or recovery phrases here. A future production release should add an app PIN/biometric gate, encrypted database and tested migration/backup flows.

## Run locally

Requirements: Node.js 20+ for the optional build script. The app can also be served as static files.

```bash
npm install
npm run start
```

Open the local Vite URL. Use HTTPS or localhost for service-worker/offline testing.

## Build Android APK

Requirements: Node.js 20+, Android Studio, Android SDK, Java 17+, and Gradle supported by the installed Capacitor Android version.

```bash
npm install
npm run build
npx cap add android       # first time only; creates the native android/ project
npx cap sync android
npx cap open android      # build > Build APK in Android Studio
```

Or, after Android Studio/SDK/Gradle are configured:

```bash
npm run android:apk
```

Debug APK output is normally `android/app/build/outputs/apk/debug/app-debug.apk`.

For a release APK, configure a signing key in Android Studio and use **Build > Generate Signed Bundle / APK**. Never commit your signing key or passwords.

## Data backup

Use Settings → Export backup regularly. The JSON file contains your financial records in readable form; store it securely. Import replaces the current local data after confirmation.

## Project structure

- `src/main.js` — application state, IndexedDB persistence, UI and feature logic.
- `src/style.css` — responsive visual design and themes.
- `sw.js` — offline asset cache.
- `manifest.webmanifest` — installable web app metadata.
- `capacitor.config.json` — Android wrapper identity and web build path.

## Maintenance

1. Edit `src/main.js` or `src/style.css`.
2. Run `npm run build`.
3. For Android, run `npx cap sync android` and rebuild from Android Studio.
4. When changing persisted data structures, increment the IndexedDB database version and write an explicit migration before shipping.


## Build APK from Android phone using GitHub Actions

1. Create a GitHub repository named `Finance`.
2. Extract this project ZIP on your phone using a ZIP/file manager.
3. Upload the contents of the `Finance` folder (including the hidden `.github` folder) to the repository's `main` branch. If your file manager hides dot-folders, enable "show hidden files".
4. Open the repository's **Actions** tab and allow GitHub Actions if asked.
5. Run **Build Finance Android APK** using **Run workflow**, or push a commit to `main`.
6. Wait for the workflow to finish successfully.
7. Open the completed workflow run, scroll to **Artifacts**, and download `Finance-debug-apk`.
8. Extract the artifact ZIP; install `app-debug.apk` on your Android phone. Allow "Install unknown apps" for the browser/file manager if Android asks.

The workflow builds a debug APK for personal testing. It is not a Play Store release or a security-audited financial product. The app's local database is not encrypted in this starter release; do not store sensitive bank credentials.
