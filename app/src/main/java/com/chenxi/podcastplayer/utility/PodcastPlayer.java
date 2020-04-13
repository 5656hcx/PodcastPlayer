package com.chenxi.podcastplayer.utility;

import android.media.MediaPlayer;
import android.net.Uri;

public class PodcastPlayer extends StreamingPlayer {

    public void load(String filePath, MediaPlayer.OnCompletionListener listener) {
        super.load(filePath);
        mediaPlayer.setOnCompletionListener(listener);
    }

    public String getTitle() {
        if (getFilePath() == null)
            return null;
        Uri uri = Uri.parse(getFilePath());
        String title = uri.getQuery();
        return title == null ? Uri.parse(getFilePath()).getLastPathSegment() : title;
    }

    public int getDuration() {
        if (mediaPlayer != null && getState() != StreamingPlayerState.ERROR) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public void setProgress(int progress) {
        if (progress >= 0 && progress <= getDuration()) {
            try {
                mediaPlayer.seekTo(progress);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void setFilePath(String path) {
        if (path != null && !path.isEmpty()) {
            filePath = path;
        }
    }
}