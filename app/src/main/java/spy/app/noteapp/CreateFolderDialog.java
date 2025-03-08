package spy.app.noteapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;

public class CreateFolderDialog extends DialogFragment {
    public interface CreateFolderListener {
        void onFolderCreated(String name, int color);
    }

    private CreateFolderListener listener;

    public void setListener(CreateFolderListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_folder, null);
        EditText folderName = view.findViewById(R.id.folderName);
        final int[] selectedColor = {ContextCompat.getColor(getContext(), android.R.color.holo_blue_light)};

        view.findViewById(R.id.redButton).setOnClickListener(v -> selectedColor[0] = ContextCompat.getColor(getContext(), android.R.color.holo_red_light));
        view.findViewById(R.id.greenButton).setOnClickListener(v -> selectedColor[0] = ContextCompat.getColor(getContext(), android.R.color.holo_green_light));
        view.findViewById(R.id.blueButton).setOnClickListener(v -> selectedColor[0] = ContextCompat.getColor(getContext(), android.R.color.holo_blue_light));

        return new AlertDialog.Builder(getContext())
                .setTitle("Create Folder")
                .setView(view)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (listener != null) {
                        listener.onFolderCreated(folderName.getText().toString(), selectedColor[0]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }
}