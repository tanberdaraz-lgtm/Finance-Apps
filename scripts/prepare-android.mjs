import { cp, readFile, writeFile, mkdir, readdir } from 'node:fs/promises';
import path from 'node:path';
const root=process.cwd();
const pkg='app/finance/offline';
const dest=path.join(root,'android/app/src/main/java',pkg);
await mkdir(dest,{recursive:true});
for(const f of ['FinanceNativePlugin.java','FinanceSmsReceiver.java','FinanceCrypto.java']) await cp(path.join(root,'android-native/src/main/java',pkg,f),path.join(dest,f));
// Register the Capacitor plugin in the generated MainActivity.
async function walk(dir){let out=[];for(const e of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())out.push(...await walk(p));else if(e.name==='MainActivity.java')out.push(p);}return out;}
const mains=await walk(path.join(root,'android/app/src/main/java'));
if(!mains.length) throw new Error('MainActivity.java not found after cap add android');
for(const main of mains){let s=await readFile(main,'utf8');if(!s.includes('FinanceNativePlugin')){s=s.replace(/(import\s+com\.getcapacitor\.BridgeActivity;\s*)/, '$1\nimport app.finance.offline.FinanceNativePlugin;\n');s=s.replace(/(public\s+class\s+MainActivity\s+extends\s+BridgeActivity\s*\{)/,'$1\n  @Override public void onCreate(android.os.Bundle savedInstanceState) {\n    registerPlugin(FinanceNativePlugin.class);\n    super.onCreate(savedInstanceState);\n    getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE);\n  }');await writeFile(main,s);}}
// Add AndroidX Biometric and core dependencies.
const gradle=path.join(root,'android/app/build.gradle');
let g=await readFile(gradle,'utf8');
if(!g.includes('androidx.biometric:biometric')) g=g.replace(/dependencies\s*\{/,"dependencies {\n    implementation 'androidx.biometric:biometric:1.1.0'\n    implementation 'androidx.core:core:1.13.1'");
await writeFile(gradle,g);
// Permissions and SMS_RECEIVED receiver.
const manifest=path.join(root,'android/app/src/main/AndroidManifest.xml');
let m=await readFile(manifest,'utf8');
if(!m.includes('android.permission.READ_SMS')) m=m.replace('<application', '<uses-permission android:name="android.permission.READ_SMS" />\n    <uses-permission android:name="android.permission.RECEIVE_SMS" />\n    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n    <application');
m=m.replace('android:allowBackup="true"','android:allowBackup="false"');
if(!m.includes('android.hardware.telephony')) m=m.replace('<application', '<uses-feature android:name="android.hardware.telephony" android:required="false" />\n    <application');
if(!m.includes('FinanceSmsReceiver')) m=m.replace('</application>', '        <receiver android:name="app.finance.offline.FinanceSmsReceiver" android:enabled="true" android:exported="true" android:permission="android.permission.BROADCAST_SMS"><intent-filter><action android:name="android.provider.Telephony.SMS_RECEIVED" /></intent-filter></receiver>\n    </application>');
await writeFile(manifest,m);
console.log('Finance Android native security, SMS receiver and biometric plugin prepared.');
