package com.roze.dbnavigator.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.dbnavigator.model.ConnectionProfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Persists connection profiles to ~/.dbnavigator/connections.json.
 *
 * NOTE: saved passwords are only Base64-obfuscated, not encrypted. For real
 * security, integrate the OS keychain (e.g. via java-keyring) or leave
 * "save password" unchecked so you are prompted per session.
 */
public final class ConnectionStore {

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "connections.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final List<ConnectionProfile> profiles = new ArrayList<>();

    private ConnectionStore() {}

    public static synchronized List<ConnectionProfile> load() {
        profiles.clear();
        if (Files.exists(FILE)) {
            try {
                List<ConnectionProfile> loaded = MAPPER.readValue(
                        FILE.toFile(), new TypeReference<List<ConnectionProfile>>() {});
                for (ConnectionProfile p : loaded) {
                    if (p.isSavePassword() && p.getPassword() != null && !p.getPassword().isEmpty()) {
                        p.setPassword(decode(p.getPassword()));
                    }
                    profiles.add(p);
                }
            } catch (IOException e) {
                System.err.println("Could not read connections file: " + e.getMessage());
            }
        }
        return new ArrayList<>(profiles);
    }

    public static synchronized void saveOrUpdate(ConnectionProfile profile) {
        profiles.removeIf(p -> p.getId().equals(profile.getId()));
        profiles.add(profile);
        persist();
    }

    public static synchronized void delete(ConnectionProfile profile) {
        profiles.removeIf(p -> p.getId().equals(profile.getId()));
        persist();
    }

    private static void persist() {
        try {
            Files.createDirectories(FILE.getParent());
            List<ConnectionProfile> toWrite = new ArrayList<>();
            for (ConnectionProfile p : profiles) {
                ConnectionProfile copy = p.copy();
                if (copy.isSavePassword() && copy.getPassword() != null) {
                    copy.setPassword(encode(copy.getPassword()));
                } else {
                    copy.setPassword("");
                }
                toWrite.add(copy);
            }
            MAPPER.writeValue(FILE.toFile(), toWrite);
        } catch (IOException e) {
            System.err.println("Could not save connections file: " + e.getMessage());
        }
    }

    private static String encode(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String decode(String s) {
        try {
            return new String(Base64.getDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return s;
        }
    }
}
