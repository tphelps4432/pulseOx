package com.example.tomphelps.pulseox;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;

import com.example.tomphelps.pulseox.PulseOxProcessing.PpgExtractor;
import com.example.tomphelps.pulseox.PulseOxProcessing.PpgWritingThread;
import com.example.tomphelps.pulseox.PulseOxProcessing.PreFilter;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Tom Phelps on 2/13/2015.
 */

/*
This class handles the interface between the audio port and the software. It gets the encoded audio data
and translates it into doubles. Then it passes the doubles to an InputDecoder to be processed.

\ */
public class MicManager {
    int sampleRate = 44100;
    public boolean isRecording = false;
    int buffSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord audioRecord;
    public ArrayList<Integer> bits = new ArrayList();
    Thread recordingThread;
    ArrayList<Double> values = new ArrayList<Double>();
    int valCount = 0;
    public static Context context;
    private ParallelWriter2 writer;

    private boolean recordData = false;
    private boolean processPPG=false;

    private File dataFile;
    private PrintWriter printOut = null;
    private boolean printWave = true;
    private PpgExtractor ppgExtractor;
    File file;
    int fileCount = 0;
    private NotchFilter notchFilter = new NotchFilter();
    private PreFilter preFilter;
    PpgWritingThread ppgThread;
    public boolean phoneTestFlag = false;

    public void stopRecording() {
        //Stops the recording, closes the audio port and terminates the recording thread.
        ppgThread.stop();
        isRecording = false;
        System.out.println("Values size: "+ ppgThread.getRedPPG().size());
        if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.stop();
            // audioRecord.release();
            System.out.println("AUDIORECORD STOPPED, State:" + audioRecord.getState());

            try {
                Thread.sleep(1000, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            audioRecord =null
            recordingThread = null;
        }
//        if(processPPG) {
//            System.out.println("PPG Stopped");
////            ppgExtractor.recordPPGStop(context);
//            processPPG=false;
//        }
        if(!phoneTestFlag)
        printWave();
    }

    public void recordData(String fileName) {
        //Create a new file and printwriter to write to the file.
        //Also creates a dir inside the phone to store your files in.
        File folder = new File(Environment.getExternalStorageDirectory() + "/PulseOxData");
        if (!folder.exists()) {
            folder.mkdir();
        }
        int fileNumba = folder.list().length+1;
        System.out.println("File Number: "+fileNumba);

        dataFile = new File(folder.getPath(), fileName + fileNumba + ".csv");
        try {
            printOut = new PrintWriter(new FileOutputStream(dataFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        writer = new ParallelWriter2(printOut);
        new Thread((writer)).start();
        recordData = true;
        startRecording(false);
    }
//    public void stopPPGRecording(){
//        ppgExtractor.recordPPGStop(context);
//    }
    public void processPPG(boolean rawData){
        processPPG=true;
//        ppgExtractor.recordPPGStart(rawData);

    }
    public void stopData() {
        recordData = false;
        writer.end();
        MediaScannerConnection.scanFile(context, new String[]{dataFile.getPath()}, null, null);
    }

    public void printFailed() {
        //Initalize file ect
        File folder = new File(Environment.getExternalStorageDirectory() + "/Waveforms");
        if (!folder.exists()) {
            System.out.println("Ran");
            folder.mkdir();
        }
        String filename = "Failed! " + fileCount;
        fileCount++;
        file = new File(folder.getPath(), filename + ".txt");
        //Write everything
        try {
            PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            System.out.println("Values size" + values.size());
            for (double value : values) {
                output.println(value + " ");
            }
            MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
            output.flush();
            output.close();
            values.clear();
            System.out.println("Done!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public synchronized void printWave() {
        //Initalize file ect
        File folder = new File(Environment.getExternalStorageDirectory() + "/PpgTests");
        if (!folder.exists()) {
            folder.mkdir();
        }
        int fileNumba = (folder.list().length+1)/3;
        String filename = "redPPG_" +OutputGenerator.scale+"_"+ fileNumba;
        file = new File(folder.getPath(), filename + ".csv");
        //Write everything
        try {
            PrintWriter output = new PrintWriter(new FileOutputStream(file));
            System.out.println("Values size" + ppgThread.getRedPPG().size());
            output.println("redPPG_"+OutputGenerator.scale+"_");
            for (double value : ppgThread.getRedPPG()) {
                output.println(value + ",");
//                System.out.print(value);
            }
            output.flush();
            output.close();
            MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
            System.out.println("Done!");
        } catch (IOException e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }

        filename = "irPPG_"+OutputGenerator.scale+"_" + fileNumba;
        file = new File(folder.getPath(), filename + ".csv");
        //Write everything
        try {
            PrintWriter output = new PrintWriter(new FileOutputStream(file));
            System.out.println("Values size" + ppgThread.getIRPPG().size());
            output.println("irPPG_"+OutputGenerator.scale+"_");
            System.out.println("Scale: "+OutputGenerator.scale);
            for (double value : ppgThread.getIRPPG()) {
                output.println(value + ",");
//                System.out.print(value);
            }
            output.flush();
            output.close();
            MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
            System.out.println("Done!");
        } catch (IOException e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }
        filename = "spo2Results " + fileNumba;
        file = new File(folder.getPath(), filename + ".csv");
        //Write everything
        try {
            PrintWriter output = new PrintWriter(new FileOutputStream(file));
            System.out.println("Values size" + ppgThread.getSp02Results().size());
            output.println("spo2_");
            for (double value : ppgThread.getSp02Results()) {
                output.println(value + ",");
            }
            output.flush();
            output.close();
            MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
            System.out.println("Done!");
        } catch (IOException e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }
        filename = "HR_Results " + fileNumba;
        file = new File(folder.getPath(), filename + ".csv");
        //Write everything
        try {
            PrintWriter output = new PrintWriter(new FileOutputStream(file));
            System.out.println("Values size" + ppgThread.getHRList().size());
            output.println("hr_");
            for (double value : ppgThread.getHRList()) {
                output.println(value + ",");
            }
            output.flush();
            output.close();
            MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
            System.out.println("Done!");
        } catch (IOException e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }
    }

    public void releaseAll() {
        audioRecord.release();
        audioRecord = null;
    }

    //    private PrintWriter output;

    public void startRecording(final boolean fsk) {
        if(!phoneTestFlag) {
            preFilter = new PreFilter();
            ppgThread = new PpgWritingThread();
            new Thread(ppgThread).start();
        }
        notchFilter=new NotchFilter();
        notchFilter.updateValues();

//        Starts the AudioRecorder and the recording thread. Once its started constantly writes any received audio to a buffer for processing.
        System.out.println("Recording");
        isRecording = true;
        values.clear();
        if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * buffSize);
        }
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000,0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    while (isRecording) {
                        if(!phoneTestFlag) {
                            writeAudioDatatoBuffer(fsk);
                        }
                        else{
                            phoneTestWrite();
                        }
                    }
                }
            });
            recordingThread.setPriority(Thread.MAX_PRIORITY);
            recordingThread.start();
        } else {
            System.out.println("Unable to get audio! State:" + audioRecord.getState());
            System.out.println("Unable to get audio! State:" + audioRecord.getState());
        }
    }
    private int writeAudioDatatoBuffer(boolean fsk) {
        byte audioBuffer[] = new byte[buffSize];
        // Changes the encoded audio data into doubles and sends them to the InputDecoder
        audioRecord.read(audioBuffer, 0, buffSize);
        String output = "";
        for (int i = 0; i < buffSize; i += 2) {
            Byte b1 = audioBuffer[i];
            Byte b2 = audioBuffer[i + 1];
            double temp = (double) (b2 << 8 | b1 & 0xFF)/32767.0;
//            double filteredTemp = notchFilter.filterValue(temp);
//            double temp2 =preFilter.filterValue(filteredTemp);
            ppgThread.add(temp);

        }
//        long time = System.nanoTime();
//        System.out.println(System.nanoTime()-time);

        return 0;
    }
    private int phoneTestWrite() {
        byte audioBuffer[] = new byte[buffSize];
        // Changes the encoded audio data into doubles and sends them to the InputDecoder
        audioRecord.read(audioBuffer, 0, buffSize);
        String output = "";
        for (int i = 0; i < buffSize; i += 2) {
            Byte b1 = audioBuffer[i];
            Byte b2 = audioBuffer[i + 1];
            double temp = (double) (b2 << 8 | b1 & 0xFF)/32767.0;
            double filteredTemp = notchFilter.filterValue(temp);
            writer.append(filteredTemp+"");
        }
//        long time = System.nanoTime();
//        System.out.println(System.nanoTime()-time);

        return 0;
    }
    private static class ParallelWriter2 implements Runnable {
    //Trying to improve the file write so that it does not interfere with the audio record thread
        private BlockingQueue<Item> q;
        private int indentation;
    private PrintWriter out;

