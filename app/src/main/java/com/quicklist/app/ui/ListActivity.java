package com.quicklist.app.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.quicklist.app.R;
import com.quicklist.app.models.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ListActivity extends AppCompatActivity {
    private String listId;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recycler;
    private ItemsAdapter adapter;
    private final List<DocumentSnapshot> docs = new ArrayList<>();
    private EditText newItemInput, qtyInput;

    private boolean lastAllDone = false;
    private boolean sortItemsAZ = false; // false = by createdAt (recent)

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
                deleteItemWithUndo(doc);
            }
            @Override public void onQtyChange(DocumentSnapshot doc, int newQty) {
                int q = Math.max(1, newQty);
                doc.getReference().update("quantity", q);
            }
        });
        recycler.setAdapter(adapter);

        attachSwipeGestures();

        newItemInput = findViewById(R.id.newItemInput);
        qtyInput = findViewById(R.id.qtyInput);
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
                    applyItemSort();
                    adapter.notifyDataSetChanged();
                    checkForConfetti();
                });
    }

    // ===== Toolbar menu (A–Z / Recent) =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (id == R.id.sort_items_recent) {
            sortItemsAZ = false;
            applyItemSort();
            adapter.notifyDataSetChanged();
            return true;
        } else if (id == R.id.sort_items_az) {
            sortItemsAZ = true;
            applyItemSort();
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyItemSort() {
        if (!sortItemsAZ) return; // Firestore already gives createdAt ASC
        // Local sort by text A–Z (null-safe)
        Collections.sort(docs, new Comparator<DocumentSnapshot>() {
            @Override public int compare(DocumentSnapshot a, DocumentSnapshot b) {
                String at = a.getString("text"); if (at == null) at = "";
                String bt = b.getString("text"); if (bt == null) bt = "";
                return at.compareToIgnoreCase(bt);
            }
        });
    }

    private void addItem() {
        String text = newItemInput.getText().toString().trim();
        if (text.isEmpty()) return;

        int qty = 1;
        try {
            if (qtyInput != null && !TextUtils.isEmpty(qtyInput.getText()))
                qty = Math.max(1, Integer.parseInt(qtyInput.getText().toString().trim()));
        } catch (NumberFormatException ignore) { qty = 1; }

        String uid = auth.getCurrentUser().getUid();
        Item item = new Item(text, uid, qty);
        db.collection("lists").document(listId).collection("items").add(item);
        newItemInput.setText("");
        if (qtyInput != null) qtyInput.setText("");
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

    private void deleteItemWithUndo(DocumentSnapshot doc) {
        final Map<String,Object> oldData = doc.getData();
        final String docId = doc.getId();
        if (oldData == null) { doc.getReference().delete(); return; }

        doc.getReference().delete()
                .addOnSuccessListener(v -> {
                    Snackbar.make(recycler, "Item deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", a ->
                                    db.collection("lists").document(listId)
                                            .collection("items").document(docId)
                                            .set(oldData)
                            ).show();
                })
                .addOnFailureListener(e -> toast(e.getMessage()));
    }

    private void attachSwipeGestures() {
        ItemTouchHelper.SimpleCallback cb =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }
                    @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                        int pos = vh.getAdapterPosition();
                        if (pos < 0 || pos >= docs.size()) return;
                        DocumentSnapshot doc = docs.get(pos);
                        if (dir == ItemTouchHelper.LEFT) {
                            Boolean checkedVal = doc.getBoolean("checked");
                            boolean checked = checkedVal != null && checkedVal;
                            doc.getReference().update("checked", !checked);
                        } else if (dir == ItemTouchHelper.RIGHT) {
                            deleteItemWithUndo(doc);
                        }
                        adapter.notifyItemChanged(pos);
                    }
                };
        new ItemTouchHelper(cb).attachToRecyclerView(recycler);
    }

    private void checkForConfetti() {
        int total = docs.size();
        int checked = 0;
        for (DocumentSnapshot d : docs) {
            if (Boolean.TRUE.equals(d.getBoolean("checked"))) checked++;
        }
        boolean allDone = total > 0 && checked == total;
        if (allDone && !lastAllDone) {
            showConfetti();
        }
        lastAllDone = allDone;
    }

    private void showConfetti() {
        final View root = findViewById(android.R.id.content);
        final int count = 24;
        for (int i = 0; i < count; i++) {
            final TextView t = new TextView(this);
            t.setText("🎉");
            t.setTextSize(22);
            t.setAlpha(0f);
            ((ViewGroup) root).addView(t);
            int startX = (root.getWidth() > 0 ? root.getWidth() : 800) * i / count;
            t.setX(startX);
            t.setY(-50);
            t.animate()
                    .alpha(1f)
                    .translationYBy((root.getHeight() > 0 ? root.getHeight() : 1200) + 100)
                    .rotationBy(360f)
                    .setDuration(1600)
                    .withEndAction(() -> ((ViewGroup) root).removeView(t))
                    .start();
        }
        Snackbar.make(recycler, "All done—nice!", Snackbar.LENGTH_SHORT).show();
    }

    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    // ----- Adapter with quantity controls -----
    static class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.VH> {
        interface Events {
            void onToggle(DocumentSnapshot doc, boolean checked);
            void onDelete(DocumentSnapshot doc);
            void onQtyChange(DocumentSnapshot doc, int newQty);
        }
        private final List<DocumentSnapshot> data; private final Events events;
        ItemsAdapter(List<DocumentSnapshot> data, Events events){ this.data=data; this.events=events; }

        static class VH extends RecyclerView.ViewHolder {
            CheckBox box; TextView text; Button del;
            TextView qtyText; View btnMinus; View btnPlus;
            VH(View v){ super(v);
                box = v.findViewById(R.id.checkBox);
                text = v.findViewById(R.id.itemText);
                del = v.findViewById(R.id.deleteBtn);
                qtyText = v.findViewById(R.id.qtyText);
                btnMinus = v.findViewById(R.id.btnMinus);
                btnPlus = v.findViewById(R.id.btnPlus);
            }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.row_item, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            DocumentSnapshot doc = data.get(pos);
            String label = doc.getString("text");
            Boolean checkedVal = doc.getBoolean("checked");
            boolean checked = checkedVal != null && checkedVal;

            Long qLong = doc.getLong("quantity");
            int qty = qLong == null ? 1 : Math.max(1, qLong.intValue());

            h.text.setText(label);
            h.box.setOnCheckedChangeListener(null);
            h.box.setChecked(checked);
            h.box.setOnCheckedChangeListener((btn, isChecked) -> events.onToggle(doc, isChecked));

            h.qtyText.setText(String.valueOf(qty));
            h.btnMinus.setOnClickListener(v -> {
                int newQty = Math.max(1, qty - 1);
                events.onQtyChange(doc, newQty);
            });
            h.btnPlus.setOnClickListener(v -> {
                int newQty = qty + 1;
                events.onQtyChange(doc, newQty);
            });

            h.del.setOnClickListener(v -> events.onDelete(doc));
        }

        @Override public int getItemCount(){ return data.size(); }
    }
}
