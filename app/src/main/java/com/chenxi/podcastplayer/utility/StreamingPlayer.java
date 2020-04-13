package com.chenxi.podcastplayer.utility;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class StreamingPlayer {
    String filePath;
    MediaPlayer mediaPlayer;
    private StreamingPlayerState state;

    public enum StreamingPlayerState {
        ERROR,
        PLAYING,
        PAUSED,
        STOPPED
    }

    StreamingPlayer() {
        this.state = StreamingPlayerState.STOPPED;
    }

    public StreamingPlayerState getState() {
        return this.state;
    }

    void load(String filePath) {
        this.filePath = filePath;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try{
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
        } catch (IOException | IllegalArgumentException e) {
            Log.e("StreamingPlayer", e.toString());
            e.printStackTrace();
            this.state = StreamingPlayerState.ERROR;
            return;
        }

        this.state = StreamingPlayerState.PLAYING;
        mediaPlayer.start();
    }

    public String getFilePath() {
        return this.filePath;
    }

    public int getProgress() {
        if(mediaPlayer!=null) {
            if(this.state == StreamingPlayerState.PAUSED || this.state == StreamingPlayerState.PLAYING)
                return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void play() {
        if(this.state == StreamingPlayerState.PAUSED) {
            mediaPlayer.start();
            this.state = StreamingPlayerState.PLAYING;
        }
    }

    public void pause() {
        if(this.state == StreamingPlayerState.PLAYING) {
            mediaPlayer.pause();
            state = StreamingPlayerState.PAUSED;
        }
    }

    public void stop() {
        if(mediaPlayer!=null) {
            if(mediaPlayer.isPlaying())
                mediaPlayer.stop();
            state = StreamingPlayerState.STOPPED;
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
