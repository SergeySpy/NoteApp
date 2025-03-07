package spy.app.noteapp;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

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
        notifyDataSetChanged();
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
        holder.cardView.setCardBackgroundColor(folder.getColor());

        // Долгий тап
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
        if (executor != null) {
            executor.execute(() -> {
                db.folderDao().delete(folder);
                folders.remove(folder);
                ((MainActivity) context).runOnUiThread(this::notifyDataSetChanged);
            });
        } else {
            android.util.Log.e("FoldersAdapter", "ExecutorService is null in deleteFolder");
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
        CardView cardView;

        public FolderViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.folderName);
            cardView = itemView.findViewById(R.id.folderCardView);
        }
    }
}