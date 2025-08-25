package com.quicklist.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.quicklist.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recycler;
    private ListsAdapter adapter;
    private final List<DocumentSnapshot> docs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_home);

        // Toolbar with return arrow
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);
            ab.setTitle("Lists");
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recycler = findViewById(R.id.listsRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListsAdapter(docs, new ListsAdapter.Events() {
            @Override public void onOpen(DocumentSnapshot doc) { openList(doc.getId()); }
            @Override public void onDelete(DocumentSnapshot doc) { deleteList(doc.getId()); }
        });
        recycler.setAdapter(adapter);

        findViewById(R.id.newListBtn).setOnClickListener(v -> showNewListDialog());

        String uid = auth.getCurrentUser().getUid();
        db.collection("lists")
                .whereArrayContains("members", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;
                    docs.clear();
                    docs.addAll(snap.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    // Inflate the menu that contains the "Sign out" button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    // Handle toolbar actions: back arrow and sign out
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (id == R.id.action_sign_out) {
            confirmSignOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign out?")
                .setMessage("You’ll be returned to the login screen.")
                .setPositiveButton("Sign out", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    // Go to login and clear back stack
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openList(String listId) {
        Intent i = new Intent(this, ListActivity.class);
        i.putExtra("LIST_ID", listId);
        startActivity(i);
    }

    private void deleteList(String listId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete list?")
                .setMessage("This will permanently remove the list and all its items.")
                .setPositiveButton("Delete", (d, w) -> {
                    com.google.firebase.firestore.DocumentReference listRef =
                            db.collection("lists").document(listId);

                    // 1) Delete all items in subcollection
                    listRef.collection("items").get()
                            .addOnSuccessListener(q -> {
                                com.google.firebase.firestore.WriteBatch batch = db.batch();
                                for (com.google.firebase.firestore.DocumentSnapshot doc : q.getDocuments()) {
                                    batch.delete(doc.getReference());
                                }
                                // 2) Delete the list document itself
                                batch.delete(listRef);
                                batch.commit()
                                        .addOnSuccessListener(v ->
                                                Toast.makeText(this, "List deleted", Toast.LENGTH_SHORT).show()
                                        )
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                                        );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNewListDialog() {
        EditText input = new EditText(this);
        input.setHint("List name");
        new AlertDialog.Builder(this)
                .setTitle("New List")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> createList(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createList(String name) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in again", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("name", name.isEmpty() ? "Untitled List" : name);
        data.put("createdBy", uid);
        data.put("members", Arrays.asList(uid));
        data.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("lists").add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "List created", Toast.LENGTH_SHORT).show();
                    openList(ref.getId());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // --- Recycler Adapter for lists with delete button ---
    static class ListsAdapter extends RecyclerView.Adapter<ListsAdapter.VH> {
        interface Events { void onOpen(DocumentSnapshot doc); void onDelete(DocumentSnapshot doc); }

        private final List<DocumentSnapshot> data;
        private final Events events;

        ListsAdapter(List<DocumentSnapshot> data, Events events) {
            this.data = data;
            this.events = events;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView title;
            ImageButton delete;
            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.listTitle);
                delete = itemView.findViewById(R.id.deleteListBtn);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_list, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DocumentSnapshot doc = data.get(pos);
            String name = doc.getString("name");
            h.title.setText(name != null ? name : "(Untitled)");
            h.itemView.setOnClickListener(v -> events.onOpen(doc));
            h.delete.setOnClickListener(v -> events.onDelete(doc));
        }

        @Override
        public int getItemCount() { return data.size(); }
    }
}
