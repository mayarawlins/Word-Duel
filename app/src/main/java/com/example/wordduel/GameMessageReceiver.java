package com.example.wordduel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class GameMessageReceiver extends BroadcastReceiver {
    private static final String TAG = "GameMessageReceiver";
    public static final String ACTION_GAME_MESSAGE = "com.example.wordduel.GAME_MESSAGE_RECEIVED";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_SENDER = "sender";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    String format = bundle.getString("format");
                    
                    if (pdus != null) {
                        for (Object pdu : pdus) {
                            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                            String messageBody = sms.getMessageBody();
                            String sender = sms.getOriginatingAddress();

                            Log.d(TAG, "SMS received from: " + sender + ", body: " + messageBody);

                            if (messageBody != null && messageBody.startsWith("WD_")) {
                                // It's a game message! Broadcast it to the activities.
                                Intent gameIntent = new Intent(ACTION_GAME_MESSAGE);
                                gameIntent.putExtra(EXTRA_MESSAGE, messageBody);
                                gameIntent.putExtra(EXTRA_SENDER, sender);
                                // We use the application context's package name to keep it internal to the app
                                gameIntent.setPackage(context.getPackageName());
                                context.sendBroadcast(gameIntent);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing SMS", e);
                }
            }
        }
    }
}
