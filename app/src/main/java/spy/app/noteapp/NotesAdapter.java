package spy.app.noteapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> notes;
    private Context context;
    private ExecutorService executorService;
    private AppDatabase db;

    public NotesAdapter(List<Note> notes, Context context) {
        this.notes = notes;
        this.context = context;
        this.executorService = ((MainActivity) context).getExecutorService();
        this.db = AppDatabase.getDatabase(context);
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
        notifyDataSetChanged();
    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.title.setText(note.getTitle());
        String content = note.getContent();
        if (content != null && !content.isEmpty()) {
            String[] lines = content.split("\n");
            holder.contentPreview.setText(lines[0]); // Первая строка
        } else {
            holder.contentPreview.setText(""); // Пусто, если контента нет
        }
        holder.lastEdited.setText("Last edited: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(note.getLastEdited())));
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NoteActivity.class);
            intent.putExtra("note_id", note.getId());
            context.startActivity(intent);
        });
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Note Options")
                    .setItems(new String[]{"Edit", "Delete", "Move"}, (dialog, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(context, NoteActivity.class);
                            intent.putExtra("note_id", note.getId());
                            context.startActivity(intent);
                        } else if (which == 1) {
                            executorService.execute(() -> {
                                db.noteDao().delete(note);
                                notes.remove(note);
                                ((MainActivity) context).runOnUiThread(() -> {
                                    notifyDataSetChanged();
                                    ((MainActivity) context).getFoldersAdapter().refreshFolders();
                                });
                            });
                        } else {
                            showMoveDialog(note);
                        }
                    })
                    .show();
            return true;
        });
    }

    private void showMoveDialog(Note note) {
        executorService.execute(() -> {
            List<Folder> folders = db.folderDao().getAllFolders();
            List<String> folderNames = folders.stream().map(Folder::getName).collect(Collectors.toList());
            ((MainActivity) context).runOnUiThread(() -> {
                new AlertDialog.Builder(context)
                        .setTitle("Move to Folder")
                        .setItems(folderNames.toArray(new String[0]), (dialog, which) -> {
                            int newFolderId = folders.get(which).getId();
                            note.setFolderId(newFolderId);
                            executorService.execute(() -> {
                                db.noteDao().update(note);
                                ((MainActivity) context).runOnUiThread(() -> {
                                    // Удаляем заметку из текущего списка, если она перемещена в другую папку
                                    int currentFolderId = ((MainActivity) context).getCurrentFolderId(); // Предполагаем метод
                                    if (currentFolderId != newFolderId && currentFolderId != 1) {
                                        notes.remove(note);
                                    }
                                    notifyDataSetChanged();
                                    ((MainActivity) context).getFoldersAdapter().refreshFolders(); // Обновляем папки
                                });
                            });
                        })
                        .show();
            });
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView contentPreview;
        TextView lastEdited;

        public NoteViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.noteTitle);
            contentPreview = itemView.findViewById(R.id.noteContentPreview);
            lastEdited = itemView.findViewById(R.id.noteLastEdited);
        }
    }
}