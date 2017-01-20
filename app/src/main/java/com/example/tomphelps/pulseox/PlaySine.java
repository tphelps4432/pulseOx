package com.example.tomphelps.pulseox;

import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * Created by Tom Phelps on 2/18/2015.
 */

//This class just generates and plays the sine wave to power the device.
public class PlaySine {
    private static final int duration = 1; // seconds
    private static final int sampleRate = 44100;
    private static final int numSamples = duration * sampleRate;
    private static double sample[] = new double[numSamples];
    private static double freqOfTone = 16000; //16000 hz
    private static double realFreq = 0;
    private static final boolean keepPlaying = true;
    private static final byte generatedSnd[] = new byte[2 * numSamples];
    private static boolean m_stop = false;
    private static AudioTrack m_audioTrack;
    private static Thread m_noiseThread;
    private static int phaseCount = 0;

    private static double dutyCycle=.5;
    private static double scale=1;
    private static double freq=2000;
    private static double onTime=dutyCycle*sampleRate/(2*freq);
    private static int sizeInSamp= (int)(sampleRate/freq);


    private static void genTone() {
        // fill out the array
        Double samplePeriod = 2 * Math.PI / freqOfTone * sampleRate;
        int i = 0;
        while (i < numSamples) {
            sample[i] = Math.sin(((2.0 * Math.PI) * (double) (i + phaseCount) / (sampleRate / freqOfTone)));
            i++;
        }
        //the phase count variable should help to make sure the sine wave does not
        //have any sudden changes in phase.
        phaseCount = i % samplePeriod.intValue();
        // convert to 16 bit pcm sound array
        int idx = 0;
        //Turn the doubles into bytes for 16 bit PCM
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    public static boolean isRuning() {
        return m_audioTrack.getState() == AudioTrack.PLAYSTATE_PLAYING;

    }

    public static void adjustFreq(double newFreq) {
        realFreq = newFreq;
        freqOfTone = (44100 - newFreq) / 2;
    }

    public static double getFreq() {
        return realFreq;
    }

    public static void start() {

        //Starts a streaming audio track that plays a sine wave out the left side forever. m_noiseThread constantly
        //fills the buffer with the next parts of the sine wave. if the frequency is changed then the next buffer load should start
        //the at the new frequency.
        m_stop = false;
        if (m_audioTrack == null) {
            try {
                int buffSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                m_audioTrack = new AudioTrack(android.media.AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                        AudioTrack.MODE_STREAM);
                m_audioTrack.setStereoVolume(m_audioTrack.getMaxVolume(), 0);
            } catch (Exception e) {
                System.out.println("UNABLE TO START PLAY SINE, STATE: " + m_audioTrack.getState());
            }
        }
        m_audioTrack.write(generatedSnd, 0, generatedSnd.length);
        m_audioTrack.play();

        m_noiseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                while (!m_stop) {
                    genTone();
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
            System.out.println("Sine was never running");
        }
    }
}
