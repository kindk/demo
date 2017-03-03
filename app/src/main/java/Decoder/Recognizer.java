package Decoder;

import android.util.Log;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

/**
 * Created by yjd on 2/22/17.
 */

public class Recognizer extends Thread {
    private static final String TAG = "Recognizer";
    private final int prefixLength = 5;
    private final byte prefixCode = 0;
    private final byte divideCode = 15;

    private boolean mExitPending = false;
    private BlockingQueue<DataBlock> mBlockingQueue;
    private RecognizerStateChangedListener mRecognizerStateChangedListener;
    private boolean isStartReceive = false;
    private boolean isReceiving = false;



    private final int dtmfCodeMaxLength = 500; //TODO
    private byte[] dtmfCode;
    private int dtmfIndex = 0;

    private final int recvCodeMaxLength = 200; //TODO
    private byte[] recvCode;
    private int recvIndex = 0;

    private final int ssidCodeMaxLength = 100; //TODO
    private char[] ssidCode;
    private int ssidIndex;

    private final int pwdCodeMaxLength = 100; //TODO
    private char[] pwdCode;
    private int pwdIndex;

    private int ssidStartIndex = 0;
    private int ssidEndIndex = 0;
    private int pwdStartIndex = 0;
    private int pwdEndIndex = 0;


    public Recognizer(BlockingQueue<DataBlock> blockingQueue,
                      RecognizerStateChangedListener listener) {
        this.mBlockingQueue = blockingQueue;
        mRecognizerStateChangedListener = listener;
        dtmfCode = new byte[dtmfCodeMaxLength];
        recvCode = new byte[recvCodeMaxLength];
        ssidCode = new char[ssidCodeMaxLength];
        pwdCode = new char[pwdCodeMaxLength];
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    }

    public void startProcess() {
        mExitPending = false;
        start();
    }

    public void stopProcess() {
        mExitPending = true;
    }

    private boolean exitPending() {
        return mExitPending;
    }

    public interface RecognizerStateChangedListener {
        public void onRecognizerStart();
        public void onRecognizerFinished(String ssid, String pwd);
    }


    private void log(String str) {
        //Log.i(TAG, str);
    }

