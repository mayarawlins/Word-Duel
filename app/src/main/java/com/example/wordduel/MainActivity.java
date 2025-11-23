package com.example.wordduel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Add button functionality here
        setupButtons();
    }

    private void setupButtons() {
        // Exit Button - Closes the app
        Button exitButton = findViewById(R.id.exit);
        exitButton.setOnClickListener(v -> {
            finish(); // Close current activity
            System.exit(0); // Fully exit the app
        });

        // High Score Button - Goes to HighScoreActivity
        Button highScoreButton = findViewById(R.id.highScore);
        highScoreButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HighScore.class);
            startActivity(intent);
        });

        // New Game Button - Goes to new activity
        Button newGameButton = findViewById(R.id.newgame);
        newGameButton.setOnClickListener(v -> {
            // Create intent to start NewGameActivity
            Intent intent = new Intent(MainActivity.this, GameSetup.class);
            startActivity(intent);
        });
    }
}