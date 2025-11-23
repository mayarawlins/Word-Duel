# Word Duel - SMS Based Word Game

Word Duel is a turn-based, multiplayer word game for Android that uses SMS for communication. Players take turns sending words that start with the last letter of the opponent's previous word.

## 📱 Features

*   **SMS-Based Gameplay:** Play with anyone in your contacts without needing a central server or internet connection.
*   **Turn-Based Logic:** Enforces turn order and validates moves.
*   **Word Validation:**
    *   Checks if the word exists in a local dictionary (`words_alpha.txt`).
    *   Ensures the word starts with the required letter (last letter of the previous word).
*   **Game Timer:** A 30-second countdown timer per turn adds pressure. If time runs out, you lose!
*   **Forfeit Option:** Players can choose to forfeit the game at any time.
*   **Persistence:** Game history is saved locally, allowing players to view past match results.
*   **High Scores:** A dedicated screen to view a history of all games played, including scores and winners.
*   **Robust Handshake:** Uses a secure handshake protocol (`WD_INV`, `WD_ACK`, `WD_OK`) to establish game sessions.

## 🛠 Technical Architecture

### Core Components

1.  **`ActiveGameScreen.java`**: The heart of the game.
    *   Manages the game loop, UI updates, and user input.
    *   Handles the 30-second `CountDownTimer`.
    *   Sends and receives SMS messages using `SmsManager` and a `BroadcastReceiver`.
    *   Validates words against the loaded dictionary.

2.  **`GameState.java`**: The data model.
    *   Encapsulates all game data: `gameId`, `myScore`, `opponentScore`, `wordChain`, `winner`, `isForfeit`.
    *   Implements `Serializable` for easy saving/loading.

3.  **`SimpleHandshakeHandler.java`**: The protocol manager.
    *   Generates unique Game IDs and Nonces.
    *   Builds and parses the raw SMS strings (e.g., `WD_MOVE|GAME123|apple`).

4.  **`HighScore.java`**: The history viewer.
    *   Reads the `game_history.txt` file.
    *   Displays a list of past games using a `ListView`.

### Persistence Strategy

*   **File:** `game_history.txt` stored in internal storage.
*   **Format:** Java Object Serialization.
*   **Method:**
    *   **New File:** Writes a standard object stream header.
    *   **Appending:** Uses a custom `AppendingObjectOutputStream` to skip the header, allowing multiple objects to be appended to the same file without corruption.

### SMS Protocol

*   **Invite:** `WD_INV | GameID | NonceA | StartingWord`
*   **Acknowledge:** `WD_ACK | GameID | NonceA | NonceB`
*   **Confirm:** `WD_OK | GameID | NonceB`
*   **Move:** `WD_MOVE | GameID | Word`
*   **Loss:** `WD_LOSS | GameID` (Sent on forfeit or timeout)

## 🚀 Setup & Installation

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/yourusername/Word-Duel.git
    ```
2.  **Open in Android Studio:**
    *   Select "Open an existing Android Studio project" and navigate to the cloned folder.
3.  **Permissions:**
    *   The app requires `SEND_SMS` and `RECEIVE_SMS` permissions.
    *   Grant these permissions when prompted on the first launch.
4.  **Run:**
    *   Connect two Android devices or use two Emulators.
    *   **Note for Emulators:** Use port numbers (e.g., `5554`) as phone numbers.

## 🎮 How to Play

1.  **Start a Game:**
    *   Enter your opponent's phone number.
    *   Tap "Start"
    *   Enter a starting word (e.g., "apple").
    *   Tap "Send Invite".
2.  **Join a Game:**
    *   Tap "Join"
    *   Wait for an invite SMS.
    *   The app will automatically handle the handshake.
    *   Once connected, reply with a word starting with the last letter of the received word (e.g., "elephant").
3.  **Gameplay:**
    *   Take turns sending valid words.
    *   You have 30 seconds per turn!
    *   Each valid word earns you 1 point.
4.  **Winning:**
    *   The game continues until someone forfeits or runs out of time.
    *   The surviving player wins!

## 🔮 Future Improvements

*   **UI Polish:** Add animations for sending/receiving words.
*   **Contact Picker:** Integrate with the phone's contact list.
*   **Push Notifications:** For game invites when the app is closed.
*   **Online Mode:** Optional Firebase/Server backend for internet play.

---
*Built for Android Course Assignment*
