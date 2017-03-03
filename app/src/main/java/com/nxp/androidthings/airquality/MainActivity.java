/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nxp.androidthings.airquality;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Skeleton of the main Android Things activity. Implement your device's logic
 * in this class.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 *
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 */
public class MainActivity extends Activity
        implements ButtonUtils.ButtonPressedListener,
        AudioInputUtils.WifiAPInfoListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int LONG_PRESSED_TIMER = 3000;//3s
    private static final int LED_BLINK_QUICK_INTERVAL = 100;//5hz
    private static final int LED_BLINK_SLOW_INTERVAL = 250; //2hz
    private static final String ip = "10.192.241.142";
    private static final int port = 6000;

    private boolean mKeyPressed;
    private long mTimePressed;
    private AudioInputUtils mAudioInputUtils;
    private ButtonUtils mButtonUtils;
    private LEDUtils mLEDUtils;
    private Sensor mSensor;
    private NetworkUtils mNetworkUtils;
    private TimerTask timerTask;
    private Timer timer;

    private Handler mHandler = new Handler();

    Thread socketThread;
    PrintWriter out;

    MediaPlayer mediaPlayer;
    boolean state = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mButtonUtils =  new ButtonUtils(this);
        mButtonUtils.init();
        mLEDUtils = new LEDUtils(mHandler);
        mLEDUtils.init();
        mSensor = new Sensor();
        mSensor.init();
        mNetworkUtils = new NetworkUtils((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE));

        mAudioInputUtils = new AudioInputUtils(this);

        if (mNetworkUtils.getCurrentNetworkState()) {
            mLEDUtils.onLED();
            mNetworkUtils.connectServer(ip, port);
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    int a , b;
                    a = mSensor.getPM2_5();
                    b = mSensor.getPM10();
                    Log.i(TAG, "SendData: " + a + ' ' + b);
                    mNetworkUtils.sendData(a, b);
                }
            };
            timer.schedule(timerTask, 5000, 5000);
        } else {
            mLEDUtils.offLED();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mButtonUtils.unInit();
        mLEDUtils.unInit();
        mSensor.unInit();
    }

    @Override
    public void onReceiveWifiInfo() {
        mLEDUtils.stopBlink();
        mLEDUtils.startBlink(LED_BLINK_SLOW_INTERVAL);
    }

    @Override
    public void onFinishedReceiveWifiInfo(String ssid, String pwd) {
        Log.d(TAG, "onWifiAPInfoDectected ssid="+ssid+", pwd="+pwd);
        mAudioInputUtils.stopAudioProcess();
        NetworkUtils.setupWifiConnection(this, ssid, pwd);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLEDUtils.stopBlink();
                if (mNetworkUtils.getCurrentNetworkState()) {
                    mLEDUtils.onLED();
                } else {
                    mLEDUtils.offLED();
                }
            }
        }, 5000);

    }

    private Runnable mAutoKeyMonitorRunnable = new Runnable() {
        @Override public void run() {
        if(mKeyPressed) {
            long currentTime = System.currentTimeMillis();
            if(currentTime > (mTimePressed + 3000)) {
                //Long pressed caught
                //fast Blink the LED
                Log.d(TAG, "Get long key pressed");
                mLEDUtils.startBlink(LED_BLINK_QUICK_INTERVAL);

                //Start audio input process
                //start the WIFI AP Info parse from audio input
                mAudioInputUtils.startAudioProcess();
            }
        }
        }
    };

    @Override
    public void onButtonPressed() {

        Log.d(TAG, "Button Pressed");
        mKeyPressed = true;
        mTimePressed = System.currentTimeMillis();
        //Start timer to check whether the key been long pressed
        mHandler.removeCallbacks(mAutoKeyMonitorRunnable);
        mHandler.postDelayed(mAutoKeyMonitorRunnable, LONG_PRESSED_TIMER);
    }

    @Override
    public void onButtonReleased() {
        Log.d(TAG, "Button Released");
        if(mKeyPressed) {
            mHandler.removeCallbacks(mAutoKeyMonitorRunnable);
        }
        mKeyPressed = false;
        mTimePressed = 0;
    }
}
