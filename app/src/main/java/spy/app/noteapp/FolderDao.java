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
    default void delete(Folder folder) {
        if (folder.getId() != 1) { // Не удаляем папку "Все"
            deleteInternal(folder);
        }
    }

    @Delete
    void deleteInternal(Folder folder); // Внутренний метод для реального удаления

    @Insert
    void insert(Folder folder);

    @Query("SELECT * FROM Folder")
    List<Folder> getAllFolders();
}
