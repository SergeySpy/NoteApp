package spy.app.noteapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class NoteActivity extends AppCompatActivity {
    private EditText noteTitle;
    private EditText noteContent;
    private TextView statsView;
    private Spinner folderSpinner;
    private Note currentNote;
    private AppDatabase db;
    private List<Folder> folders;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        // Инициализация компонентов
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        noteTitle = findViewById(R.id.noteTitle);
        noteContent = findViewById(R.id.noteContent);
        statsView = findViewById(R.id.statsView);
        folderSpinner = findViewById(R.id.folderSpinner);
        ImageButton saveButton = findViewById(R.id.saveButton);

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor(); // Инициализируем

        // Проверяем, редактируем ли существующую заметку
        int noteId = getIntent().getIntExtra("note_id", -1);
        loadFolders(noteId); // Передаём noteId для загрузки после папок

        noteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStats();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        saveButton.setOnClickListener(v -> saveNote());
    }

    private void loadNote(int noteId) {
        executorService.execute(() -> {
            currentNote = db.noteDao().getNoteById(noteId);
            runOnUiThread(() -> {
                noteTitle.setText(currentNote.getTitle());
                noteContent.setText(currentNote.getContent());
                // Устанавливаем текущую папку в Spinner
                if (folders != null) { // Проверяем, что папки уже загружены
                    int folderPosition = -1;
                    for (int i = 0; i < folders.size(); i++) {
                        if (folders.get(i).getId() == currentNote.getFolderId()) {
                            folderPosition = i;
                            break;
                        }
                    }
                    if (folderPosition != -1) {
                        folderSpinner.setSelection(folderPosition);
                    }
                }
                updateStats();
            });
        });
    }

    private void loadFolders(int noteId) {
        executorService.execute(() -> {
            folders = db.folderDao().getAllFolders();
            List<String> folderNames = folders.stream().map(Folder::getName).collect(Collectors.toList());
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, folderNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                folderSpinner.setAdapter(adapter);
                folderSpinner.setSelection(0); // По умолчанию "Все"
                if (noteId != -1) {
                    loadNote(noteId); // Загружаем заметку после папок
                }
            });
        });
    }

    private void saveNote() {
        executorService.execute(() -> {
            if (currentNote == null) {
                // Новая заметка
                currentNote = new Note();
                currentNote.setTitle(noteTitle.getText().toString());
                currentNote.setContent(noteContent.getText().toString());
                int selectedFolderPos = folderSpinner.getSelectedItemPosition();
                currentNote.setFolderId(folders.get(selectedFolderPos).getId()); // Пока без папки
                currentNote.setLastEdited(System.currentTimeMillis());
                db.noteDao().insert(currentNote); // insert обновит, если запись существует
            } else {
                // Существующая заметка
                currentNote.setTitle(noteTitle.getText().toString());
                currentNote.setContent(noteContent.getText().toString());
                int selectedFolderPos = folderSpinner.getSelectedItemPosition();
                currentNote.setFolderId(folders.get(selectedFolderPos).getId());
                currentNote.setLastEdited(System.currentTimeMillis());
                db.noteDao().update(currentNote); // Используем update вместо insert
            }
            runOnUiThread(this::finish); // Закрываем активити после сохранения
        });
    }

    private void updateStats() {
        String content = noteContent.getText().toString();
        int lines = content.split("\n").length;
        int chars = content.length();
        int words = content.split("\\s+").length;
        statsView.setText(String.format("Lines: %d, Chars: %d, Words: %d", lines, chars, words));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}

