package com.example.wordduel;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class HighScore extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> gameStrings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_score);

        listView = findViewById(R.id.listView);
        
        gameStrings = new ArrayList<>();
        loadGameHistory();
        
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, gameStrings);
        listView.setAdapter(adapter);
    }

    private void loadGameHistory() {
        try (FileInputStream fis = openFileInput("game_history.txt");
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            
            while (true) {
                try {
                    Object obj = ois.readObject();
                    if (obj instanceof GameState) {
                        GameState game = (GameState) obj;
                        String winner = game.getWinner() != null ? game.getWinner() : "In Progress";
                        String displayString = "Game: " + game.getGameId() + "\n" +
                                "Me: " + game.getMyScore() + " | Opp: " + game.getOpponentScore() + "\n" +
                                "Winner: " + winner;
                        gameStrings.add(displayString);
                    }
                } catch (EOFException e) {
                    break; // End of file
                }
            }
            
        } catch (IOException | ClassNotFoundException e) {
            // File might not exist or is empty
            // Toast.makeText(this, "No history found", Toast.LENGTH_SHORT).show();
        }
    }
}
