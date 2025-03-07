package spy.app.noteapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> notes; // Используйте ваш собственный класс Note
    private Context context;

    public NotesAdapter(List<Note> notes, Context context) {
        this.notes = notes;
        this.context = context;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
        notifyDataSetChanged(); // Уведомляем адаптер об изменении данных
    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = notes.get(position); // Используйте ваш собственный класс Note
        holder.title.setText(note.getTitle());
        holder.content.setText(note.getContent());

        // Обработка клика для открытия заметки
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NoteActivity.class);
            intent.putExtra("note_id", note.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content;

        public NoteViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.noteTitle);
            content = itemView.findViewById(R.id.noteContent);
        }
    }
}
