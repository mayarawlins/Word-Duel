package com.example.wordduel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Game_Over_Screen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game_over_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Add button functionality
        setupButtons();
        
        // Display results
        displayResults();
    }
    
    private void displayResults() {
        int myScore = getIntent().getIntExtra("myScore", 0);
        int opponentScore = getIntent().getIntExtra("opponentScore", 0);
        String winner = getIntent().getStringExtra("winner");
        
        TextView playerScoreText = findViewById(R.id.playerscore);
        TextView oppScoreText = findViewById(R.id.oppscore);
        TextView resultText = findViewById(R.id.resultText);
        
        playerScoreText.setText(String.valueOf(myScore));
        oppScoreText.setText(String.valueOf(opponentScore));
        
        if ("Me".equals(winner)) {
            resultText.setText("You Win");
        } else if ("Opponent".equals(winner)) {
            resultText.setText("You Lose");
        } else {
            resultText.setText("Game Over");
        }
    }


    private void setupButtons() {
        // MENU Button - Goes back to MainActivity
        Button menuButton = findViewById(R.id.menu2);
        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(Game_Over_Screen.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clears all activities above MainActivity
            startActivity(intent);
            finish();
        });

        // NEW GAME Button - Goes to GameSetup
        Button newGameButton = findViewById(R.id.playAgain);
        newGameButton.setOnClickListener(v -> {
            Intent intent = new Intent(Game_Over_Screen.this, GameSetup.class);
            startActivity(intent);
            finish();
        });
    }
}