package spy.app.noteapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FolderDao {

    @Update
    void update(Folder folder);

    @Delete
    void delete(Folder folder);

    @Insert
    void insert(Folder folder);

    @Query("SELECT * FROM Folder")
    List<Folder> getAllFolders();
}
