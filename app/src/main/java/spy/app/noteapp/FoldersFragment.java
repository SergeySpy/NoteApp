package spy.app.noteapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FoldersFragment extends Fragment {
    private RecyclerView recyclerView;
    private FoldersAdapter foldersAdapter;

    public FoldersFragment() {
        // Пустой конструктор обязателен
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Получаем адаптер из MainActivity
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            foldersAdapter = activity.getFoldersAdapter();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folders, container, false);
        recyclerView = view.findViewById(R.id.foldersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (foldersAdapter != null) {
            recyclerView.setAdapter(foldersAdapter);
        }
        return view;
    }
}