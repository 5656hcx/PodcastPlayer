package com.chenxi.podcastplayer.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chenxi.podcastplayer.R;

public class AddPodcastActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_podcast);
    }

    public void buttonAction(View view) {
        int id = view.getId();
        if (id == R.id.button_cancel) {
            setResult(RESULT_CANCELED);
            finish();
        }
        else if (id == R.id.button_add) {
            EditText editText = findViewById(R.id.edit_add_url);
            Uri uri = Uri.parse(editText.getText().toString().trim());
            if (uri != null && uri.getScheme() != null &&
                    (uri.getScheme().toLowerCase().equals("http") ||
                            uri.getScheme().toLowerCase().equals("https"))) {
                Intent result = new Intent(this, MainActivity.class);
                result.setData(uri);
                setResult(RESULT_OK, result);
                finish();
            } else
                Toast.makeText(this, R.string.toast_invalid_url, Toast.LENGTH_SHORT).show();
        }
    }
}
