package com.chenxi.podcastplayer.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.chenxi.podcastplayer.R;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(R.string.bar_title_playlist);
        }

        final ArrayList<String> playlist = getIntent().getStringArrayListExtra("playlist");
        if (playlist != null && !playlist.isEmpty()) {
            ListView listView = findViewById(R.id.playlistView);
            ArrayList<String> list = new ArrayList<>();
            int index = 1;
            for (String url: playlist) {
                Uri uri = Uri.parse(url);
                String title = uri.getQuery();
                if (title == null || title.isEmpty()) {
                    title = uri.getLastPathSegment();
                    if (title == null || title.isEmpty())
                        title = getString(R.string.text_unknown_source);
                }
                list.add(index + ".\t" + title);
                index++;
            }
            listView.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, list));
            listView.setOnItemClickListener((parent, view, position, id) -> {
                Intent result = new Intent(PlaylistActivity.this, MainActivity.class);
                result.putExtra("data", position);
                setResult(RESULT_OK, result);
                finish();
            });
            listView.setOnItemLongClickListener((parent, view, pos, id) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(playlist.get(pos)), "audio/*");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
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
}
