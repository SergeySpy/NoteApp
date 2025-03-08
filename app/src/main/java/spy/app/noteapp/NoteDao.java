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

    @Query("SELECT * FROM Note WHERE title LIKE :query OR content LIKE :query")
    List<Note> searchNotes(String query);

    @Query("SELECT * FROM Note ORDER BY id ASC")
    List<Note> getAllNotesSortedByCreatedAsc();

    @Query("SELECT * FROM Note ORDER BY id DESC")
    List<Note> getAllNotesSortedByCreatedDesc();

    @Query("SELECT * FROM Note ORDER BY lastEdited ASC")
    List<Note> getAllNotesSortedByModifiedAsc();

    @Query("SELECT * FROM Note ORDER BY lastEdited DESC")
    List<Note> getAllNotesSortedByModifiedDesc();

    @Query("SELECT * FROM Note ORDER BY title ASC")
    List<Note> getAllNotesSortedByTitleAsc();

    @Query("SELECT * FROM Note ORDER BY title DESC")
    List<Note> getAllNotesSortedByTitleDesc();
}
