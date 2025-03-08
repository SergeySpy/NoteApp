package spy.app.noteapp;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class FoldersAdapter extends RecyclerView.Adapter<FoldersAdapter.FolderViewHolder> {
    private List<Folder> folders;
    private Context context;
    private ExecutorService executorService; // Добавляем ExecutorService
    private AppDatabase db;

    // Обновлённый конструктор с ExecutorService
    public FoldersAdapter(List<Folder> folders, ExecutorService executorService) {
        this.folders = folders;
        this.executorService = executorService;
    }

    public void setFolders(List<Folder> folders) {
        this.folders = folders;
        Collections.sort(this.folders, (f1, f2) -> {
            if (f1.getId() == 1) return -1; // "Все" всегда сверху
            if (f2.getId() == 1) return 1;
            return f1.getName().compareTo(f2.getName());
        });
        notifyDataSetChanged();
    }

    public void refreshFolders() {
        ExecutorService executor = getExecutorService();
        if (executor != null) {
            executor.execute(() -> {
                List<Folder> updatedFolders = db.folderDao().getAllFolders();
                for (Folder folder : updatedFolders) {
                    folder.setNotesCount(db.noteDao().getNotesCountByFolder(folder.getId()));
                }
                ((MainActivity) context).runOnUiThread(() -> setFolders(updatedFolders));
            });
        }
    }

    @Override
    public FolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        db = AppDatabase.getDatabase(context); // Инициализируем базу данных
        View view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FolderViewHolder holder, int position) {
        Folder folder = folders.get(position);
        holder.name.setText(folder.getName());
        holder.cardView.setCardBackgroundColor(getPastelColor(folder.getColor()));
        holder.notesCount.setText(String.valueOf(folder.getNotesCount())); // Используем кэшированное значение
        holder.itemView.setOnClickListener(v -> {
            ((MainActivity) context).showNotesForFolder(folder.getId());
        });

        // Долгий тап
        if (folder.getId() != 1) { // Отключаем долгий тап для "Все"
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Folder Options")
                        .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                            if (which == 0) {
                                editFolder(folder);
                            } else {
                                deleteFolder(folder);
                            }
                        })
                        .show();
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null); // Убираем обработчик
        }
    }

    private void editFolder(Folder folder) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_folder, null);
        EditText folderName = dialogView.findViewById(R.id.folderName);
        folderName.setText(folder.getName());

        final int[] selectedColor = {folder.getColor()}; // Сохраняем текущий цвет как начальный

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Edit Folder")
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    folder.setName(folderName.getText().toString());
                    folder.setColor(selectedColor[0]); // Применяем выбранный цвет
                    ExecutorService executor = getExecutorService();
                    if (executor != null) {
                        executor.execute(() -> {
                            db.folderDao().update(folder);
                            ((MainActivity) context).runOnUiThread(this::notifyDataSetChanged);
                        });
                    } else {
                        android.util.Log.e("FoldersAdapter", "ExecutorService is null in editFolder");
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        // Обработка выбора цвета
        dialogView.findViewById(R.id.redButton).setOnClickListener(v -> selectedColor[0] = ContextCompat.getColor(context, android.R.color.holo_red_light));
        dialogView.findViewById(R.id.greenButton).setOnClickListener(v -> selectedColor[0] = ContextCompat.getColor(context, android.R.color.holo_green_light));
        dialogView.findViewById(R.id.blueButton).setOnClickListener(v -> selectedColor[0] = ContextCompat.getColor(context, android.R.color.holo_blue_light));
    }

    private void deleteFolder(Folder folder) {
        ExecutorService executor = getExecutorService();
        if (executor != null && folder.getId() != 1) { // Не удаляем папку "Все"
            executor.execute(() -> {
                db.folderDao().delete(folder);
                folders.remove(folder);
                ((MainActivity) context).runOnUiThread(this::notifyDataSetChanged);
            });
        }
    }

    private ExecutorService getExecutorService() {
        // Если executorService уже задан, используем его
        if (executorService != null) {
            return executorService;
        }
        // Иначе пытаемся взять из MainActivity
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            return activity.getExecutorService(); // Добавим этот метод в MainActivity
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView notesCount; // Новое поле
        CardView cardView;

        public FolderViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.folderName);
            notesCount = itemView.findViewById(R.id.notesCount);
            cardView = itemView.findViewById(R.id.folderCardView);
        }
    }

    private int getPastelColor(int color) {
        // Преобразуем яркие цвета в пастельные
        int alpha = 0xFF;
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        // Делаем цвет светлее и менее насыщенным
        red = (red + 255) / 2;
        green = (green + 255) / 2;
        blue = (blue + 255) / 2;
        return Color.argb(alpha, red, green, blue);
    }
}