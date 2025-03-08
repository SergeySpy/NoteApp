package spy.app.noteapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM Note WHERE id = :noteId")
    Note getNoteById(int noteId);

    @Query("SELECT * FROM Note WHERE folderId = :folderId")
    List<Note> getNotesByFolderId(int folderId);

    @Query("SELECT * FROM Note")
    List<Note> getAllNotes();

    @Query("SELECT COUNT(*) FROM Note WHERE folderId = :folderId")
    int getNotesCountByFolder(int folderId);

    @Query("SELECT * FROM Note WHERE folderId = :folderId")
    List<Note> getNotesByFolder(int folderId);
}
