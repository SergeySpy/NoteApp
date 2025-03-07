package spy.app.noteapp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ColorPickerDialog extends BottomSheetDialogFragment {

    private ColorPickerListener listener;

    public interface ColorPickerListener {
        void onColorPicked(int color);
    }

    public void setListener(ColorPickerListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);

        Button redButton = view.findViewById(R.id.redButton);
        Button greenButton = view.findViewById(R.id.greenButton);
        Button blueButton = view.findViewById(R.id.blueButton);

        redButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorPicked(Color.RED);
            }
            dismiss();
        });

        greenButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorPicked(Color.GREEN);
            }
            dismiss();
        });

        blueButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorPicked(Color.BLUE);
            }
            dismiss();
        });

        return new Dialog(getContext()) {{
            setContentView(view);
        }};
    }
}
