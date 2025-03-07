package spy.app.noteapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.EditText;
import androidx.appcompat.widget.Toolbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteActivity extends AppCompatActivity {
    private EditText noteTitle, noteContent;
    private Toolbar toolbar;
    private ExecutorService executorService;
    private AppDatabase db;
    private Note currentNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        // Инициализация компонентов
        noteTitle = findViewById(R.id.noteTitle);
        noteContent = findViewById(R.id.noteContent);
        toolbar = findViewById(R.id.toolbar);

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Проверяем, редактируем ли существующую заметку
        int noteId = getIntent().getIntExtra("note_id", -1);
        if (noteId != -1) {
            loadNote(noteId);
        }

        // Обработка нажатия на кнопку сохранения
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                // Сохранить заметку
                saveNote();
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_note, menu);
        return true;
    }

    private void loadNote(int noteId) {
        executorService.execute(() -> {
            currentNote = db.noteDao().getNoteById(noteId);
            if (currentNote != null) {
                runOnUiThread(() -> {
                    noteTitle.setText(currentNote.getTitle());
                    noteContent.setText(currentNote.getContent());
                });
            }
        });
    }

    private void saveNote() {
        executorService.execute(() -> {
                    if (currentNote == null) {
                        // Новая заметка
                        currentNote = new Note();
                        currentNote.setTitle(noteTitle.getText().toString());
                        currentNote.setContent(noteContent.getText().toString());
                        currentNote.setFolderId(0); // Пока без папки
                        db.noteDao().insert(currentNote); // insert обновит, если запись существует
                    } else {
                        // Существующая заметка
                        currentNote.setTitle(noteTitle.getText().toString());
                        currentNote.setContent(noteContent.getText().toString());
                        db.noteDao().update(currentNote); // Используем update вместо insert
                    }
            runOnUiThread(this::finish); // Закрываем активити после сохранения
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}

