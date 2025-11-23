package com.example.wordduel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GameSetup extends AppCompatActivity {

    EditText opponentNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game_setup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        opponentNumber = findViewById(R.id.editTextPhone);

        // Add button functionality
        setupButtons();
    }

    private void setupButtons() {
        // MENU Button - Goes back to MainActivity
        Button menuButton = findViewById(R.id.menu);
        menuButton.setOnClickListener(v -> {
            finish(); // Close this activity and go back to MainActivity
        });

        // START Button - Goes to ActiveGameScreen
        Button startButton = findViewById(R.id.start);
        startButton.setOnClickListener(v -> {
            String number = opponentNumber.getText().toString().trim();

            if (number.isEmpty()) {
                opponentNumber.setError("Please enter a phone number");
                return;
            }

            Intent intent = new Intent(GameSetup.this, ActiveGameScreen.class);
            intent.putExtra("opponentPhone", number);
            startActivity(intent);
        });

        // JOIN Button - Goes to JoinMatchScreen
        Button joinButton = findViewById(R.id.join);
        joinButton.setOnClickListener(v -> {
            String number = opponentNumber.getText().toString().trim();

            if (number.isEmpty()) {
                opponentNumber.setError("Please enter a phone number");
                return;
            }

            Intent intent = new Intent(GameSetup.this, JoinMatchScreen.class);
            intent.putExtra("opponentPhone", number);
            startActivity(intent);
        });
    }
}