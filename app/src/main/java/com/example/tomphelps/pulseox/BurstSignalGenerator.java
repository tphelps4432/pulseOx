package com.example.tomphelps.pulseox;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.os.Build;

/**
 * Created by Tom Phelps on 9/9/2016.
 */

public class BurstSignalGenerator {
    private SoundPool pool;
    private SoundPool.Builder builder;
    private Context context;
    private int rightID;
    private int leftID;
    private boolean flag = true;

    public BurstSignalGenerator(Context context){
        this.context=context;

        if(Build.VERSION.SDK_INT>=21){
            builder = new SoundPool.Builder();
            builder.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());

            builder.setMaxStreams(2);
            pool = builder.build();
        }
        else{
            pool = new SoundPool(2, AudioManager.STREAM_MUSIC,0);
        }
        rightID = pool.load(context,R.raw.right_signal,1);
        leftID = pool.load(context,R.raw.left_signal,1);
    }
    public void start(){
        System.out.println("Started Burst Signals");
        if(flag) {
            pool.play(leftID, 0, 1, 1, -1, 1);
            pool.play(rightID, 1, 0, 1, -1, 1);
        }
        else{
            pool.autoResume();
        }
    }
    public void stop(){
        System.out.println("Stopped Burst Signals");
        pool.autoPause();
    }

}