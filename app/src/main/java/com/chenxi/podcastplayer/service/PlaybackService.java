package com.chenxi.podcastplayer.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.chenxi.podcastplayer.R;
import com.chenxi.podcastplayer.activity.MainActivity;
import com.chenxi.podcastplayer.utility.PodcastPlayer;

import java.io.IOException;
import java.util.ArrayList;

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener {

    public final static int NOTIFICATION_ID = 1024;
    public final static String CHANNEL_ID = "Playback_0";
    public final static String CHANNERL_NAME = "Playback";
    private final PlayerBinder binder = new PlayerBinder();
    private final PodcastPlayer player = new PodcastPlayer();
    private ArrayList<String> playList;
    private static int currentSong = -1;
    private NotificationCompat.Builder builder;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notify_podcast_24)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT));
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                        CHANNERL_NAME, NotificationManager.IMPORTANCE_LOW));
            }
        super.onCreate();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();
        try {
            if (currentSong + 1 == playList.size()) {
                currentSong = 0;
                mp.setDataSource(playList.get(currentSong));
                mp.prepare();
                mp.start();
                player.pause();
                player.setFilePath(playList.get(currentSong));
                sendNotification(false);
            } else {
                mp.setDataSource(playList.get(++currentSong));
                mp.prepare();
                mp.start();
                player.setFilePath(playList.get(currentSong));
                sendNotification(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (playList == null) {
            playList = intent.getStringArrayListExtra("playlist");
            if (playList != null && !playList.isEmpty()) {
                currentSong = 0;
                player.load(playList.get(currentSong), this);
                player.pause();
                sendNotification(false);
            }
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        player.stop();
        notificationManager.cancelAll();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
            notificationManager.deleteNotificationChannel(CHANNEL_ID);
        super.onDestroy();
    }

    private boolean playNext() {
        if (playList != null && currentSong < playList.size()-1) {
            player.stop();
            player.load(playList.get(++currentSong), this);
            sendNotification(true);
            return true;
        }
        return false;
    }

    private boolean playPrev() {
        if (currentSong > 0 && playList != null) {
            player.stop();
            player.load(playList.get(--currentSong), this);
            sendNotification(true);
            return true;
        }
        return false;
    }

    private boolean add(String source) {
        if (playList.indexOf(source) < 0) {
            currentSong = 0;
            playList.add(currentSong, source);
            player.stop();
            player.load(source, this);
            sendNotification(true);
            return true;
        }
        return false;
    }

    private void sendNotification(boolean onGoing) {
        String info = " (" + (currentSong + 1) + "/" + playList.size() + ")";
        Uri uri = Uri.parse(player.getFilePath());
        String title = uri.getQuery();
        if (title == null || title.isEmpty()) {
            title = uri.getLastPathSegment();
            if (title == null || title.isEmpty())
                title = getString(R.string.text_unknown_source);
        }
        builder.setContentTitle(title + info);
        builder.setContentText(player.getFilePath());
        if (onGoing)
            startForeground(NOTIFICATION_ID, builder.build());
        else
            notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    void play(int index) {
        if (index != -1 && index != currentSong) {
            currentSong = index;
            player.stop();
            player.load(playList.get(currentSong), PlaybackService.this);
            sendNotification(true);
        }
    }

    public class PlayerBinder extends Binder {
        public String getUrl() { return player.getFilePath(); }
        public String getTitle() { return player.getTitle(); }
        public int getProgress() { return player.getProgress(); }
        public int getDuration() { return player.getDuration(); }
        public PodcastPlayer.StreamingPlayerState getState() { return player.getState(); }
        public boolean playNext() { return PlaybackService.this.playNext(); }
        public boolean playPrev() { return PlaybackService.this.playPrev(); }
        public boolean add(String source) { return PlaybackService.this.add(source); }
        public ArrayList<String> getPlaylist() { return playList; }
        public void setProgress(int progress) { player.setProgress(progress); }

        public void stop() {
            player.stop();
            stopForeground(false);
        }

        public void play(int index) { PlaybackService.this.play(index); }
        public void play() {
            switch (player.getState()) {
                case PLAYING:
                    player.pause();
                    stopForeground(false);
                    sendNotification(false);
                    break;
                case PAUSED:
                    player.play();
                    startForeground(NOTIFICATION_ID, builder.build());
                    break;
                case ERROR:
                case STOPPED:
                    if (playList != null) {
                        player.load(playList.get(currentSong), PlaybackService.this);
                        startForeground(NOTIFICATION_ID, builder.build());
                    }
                    break;
            }
        }
    }
}
