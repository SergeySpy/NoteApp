package spy.app.noteapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements CreateFolderDialog.CreateFolderListener {

    private ViewPager2 viewPager;
    private FloatingActionButton fab;
    private TabLayout tabLayout;
    private ImageButton menuButton;
    private NotesAdapter notesAdapter;
    private FoldersAdapter foldersAdapter;
    private static final int TAB_NOTES = 0;
    private static final int TAB_FOLDERS = 1;
    private int currentTab = TAB_NOTES;
    private int currentFolderId = 1; // По умолчанию "Все"
    private ExecutorService executorService; // Для фоновых задач
    private Handler mainHandler; // Для обновления UI
    private AppDatabase db; // База данных
    private static final String TAG = "MainActivity"; // Добавляем TAG

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация компонентов
        viewPager = findViewById(R.id.viewPager);
        fab = findViewById(R.id.fab);
        tabLayout = findViewById(R.id.tabLayout);
        menuButton = findViewById(R.id.menuButton);

        // Инициализация базы данных
        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Установка адаптеров и LayoutManager
        notesAdapter = new NotesAdapter(new ArrayList<>(), this);
        foldersAdapter = new FoldersAdapter(new ArrayList<>(), executorService); // Передаём executorService

        // Инициализация потоков
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Загрузка начальных данных
        loadFolders();
        loadNotes();

        // Настройка ViewPager2
        viewPager.setAdapter(new ViewPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == TAB_NOTES ? "Notes" : "Folders");
            tab.setIcon(position == TAB_NOTES ? android.R.drawable.ic_menu_edit : android.R.drawable.ic_menu_manage);
        }).attach();

        // Настройка TabLayout
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Инициализация UI, но загрузка данных асинхронно
        setupViewPager();
        initializeData();

        // Обработка нажатия на кнопку создания
        fab.setOnClickListener(v -> {
            if (currentTab == TAB_FOLDERS) {
                CreateFolderDialog dialog = new CreateFolderDialog();
                dialog.setListener(this);
                dialog.show(getSupportFragmentManager(), "createFolderDialog");
            } else {
                // Логика для создания новой заметки
                createNewNote();
            }
        });

        // Обработка кнопки меню (заглушка)
        menuButton.setOnClickListener(v -> {
            // Здесь можно открыть меню или показать Toast для теста
            android.widget.Toast.makeText(this, "Menu clicked", android.widget.Toast.LENGTH_SHORT).show();
        });

        // Настраиваем обработку кнопки "Назад"
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewPager.getCurrentItem() == TAB_NOTES) {
                    viewPager.setCurrentItem(TAB_FOLDERS); // Возвращаемся к папкам
                } else if (viewPager.getCurrentItem() == TAB_FOLDERS) {
                    showNotesForFolder(1); // Сбрасываем на все заметки
                } else {
                    finish(); // Выход из приложения
                }
            }
        });
    }

    private void setupViewPager() {
        viewPager.setAdapter(new ViewPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == TAB_NOTES ? "Notes" : "Folders");
            tab.setIcon(position == TAB_NOTES ? android.R.drawable.ic_menu_edit : android.R.drawable.ic_menu_manage);
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void refreshFolders() {
        executorService.execute(() -> {
            List<Folder> folders = db.folderDao().getAllFolders();
            for (Folder folder : folders) {
                if (folder.getId() == 1) {
                    folder.setNotesCount(db.noteDao().getAllNotes().size()); // Все заметки для "Все"
                } else {
                    folder.setNotesCount(db.noteDao().getNotesCountByFolder(folder.getId()));
                }
            }
            mainHandler.post(() -> foldersAdapter.setFolders(folders));
        });
    }

    private void initializeData() {
        executorService.execute(() -> {
            try {
                List<Folder> folders = db.folderDao().getAllFolders();
                for (Folder folder : folders) {
                    if (folder.getId() == 1) {
                        folder.setNotesCount(db.noteDao().getAllNotes().size());
                    } else {
                        folder.setNotesCount(db.noteDao().getNotesCountByFolder(folder.getId()));
                    }
                }
                List<Note> notes = db.noteDao().getAllNotes();
                mainHandler.post(() -> {
                    foldersAdapter.setFolders(folders);
                    notesAdapter.setNotes(notes);
                    Log.d(TAG, "Data initialized successfully");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error initializing data", e);
            }
        });
    }

    private void createNewNote() {
        // Логика для создания новой заметки
        Intent intent = new Intent(this, NoteActivity.class);
        startActivity(intent);
    }

    @Override
    public void onFolderCreated(String name, int color) {
        executorService.execute(() -> {
            // Создание и вставка папки в фоновом потоке
            Folder folder = new Folder();
            folder.setName(name);
            folder.setColor(color);
            db.folderDao().insert(folder);
            refreshFolders(); // Обновляем после создания
        });
    }

    private void loadFolders() {
        executorService.execute(() -> {
            List<Folder> folders = db.folderDao().getAllFolders();
            mainHandler.post(() -> foldersAdapter.setFolders(folders));
        });
    }

    private void loadNotes() {
        executorService.execute(() -> {
            List<Note> notes = db.noteDao().getAllNotes();
            mainHandler.post(() -> notesAdapter.setNotes(notes));
        });
    }

    public void showNotesForFolder(int folderId) {
        currentFolderId = folderId; // Сохраняем текущую папку
        executorService.execute(() -> {
            List<Note> folderNotes = (folderId == 1) ? db.noteDao().getAllNotes() : db.noteDao().getNotesByFolder(folderId);
            mainHandler.post(() -> {
                notesAdapter.setNotes(folderNotes);
                viewPager.setCurrentItem(TAB_NOTES);
                refreshFolders(); // Обновляем после перехода
            });
        });
    }

    public int getCurrentFolderId() {
        return currentFolderId;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(); // Обновляем заметки при возвращении в активность
        refreshFolders(); // Обновляем при возвращении
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // Очищаем пул потоков
    }

    public NotesAdapter getNotesAdapter() { // Добавляем геттер
        return notesAdapter;
    }

    public FoldersAdapter getFoldersAdapter() {
        return foldersAdapter;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    // Адаптер для ViewPager2
    private class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == TAB_NOTES) {
                return new NotesFragment();
            } else {
                return new FoldersFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
