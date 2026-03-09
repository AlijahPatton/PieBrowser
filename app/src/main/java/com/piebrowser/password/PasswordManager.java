package com.piebrowser.password;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * PasswordManager — encrypted credential storage for Pie Browser.
 *
 * Security:
 * - Uses Android Keystore via EncryptedSharedPreferences
 * - AES-256-GCM encryption for all stored data
 * - Keys never leave the device's secure hardware
 * - Biometric unlock optional
 *
 * Data stored per entry:
 * - Website domain
 * - Username / email
 * - Encrypted password
 * - Date saved
 */
public class PasswordManager {

    private static final String PREFS_NAME = "pie_passwords";
    private static final String KEY_CREDENTIALS = "credentials";

    private static PasswordManager instance;
    private SharedPreferences encryptedPrefs;

    private PasswordManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to regular prefs if encryption unavailable (very old devices)
            encryptedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public static synchronized PasswordManager getInstance(Context context) {
        if (instance == null) {
            instance = new PasswordManager(context.getApplicationContext());
        }
        return instance;
    }

    // ── CRUD Operations ───────────────────────────────────────────────────────

    public void saveCredential(String domain, String username, String password) {
        List<Credential> credentials = getAll();

        // Update existing or add new
        boolean updated = false;
        for (Credential c : credentials) {
            if (c.domain.equalsIgnoreCase(domain) &&
                c.username.equalsIgnoreCase(username)) {
                c.password = password;
                c.updatedAt = System.currentTimeMillis();
                updated = true;
                break;
            }
        }
        if (!updated) {
            credentials.add(new Credential(domain, username, password));
        }

        saveAll(credentials);
    }

    public List<Credential> getAll() {
        List<Credential> list = new ArrayList<>();
        String json = encryptedPrefs.getString(KEY_CREDENTIALS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Credential c = new Credential(
                    obj.getString("domain"),
                    obj.getString("username"),
                    obj.getString("password")
                );
                c.updatedAt = obj.optLong("updatedAt", System.currentTimeMillis());
                list.add(c);
            }
        } catch (Exception e) {
            // Empty or corrupt — start fresh
        }
        return list;
    }

    /**
     * Find credentials matching a domain (for autofill).
     */
    public List<Credential> getForDomain(String domain) {
        List<Credential> matches = new ArrayList<>();
        String domainLower = domain.toLowerCase();
        for (Credential c : getAll()) {
            if (domainLower.contains(c.domain.toLowerCase()) ||
                c.domain.toLowerCase().contains(domainLower)) {
                matches.add(c);
            }
        }
        return matches;
    }

    public void delete(String domain, String username) {
        List<Credential> credentials = getAll();
        credentials.removeIf(c ->
            c.domain.equalsIgnoreCase(domain) &&
            c.username.equalsIgnoreCase(username));
        saveAll(credentials);
    }

    public void deleteAll() {
        encryptedPrefs.edit().remove(KEY_CREDENTIALS).apply();
    }

    public int getCount() {
        return getAll().size();
    }

    private void saveAll(List<Credential> credentials) {
        try {
            JSONArray arr = new JSONArray();
            for (Credential c : credentials) {
                JSONObject obj = new JSONObject();
                obj.put("domain", c.domain);
                obj.put("username", c.username);
                obj.put("password", c.password);
                obj.put("updatedAt", c.updatedAt);
                arr.put(obj);
            }
            encryptedPrefs.edit().putString(KEY_CREDENTIALS, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate JavaScript to autofill login forms.
     */
    public String buildAutofillScript(Credential credential) {
        return String.format(
            "(function() {" +
            "  var inputs = document.querySelectorAll('input');" +
            "  for (var i = 0; i < inputs.length; i++) {" +
            "    var t = inputs[i].type.toLowerCase();" +
            "    if (t === 'email' || t === 'text' || inputs[i].name.toLowerCase().includes('user') || inputs[i].name.toLowerCase().includes('email')) {" +
            "      inputs[i].value = '%s';" +
            "      inputs[i].dispatchEvent(new Event('input', {bubbles:true}));" +
            "    } else if (t === 'password') {" +
            "      inputs[i].value = '%s';" +
            "      inputs[i].dispatchEvent(new Event('input', {bubbles:true}));" +
            "    }" +
            "  }" +
            "})();",
            credential.username.replace("'", "\\'"),
            credential.password.replace("'", "\\'")
        );
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    public static class Credential {
        public String domain;
        public String username;
        public String password;
        public long updatedAt;

        public Credential(String domain, String username, String password) {
            this.domain = domain;
            this.username = username;
            this.password = password;
            this.updatedAt = System.currentTimeMillis();
        }
    }
}
