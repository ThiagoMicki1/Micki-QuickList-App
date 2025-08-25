package com.quicklist.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
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
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.quicklist.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recycler;
    private View emptyState;
    private EditText searchInput;
    private ListsAdapter adapter;

    // Raw docs from Firestore (unfiltered)
    private final List<DocumentSnapshot> docs = new ArrayList<>();
    // Visible list after filters/sort
    private final List<DocumentSnapshot> visible = new ArrayList<>();

    private boolean sortAZ = false;          // false = recent
    private boolean showArchived = false;    // hide archived by default

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);
            ab.setTitle("Lists");
        }

        emptyState = findViewById(R.id.emptyState);
        searchInput = findViewById(R.id.searchInput);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recycler = findViewById(R.id.listsRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListsAdapter(visible, new ListsAdapter.Events() {
            @Override public void onOpen(DocumentSnapshot doc)  { openList(doc.getId()); }
            @Override public void onShare(DocumentSnapshot doc) { showShareDialog(doc.getId()); }
            @Override public void onPinToggle(DocumentSnapshot doc) { togglePin(doc); }
            @Override public void onArchiveToggle(DocumentSnapshot doc) { toggleArchiveWithUndo(doc); }
            @Override public void onColor(DocumentSnapshot doc) { pickColor(doc); }
            @Override public void onEmoji(DocumentSnapshot doc) { pickEmoji(doc); }
        });
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddList);
        fab.setOnClickListener(v -> showNewListDialog());

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
                    rebuildVisible();
                });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { rebuildVisible(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        rebuildVisible();
    }

    // ===== Toolbar menu =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        menu.findItem(R.id.action_show_archived).setChecked(showArchived);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (id == R.id.sort_recent) {
            sortAZ = false; rebuildVisible(); return true;
        } else if (id == R.id.sort_az) {
            sortAZ = true; rebuildVisible(); return true;
        } else if (id == R.id.action_show_archived) {
            showArchived = !item.isChecked();
            item.setChecked(showArchived);
            rebuildVisible();
            return true;
        } else if (id == R.id.action_sign_out) {
            confirmSignOut(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign out?")
                .setMessage("You’ll be returned to the login screen.")
                .setPositiveButton("Sign out", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ===== Filtering + Sorting + Pin first =====
    private void rebuildVisible() {
        String q = searchInput.getText() != null ? searchInput.getText().toString().trim().toLowerCase() : "";
        visible.clear();

        for (DocumentSnapshot d : docs) {
            Boolean archived = d.getBoolean("archived");
            if (!showArchived && archived != null && archived) continue;

            String name = String.valueOf(d.getString("name")).toLowerCase();
            if (!q.isEmpty() && (name == null || !name.contains(q))) continue;

            visible.add(d);
        }

        // Pin first
        Collections.sort(visible, (a, b) -> {
            boolean ap = Boolean.TRUE.equals(a.getBoolean("pinned"));
            boolean bp = Boolean.TRUE.equals(b.getBoolean("pinned"));
            if (ap != bp) return ap ? -1 : 1;
            if (sortAZ) {
                String an = a.getString("name"); if (an == null) an = "";
                String bn = b.getString("name"); if (bn == null) bn = "";
                return an.compareToIgnoreCase(bn);
            } else {
                // Recent first by createdAt (already by query order); keep stable
                return 0;
            }
        });

        adapter.notifyDataSetChanged();
        toggleEmpty();
    }

    private void toggleEmpty() {
        boolean isEmpty = visible.isEmpty();
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    // ===== Row actions =====
    private void openList(String listId) {
        Intent i = new Intent(this, ListActivity.class);
        i.putExtra("LIST_ID", listId);
        startActivity(i);
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
        data.put("pinned", false);
        data.put("archived", false);
        data.put("color", "#16A34A");
        data.put("emoji", "✅");

        db.collection("lists").add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "List created", Toast.LENGTH_SHORT).show();
                    openList(ref.getId());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void showShareDialog(String listId) {
        EditText emailInput = new EditText(this);
        emailInput.setHint("Friend's email");
        new AlertDialog.Builder(this)
                .setTitle("Share by email")
                .setView(emailInput)
                .setPositiveButton("Invite", (d, w) -> {
                    String email = emailInput.getText().toString().trim();
                    inviteByEmail(listId, email);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void inviteByEmail(String listId, String email) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) {
                        Toast.makeText(this, "No user with that email.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String inviteeUid = q.getDocuments().get(0).getId();
                    db.collection("lists").document(listId)
                            .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(inviteeUid))
                            .addOnSuccessListener(v ->
                                    Toast.makeText(this, "Shared!", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void togglePin(DocumentSnapshot doc) {
        boolean pinned = Boolean.TRUE.equals(doc.getBoolean("pinned"));
        doc.getReference().update("pinned", !pinned);
    }

    private void toggleArchiveWithUndo(DocumentSnapshot doc) {
        boolean archived = Boolean.TRUE.equals(doc.getBoolean("archived"));
        doc.getReference().update("archived", !archived)
                .addOnSuccessListener(v -> {
                    String msg = archived ? "Unarchived" : "Archived";
                    Snackbar.make(recycler, msg, Snackbar.LENGTH_LONG)
                            .setAction("UNDO", a ->
                                    doc.getReference().update("archived", archived))
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void pickColor(DocumentSnapshot doc) {
        // Simple palette
        final String[] names = {"Green","Blue","Purple","Orange","Red","Teal"};
        final String[] hexes = {"#16A34A","#2563EB","#7C3AED","#F97316","#DC2626","#14B8A6"};
        new AlertDialog.Builder(this)
                .setTitle("Pick a color")
                .setItems(names, (d, which) ->
                        doc.getReference().update("color", hexes[which]))
                .show();
    }

    private void pickEmoji(DocumentSnapshot doc) {
        final String[] emojis = {"✅","📝","🛒","🎒","✈️","🏫","🧹","💼","📦","🎯"};
        new AlertDialog.Builder(this)
                .setTitle("Pick an emoji")
                .setItems(emojis, (d, which) ->
                        doc.getReference().update("emoji", emojis[which]))
                .show();
    }

    // ===== Adapter =====
    static class ListsAdapter extends RecyclerView.Adapter<ListsAdapter.VH> {
        interface Events {
            void onOpen(DocumentSnapshot doc);
            void onShare(DocumentSnapshot doc);
            void onPinToggle(DocumentSnapshot doc);
            void onArchiveToggle(DocumentSnapshot doc);
            void onColor(DocumentSnapshot doc);
            void onEmoji(DocumentSnapshot doc);
        }

        private final List<DocumentSnapshot> data;
        private final Events events;

        ListsAdapter(List<DocumentSnapshot> data, Events events) {
            this.data = data;
            this.events = events;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, emoji;
            View colorStripe;
            ImageButton more;
            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.listTitle);
                emoji = itemView.findViewById(R.id.emoji);
                colorStripe = itemView.findViewById(R.id.colorStripe);
                more  = itemView.findViewById(R.id.moreBtn);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_list, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DocumentSnapshot doc = data.get(pos);
            String name = doc.getString("name");
            String emoji = doc.getString("emoji");
            String color = doc.getString("color");
            boolean archived = Boolean.TRUE.equals(doc.getBoolean("archived"));
            boolean pinned = Boolean.TRUE.equals(doc.getBoolean("pinned"));

            h.title.setText(name != null ? name : "(Untitled)");
            h.emoji.setText(!TextUtils.isEmpty(emoji) ? emoji : "✅");
            try { h.colorStripe.setBackgroundColor(Color.parseColor(color != null ? color : "#16A34A")); }
            catch (Exception ignored) { h.colorStripe.setBackgroundColor(Color.parseColor("#16A34A")); }

            // Whole row opens when not archived; archived still opens
            h.itemView.setOnClickListener(v -> events.onOpen(doc));

            h.more.setOnClickListener(v -> {
                PopupMenu pm = new PopupMenu(h.more.getContext(), h.more);
                pm.getMenuInflater().inflate(R.menu.menu_row_list, pm.getMenu());
                // Toggle titles dynamically
                pm.getMenu().findItem(R.id.action_pin).setTitle(pinned ? "Unpin" : "Pin");
                pm.getMenu().findItem(R.id.action_archive).setTitle(archived ? "Unarchive" : "Archive");
                pm.setOnMenuItemClickListener(mi -> {
                    int id = mi.getItemId();
                    if (id == R.id.action_open)   events.onOpen(doc);
                    if (id == R.id.action_share)  events.onShare(doc);
                    if (id == R.id.action_pin)    events.onPinToggle(doc);
                    if (id == R.id.action_archive)events.onArchiveToggle(doc);
                    if (id == R.id.action_color)  events.onColor(doc);
                    if (id == R.id.action_emoji)  events.onEmoji(doc);
                    return true;
                });
                pm.show();
            });
        }

        @Override public int getItemCount() { return data.size(); }
    }
}
