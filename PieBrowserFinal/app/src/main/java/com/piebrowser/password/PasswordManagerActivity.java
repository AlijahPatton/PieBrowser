package com.piebrowser.password;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.piebrowser.R;
import com.piebrowser.ui.theme.ThemeManager;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * PasswordManagerActivity — view, edit, and delete saved passwords.
 * Protected by biometric authentication on entry.
 */
public class PasswordManagerActivity extends AppCompatActivity {

    private PasswordManager passwordManager;
    private RecyclerView recyclerView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passwords);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Saved Passwords");
        }

        passwordManager = PasswordManager.getInstance(this);
        recyclerView = findViewById(R.id.passwordsRecycler);
        emptyView    = findViewById(R.id.emptyPasswords);

        // Require biometric auth before showing passwords
        authenticateAndLoad();
    }

    private void authenticateAndLoad() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(
                        BiometricPrompt.AuthenticationResult result) {
                    loadPasswords();
                }

                @Override
                public void onAuthenticationError(int code, CharSequence errStr) {
                    Toast.makeText(PasswordManagerActivity.this,
                            "Authentication required", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onAuthenticationFailed() {
                    Toast.makeText(PasswordManagerActivity.this,
                            "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Passwords")
                .setSubtitle("Authenticate to view saved passwords")
                .setNegativeButtonText("Cancel")
                .build();

        prompt.authenticate(info);
    }

    private void loadPasswords() {
        List<PasswordManager.Credential> credentials = passwordManager.getAll();

        if (credentials.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new PasswordAdapter(credentials));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class PasswordAdapter extends
            RecyclerView.Adapter<PasswordAdapter.ViewHolder> {

        private final List<PasswordManager.Credential> items;

        PasswordAdapter(List<PasswordManager.Credential> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_password, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PasswordManager.Credential cred = items.get(position);
            holder.domain.setText(cred.domain);
            holder.username.setText(cred.username);
            holder.password.setText("••••••••");

            // Toggle password visibility
            boolean[] shown = {false};
            holder.toggleBtn.setOnClickListener(v -> {
                shown[0] = !shown[0];
                holder.password.setText(shown[0] ? cred.password : "••••••••");
            });

            // Delete
            holder.deleteBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(PasswordManagerActivity.this)
                    .setTitle("Delete Password")
                    .setMessage("Remove saved password for " + cred.domain + "?")
                    .setPositiveButton("Delete", (d, w) -> {
                        passwordManager.delete(cred.domain, cred.username);
                        items.remove(position);
                        notifyItemRemoved(position);
                        if (items.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView domain, username, password;
            ImageButton toggleBtn, deleteBtn;

            ViewHolder(View v) {
                super(v);
                domain    = v.findViewById(R.id.pw_domain);
                username  = v.findViewById(R.id.pw_username);
                password  = v.findViewById(R.id.pw_password);
                toggleBtn = v.findViewById(R.id.pw_toggle);
                deleteBtn = v.findViewById(R.id.pw_delete);
            }
        }
    }
}
