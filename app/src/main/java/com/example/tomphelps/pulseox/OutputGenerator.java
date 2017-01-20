package com.example.tomphelps.pulseox;

import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * Created by Tom Phelps on 5/9/2016.
 */
public class OutputGenerator {
    private static final int sampleRate = 44100;
    private static double sample[];
    private static double dutyCycle=.5;
    public static double scale=0.4;
    private static double freq=200/2;
    private static boolean m_stop = false;
    private static AudioTrack m_audioTrack;
    private static Thread m_noiseThread;
    private static double onTime=dutyCycle*sampleRate/(2*freq);
    private static int sizeInSamp= (int)(sampleRate/freq);
    private static byte generatedSnd[];


    public OutputGenerator(double freq,double scale, double dutyCycle){
        updateValues(freq,scale,dutyCycle);
    }
    public void updateValues(double freq,double scale,double dutyCycle){
        this.scale=scale;
        this.dutyCycle=dutyCycle;
        this.freq=freq/2;
        onTime=dutyCycle*sampleRate/(2*this.freq);
        sizeInSamp= (int)(sampleRate/this.freq);
        generatedSnd = new byte[2 * sizeInSamp];
        genTone();
    }
    public static void genTone(){
        System.out.println("On time: " + onTime );
        System.out.println(sizeInSamp);
        sample=new double[sizeInSamp];
        generatedSnd = new byte[2 * sizeInSamp];


        for(int i=0;i<sizeInSamp/2;i++){
            if(i<onTime){
                sample[i]=1;
            }
            else{
                sample[i]=0;
            }
        }
        for(int i=0;i<sizeInSamp/2;i++){
            if(i<onTime){
                sample[sizeInSamp/2+i]=-1 ;
            }
            else{
                sample[sizeInSamp/2+i-1]=0;
            }
        }
        generateSound();
    }
    private static void generateSound(){
//        genTone();
        int idx=0;

        for (final double dVal : sample) {
            // scale to maximum amplitude
            short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }
    public static void start() {
        m_stop = false;
        if (m_audioTrack == null) {
            try {
                int buffSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                m_audioTrack = new AudioTrack(android.media.AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, buffSize,
                        AudioTrack.MODE_STREAM);
            } catch (Exception e) {
                System.out.println("UNABLE TO START PLAY OUTPUT, STATE");
            }
        }
        m_audioTrack.setStereoVolume(0,(float)(m_audioTrack.getMaxVolume()*scale));
        System.out.println("Volume set to: "+m_audioTrack.getMaxVolume()*scale);
        m_audioTrack.write(generatedSnd, 0, generatedSnd.length);
        m_audioTrack.play();
        m_noiseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                while (!m_stop) {
//                    genTone();
                    //AudioTrack.write is locking so it wont return until all the audio data is written
                    m_audioTrack.write(generatedSnd, 0, generatedSnd.length);
                }
            }
        });
        m_noiseThread.start();
    }

    public static void stop() {
        m_stop = true;
        try {
            m_audioTrack.stop();
        } catch (Exception e) {
            System.out.println("OutputGenerator was never running");
        }
    }
}

