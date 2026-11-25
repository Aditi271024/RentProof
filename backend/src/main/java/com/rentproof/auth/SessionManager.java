package com.rentproof.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static String createSession(int landlordId, String email) {
        purgeExpired();
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        SESSIONS.put(token, new Session(landlordId, email, System.currentTimeMillis()));
        return token;
    }

    public static Integer getLandlordId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Session session = SESSIONS.get(token);
        if (session == null) {
            return null;
        }
        if (System.currentTimeMillis() - session.lastAccessMs > SESSION_TIMEOUT_MS) {
            SESSIONS.remove(token);
            return null;
        }
        session.lastAccessMs = System.currentTimeMillis();
        return session.landlordId;
    }

    public static void invalidate(String token) {
        if (token != null) {
            SESSIONS.remove(token);
        }
    }

    public static void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().lastAccessMs > SESSION_TIMEOUT_MS) {
                iterator.remove();
            }
        }
    }

    private static final class Session {
        private final int landlordId;
        private final String email;
        private volatile long lastAccessMs;

        private Session(int landlordId, String email, long lastAccessMs) {
            this.landlordId = landlordId;
            this.email = email;
            this.lastAccessMs = lastAccessMs;
        }
    }
}
