package my.utar.uccd3223.uccd3223_individual_assignment_password_manager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class DashboardActivity extends AppCompatActivity
        implements PasswordAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private PasswordAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<PasswordEntry> entryList;
    private TextView tvEmptyState;
    private SearchView searchView;
    private boolean isNavigatingInternally = false;

    // Biometric prompt for re-authentication on sensitive actions
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    // Action to run after biometric auth succeeds
    private Runnable pendingAuthAction;

    private final ActivityResultLauncher<Intent> addEditLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> loadData()
            );

    // SAF launcher for exporting backup file
    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/octet-stream"),
                    uri -> {
                        if (uri != null) {
                            exportDatabase(uri);
                        }
                    });

    // SAF launcher for selecting backup file to import
    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            confirmAndImportDatabase(uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DatabaseHelper(this);
        entryList = new ArrayList<>();

        setupBiometric();

        recyclerView = findViewById(R.id.recyclerView);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        searchView = findViewById(R.id.searchView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        setupToolbarMenu();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PasswordAdapter(this, entryList, this);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditEntryActivity.class);
            isNavigatingInternally = true;
            addEditLauncher.launch(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterData(newText);
                return true;
            }
        });

        loadData();
    }

    // Set up toolbar overflow menu for export and import actions
    private void setupToolbarMenu() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_dashboard);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_export) {
                isNavigatingInternally = true;
                exportLauncher.launch("securevault_backup.db");
                return true;
            } else if (id == R.id.action_import) {
                isNavigatingInternally = true;
                importLauncher.launch(new String[]{"*/*"});
                return true;
            }
            return false;
        });
    }

    // Check if device supports biometric or device credential auth
    private boolean canAuthenticate() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int result = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    // Initialize BiometricPrompt and its callback
    private void setupBiometric() {
        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        if (pendingAuthAction != null) {
                            pendingAuthAction.run();
                            pendingAuthAction = null;
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                            @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        pendingAuthAction = null;
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Toast.makeText(DashboardActivity.this,
                                    getString(R.string.auth_error, errString),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(DashboardActivity.this,
                                R.string.auth_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.auth_action_subtitle))
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
    }

    // Authenticate then run action; skip if no authenticator enrolled
    private void authenticateAndRun(Runnable action) {
        if (canAuthenticate()) {
            pendingAuthAction = action;
            isNavigatingInternally = true;
            biometricPrompt.authenticate(promptInfo);
        } else {
            action.run();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isNavigatingInternally = false;
        loadData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isNavigatingInternally) {
            finish();
        }
    }

    private void loadData() {
        entryList = dbHelper.getAllData();
        adapter.updateData(entryList);
        updateEmptyState();
    }

    private void filterData(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadData();
        } else {
            List<PasswordEntry> filtered = dbHelper.searchData(query.trim());
            adapter.updateData(filtered);
            tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        if (entryList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Open entry for editing after biometric auth
    @Override
    public void onItemClick(PasswordEntry entry) {
        authenticateAndRun(() -> {
            Intent intent = new Intent(this, AddEditEntryActivity.class);
            intent.putExtra("ENTRY_ID", entry.getId());
            isNavigatingInternally = true;
            addEditLauncher.launch(intent);
        });
    }

    // Show copy popup after biometric auth
    @Override
    public void onCopyClick(PasswordEntry entry, View anchorView) {
        authenticateAndRun(() -> showCopyPopup(entry, anchorView));
    }

    // Display popup to choose which field to copy to clipboard
    private void showCopyPopup(PasswordEntry entry, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);

        final int ID_PASSWORD = 1;
        final int ID_USERNAME = 2;
        final int ID_PIN = 3;

        popup.getMenu().add(0, ID_PASSWORD, 0,
                getString(R.string.copy_password));

        String username = entry.getUsername();
        if (username != null && !username.isEmpty()) {
            popup.getMenu().add(0, ID_USERNAME, 1,
                    getString(R.string.copy_username));
        }

        String pin = entry.getExtraPin();
        if (pin != null && !pin.isEmpty()) {
            popup.getMenu().add(0, ID_PIN, 2,
                    getString(R.string.copy_pin));
        }

        popup.setOnMenuItemClickListener(item -> {
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip;
            switch (item.getItemId()) {
                case ID_USERNAME:
                    clip = ClipData.newPlainText("username",
                            entry.getUsername());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, R.string.username_copied,
                            Toast.LENGTH_SHORT).show();
                    return true;
                case ID_PIN:
                    clip = ClipData.newPlainText("pin",
                            entry.getExtraPin());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, R.string.pin_copied,
                            Toast.LENGTH_SHORT).show();
                    return true;
                case ID_PASSWORD:
                default:
                    clip = ClipData.newPlainText("password",
                            entry.getPassword());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, R.string.password_copied,
                            Toast.LENGTH_SHORT).show();
                    return true;
            }
        });

        popup.show();
    }

    // Export encrypted database to user-chosen location via SAF
    private void exportDatabase(Uri destinationUri) {
        try {
            // Flush WAL to ensure exported file contains all data
            dbHelper.getWritableDatabase().execSQL("PRAGMA wal_checkpoint(FULL)");
        } catch (Exception ignored) {
            // Non-critical, proceed with export
        }

        File dbFile = getDatabasePath(DatabaseHelper.DATABASE_NAME);
        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = getContentResolver().openOutputStream(destinationUri)) {
            if (out == null) throw new IOException("Cannot open output stream");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            Toast.makeText(this, R.string.export_success,
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.export_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Show confirmation dialog before importing backup
    private void confirmAndImportDatabase(Uri sourceUri) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_confirm_title)
                .setMessage(R.string.import_confirm_message)
                .setPositiveButton(R.string.yes, (dialog, which) ->
                        importDatabase(sourceUri))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    // Replace current database with selected backup file
    private void importDatabase(Uri sourceUri) {
        // Close current database to release file locks
        dbHelper.close();

        File dbFile = getDatabasePath(DatabaseHelper.DATABASE_NAME);
        try (InputStream in = getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(dbFile)) {
            if (in == null) throw new IOException("Cannot open input stream");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            // Remove leftover journal files
            new File(dbFile.getPath() + "-wal").delete();
            new File(dbFile.getPath() + "-shm").delete();

            // Re-open the database and refresh UI
            dbHelper = new DatabaseHelper(this);
            loadData();

            Toast.makeText(this, R.string.import_success,
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            // Re-open database to prevent crash
            dbHelper = new DatabaseHelper(this);
            loadData();
            Toast.makeText(this, R.string.import_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
