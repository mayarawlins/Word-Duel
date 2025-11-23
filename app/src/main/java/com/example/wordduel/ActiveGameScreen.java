package com.example.wordduel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import android.os.CountDownTimer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class ActiveGameScreen extends AppCompatActivity {

    TextView statusText;
    TextView lastWordText;
    TextView requiredLetterText;
    TextView scoreText;
    TextView timerText;
    EditText wordInput;
    Button sendWordButton;
    Button forfeitButton;

    
    private CountDownTimer turnTimer;


    private String opponentNumber;
    private String gameId;
    private String nonceA;
    private String receivedNonceB;
    
    private GameState gameState;

    private String lastWord;

    private char requiredStartLetter;
    private boolean isMyTurn;

    private static final int REQUEST_SMS_PERMISSIONS = 1;


    private BroadcastReceiver smsReceiver;

    private boolean isInitiator = false;
    private boolean handshakeComplete = false;
    
    private Set<String> dictionary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_active_game_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusText = findViewById(R.id.statusTextView);
        lastWordText = findViewById(R.id.lastWordTextView);
        requiredLetterText = findViewById(R.id.requiredLetterTextView);
        scoreText = findViewById(R.id.textView7);
        timerText = findViewById(R.id.timerText);
        wordInput = findViewById(R.id.wordInput);
        sendWordButton = findViewById(R.id.sendWordButton);
        forfeitButton = findViewById(R.id.forfeitButton);
        
        forfeitButton.setOnClickListener(v -> {
            if (turnTimer != null) turnTimer.cancel();
            
            gameState.setForfeit(true);
            gameState.setWinner("Opponent");
            saveGame();
            
            // Send Loss Notification
            String lossMessage = "WD_LOSS|" + gameId;
            sendSms(opponentNumber, lossMessage);

            
            Toast.makeText(this, "You Forfeited!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ActiveGameScreen.this, Game_Over_Screen.class);
            intent.putExtra("myScore", gameState.getMyScore());
            intent.putExtra("opponentScore", gameState.getOpponentScore());
            intent.putExtra("winner", "Opponent");
            startActivity(intent);
            finish();
        });


        
        updateScoreDisplay();

        // Get data from intent
        opponentNumber = getIntent().getStringExtra("opponentPhone");
        gameId = getIntent().getStringExtra("gameId");
        lastWord = getIntent().getStringExtra("lastWord");

        // Determine if this player is the initiator or joiner
        if (gameId == null) {
            // No gameId means this player is the initiator (Player A)
            isInitiator = true;
            setupInitiatorMode();
        } else {
            // Has gameId means came from JoinMatchScreen (Player B)
            isInitiator = false;
            handshakeComplete = true;
            setupJoinerMode();
        }

        // Check SMS permissions
        checkSMSPermissions();
        
        // Load dictionary
        loadDictionary();
    }
    
    private void loadDictionary() {
        dictionary = new HashSet<>();
        new Thread(() -> {
            try {
                InputStream inputStream = getResources().openRawResource(R.raw.words_alpha);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    dictionary.add(line.trim().toLowerCase());
                }
                reader.close();
                runOnUiThread(() -> Toast.makeText(this, "Dictionary Loaded", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error loading dictionary", Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    /**
     * Setup for Player A (Initiator) - Must send starting word
     */
    private void setupInitiatorMode() {
        statusText.setText("Your Turn!");
        lastWordText.setText("Last Word: None");
        requiredLetterText.setText("Starting The Game...");

        wordInput.setEnabled(true);
        wordInput.setHint("Enter Starting Word");
        sendWordButton.setText("Send Invite");
        sendWordButton.setEnabled(true);

        isMyTurn = true; // Player A goes first

        sendWordButton.setOnClickListener(v -> {
            String word = wordInput.getText().toString().trim().toLowerCase();

            if (word.isEmpty()) {
                Toast.makeText(this, "Please enter a starting word", Toast.LENGTH_SHORT).show();
                return;
            }

            if (opponentNumber == null || opponentNumber.isEmpty()) {
                Toast.makeText(this, "No opponent number!", Toast.LENGTH_LONG).show();
                return;
            }

            // Validate word is in dictionary
            if (dictionary != null && !dictionary.contains(word)) {
                Toast.makeText(this, "Word not found in dictionary!", Toast.LENGTH_SHORT).show();
                return;
            }

            sendInvite(word);
        });

    }

    /**
     * Setup for Player B (Joiner) - Received starting word, must respond
     */
    private void setupJoinerMode() {
        // Player B received the starting word in the invite
        if (lastWord != null && !lastWord.isEmpty()) {
            requiredStartLetter = lastWord.charAt(lastWord.length() - 1);
            
            // Initialize GameState for Player B
            gameState = new GameState(gameId);
            gameState.addWord(lastWord);

            lastWordText.setText("Last Word: " + lastWord);
            requiredLetterText.setText("Send Word Starting With: " + Character.toUpperCase(requiredStartLetter));
            statusText.setText("Your Turn!");

            isMyTurn = true; // Player B's turn after receiving invite

            wordInput.setEnabled(true);
            wordInput.setHint("Word Starting With '" + requiredStartLetter + "'");
            sendWordButton.setText("Send Word");
            sendWordButton.setEnabled(true);
            
            startTurnTimer();

            sendWordButton.setOnClickListener(v -> sendMove());

        } else {
            statusText.setText("Error: No starting word received");
            wordInput.setEnabled(false);
            sendWordButton.setEnabled(false);
        }
    }

    /**
     * Player A sends the initial invite with starting word
     */
    private void sendInvite(String startingWord) {
        // Generate unique game ID and nonce
        gameId = SimpleHandshakeHandler.generateGameId();
        
        // Initialize GameState
        gameState = new GameState(gameId);
        
        nonceA = SimpleHandshakeHandler.generateNonce();

        // Build invite message

        String inviteMessage = SimpleHandshakeHandler.buildInvite(gameId, nonceA, startingWord);

        // Send SMS
        sendSms(opponentNumber, inviteMessage);

        // Store the starting word
        lastWord = startingWord;
        gameState.addWord(startingWord);

        statusText.setText("Invite Sent! Waiting For Opponent...");

        wordInput.setEnabled(false);
        sendWordButton.setEnabled(false);

        isMyTurn = false; // Player A's turn is done

        // Start listening for ACK
    }


    /**
     * Send a game move (word) to opponent
     */
    private void sendMove() {
        String word = wordInput.getText().toString().trim().toLowerCase();

        if (word.isEmpty()) {
            Toast.makeText(this, "Please enter a word", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate word starts with required letter
        if (word.charAt(0) != requiredStartLetter) {
            Toast.makeText(this, "Word must start with '" + requiredStartLetter + "'", Toast.LENGTH_LONG).show();
            return;
        }

        // Validate word is in dictionary
        if (dictionary != null && !dictionary.contains(word)) {
            Toast.makeText(this, "Word not found in dictionary!", Toast.LENGTH_SHORT).show();
            return;
        }


        // Build move message
        String moveMessage = SimpleHandshakeHandler.buildMove(gameId, word);

        // Send SMS
        sendSms(opponentNumber, moveMessage);
        
        stopTurnTimer();

        // Update game state
        lastWord = word;
        gameState.addWord(word);
        lastWordText.setText("Last Word: " + word);

        statusText.setText("Word sent! Waiting For Opponent...");
        
        char nextRequired = word.charAt(word.length() - 1);
        requiredLetterText.setText("Waiting For Word Starting With: " + Character.toUpperCase(nextRequired));

        wordInput.setText("");
        wordInput.setEnabled(false);
        sendWordButton.setEnabled(false);

        isMyTurn = false;

        // Continue listening for opponent's response
    }


    /**
     * Display opponent's move and enable player's turn
     */
    private void enablePlayerTurn(String opponentWord) {
        lastWord = opponentWord;
        requiredStartLetter = opponentWord.charAt(opponentWord.length() - 1);

        lastWordText.setText("Last Word: " + opponentWord);
        requiredLetterText.setText("Send Word Starting With: " + Character.toUpperCase(requiredStartLetter));
        statusText.setText("Your Turn!");

        wordInput.setEnabled(true);
        wordInput.setHint("Word starting with '" + requiredStartLetter + "'");
        wordInput.setText("");
        sendWordButton.setText("Send Word");
        sendWordButton.setEnabled(true);
        
        startTurnTimer();

        isMyTurn = true;
    }
    
    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        
        turnTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("⏱ " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                timerText.setText("Time's Up!");
                Toast.makeText(ActiveGameScreen.this, "Time's Up! You Lost!", Toast.LENGTH_LONG).show();
                
                gameState.setWinner("Opponent");
                saveGame();
                
                // Send Loss Notification
                String lossMessage = "WD_LOSS|" + gameId;
                sendSms(opponentNumber, lossMessage);

                
                // Go to Game Over screen
                Intent intent = new Intent(ActiveGameScreen.this, Game_Over_Screen.class);
                intent.putExtra("myScore", gameState.getMyScore());
                intent.putExtra("opponentScore", gameState.getOpponentScore());
                intent.putExtra("winner", "Opponent");
                startActivity(intent);
                finish();

            }
        }.start();
    }
    
    private void stopTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        timerText.setText("⏱");
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
            // Both permissions granted - register receiver for game messages
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
     * Register a BroadcastReceiver to listen for internal game messages
     * This stays active for the entire game duration
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
                        processMessage(message);
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
     * Process incoming messages (both handshake and game moves)
     */
    private void processMessage(String msg) {
        Toast.makeText(this, "Received: " + msg, Toast.LENGTH_LONG).show();

        String[] parts = SimpleHandshakeHandler.parseMessage(msg);

        if (parts.length == 0) {
            Toast.makeText(this, "Invalid message format", Toast.LENGTH_SHORT).show();
            return;
        }


        String cmd = parts[0];

        switch (cmd) {
            case "WD_ACK":
                // Only Player A (initiator) receives ACK during handshake
                handleAck(parts);
                break;

            case "WD_MOVE":
                // Both players receive moves during gameplay
                handleMove(parts);
                break;
                
            case "WD_LOSS":
                handleLoss(parts);
                break;


            default:
                statusText.setText("Unknown Command: " + cmd);
        }

    }

    /**
     * Player A receives ACK from Player B during handshake:
     *   WD_ACK | gameId | nonceA | nonceB
     */
    private void handleAck(String[] p) {
        if (p.length != 4) {
            statusText.setText("Invalid ACK Format. Expected 4 Parts, Got " + p.length);
            return;
        }


        String ackGameId = p[1];
        String ackNonceA = p[2];
        receivedNonceB = p[3];

        // Validate gameId and nonceA
        if (!ackGameId.equals(gameId)) {

            statusText.setText("Game ID Mismatch!");
            Toast.makeText(this, "Invalid ACK: Game ID doesn't match", Toast.LENGTH_LONG).show();
            return;
        }

        if (!ackNonceA.equals(nonceA)) {
            statusText.setText("Nonce A Mismatch!");
            Toast.makeText(this, "Invalid ACK: Nonce doesn't match", Toast.LENGTH_LONG).show();
            return;
        }

        // ACK is valid, send OK confirmation
        statusText.setText("Opponent Joined! Sending Confirmation...");

        String okMessage = SimpleHandshakeHandler.buildOk(gameId, receivedNonceB);
        sendSms(opponentNumber, okMessage);

        // Handshake complete - now waiting for opponent's move
        handshakeComplete = true;

        requiredStartLetter = lastWord.charAt(lastWord.length() - 1);
        lastWordText.setText("Last Word: " + lastWord);
        requiredLetterText.setText("Waiting For Word Starting With: " + Character.toUpperCase(requiredStartLetter));
        statusText.setText("Opponent's Turn...");

        // Continue listening for opponent's move
    }


    /**
     * Handle opponent's game move:
     *   WD_MOVE | gameId | word
     */
    private void handleMove(String[] p) {
        if (p.length != 3) {
            Toast.makeText(this, "Invalid move format", Toast.LENGTH_SHORT).show();
            return;
        }


        String moveGameId = p[1];
        String opponentWord = p[2];

        // Validate it's for this game
        if (!moveGameId.equals(gameId)) {
            Toast.makeText(this, "Move for wrong game!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate opponent's word starts with correct letter
        char requiredChar = lastWord.charAt(lastWord.length() - 1);
        if (Character.toLowerCase(opponentWord.charAt(0)) != Character.toLowerCase(requiredChar)) {
            Toast.makeText(this, "Invalid move: Word doesn't start with required letter", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Opponent played: " + opponentWord, Toast.LENGTH_LONG).show();

        gameState.addWord(opponentWord);
        gameState.incrementOpponentScore();
        updateScoreDisplay();

        // Enable this player's turn

        enablePlayerTurn(opponentWord);
    }
    
    /**
     * Handle opponent's loss notification:
     *   WD_LOSS | gameId
     */
    private void handleLoss(String[] p) {
        if (p.length != 2) {
            return;
        }
        
        String lossGameId = p[1];
        
        if (!lossGameId.equals(gameId)) {
            return;
        }
        
        Toast.makeText(this, "Opponent Lost! You Win!", Toast.LENGTH_LONG).show();
        
        if (turnTimer != null) turnTimer.cancel();
        
        gameState.setWinner("Me");
        saveGame();
        
        Intent intent = new Intent(ActiveGameScreen.this, Game_Over_Screen.class);
        intent.putExtra("myScore", gameState.getMyScore());
        intent.putExtra("opponentScore", gameState.getOpponentScore());
        intent.putExtra("winner", "Me");
        startActivity(intent);
        finish();
    }


    /**
     * Send SMS using SmsManager
     */
    private void sendSms(String toNumber, String message) {
        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(toNumber, null, message, null, null);

            Toast.makeText(this, "SMS sent", Toast.LENGTH_SHORT).show();

            // Only count score for game moves, not handshake messages (WD_INV, WD_OK)
            if (message.startsWith("WD_MOVE")) {
                gameState.incrementMyScore();
                updateScoreDisplay();
            }

        } catch (Exception e) {

            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void updateScoreDisplay() {
        if (gameState != null) {
            scoreText.setText("You: " + gameState.getMyScore() + " VS Opponent: " + gameState.getOpponentScore());
        }
    }
    
    private void saveGame() {
        if (gameState == null) return;

        boolean append = false;
        for (String file : fileList()) {
            if (file.equals("game_history.txt")) {
                append = true;
                break;
            }
        }

        try {
            FileOutputStream fos = openFileOutput("game_history.txt", MODE_APPEND);
            ObjectOutputStream oos;
            
            if (append) {
                oos = new AppendingObjectOutputStream(fos);
            } else {
                oos = new ObjectOutputStream(fos);
            }
            
            oos.writeObject(gameState);
            oos.close();
            Toast.makeText(this, "Game Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving game", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Custom ObjectOutputStream that doesn't write a header when appending
    private static class AppendingObjectOutputStream extends ObjectOutputStream {
        public AppendingObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }
        
        @Override
        protected void writeStreamHeader() throws IOException {
            // Do not write a header, but reset:
            reset();
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
                // All permissions granted - register receiver
                registerSmsReceiver();
            } else {
                // Some permissions denied
                Toast.makeText(this, "SMS permissions required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        // Unregister receiver when leaving the game

        if (smsReceiver != null) {
            try {
                unregisterReceiver(smsReceiver);
            } catch (Exception e) {
                // Already unregistered
            }
        }
    }
}