package com.y9pro.v5;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;

/** Muted, looping, full-screen (center-crop) video background from res/raw. Pauses with the activity. */
final class CoverVideoView extends TextureView implements TextureView.SurfaceTextureListener {
    private MediaPlayer mp;
    private Surface surface;
    private int rawId, vw, vh;
    private boolean prepared, wantPlay = true;

    CoverVideoView(Context c) {
        super(c);
        setSurfaceTextureListener(this);
        setOpaque(false);
    }

    void setSource(int rawResId) {
        rawId = rawResId;
        if (surface != null) start();
    }

    private void start() {
        release();
        if (rawId == 0 || surface == null) return;
        try {
            mp = new MediaPlayer();
            mp.setSurface(surface);
            AssetFileDescriptor afd = getResources().openRawResourceFd(rawId);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setLooping(true);
            mp.setVolume(0f, 0f);
            mp.setOnVideoSizeChangedListener((m, w, h) -> { vw = w; vh = h; cover(); });
            mp.setOnPreparedListener(m -> { prepared = true; if (wantPlay) m.start(); });
            mp.setOnErrorListener((m, what, extra) -> true); // never crash because of the video
            mp.prepareAsync();
        } catch (Exception e) {
            release();
        }
    }

    void pause() {
        wantPlay = false;
        if (mp != null && prepared && mp.isPlaying()) mp.pause();
    }

    void resume() {
        wantPlay = true;
        if (mp != null && prepared && !mp.isPlaying()) mp.start();
    }

    void release() {
        prepared = false;
        if (mp != null) {
            try { mp.release(); } catch (Exception ignored) { }
            mp = null;
        }
    }

    /** TextureView stretches the video to the view; undo the stretch and crop = "cover". */
    private void cover() {
        int w = getWidth(), h = getHeight();
        if (vw == 0 || vh == 0 || w == 0 || h == 0) return;
        float s = Math.max(w / (float) vw, h / (float) vh);
        Matrix m = new Matrix();
        m.setScale(vw * s / w, vh * s / h, w / 2f, h / 2f);
        setTransform(m);
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) { surface = new Surface(st); start(); }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) { cover(); }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
        release();
        if (surface != null) { surface.release(); surface = null; }
        return true;
    }

    @Override public void onSurfaceTextureUpdated(SurfaceTexture st) { }
}
