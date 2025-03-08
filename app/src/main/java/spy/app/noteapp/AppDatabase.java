package spy.app.noteapp;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Note.class, Folder.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao noteDao();
    public abstract FolderDao folderDao();

    private static volatile AppDatabase INSTANCE;
    private static final String TAG = "AppDatabase";

    // Миграция с версии 1 на версию 2
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Добавляем поле notesCount в таблицу Folder с значением по умолчанию 0
            database.execSQL("ALTER TABLE Folder ADD COLUMN notesCount INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "note_database")
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d(TAG, "Database created, starting initial population");
                                    ExecutorService executor = Executors.newSingleThreadExecutor();
                                    executor.execute(() -> {
                                        try {
                                            populateInitialData(INSTANCE);
                                            Log.d(TAG, "Initial population completed");
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error during initial population", e);
                                        }
                                        executor.shutdown();
                                    });
                                }
                            })
                            .addMigrations(MIGRATION_1_2) // Добавляем миграцию
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void populateInitialData(AppDatabase db) {
        Folder defaultFolder = new Folder();
        defaultFolder.setId(1); // Фиксированный ID для "Все"
        defaultFolder.setName("Все");
        defaultFolder.setColor(Color.parseColor("#E0E0E0")); // Нейтральный цвет
        defaultFolder.setNotesCount(0); // Устанавливаем начальное значение
        db.folderDao().insert(defaultFolder);
    }
}
