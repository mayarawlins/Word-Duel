package com.example.wordduel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;


public class JoinMatchScreen extends AppCompatActivity {

    TextView statusText;
    Button cancelButton;

    private String receivedGameId;
    private String receivedNonceA;
    private String opponentNumber;
    private String startingWord;

    private static final int REQUEST_SEND_SMS = 1;
    private static final int REQUEST_SMS_PERMISSIONS = 2;


    private String pendingSmsNumber;
    private String pendingSmsText;

    private BroadcastReceiver smsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_join_match_screen);

        // Glide was implemented to load the GIF
        ImageView gifView = findViewById(R.id.loadingGIF);
        Glide.with(this).asGif().load(R.drawable.loading).into(gifView);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusText = findViewById(R.id.statusTextView);
        cancelButton = findViewById(R.id.cancelButton);

        statusText.setText("Waiting For Invite...");

        cancelButton.setOnClickListener(v -> finish());

        // Get opponent number from intent (if sent from previous screen)
        opponentNumber = getIntent().getStringExtra("opponentPhone");

        setupTestButtons();

        // Check SMS permissions
        checkSMSPermissions();
    }

    /**
     * Check both SEND_SMS and RECEIVE_SMS permissions
     */
    private void checkSMSPermissions() {
        boolean hasSendSms = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

        boolean hasReceiveSms = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;

        if (hasSendSms && hasReceiveSms) {
            // Both permissions granted
            registerSmsReceiver();
        } else {

            // Request missing permissions
            java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();

            if (!hasSendSms) {
                permissionsToRequest.add(android.Manifest.permission.SEND_SMS);
            }
            if (!hasReceiveSms) {
                permissionsToRequest.add(android.Manifest.permission.RECEIVE_SMS);
            }

            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_SMS_PERMISSIONS);
        }
    }

    /**
     * Setup test buttons for debugging (optional - remove in production)
     */
    private void setupTestButtons() {
        // Test Invite Button
        Button testInviteButton = findViewById(R.id.testInviteButton);
        if (testInviteButton != null) {
            testInviteButton.setOnClickListener(v -> {
                String testMsg = "WD_INV|12345-abcdef|654321|apple";
                processHandshakeMessage(testMsg);
            });
        }

        // Test OK Button
        Button testOkButton = findViewById(R.id.testOkButton);
        if (testOkButton != null) {
            testOkButton.setOnClickListener(v -> {
                // Use the received game ID from the invite
                if (receivedGameId == null) {
                    Toast.makeText(this, "Click TEST INVITE first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String testOk = "WD_OK|" + receivedGameId + "|999999";
                processHandshakeMessage(testOk);
            });
        }
    }

    /**
     * Register a BroadcastReceiver to listen for internal game messages
     */
    private void registerSmsReceiver() {
        if (smsReceiver != null) return;

        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (GameMessageReceiver.ACTION_GAME_MESSAGE.equals(intent.getAction())) {
                    String message = intent.getStringExtra(GameMessageReceiver.EXTRA_MESSAGE);
                    String sender = intent.getStringExtra(GameMessageReceiver.EXTRA_SENDER);
                    
                    // Optional: Check if sender matches opponentNumber if we have one
                    if (opponentNumber != null && !opponentNumber.isEmpty() && sender != null) {
                         // Simple normalization check could be added here
                    }

                    if (message != null) {
                        processHandshakeMessage(message);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(GameMessageReceiver.ACTION_GAME_MESSAGE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(smsReceiver, filter);
        }
    }

    /**
     * Main method: handle INV, ACK, OK messages.
     */
    private void processHandshakeMessage(String msg) {

        // Log the message for debugging
        Toast.makeText(this, "Received: " + msg, Toast.LENGTH_LONG).show();

        String[] parts = SimpleHandshakeHandler.parseMessage(msg);

        if (parts.length == 0) {
            Toast.makeText(this, "Invalid message format", Toast.LENGTH_SHORT).show();
            return;
        }


        String cmd = parts[0];

        switch (cmd) {
            case "WD_INV":
                handleInvite(parts);
                break;

            case "WD_OK":
                handleOk(parts);
                break;

            default:
                statusText.setText("Unknown Command: " + cmd);
        }

    }

    /**
     * Player B receives invite:
     *   WD_INV | gameId | nonceA | word
     */
    private void handleInvite(String[] p) {

        if (p.length != 4) {
            statusText.setText("Invalid invite format. Expected 4 parts, got " + p.length);
            return;
        }


        receivedGameId = p[1];
        receivedNonceA = p[2];
        startingWord = p[3];

        statusText.setText("Invite Received!\nStarting Word: " + startingWord);

        // Player B generates their nonce
        String nonceB = SimpleHandshakeHandler.generateNonce();

        // Prepare ACK SMS
        String ackMessage = SimpleHandshakeHandler.buildAck(receivedGameId, receivedNonceA, nonceB);

        // If we don't have opponent number, we can't send ACK
        if (opponentNumber == null || opponentNumber.isEmpty()) {
            Toast.makeText(this, "No opponent number! Cannot send ACK", Toast.LENGTH_LONG).show();
            statusText.setText("Error: Missing opponent number");
            return;
        }

        // Send ACK
        checkSMSPermission(opponentNumber, ackMessage);

        statusText.setText("ACK Sent. Awaiting Confirmation (WD_OK)...");
    }


    /**
     * Player B receives confirmation:
     *   WD_OK | gameId | nonceB
     */
    private void handleOk(String[] p) {

        if (p.length != 3) {
            statusText.setText("Invalid OK format. Expected 3 parts, got " + p.length);
            return;
        }


        String okGameId = p[1];

        if (!okGameId.equals(receivedGameId)) {
            statusText.setText("Game ID Mismatch.");
            return;
        }


        statusText.setText("Handshake Complete!");

        goToGameScreen();
    }

    private void checkSMSPermission(String toNumber, String text) {
        // Save the info in case we need to request permission
        pendingSmsNumber = toNumber;
        pendingSmsText = text;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission not granted: request it
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.SEND_SMS},
                    REQUEST_SEND_SMS);

        } else {
            // Permission granted, send SMS directly
            sendSms();
        }
    }

    /**
     * Send SMS using SmsManager
     */
    private void sendSms() {
        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(pendingSmsNumber, null, pendingSmsText, null, null);

            Toast.makeText(this, "SMS sent to " + pendingSmsNumber, Toast.LENGTH_SHORT).show();

            // Clear the pending info
            pendingSmsNumber = null;
            pendingSmsText = null;

        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_SMS_PERMISSIONS) {
            // Check if all permissions were granted
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // All permissions granted
                registerSmsReceiver();
            } else {

                // Some permissions denied
                Toast.makeText(this, "SMS permissions required", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted: send the SMS
                sendSms();
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied! Cannot send SMS.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Launch the actual word duel game.
     */
    private void goToGameScreen() {
        Intent intent = new Intent(this, ActiveGameScreen.class);
        intent.putExtra("gameId", receivedGameId);
        intent.putExtra("opponentPhone", opponentNumber);
        intent.putExtra("lastWord", startingWord);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver to prevent memory leaks
        if (smsReceiver != null) {
            try {
                unregisterReceiver(smsReceiver);
            } catch (Exception e) {
                // Already unregistered
            }
        }
    }
}