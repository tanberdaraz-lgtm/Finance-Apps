package app.finance.offline;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

@CapacitorPlugin(name = "FinanceNative", permissions = {
  @Permission(alias = "sms", strings = { Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS }),
  @Permission(alias = "notifications", strings = { Manifest.permission.POST_NOTIFICATIONS })
})
public class FinanceNativePlugin extends Plugin {
  private static final String PREF = "finance_secure_prefs";
  private static final String KEY_ALIAS = "finance_data_key_v1";
  private static final String PIN_SALT = "pin_salt";
  private static final String PIN_HASH = "pin_hash";

  @PluginMethod public void hasPin(PluginCall call) {
    boolean has = getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE).contains(PIN_HASH);
    JSObject out = new JSObject(); out.put("hasPin", has); call.resolve(out);
  }

  @PluginMethod public void setPin(PluginCall call) {
    String pin = call.getString("pin", "");
    if (!pin.matches("[0-9]{4}")) { call.reject("PIN must contain exactly 4 digits"); return; }
    try {
      byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
      byte[] hash = pinHash(pin, salt);
      getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
        .putString(PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        .putString(PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP)).apply();
      ensureKey(); call.resolve();
    } catch (Exception e) { call.reject("Could not set PIN", e); }
  }

  @PluginMethod public void verifyPin(PluginCall call) {
    String pin = call.getString("pin", "");
    try {
      android.content.SharedPreferences p = getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
      String saltText = p.getString(PIN_SALT, null), hashText = p.getString(PIN_HASH, null);
      boolean ok = false;
      if (saltText != null && hashText != null && pin.matches("[0-9]{4}")) {
        byte[] expected = Base64.decode(hashText, Base64.NO_WRAP);
        byte[] actual = pinHash(pin, Base64.decode(saltText, Base64.NO_WRAP));
        ok = MessageDigest.isEqual(expected, actual);
      }
      JSObject out = new JSObject(); out.put("valid", ok); call.resolve(out);
    } catch (Exception e) { call.reject("PIN verification failed", e); }
  }

  private byte[] pinHash(String pin, byte[] salt) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 150000, 256);
    try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
    finally { spec.clearPassword(); }
  }

  private SecretKey ensureKey() throws Exception { return FinanceCrypto.key(); }

  @PluginMethod public void encryptData(PluginCall call) {
    try {
      String plain = call.getString("plain", "");
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, ensureKey());
      byte[] encrypted = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
      byte[] iv = c.getIV(); byte[] packed = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, packed, 0, iv.length); System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
      JSObject out = new JSObject(); out.put("cipher", Base64.encodeToString(packed, Base64.NO_WRAP)); call.resolve(out);
    } catch (Exception e) { call.reject("Encryption failed", e); }
  }

  @PluginMethod public void decryptData(PluginCall call) {
    try {
      byte[] packed = Base64.decode(call.getString("cipher", ""), Base64.NO_WRAP);
      if (packed.length < 29) throw new IllegalArgumentException("Invalid encrypted data");
      byte[] iv = Arrays.copyOfRange(packed, 0, 12), body = Arrays.copyOfRange(packed, 12, packed.length);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, ensureKey(), new GCMParameterSpec(128, iv));
      JSObject out = new JSObject(); out.put("plain", new String(c.doFinal(body), StandardCharsets.UTF_8)); call.resolve(out);
    } catch (Exception e) { call.reject("Decryption failed; secure data may be unavailable", e); }
  }

  @PluginMethod public void biometricAvailable(PluginCall call) {
    int result = BiometricManager.from(getContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
    JSObject out = new JSObject(); out.put("available", result == BiometricManager.BIOMETRIC_SUCCESS); call.resolve(out);
  }

  @PluginMethod public void authenticateBiometric(PluginCall call) {
    if (!(getActivity() instanceof FragmentActivity)) { call.reject("Biometric authentication is unavailable"); return; }
    final FragmentActivity activity = (FragmentActivity) getActivity();
    // BiometricPrompt registers a lifecycle observer, which must happen on the main thread.
    activity.runOnUiThread(() -> {
      try {
        Executor executor = ContextCompat.getMainExecutor(getContext());
        BiometricPrompt prompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
          @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) { call.resolve(); }
          @Override public void onAuthenticationError(int code, CharSequence message) { call.reject(String.valueOf(message)); }
          @Override public void onAuthenticationFailed() { /* prompt remains active */ }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder().setTitle("Unlock Finance")
          .setSubtitle("Verify your identity").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL).build();
        prompt.authenticate(info);
      } catch (Exception e) { call.reject("Biometric authentication failed", e); }
    });
  }

  @PluginMethod public void requestSmsAccess(PluginCall call) {
    requestPermissionForAlias("sms", call, "smsPermissionCallback");
  }
  @PermissionCallback private void smsPermissionCallback(PluginCall call) {
    if (Build.VERSION.SDK_INT >= 33) { requestPermissionForAlias("notifications", call, "notificationPermissionCallback"); return; }
    resolvePermissionResult(call);
  }
  @PermissionCallback private void notificationPermissionCallback(PluginCall call) { resolvePermissionResult(call); }
  private void resolvePermissionResult(PluginCall call) {
    JSObject out = new JSObject();
    out.put("granted", getPermissionState("sms") == PermissionState.GRANTED);
    out.put("notificationsGranted", Build.VERSION.SDK_INT < 33 || getPermissionState("notifications") == PermissionState.GRANTED);
    call.resolve(out);
  }

  @PluginMethod public void openAppSettings(PluginCall call) {
    try {
      android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      intent.setData(android.net.Uri.parse("package:" + getContext().getPackageName()));
      intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
      getContext().startActivity(intent);
      call.resolve();
    } catch (Exception e) { call.reject("Could not open app settings", e); }
  }

  @PluginMethod public void readSmsInbox(PluginCall call) {
    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) { call.reject("SMS permission not granted"); return; }
    int limit = Math.max(1, Math.min(call.getInt("limit", 100), 500)); JSArray items = new JSArray();
    Uri uri = Uri.parse("content://sms/inbox");
    try (Cursor c = getContext().getContentResolver().query(uri, new String[]{"_id","address","body","date"}, null, null, "date DESC")) {
      if (c != null) { int n=0; while (c.moveToNext() && n++<limit) { JSObject row=new JSObject(); row.put("id",c.getString(0)); row.put("sender",c.getString(1)); row.put("text",c.getString(2)); row.put("timestamp",c.getLong(3)); items.put(row); } }
      JSObject out=new JSObject(); out.put("messages",items); call.resolve(out);
    } catch (Exception e) { call.reject("Could not read SMS inbox", e); }
  }
  @PluginMethod public void consumePendingSms(PluginCall call) {
    try {
      android.content.SharedPreferences prefs=getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
      String blob=prefs.getString("pending_sms", null);
      JSArray messages=new JSArray();
      if(blob!=null){org.json.JSONArray raw=new org.json.JSONArray(FinanceCrypto.decrypt(blob));for(int i=0;i<raw.length();i++)messages.put(new JSObject(raw.getJSONObject(i).toString()));}
      prefs.edit().remove("pending_sms").apply();
      JSObject out=new JSObject();out.put("messages",messages);call.resolve(out);
    } catch(Exception e) { call.reject("Could not read encrypted pending SMS",e); }
  }

}
