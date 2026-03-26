package my.utar.uccd3223.uccd3223_individual_assignment_password_manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "secure_vault.db";
    private static final int DATABASE_VERSION = 2;
    private static final String MASTER_KEY = "SecureVault@2024#MasterKey!";

    public static final String TABLE_PASSWORDS = "passwords_table";
    public static final String COL_ID = "ID";
    public static final String COL_TITLE = "TITLE";
    public static final String COL_URL = "URL";
    public static final String COL_USERNAME = "USERNAME";
    public static final String COL_PASSWORD = "PASSWORD";
    public static final String COL_CATEGORY = "CATEGORY";
    public static final String COL_EXTRA_PIN = "EXTRA_PIN";
    public static final String COL_REMARK = "REMARK";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, MASTER_KEY, null, DATABASE_VERSION,
                0, null, null, false);
        System.loadLibrary("sqlcipher");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_PASSWORDS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT NOT NULL, " +
                COL_URL + " TEXT, " +
                COL_USERNAME + " TEXT, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_CATEGORY + " TEXT, " +
                COL_EXTRA_PIN + " TEXT, " +
                COL_REMARK + " TEXT)";
        db.execSQL(createTable);
        insertSeedData(db);
    }

    private void insertSeedData(SQLiteDatabase db) {
        // Gmail - Website entry
        ContentValues v1 = new ContentValues();
        v1.put(COL_TITLE, "Gmail");
        v1.put(COL_URL, "https://mail.google.com");
        v1.put(COL_USERNAME, "johndoe@gmail.com");
        v1.put(COL_PASSWORD, "G!mail#Secure2024");
        v1.put(COL_CATEGORY, "Website");
        v1.put(COL_EXTRA_PIN, "");
        v1.put(COL_REMARK, "Personal email account");
        db.insert(TABLE_PASSWORDS, null, v1);

        // Instagram - App entry
        ContentValues v2 = new ContentValues();
        v2.put(COL_TITLE, "Instagram");
        v2.put(COL_URL, "com.instagram.android");
        v2.put(COL_USERNAME, "john_doe_99");
        v2.put(COL_PASSWORD, "Insta@Pr1vate!");
        v2.put(COL_CATEGORY, "App");
        v2.put(COL_EXTRA_PIN, "");
        v2.put(COL_REMARK, "Personal social media");
        db.insert(TABLE_PASSWORDS, null, v2);

        // Maybank - Bank PIN entry
        ContentValues v3 = new ContentValues();
        v3.put(COL_TITLE, "Maybank Debit Card");
        v3.put(COL_URL, "");
        v3.put(COL_USERNAME, "");
        v3.put(COL_PASSWORD, "482916");
        v3.put(COL_CATEGORY, "Bank PIN");
        v3.put(COL_EXTRA_PIN, "7731");
        v3.put(COL_REMARK, "Card ending 5542, PIN + CVV stored");
        db.insert(TABLE_PASSWORDS, null, v3);

        // Netflix - Website entry
        ContentValues v4 = new ContentValues();
        v4.put(COL_TITLE, "Netflix");
        v4.put(COL_URL, "https://www.netflix.com");
        v4.put(COL_USERNAME, "johndoe@outlook.com");
        v4.put(COL_PASSWORD, "N3tflix$tream!");
        v4.put(COL_CATEGORY, "Website");
        v4.put(COL_EXTRA_PIN, "");
        v4.put(COL_REMARK, "Family plan subscription");
        db.insert(TABLE_PASSWORDS, null, v4);

        // Home WiFi - Custom entry
        ContentValues v5 = new ContentValues();
        v5.put(COL_TITLE, "Home WiFi");
        v5.put(COL_URL, "");
        v5.put(COL_USERNAME, "MyHome_5G");
        v5.put(COL_PASSWORD, "W1f1P@ssw0rd#Home");
        v5.put(COL_CATEGORY, "Custom");
        v5.put(COL_EXTRA_PIN, "");
        v5.put(COL_REMARK, "Router admin: 192.168.0.1 / admin");
        db.insert(TABLE_PASSWORDS, null, v5);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_PASSWORDS +
                    " ADD COLUMN " + COL_REMARK + " TEXT");
        }
    }

    public boolean insertData(String title, String url, String username,
                              String password, String category, String extraPin,
                              String remark) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_URL, url);
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        values.put(COL_CATEGORY, category);
        values.put(COL_EXTRA_PIN, extraPin);
        values.put(COL_REMARK, remark);
        long result = db.insert(TABLE_PASSWORDS, null, values);
        return result != -1;
    }

    public boolean updateData(int id, String title, String url, String username,
                              String password, String category, String extraPin,
                              String remark) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_URL, url);
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        values.put(COL_CATEGORY, category);
        values.put(COL_EXTRA_PIN, extraPin);
        values.put(COL_REMARK, remark);
        int result = db.update(TABLE_PASSWORDS, values,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    public boolean deleteData(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_PASSWORDS,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    public List<PasswordEntry> getAllData() {
        List<PasswordEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PASSWORDS +
                " ORDER BY " + COL_TITLE + " ASC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                PasswordEntry entry = cursorToEntry(cursor);
                list.add(entry);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<PasswordEntry> searchData(String query) {
        List<PasswordEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String searchQuery = "SELECT * FROM " + TABLE_PASSWORDS +
                " WHERE " + COL_TITLE + " LIKE ? OR " +
                COL_USERNAME + " LIKE ? OR " +
                COL_URL + " LIKE ? OR " +
                COL_CATEGORY + " LIKE ? OR " +
                COL_REMARK + " LIKE ?" +
                " ORDER BY " + COL_TITLE + " ASC";
        String wildcard = "%" + query + "%";
        Cursor cursor = db.rawQuery(searchQuery,
                new String[]{wildcard, wildcard, wildcard, wildcard, wildcard});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                PasswordEntry entry = cursorToEntry(cursor);
                list.add(entry);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public PasswordEntry getEntryById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PASSWORDS +
                " WHERE " + COL_ID + " = ?", new String[]{String.valueOf(id)});
        if (cursor != null && cursor.moveToFirst()) {
            PasswordEntry entry = cursorToEntry(cursor);
            cursor.close();
            return entry;
        }
        return null;
    }

    private PasswordEntry cursorToEntry(Cursor cursor) {
        PasswordEntry entry = new PasswordEntry();
        entry.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
        entry.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
        entry.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(COL_URL)));
        entry.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)));
        entry.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)));
        entry.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
        entry.setExtraPin(cursor.getString(cursor.getColumnIndexOrThrow(COL_EXTRA_PIN)));
        entry.setRemark(cursor.getString(cursor.getColumnIndexOrThrow(COL_REMARK)));
        return entry;
    }
}
