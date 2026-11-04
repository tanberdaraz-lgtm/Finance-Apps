package app.finance.offline;

import android.content.Context;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

final class FinanceCrypto {
  private static final String ALIAS="finance_data_key_v1";
  private FinanceCrypto() {}
  static SecretKey key() throws Exception {
    KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
    if(ks.containsAlias(ALIAS))return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
    KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());
    return gen.generateKey();
  }
  static String encrypt(String plain) throws Exception {
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());byte[] body=c.doFinal(plain.getBytes(StandardCharsets.UTF_8));byte[] iv=c.getIV();byte[] all=new byte[iv.length+body.length];System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(body,0,all,iv.length,body.length);return Base64.encodeToString(all,Base64.NO_WRAP);
  }
  static String decrypt(String encoded) throws Exception {
    byte[] all=Base64.decode(encoded,Base64.NO_WRAP);byte[] iv=java.util.Arrays.copyOfRange(all,0,12);byte[] body=java.util.Arrays.copyOfRange(all,12,all.length);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));return new String(c.doFinal(body),StandardCharsets.UTF_8);
  }
}
