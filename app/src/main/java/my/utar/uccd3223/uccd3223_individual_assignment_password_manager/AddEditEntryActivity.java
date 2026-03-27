package my.utar.uccd3223.uccd3223_individual_assignment_password_manager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.security.SecureRandom;

public class AddEditEntryActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etUsername, etPassword, etUrl, etExtraPin, etRemark;
    private TextInputLayout tilUrl, tilTitle, tilExtraPin, tilRemark;
    private ChipGroup chipGroupCategory;
    private ProgressBar progressStrength;
    private TextView tvStrengthLabel;
    private MaterialButton btnSave, btnDelete, btnGeneratePassword, btnChooseApp;
    private DatabaseHelper dbHelper;
    private int entryId = -1;
    private String selectedCategory = "Website";

    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_add_edit);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupToolbar();
        setupCategoryChips();
        setupPasswordStrengthMeter();
        setupGeneratePassword();
        setupChooseAppButton();
        setupSaveButton();
        setupDeleteButton();

        entryId = getIntent().getIntExtra("ENTRY_ID", -1);
        if (entryId != -1) {
            loadEntry(entryId);
        }
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etUrl = findViewById(R.id.etUrl);
        etExtraPin = findViewById(R.id.etExtraPin);
        etRemark = findViewById(R.id.etRemark);
        tilUrl = findViewById(R.id.tilUrl);
        tilTitle = findViewById(R.id.tilTitle);
        tilExtraPin = findViewById(R.id.tilExtraPin);
        tilRemark = findViewById(R.id.tilRemark);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        progressStrength = findViewById(R.id.progressStrength);
        tvStrengthLabel = findViewById(R.id.tvStrengthLabel);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnGeneratePassword = findViewById(R.id.btnGeneratePassword);
        btnChooseApp = findViewById(R.id.btnChooseApp);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (entryId != -1) {
            toolbar.setTitle(R.string.edit_entry);
        }
    }

    private void setupCategoryChips() {
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                selectedCategory = chip.getText().toString();
            }
            updateUrlVisibility();
        });

        // Default selection
        ((Chip) findViewById(R.id.chipWebsite)).setChecked(true);
    }

    // Show or hide URL field based on selected category
    private void updateUrlVisibility() {
        switch (selectedCategory) {
            case "Website":
                tilUrl.setVisibility(View.VISIBLE);
                tilUrl.setHint(getString(R.string.url_hint_website));
                btnChooseApp.setVisibility(View.GONE);
                break;
            case "App":
                tilUrl.setVisibility(View.VISIBLE);
                tilUrl.setHint(getString(R.string.url_hint_app));
                btnChooseApp.setVisibility(View.VISIBLE);
                break;
            case "Bank PIN":
            case "Custom":
                tilUrl.setVisibility(View.GONE);
                etUrl.setText("");
                btnChooseApp.setVisibility(View.GONE);
                break;
            default:
                tilUrl.setVisibility(View.VISIBLE);
                tilUrl.setHint(getString(R.string.url));
                btnChooseApp.setVisibility(View.GONE);
                break;
        }
    }

    private void setupPasswordStrengthMeter() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
            }
        });
    }

    private void updatePasswordStrength(String password) {
        int strength = calculateStrength(password);
        progressStrength.setProgress(strength);

        if (strength <= 33) {
            progressStrength.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.strength_weak)));
            tvStrengthLabel.setText(R.string.password_strength_weak);
            tvStrengthLabel.setTextColor(
                    ContextCompat.getColor(this, R.color.strength_weak));
        } else if (strength <= 66) {
            progressStrength.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.strength_medium)));
            tvStrengthLabel.setText(R.string.password_strength_medium);
            tvStrengthLabel.setTextColor(
                    ContextCompat.getColor(this, R.color.strength_medium));
        } else {
            progressStrength.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.strength_strong)));
            tvStrengthLabel.setText(R.string.password_strength_strong);
            tvStrengthLabel.setTextColor(
                    ContextCompat.getColor(this, R.color.strength_strong));
        }
    }

    private int calculateStrength(String password) {
        if (password.isEmpty()) return 0;

        int score = 0;

        // Length scoring
        if (password.length() >= 4) score += 15;
        if (password.length() >= 8) score += 15;
        if (password.length() >= 12) score += 15;
        if (password.length() >= 16) score += 10;

        // Character variety
        if (password.matches(".*[a-z].*")) score += 10;
        if (password.matches(".*[A-Z].*")) score += 10;
        if (password.matches(".*\\d.*")) score += 10;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*")) score += 15;

        return Math.min(score, 100);
    }

    private void setupGeneratePassword() {
        btnGeneratePassword.setOnClickListener(v -> {
            String generated = generateRandomPassword(16);
            etPassword.setText(generated);
        });
    }

    private String generateRandomPassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(PASSWORD_CHARS.length());
            sb.append(PASSWORD_CHARS.charAt(index));
        }
        return sb.toString();
    }

    // Open app picker dialog on button click
    private void setupChooseAppButton() {
        btnChooseApp.setOnClickListener(v -> showAppPickerDialog());
    }

    // Show dialog listing all installed launchable apps
    private void showAppPickerDialog() {
        PackageManager pm = getPackageManager();

        // Query all apps with a launcher activity
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        // Sort alphabetically by app label
        apps.sort((a, b) -> {
            String labelA = a.loadLabel(pm).toString();
            String labelB = b.loadLabel(pm).toString();
            return labelA.compareToIgnoreCase(labelB);
        });

        // Parallel lists for name, package, and icon
        List<String> appNames = new ArrayList<>();
        List<String> packageNames = new ArrayList<>();
        List<Drawable> appIcons = new ArrayList<>();

        for (ResolveInfo info : apps) {
            appNames.add(info.loadLabel(pm).toString());
            packageNames.add(info.activityInfo.packageName);
            appIcons.add(info.loadIcon(pm));
        }

        // Custom adapter showing app icon and name per row
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.select_dialog_item,
                android.R.id.text1, appNames) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);

                // Scale icon to 48dp
                Drawable icon = appIcons.get(position);
                int size = (int) (48 * getResources().getDisplayMetrics().density);
                icon.setBounds(0, 0, size, size);

                tv.setCompoundDrawablesRelative(icon, null, null, null);
                tv.setCompoundDrawablePadding(
                        (int) (16 * getResources().getDisplayMetrics().density));
                return view;
            }
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_app_title)
                .setAdapter(adapter, (dialog, which) -> {
                    // Set URL to selected package name
                    etUrl.setText(packageNames.get(which));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            String title = getText(etTitle);
            String username = getText(etUsername);
            String password = getText(etPassword);
            String url = getText(etUrl);
            String extraPin = getText(etExtraPin);
            String remark = getText(etRemark);

            if (title.isEmpty()) {
                etTitle.setError(getString(R.string.title_required));
                etTitle.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError(getString(R.string.password_required));
                etPassword.requestFocus();
                return;
            }

            boolean success;
            if (entryId == -1) {
                success = dbHelper.insertData(title, url, username,
                        password, selectedCategory, extraPin, remark);
            } else {
                success = dbHelper.updateData(entryId, title, url, username,
                        password, selectedCategory, extraPin, remark);
            }

            if (success) {
                Toast.makeText(this, R.string.entry_saved,
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void setupDeleteButton() {
        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm_delete_title)
                        .setMessage(R.string.confirm_delete)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            if (dbHelper.deleteData(entryId)) {
                                Toast.makeText(this, R.string.entry_deleted,
                                        Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show()
        );
    }

    private void loadEntry(int id) {
        PasswordEntry entry = dbHelper.getEntryById(id);
        if (entry == null) return;

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.edit_entry);

        etTitle.setText(entry.getTitle());
        etUsername.setText(entry.getUsername());
        etPassword.setText(entry.getPassword());
        etUrl.setText(entry.getUrl());
        etExtraPin.setText(entry.getExtraPin());
        etRemark.setText(entry.getRemark());

        if (entry.getCategory() != null) {
            selectedCategory = entry.getCategory();
            switch (entry.getCategory()) {
                case "Website":
                    ((Chip) findViewById(R.id.chipWebsite)).setChecked(true);
                    break;
                case "App":
                    ((Chip) findViewById(R.id.chipApp)).setChecked(true);
                    break;
                case "Bank PIN":
                    ((Chip) findViewById(R.id.chipBankPin)).setChecked(true);
                    break;
                case "Custom":
                    ((Chip) findViewById(R.id.chipCustom)).setChecked(true);
                    break;
            }
        }

        btnDelete.setVisibility(View.VISIBLE);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ?
                editText.getText().toString().trim() : "";
    }
}
