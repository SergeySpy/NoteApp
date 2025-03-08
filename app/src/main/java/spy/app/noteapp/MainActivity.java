package spy.app.noteapp;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements CreateFolderDialog.CreateFolderListener {
    private ViewPager2 viewPager;
    private FloatingActionButton fab;
    private Button notesTab;
    private Button foldersTab;
    private ImageButton menuButton;
    private ImageButton filterButton;
    private ImageButton backButton;
    private EditText searchInput;
    private TextView folderTitle;
    private CoordinatorLayout mainLayout;
    private LinearLayout folderHeader;
    private LinearLayout tabsContainer;
    private View toggleIndicator;
    private NotesAdapter notesAdapter;
    private FoldersAdapter foldersAdapter;
    private static final int TAB_NOTES = 0;
    private static final int TAB_FOLDERS = 1;
    private int currentTab = TAB_NOTES;
    private int currentFolderId = 1; // По умолчанию "Все"
    private ExecutorService executorService;
    private Handler mainHandler;
    private AppDatabase db;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        fab = findViewById(R.id.fab);
        notesTab = findViewById(R.id.notesTab);
        foldersTab = findViewById(R.id.foldersTab);
        menuButton = findViewById(R.id.menuButton);
        filterButton = findViewById(R.id.filterButton);
        backButton = findViewById(R.id.backButton);
        searchInput = findViewById(R.id.searchInput);
        folderTitle = findViewById(R.id.folderTitle);
        mainLayout = findViewById(R.id.mainLayout);
        folderHeader = findViewById(R.id.folderHeader);
        tabsContainer = findViewById(R.id.tabsContainer);
        toggleIndicator = findViewById(R.id.toggleIndicator);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Отключаем стандартный заголовок

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        notesAdapter = new NotesAdapter(new ArrayList<>(), this);
        foldersAdapter = new FoldersAdapter(new ArrayList<>(), executorService);

        setupViewPager();
        initializeData();
        setupSearchAndFilter();

        fab.setOnClickListener(v -> {
            if (currentTab == TAB_FOLDERS) {
                CreateFolderDialog dialog = new CreateFolderDialog();
                dialog.setListener(this);
                dialog.show(getSupportFragmentManager(), "createFolderDialog");
            } else {
                createNewNote();
            }
        });

        menuButton.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Menu clicked", android.widget.Toast.LENGTH_SHORT).show();
        });

        backButton.setOnClickListener(v -> {
            resetToolbar();
            viewPager.setCurrentItem(TAB_FOLDERS);
        });

        notesTab.setOnClickListener(v -> {
            if (currentTab != TAB_NOTES) {
                viewPager.setCurrentItem(TAB_NOTES);
                animateToggle(TAB_NOTES);
            }
        });

        foldersTab.setOnClickListener(v -> {
            if (currentTab != TAB_FOLDERS) {
                viewPager.setCurrentItem(TAB_FOLDERS);
                animateToggle(TAB_FOLDERS);
            }
        });

        updateTabSelection(TAB_NOTES); // Изначально выбрана вкладка "Notes"
        animateToggle(TAB_NOTES); // Устанавливаем начальное положение индикатора без анимации

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewPager.getCurrentItem() == TAB_NOTES) {
                    if (currentFolderId != 1) {
                        resetToolbar();
                        viewPager.setCurrentItem(TAB_FOLDERS);
                    } else {
                        viewPager.setCurrentItem(TAB_FOLDERS);
                    }
                } else if (viewPager.getCurrentItem() == TAB_FOLDERS) {
                    showNotesForFolder(1);
                } else {
                    finish();
                }
            }
        });
    }

    private void setupViewPager() {
        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentTab = position;
                animateToggle(position);
                updateToolbarButtonsVisibility();
            }
        });
    }

    private void animateToggle(int selectedTab) {
        int startX = selectedTab == TAB_NOTES ? foldersTab.getWidth() : 0;
        int endX = selectedTab == TAB_NOTES ? 0 : foldersTab.getWidth();
        ValueAnimator animator = ValueAnimator.ofInt(startX, endX);
        animator.setDuration(200); // Ускоряем до 200 мс
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            toggleIndicator.setTranslationX(value);
        });
        animator.start();
        updateTabSelection(selectedTab);
    }

    private void updateTabSelection(int selectedTab) {
        notesTab.setSelected(selectedTab == TAB_NOTES);
        foldersTab.setSelected(selectedTab == TAB_FOLDERS);
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
        Intent intent = new Intent(this, NoteActivity.class);
        startActivity(intent);
    }

    @Override
    public void onFolderCreated(String name, int color) {
        executorService.execute(() -> {
            Folder folder = new Folder();
            folder.setName(name);
            folder.setColor(color);
            db.folderDao().insert(folder);
            refreshFolders();
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
        currentFolderId = folderId;
        executorService.execute(() -> {
            List<Note> folderNotes = (folderId == 1) ? db.noteDao().getAllNotes() : db.noteDao().getNotesByFolder(folderId);
            Folder folder = db.folderDao().getFolderById(folderId);
            mainHandler.post(() -> {
                notesAdapter.setNotes(folderNotes);
                viewPager.setCurrentItem(TAB_NOTES);
                refreshFolders();
                if (folderId != 1) {
                    tabsContainer.setVisibility(View.GONE);
                    folderHeader.setVisibility(View.VISIBLE);
                    folderTitle.setText(folder.getName());
                    mainLayout.setBackgroundColor(getPastelColor(folder.getColor()));
                } else {
                    resetToolbar();
                }
            });
        });
    }

    private void refreshFolders() {
        executorService.execute(() -> {
            List<Folder> folders = db.folderDao().getAllFolders();
            for (Folder folder : folders) {
                if (folder.getId() == 1) {
                    folder.setNotesCount(db.noteDao().getAllNotes().size());
                } else {
                    folder.setNotesCount(db.noteDao().getNotesCountByFolder(folder.getId()));
                }
            }
            mainHandler.post(() -> foldersAdapter.setFolders(folders));
        });
    }

    private void setupSearchAndFilter() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                executorService.execute(() -> {
                    List<Note> filteredNotes = db.noteDao().searchNotes("%" + query + "%");
                    mainHandler.post(() -> notesAdapter.setNotes(filteredNotes));
                });
                if (s.length() > 0) {
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_clear_modern, null);
                    int size = dpToPx(8); // Размер 8dp
                    drawable.setBounds(0, 0, size, size);
                    searchInput.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
                    searchInput.setCompoundDrawablePadding(dpToPx(8)); // Увеличиваем отступ до 8dp
                    Log.d(TAG, "Clear icon size: " + size + "px (" + 8 + "dp)");
                } else {
                    searchInput.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawable = searchInput.getCompoundDrawables()[2]; // drawableEnd
                if (drawable != null && event.getRawX() >= (searchInput.getRight() - drawable.getBounds().width() - dpToPx(8))) {
                    searchInput.setText("");
                    return true;
                }
            }
            return false;
        });

        filterButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sort Notes")
                    .setItems(new String[]{
                            "Date Created (Asc)", "Date Created (Desc)",
                            "Date Modified (Asc)", "Date Modified (Desc)",
                            "Title (Asc)", "Title (Desc)"
                    }, (dialog, which) -> {
                        executorService.execute(() -> {
                            List<Note> sortedNotes;
                            switch (which) {
                                case 0: sortedNotes = db.noteDao().getAllNotesSortedByCreatedAsc(); break;
                                case 1: sortedNotes = db.noteDao().getAllNotesSortedByCreatedDesc(); break;
                                case 2: sortedNotes = db.noteDao().getAllNotesSortedByModifiedAsc(); break;
                                case 3: sortedNotes = db.noteDao().getAllNotesSortedByModifiedDesc(); break;
                                case 4: sortedNotes = db.noteDao().getAllNotesSortedByTitleAsc(); break;
                                case 5: sortedNotes = db.noteDao().getAllNotesSortedByTitleDesc(); break;
                                default: sortedNotes = db.noteDao().getAllNotes();
                            }
                            mainHandler.post(() -> notesAdapter.setNotes(sortedNotes));
                        });
                    })
                    .show();
        });
    }

    // Вспомогательный метод для конверсии dp в px
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void updateToolbarButtonsVisibility() {
        if (currentTab == TAB_FOLDERS) {
            searchInput.setVisibility(View.GONE);
            filterButton.setVisibility(View.GONE);
        } else {
            searchInput.setVisibility(View.VISIBLE);
            filterButton.setVisibility(View.VISIBLE);
        }
    }

    private void resetToolbar() {
        tabsContainer.setVisibility(View.VISIBLE);
        folderHeader.setVisibility(View.GONE);
        mainLayout.setBackgroundColor(Color.WHITE);
        updateToolbarButtonsVisibility();
    }

    private int getPastelColor(int color) {
        int alpha = 0xFF;
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        red = (red + 255) / 2;
        green = (green + 255) / 2;
        blue = (blue + 255) / 2;
        return Color.argb(alpha, red, green, blue);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
        refreshFolders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    public FoldersAdapter getFoldersAdapter() {
        return foldersAdapter;
    }

    public NotesAdapter getNotesAdapter() {
        return notesAdapter;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public int getCurrentFolderId() {
        return currentFolderId;
    }

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