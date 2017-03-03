package com.nxp.androidthings.airquality;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Decoder.DataBlock;
import Decoder.Recognizer;

/**
 * Created by b07211 on 12/26/16.
 */

public class AudioInputUtils extends Thread implements Recognizer.RecognizerStateChangedListener {
    private static final String TAG = "AudioInputUtils";
    private static final int frequency = 16000;
    private static final int channel = AudioFormat.CHANNEL_IN_MONO;
    private static final int encoding = AudioFormat.ENCODING_PCM_16BIT;
    private static final int bufferSize = 1024;

    private static boolean mStopAudioProcess = false;
    private WifiAPInfoListener mWifiAPInfoListener;
    private BlockingQueue<DataBlock> blockingQueue;
    private Recognizer mRecognizer;

    public AudioInputUtils(WifiAPInfoListener listener)
    {
        blockingQueue = new LinkedBlockingQueue<DataBlock>();
        mWifiAPInfoListener = listener;
        mRecognizer = new Recognizer(blockingQueue, this);
    }

    @Override
    public void run() {

        int minBufferSize = AudioRecord.getMinBufferSize(frequency, channel, encoding);
        Log.i(TAG, "AudioRecord.getMinBufferSize: " + minBufferSize);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                frequency, channel, encoding, minBufferSize);
        short[] buffer = new short[bufferSize];
        audioRecord.startRecording();

        try
        {
            while(!mStopAudioProcess)
            {
                int bufferReadSize = audioRecord.read(buffer, 0, bufferSize); //60ms
                DataBlock dataBlock = new DataBlock(buffer, bufferSize, bufferReadSize);
                blockingQueue.put(dataBlock);
            }
        }
        catch(Throwable x)
        {
            Log.w(TAG, "Error reading voice audio", x);
        }
        finally
        {
            audioRecord.stop();
            audioRecord.release();
            Log.i(TAG, "Thread finished!");
        }
    }

    public void startAudioProcess() {
        mStopAudioProcess = false;
        start();
        mRecognizer.startProcess();
    }

    public void stopAudioProcess() {
        mStopAudioProcess = true;
    }

    public static boolean getStopState() { return mStopAudioProcess; }

    public interface WifiAPInfoListener {
        public void onReceiveWifiInfo();
        public void onFinishedReceiveWifiInfo(String ssid, String pwd);
    }

    @Override
    public void onRecognizerStart() {
        mWifiAPInfoListener.onReceiveWifiInfo();
    }

    @Override
    public void onRecognizerFinished(String ssid, String pwd) {
        mWifiAPInfoListener.onFinishedReceiveWifiInfo(ssid, pwd);
        mRecognizer.stopProcess();
    }
}
