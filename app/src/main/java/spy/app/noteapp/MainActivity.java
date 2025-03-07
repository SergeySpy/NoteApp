package spy.app.noteapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    //private RecyclerView notesRecyclerView, foldersRecyclerView;
    private ViewPager2 viewPager;
    private FloatingActionButton fab;
    private TabLayout tabLayout;
    private ImageButton menuButton;
    NotesAdapter notesAdapter;
    FoldersAdapter foldersAdapter;
    private static final int TAB_NOTES = 0;
    private static final int TAB_FOLDERS = 1;
    private int currentTab = TAB_NOTES;
    private ExecutorService executorService; // Для фоновых задач
    private Handler mainHandler; // Для обновления UI
    private AppDatabase db; // База данных

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

            // Получение обновлённого списка папок
            List<Folder> folders = db.folderDao().getAllFolders();

            // Обновление UI в главном потоке
            mainHandler.post(() -> foldersAdapter.setFolders(folders));
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

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(); // Обновляем заметки при возвращении в активность
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // Очищаем пул потоков
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
