package com.example.mainproj.lists;

import android.os.Bundle;
import android.view.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mainproj.R;
import com.example.mainproj.shared_lists_adapter_placeholder.ListAdapter;
import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;

public class FragmentWhitelist extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);
        RecyclerView rv = view.findViewById(R.id.recyclerViewList);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new ListAdapter(PlaceholderContent.ITEMS));

        return view;
    }
}
