package com.example.wordduel;

import java.util.UUID;
import java.security.SecureRandom;

public class SimpleHandshakeHandler {

    private static final SecureRandom random = new SecureRandom();

    public static String generateNonce() {
        int num = random.nextInt(900000) + 100000;
        return String.valueOf(num);
    }

    public static String generateGameId() {
        return UUID.randomUUID().toString();
    }

    public static String buildInvite(String gameId, String nonceA, String word) {
        return "WD_INV|" + gameId + "|" + nonceA + "|" + word;
    }

    public static String buildAck(String gameId, String nonceA, String nonceB) {
        return "WD_ACK|" + gameId + "|" + nonceA + "|" + nonceB;
    }

    public static String buildOk(String gameId, String nonceB) {
        return "WD_OK|" + gameId + "|" + nonceB;
    }

    public static String buildMove(String gameId, String word) {
        return "WD_MOVE|" + gameId + "|" + word;
    }

    public static String[] parseMessage(String msg) {
        return msg.split("\\|");
    }
}