    @Override
    public void run() {
        log("start recognizer!");
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!isStartReceive && isReceiving) { //Start receive Sth.
                    isStartReceive = true;
                    mRecognizerStateChangedListener.onRecognizerStart();
                    log("Start receive sth.");
                } else if (isStartReceive && !isReceiving) {
                    //Receive finished
                    //Data:   dtmfCode[]
                    //Length: dtmfIndex
                    isStartReceive = false;

                    dtmfCode2Digital(dtmfCode, dtmfIndex);
                    //Now, dtmfCode include many duplicate code than original code
                    // as recording rate is not same as tone playing rate

                    //哨兵,Only for finding the pwdEndIndex, not equal to 0
                    dtmfCode[dtmfIndex++] = 1;

                    parse();
                    dtmfIndex = 0;
                }
                isReceiving = false;
            }
        };

        timer.schedule(timerTask, 1000, 1000);

        while (!exitPending()) {
            try {
                DataBlock dataBlock = mBlockingQueue.take();
                Spectrum spectrum = dataBlock.FFT();
                spectrum.normalize();
                StatelessRecognizer statelessRecognizer = new StatelessRecognizer(spectrum);
                char key = statelessRecognizer.getRecognizedKey();

                if (key != ' ') {
                    isReceiving = true;
                    dtmfCode[dtmfIndex++] = (byte)key;
                    if (dtmfIndex >= dtmfCodeMaxLength) {
                        dtmfIndex = 0;
                    }
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "BlockingQueue task execption");
            }
        }
    }

    private void parse() {
        //Start parse duplicate dtmfCode to recvCode
        recvCode[0] = dtmfCode[0]; //TODO clear recvCode
        recvIndex = 1;

        ssidStartIndex = 0;
        ssidEndIndex = 0;
        pwdStartIndex = 0;
        pwdEndIndex = 0;

        byte prevCode;
        byte currCode;
        prevCode = dtmfCode[0];
        int repeatedCnt = 1;
        for (int i = 1; i < dtmfIndex; i++) {
            currCode = dtmfCode[i];

            if (currCode == prevCode) {
                repeatedCnt ++;
            } else {
                if (prevCode == prefixCode && repeatedCnt >= prefixLength) {
                    //Now meet ssid start or pwd start
                    //At DTMF_Encoder, there are 5 prefixCode
                    for (int j = 0; j < 4; j++) {
                        recvCode[recvIndex++] = prefixCode;
                    }

                    if (ssidStartIndex == 0) {
                        ssidStartIndex = recvIndex;
                    } else if (pwdStartIndex == 0) {
                        ssidEndIndex = recvIndex - 6;
                        pwdStartIndex = recvIndex;
                    } else if (pwdEndIndex == 0) {
                        pwdEndIndex = recvIndex - 6;
                    } else {
                        log("Wrong Index");
                    }
                } else if (repeatedCnt == 1) {
                    //If we get a code which only occurs once, suppose it
                    //is a wrong code and discards it.
                    //Because sampling rate is much larger than tone playing rate,
                    //every code we received should duplicate 3 or 4 times.
                    //Of course, this related to tone playPeriod, playInternal,
                    //and record sleep time.
                    Log.w(TAG, "=====Index: " + recvIndex + " " + i + " code: " + prevCode);

                    recvCode[recvIndex-1] = currCode;
                    prevCode = currCode;
                    repeatedCnt = 1;

                    continue;
                }


                prevCode = currCode;
                repeatedCnt = 1;

                recvCode[recvIndex++] = currCode;
            }
        }

        log("recvCode Index: " + ssidStartIndex + " " + ssidEndIndex + " " + pwdStartIndex + " " + pwdEndIndex);
        log(Arrays.toString(recvCode));


        // recvCode -------> ssidCode
        ssidIndex = 0;
        byte h = 0,l = 0;
        int k = 0;
        for (int i = ssidStartIndex; i <= ssidEndIndex; i++) {
            if (recvCode[i] == divideCode) {
                continue;
            }

            if (k % 2 == 0) {
                h = recvCode[i];
            } else {
                l = recvCode[i];
                ssidCode[ssidIndex++] = (char)(h * 15 + l);
            }
            k++;
        }
        log("ssidCode length: " + ssidIndex);
        log(new String(ssidCode));

        pwdIndex = 0;
        h = 0;
        l = 0;
        k = 0;
        for (int i = pwdStartIndex; i <= pwdEndIndex; i++) {
            if (recvCode[i] == 15) {
                continue;
            }

            if (k % 2 == 0) {
                h = recvCode[i];
            } else {
                l = recvCode[i];
                pwdCode[pwdIndex++] = (char)(h * 15 + l);
            }
            k++;
        }
        log("pwdCode length: " + pwdIndex);
        log(new String(pwdCode));

        String strSSID = new String(ssidCode, 0, ssidIndex);
        String strPWD = new String(pwdCode, 0, pwdIndex);

        mRecognizerStateChangedListener.onRecognizerFinished(strSSID, strPWD);

        int i;
        for (i = 0; i < ssidIndex; i++) {
            ssidCode[i] = 0;
        }
        for (i = 0; i < pwdIndex; i++) {
            pwdCode[i] = 0;
        }
    }

    private void dtmfCode2Digital(byte[] code, int length) {

        for (int i = 0; i < length; i++) {
            if (code[i] >= '0' && code[i] <= '9') {
                code[i] -= '0';
            }

            if (code[i] >= 'A' && code[i] <= 'D') {
                code[i] = (byte) (code[i] - 'A' + 12);
            }

            if (code[i] == '*') {
                code[i] = 10;
            }

            if (code[i] == '#') {
                code[i] = 11;
            }
        }

        log("(dtmfCode2Digital) Digital Code Length:" + length);
        log(Arrays.toString(code));
    }
}
