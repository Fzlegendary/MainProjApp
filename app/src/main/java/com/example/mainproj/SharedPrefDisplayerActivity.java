package com.example.mainproj;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SharedPrefDisplayerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TreeAdapter adapter;
    private List<TreeNode> rootNodes = new ArrayList<>();
    private List<TreeNode> visibleNodes = new ArrayList<>();
    private TextView tvPath;

    static class TreeNode {
        String name;
        String value;
        boolean isFolder;
        boolean isExpanded;
        int depth;
        List<TreeNode> children;

        TreeNode(String name, String value, boolean isFolder, int depth) {
            this.name = name;
            this.value = value;
            this.isFolder = isFolder;
            this.depth = depth;
            this.isExpanded = false;
            this.children = isFolder ? new ArrayList<TreeNode>() : null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_pref_displayer);

        tvPath = findViewById(R.id.tvPath);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TreeAdapter();
        recyclerView.setAdapter(adapter);

        loadAllPrefs();
    }

    private void loadAllPrefs() {
        new Thread(() -> {
            List<TreeNode> roots = new ArrayList<>();
            File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
            File[] files = prefsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    if (name.endsWith(".xml")) {
                        String prefName = name.substring(0, name.length() - 4);
                        TreeNode fileNode = new TreeNode(prefName, null, true, 0);
                        loadPrefIntoNode(prefName, fileNode);
                        roots.add(fileNode);
                    }
                }
            }
            runOnUiThread(() -> {
                rootNodes = roots;
                rebuildVisibleList();
                tvPath.setText("shared_prefs/ (" + rootNodes.size() + " files)");
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void loadPrefIntoNode(String prefName, TreeNode parent) {
        SharedPreferences prefs = getSharedPreferences(prefName, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            String strVal = val != null ? val.toString() : "null";
            TreeNode child = tryParseJson(key, strVal, parent.depth + 1);
            if (child != null) {
                parent.children.add(child);
            } else {
                parent.children.add(new TreeNode(key, strVal, false, parent.depth + 1));
            }
        }
    }

    private TreeNode tryParseJson(String key, String value, int depth) {
        if (value == null || value.isEmpty()) return null;
        value = value.trim();
        if (!value.startsWith("{") && !value.startsWith("[")) return null;
        try {
            if (value.startsWith("{")) {
                JSONObject obj = new JSONObject(value);
                TreeNode folder = new TreeNode(key, null, true, depth);
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String childKey = keys.next();
                    Object childVal = obj.get(childKey);
                    addJsonChild(folder, childKey, childVal, depth + 1);
                }
                return folder;
            } else if (value.startsWith("[")) {
                JSONArray arr = new JSONArray(value);
                TreeNode folder = new TreeNode(key, null, true, depth);
                for (int i = 0; i < arr.length(); i++) {
                    addJsonChild(folder, "[" + i + "]", arr.get(i), depth + 1);
                }
                return folder;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void addJsonChild(TreeNode parent, String key, Object val, int depth) {
        if (val instanceof JSONObject) {
            TreeNode folder = new TreeNode(key, null, true, depth);
            JSONObject obj = (JSONObject) val;
            Iterator<String> childKeys = obj.keys();
            while (childKeys.hasNext()) {
                String childKey = childKeys.next();
                try {
                    addJsonChild(folder, childKey, obj.get(childKey), depth + 1);
                } catch (Exception ignored) {
                }
            }
            parent.children.add(folder);
        } else if (val instanceof JSONArray) {
            JSONArray arr = (JSONArray) val;
            TreeNode folder = new TreeNode(key, null, true, depth);
            for (int i = 0; i < arr.length(); i++) {
                try {
                    addJsonChild(folder, "[" + i + "]", arr.get(i), depth + 1);
                } catch (Exception ignored) {
                }
            }
            parent.children.add(folder);
        } else {
            String strVal = val == null ? "null" : val.toString();
            parent.children.add(new TreeNode(key, strVal, false, depth));
        }
    }

    private void rebuildVisibleList() {
        visibleNodes.clear();
        for (TreeNode root : rootNodes) {
            visibleNodes.add(root);
            if (root.isExpanded) {
                addChildrenRecursive(root);
            }
        }
    }

    private void addChildrenRecursive(TreeNode parent) {
        if (parent.children == null) return;
        for (TreeNode child : parent.children) {
            visibleNodes.add(child);
            if (child.isExpanded) {
                addChildrenRecursive(child);
            }
        }
    }

    private void toggleNode(TreeNode node) {
        if (!node.isFolder) return;
        node.isExpanded = !node.isExpanded;
        rebuildVisibleList();
        adapter.notifyDataSetChanged();
    }

    private class TreeAdapter extends RecyclerView.Adapter<TreeAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tree_node, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TreeNode node = visibleNodes.get(position);
            holder.bind(node);
        }

        @Override
        public int getItemCount() {
            return visibleNodes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivArrow;
            TextView tvName;
            TextView tvValue;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivArrow = itemView.findViewById(R.id.ivArrow);
                tvName = itemView.findViewById(R.id.tvName);
                tvValue = itemView.findViewById(R.id.tvValue);
            }

            void bind(TreeNode node) {
                int indent = node.depth * 48;
                itemView.setPadding(indent, 0, 0, 0);

                if (node.isFolder) {
                    ivArrow.setVisibility(View.VISIBLE);
                    ivArrow.setImageResource(node.isExpanded
                            ? android.R.drawable.arrow_down_float
                            : android.R.drawable.arrow_up_float);
                    tvName.setText(node.name);
                    tvName.setTextColor(0xFFE0E0E0);
                    tvValue.setText(node.isExpanded ? "" : "(" + node.children.size() + ")");
                } else {
                    ivArrow.setVisibility(View.GONE);
                    tvName.setText(node.name);
                    tvName.setTextColor(0xFFB0B0B0);
                    tvValue.setText(node.value != null ? node.value : "null");
                }

                itemView.setOnClickListener(v -> {
                    if (node.isFolder) {
                        toggleNode(node);
                    } else if (node.value != null) {
                        new AlertDialog.Builder(SharedPrefDisplayerActivity.this)
                                .setTitle(node.name)
                                .setMessage(node.value)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
            }
        }
    }
}
