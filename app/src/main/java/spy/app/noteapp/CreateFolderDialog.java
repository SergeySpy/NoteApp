package spy.app.noteapp;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

public class CreateFolderDialog extends DialogFragment {

    private CreateFolderListener listener;

    public interface CreateFolderListener {
        void onFolderCreated(String name, int color);
    }

    public void setListener(CreateFolderListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_create_folder, null);

        EditText folderName = view.findViewById(R.id.folderName);
        Button redButton = view.findViewById(R.id.redButton);
        Button greenButton = view.findViewById(R.id.greenButton);
        Button blueButton = view.findViewById(R.id.blueButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        Button okButton = view.findViewById(R.id.okButton);

        final int[] selectedColor = {ContextCompat.getColor(getContext(), R.color.red)}; // По умолчанию красный

        redButton.setOnClickListener(v -> selectedColor[0] = ContextCompat.getColor(getContext(), R.color.red));
        greenButton.setOnClickListener(v -> selectedColor[0] = ContextCompat.getColor(getContext(), R.color.green));
        blueButton.setOnClickListener(v -> selectedColor[0] = ContextCompat.getColor(getContext(), R.color.blue));

        cancelButton.setOnClickListener(v -> dismiss());

        okButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFolderCreated(folderName.getText().toString(), selectedColor[0]);
            }
            dismiss();
        });

        builder.setView(view);
        return builder.create();
    }
}