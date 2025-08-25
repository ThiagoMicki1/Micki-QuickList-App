package com.quicklist.app.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.ActionBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.quicklist.app.R;
import com.quicklist.app.models.Item;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {
    private String listId;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recycler;
    private ItemsAdapter adapter;
    private List<DocumentSnapshot> docs = new ArrayList<>();
    private EditText newItemInput;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);
            ab.setTitle("Items");
        }

        listId = getIntent().getStringExtra("LIST_ID");
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recycler = findViewById(R.id.itemsRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemsAdapter(docs, new ItemsAdapter.Events() {
            @Override public void onToggle(DocumentSnapshot doc, boolean checked) {
                doc.getReference().update("checked", checked);
            }
            @Override public void onDelete(DocumentSnapshot doc) {
                doc.getReference().delete();
            }
        });
        recycler.setAdapter(adapter);

        newItemInput = findViewById(R.id.newItemInput);
        Button addBtn = findViewById(R.id.addItemBtn);
        addBtn.setOnClickListener(v -> addItem());

        Button shareBtn = findViewById(R.id.shareBtn);
        shareBtn.setOnClickListener(v -> showShareDialog());

        db.collection("lists").document(listId)
                .collection("items").orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    docs.clear();
                    docs.addAll(snap.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addItem() {
        String text = newItemInput.getText().toString().trim();
        if (text.isEmpty()) return;
        String uid = auth.getCurrentUser().getUid();
        Item item = new Item(text, uid);
        db.collection("lists").document(listId).collection("items").add(item);
        newItemInput.setText("");
    }

    private void showShareDialog() {
        EditText emailInput = new EditText(this);
        emailInput.setHint("Friend's email");
        new AlertDialog.Builder(this)
                .setTitle("Share by email")
                .setView(emailInput)
                .setPositiveButton("Invite", (d, w) -> inviteByEmail(emailInput.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Email-based sharing: look up uid from users collection, add to list.members
    private void inviteByEmail(String email) {
        if (TextUtils.isEmpty(email)) return;
        db.collection("users").whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) { toast("No user with that email."); return; }
                    String inviteeUid = q.getDocuments().get(0).getId();
                    db.collection("lists").document(listId)
                            .update("members", FieldValue.arrayUnion(inviteeUid))
                            .addOnSuccessListener(v -> toast("Shared!"))
                            .addOnFailureListener(e -> toast(e.getMessage()));
                })
                .addOnFailureListener(e -> toast(e.getMessage()));
    }

    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    // ----- Adapter -----
    static class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.VH> {
        interface Events {
            void onToggle(DocumentSnapshot doc, boolean checked);
            void onDelete(DocumentSnapshot doc);
        }
        private final List<DocumentSnapshot> data; private final Events events;
        ItemsAdapter(List<DocumentSnapshot> data, Events events){ this.data=data; this.events=events; }

        static class VH extends RecyclerView.ViewHolder {
            CheckBox box; TextView text; Button del;
            VH(View v){ super(v);
                box = v.findViewById(R.id.checkBox);
                text = v.findViewById(R.id.itemText);
                del = v.findViewById(R.id.deleteBtn);
            }
        }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.row_item, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            DocumentSnapshot doc = data.get(pos);
            String label = doc.getString("text");
            Boolean checkedVal = doc.getBoolean("checked");
            boolean checked = checkedVal != null && checkedVal;
            h.text.setText(label);
            h.box.setOnCheckedChangeListener(null);
            h.box.setChecked(checked);
            h.box.setOnCheckedChangeListener((btn, isChecked) -> events.onToggle(doc, isChecked));
            h.del.setOnClickListener(v -> events.onDelete(doc));
        }
        @Override public int getItemCount(){ return data.size(); }
    }
}
