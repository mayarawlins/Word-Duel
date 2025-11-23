package com.example.wordduel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    private String gameId;
    private int myScore;
    private int opponentScore;
    private List<String> wordChain;
    private String winner; // "Me", "Opponent", or "None"
    private boolean isForfeit;

    public GameState(String gameId) {
        this.gameId = gameId;
        this.myScore = 0;
        this.opponentScore = 0;
        this.wordChain = new ArrayList<>();
        this.winner = "None";
        this.isForfeit = false;
    }

    public void addWord(String word) {
        wordChain.add(word);
    }

    public void incrementMyScore() {
        this.myScore++;
    }

    public void incrementOpponentScore() {
        this.opponentScore++;
    }

    public int getMyScore() {
        return myScore;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    public List<String> getWordChain() {
        return wordChain;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }
    
    public String getWinner() {
        return winner;
    }
    
    public String getGameId() {
        return gameId;
    }

    public void setForfeit(boolean forfeit) {
        isForfeit = forfeit;
    }

    // Custom serialization to string
    public String toSaveString() {
        StringBuilder sb = new StringBuilder();
        sb.append("---BEGIN GAME STATE---\n");
        sb.append("GameID: ").append(gameId).append("\n");
        sb.append("MyScore: ").append(myScore).append("\n");
        sb.append("OpponentScore: ").append(opponentScore).append("\n");
        sb.append("Winner: ").append(winner).append("\n");
        sb.append("Forfeit: ").append(isForfeit).append("\n");
        sb.append("WordChain: ");
        
        for (int i = 0; i < wordChain.size(); i++) {
            sb.append(wordChain.get(i));
            if (i < wordChain.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("\n");
        sb.append("---END GAME STATE---\n");
        return sb.toString();
    }
}