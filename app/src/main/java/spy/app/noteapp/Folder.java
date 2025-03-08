package spy.app.noteapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Folder {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private int color;
    private int notesCount; // Новое поле

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getNotesCount() {
        return notesCount;
    }

    public void setNotesCount(int notesCount) {
        this.notesCount = notesCount;
    }
}