        public ParallelWriter2(PrintWriter out ){
            q = new LinkedBlockingQueue<Item>();
            indentation = 0;
            this.out=out;
        }

        public ParallelWriter2 append( CharSequence str ){
            try {
                CharSeqItem item = new CharSeqItem();
                item.content = str;
                item.type = ItemType.CHARSEQ;
                q.put(item);
                return this;
            } catch (InterruptedException ex) {
                throw new RuntimeException( ex );
            }
        }

        public ParallelWriter2 newLine(){
            try {
                Item item = new Item();
                item.type = ItemType.NEWLINE;
                q.put(item);
                return this;
            } catch (InterruptedException ex) {
                throw new RuntimeException( ex );
            }
        }

        public void setIndent(int indentation) {
            try{
                IndentCommand item = new IndentCommand();
                item.type = ItemType.INDENT;
                item.indent = indentation;
                q.put(item);
            } catch (InterruptedException ex) {
                throw new RuntimeException( ex );
            }
        }

        public void end(){
            try {
                Item item = new Item();
                item.type = ItemType.POISON;
                q.put(item);
            } catch (InterruptedException ex) {
                throw new RuntimeException( ex );
            }
        }

        public void run() {
            Item item = null;

            try{
//                out = new PrintWriter(new BufferedWriter( new FileWriter( mfile ) ));
                while( (item = q.take()).type != ItemType.POISON ){
                    switch( item.type ){
                        case NEWLINE:
                            out.println("");
                            for( int i = 0; i < indentation; i++ )
                                out.append("   ");
                            break;
                        case INDENT:
                            indentation = ((IndentCommand)item).indent;
                            break;
                        case CHARSEQ:
                            out.println( ((CharSeqItem)item).content );
                    }
                }
            } catch (InterruptedException ex){
                throw new RuntimeException( ex );
            } catch  (Exception e) {
                throw new RuntimeException( e );
            } finally {
                if( out != null ) try {
                    out.flush();
                    out.close();
//                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                    intent.setData(Uri.fromFile(dataFile));
//                    context.sendBroadcast(intent);
                } catch (Exception ex) {
                    throw new RuntimeException( ex );
                }
            }
        }

        private enum ItemType {
            CHARSEQ, NEWLINE, INDENT, POISON;
        }
        private static class Item {
            ItemType type;
        }
        private static class CharSeqItem extends Item {
            CharSequence content;
        }
        private static class IndentCommand extends Item {
            int indent;
        }
    }
}

