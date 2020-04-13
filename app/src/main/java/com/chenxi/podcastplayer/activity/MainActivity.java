package com.chenxi.podcastplayer.activity;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.chenxi.podcastplayer.R;
import com.chenxi.podcastplayer.database.DBHelper;
import com.chenxi.podcastplayer.service.PlaybackService;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int MESSAGE_UPDATE = 0;
    private static final int MESSAGE_PAUSE = 1;
    private static final int REQUEST_URL = 0x0a;
    private static final int REQUEST_PLAY = 0X0b;
    private PlaybackService.PlayerBinder binder;
    private SQLiteDatabase database;
    private int index = -1;

    private TextView title;
    private TextView time_played;
    private TextView time_remain;
    private SeekBar seekBar;
    private ImageButton button_play;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE:
                    if (binder != null) {
                        if (index != -1) {
                            binder.play(index);
                            index = -1;
                        }
                        sync();
                    }
                    sendEmptyMessageDelayed(MESSAGE_UPDATE, 250);
                    break;
                case MESSAGE_PAUSE:
                    removeMessages(MESSAGE_UPDATE);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Configuration configuration = this.getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            setContentView(R.layout.activity_main_land);
        else
            setContentView(R.layout.activity_main);

        database = new DBHelper(this).getWritableDatabase();
        title = findViewById(R.id.text_title);
        time_played = findViewById(R.id.text_time_played);
        time_remain = findViewById(R.id.text_time_remain);
        seekBar = findViewById(R.id.seekBar);
        button_play = findViewById(R.id.button_play);

        if (savedInstanceState == null) {
            ArrayList<String> playList = new ArrayList<>();
            playList.add("http://music.163.com/song/media/outer/url?id=347246");
            playList.add("http://music.163.com/song/media/outer/url?id=347247");
            Intent intent = new Intent(this, PlaybackService.class);
            intent.putExtra("playlist", playList);
            startService(intent);
        }
        bindService(new Intent(this, PlaybackService.class), serviceConnection, 0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.sendEmptyMessage(MESSAGE_PAUSE);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                binder.setProgress(seekBar.getProgress());
                handler.sendEmptyMessage(MESSAGE_UPDATE);
            }
        });
    }

    @Override
    protected void onStart() {
        handler.sendEmptyMessage(MESSAGE_UPDATE);
        super.onStart();
    }

    @Override
    protected void onStop() {
        handler.sendEmptyMessage(MESSAGE_PAUSE);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        unbindService(serviceConnection);
        database.close();
        super.onDestroy();
    }

    public void operation(View view) {
        if (binder != null) {
            switch (view.getId()) {
                case R.id.button_next:
                    if (!binder.playNext())
                        Toast.makeText(this, R.string.toast_last_play, Toast.LENGTH_SHORT).show();
                    break;
                case R.id.button_play:
                    binder.play();
                    break;
                case R.id.button_prev:
                    if (!binder.playPrev())
                        Toast.makeText(this, R.string.toast_first_play, Toast.LENGTH_SHORT).show();
                    break;
                case R.id.button_stop:
                    binder.stop();
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_URL) {
                if (binder.getPlaylist() != null)
                    if (!binder.add(data.getDataString()))
                        Toast.makeText(this, R.string.toast_existence, Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_PLAY) {
                index = data.getIntExtra("data", -1);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (PlaybackService.PlayerBinder) service;
            handler.sendEmptyMessage(MESSAGE_UPDATE);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            handler.sendEmptyMessage(MESSAGE_PAUSE);
            binder = null;
            finish();
        }
    };

    private String timeToText(int progress, int duration) {
        int minute_dura = (int) (duration / 60000f);
        int second_dura = (int) (60 * (duration / 60000f - minute_dura));
        int minute_prog = (int) (progress / 60000f);
        int second_prog = (int) (60 * (progress / 60000f - minute_prog));

        int second = second_dura - second_prog;
        int minute = minute_dura - minute_prog;
        if (second < 0) {
            second = second + 60;
            minute = minute - 1;
        }

        if (second < 10)
            return minute + ":0" + second;
        else
            return minute + ":" + second;
    }

    private void sync() {
        if (binder != null) {
            String temp = binder.getTitle();
            if (temp == null || temp.isEmpty())
                title.setText(R.string.text_unknown_source);
            else
                title.setText(temp);
            switch (binder.getState()) {
                case PLAYING:
                    button_play.setImageResource(android.R.drawable.ic_media_pause);
                    break;
                case PAUSED:
                case STOPPED:
                case ERROR:
                    button_play.setImageResource(android.R.drawable.ic_media_play);
                    break;
            }
            int progress = binder.getProgress();
            int duration = binder.getDuration();
            if (progress > duration)
                progress = duration;
            time_played.setText(timeToText(0, progress));
            time_remain.setText(timeToText(progress, duration));
            seekBar.setMax(duration);
            seekBar.setProgress(progress);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_url:
                startActivityForResult(new Intent(this, AddPodcastActivity.class), REQUEST_URL);
                break;
            case R.id.menu_comments:
                if (binder.getUrl() != null) {
                    Intent intent = new Intent(this, CommentsActivity.class);
                    intent.putExtra("data", binder.getUrl().hashCode());
                    startActivity(intent);
                }
                break;
            case R.id.menu_clearAll:
                if (binder.getUrl() != null &&
                        database.delete(DBHelper.TABLE_NAME, DBHelper.COL_FIRST + "=?",
                                new String[] { String.valueOf(binder.getUrl().hashCode()) }) > 0)
                    Toast.makeText(this, R.string.toast_comment_cleared, Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_playlist:
                Intent intent = new Intent(this, PlaylistActivity.class);
                intent.putStringArrayListExtra("playlist", binder.getPlaylist());
                startActivityForResult(intent, REQUEST_PLAY);
                break;
            case R.id.menu_exit:
                stopService(new Intent(this, PlaybackService.class));
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return true;
    }

    public void sendComment(View view) {
        if (binder.getUrl() == null) {
            Toast.makeText(this, R.string.toast_comment_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        EditText editText = findViewById(R.id.edit_comments);
        String comment = editText.getText().toString().trim();
        if (comment.isEmpty() || comment.length() < 5)
            Toast.makeText(this, R.string.toast_text_min_lenth, Toast.LENGTH_SHORT).show();
        else if (comment.length() > 1000)
            Toast.makeText(this, R.string.toast_text_max_lenth, Toast.LENGTH_SHORT).show();
        else {
            ContentValues row = new ContentValues();
            row.put(DBHelper.COL_FIRST, binder.getUrl().hashCode());
            row.put(DBHelper.COL_SECOND, comment);
            if (database.insert(DBHelper.TABLE_NAME, null, row) == -1)
                Toast.makeText(this, R.string.toast_comment_failed, Toast.LENGTH_SHORT).show();
            else {
                editText.getText().clear();
                Toast.makeText(this, R.string.toast_comment_saved, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
