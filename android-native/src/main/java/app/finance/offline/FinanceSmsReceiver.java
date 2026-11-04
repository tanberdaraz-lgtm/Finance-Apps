package app.finance.offline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class FinanceSmsReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context, Intent intent) {
    if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;
    Bundle bundle=intent.getExtras(); if(bundle==null)return;
    Object[] pdus=(Object[])bundle.get("pdus"); if(pdus==null)return;
    StringBuilder body=new StringBuilder(); String sender="Bank SMS";String format=bundle.getString("format");long received=System.currentTimeMillis();
    for(Object pdu:pdus){SmsMessage sms=Build.VERSION.SDK_INT>=23?SmsMessage.createFromPdu((byte[])pdu,format):SmsMessage.createFromPdu((byte[])pdu);if(sms!=null){sender=sms.getOriginatingAddress();body.append(sms.getMessageBody());}}
    String text=body.toString(); String lower=text.toLowerCase();
    if(!(lower.matches("(?s).*(debited|credited|withdrawn|deposit|transferred|transaction|purchase|payment|balance|cash ?in|cash ?out|received|\\bbdt\\b|\\btk\\b|\\u09F3).*")))return;
    // Keep only transaction-like messages, encrypted at rest with the Android Keystore key.
    try {
      android.content.SharedPreferences prefs=context.getSharedPreferences("finance_secure_prefs",Context.MODE_PRIVATE);
      String old=prefs.getString("pending_sms",null);JSONArray list=old==null?new JSONArray():new JSONArray(FinanceCrypto.decrypt(old));
      JSONObject row=new JSONObject();row.put("id",sender+":"+received);row.put("sender",sender);row.put("text",text);row.put("timestamp",received);list.put(row);
      while(list.length()>100)list.remove(0);
      prefs.edit().putString("pending_sms",FinanceCrypto.encrypt(list.toString())).apply();
    } catch(Exception ignored) { }
    String channel="finance_bank_alerts";
    if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel(channel,"Bank transaction alerts",NotificationManager.IMPORTANCE_DEFAULT));}
    NotificationCompat.Builder b=new NotificationCompat.Builder(context,channel).setSmallIcon(context.getApplicationInfo().icon).setContentTitle("Bank message received").setContentText("Open Finance to review a possible transaction").setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true);
    try{NotificationManagerCompat.from(context).notify((int)received,b.build());}catch(SecurityException ignored){}
  }
}
