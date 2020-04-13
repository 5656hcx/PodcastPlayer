package com.chenxi.podcastplayer.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.chenxi.podcastplayer.R;
import com.chenxi.podcastplayer.database.DBHelper;

public class CommentsActivity extends AppCompatActivity {

    private SQLiteDatabase database;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(R.string.bar_title_comm);
        }

        DBHelper dbHelper = new DBHelper(this);
        database = dbHelper.getReadableDatabase();
        cursor = database.query(DBHelper.TABLE_NAME,
                new String [] { "_id", DBHelper.COL_SECOND }, DBHelper.COL_FIRST + "=?",
                new String[] { String.valueOf(getIntent().getIntExtra("data", 0)) },
                null, null, null);
        if (cursor.moveToLast()) {
            findViewById(R.id.textView_empty).setVisibility(View.INVISIBLE);
            ListView listView = findViewById(R.id.listView);
            listView.setAdapter(new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_1,
                    cursor, new String[] { DBHelper.COL_SECOND },
                    new int[] { android.R.id.text1}, 0));
            listView.setOnItemLongClickListener((parent, view, pos, id) -> {
                cursor.moveToPosition(pos);
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("comment", cursor.getString(1)));
                }
                Toast.makeText(CommentsActivity.this,
                        R.string.toast_copied, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        cursor.close();
        database.close();
        super.onDestroy();
    }
}