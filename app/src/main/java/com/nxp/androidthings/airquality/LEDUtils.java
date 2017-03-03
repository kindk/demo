package com.nxp.androidthings.airquality;

import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Created by b07211 on 12/26/16.
 */

public class LEDUtils {
    private static final String TAG = LEDUtils.class.getSimpleName();

    private Gpio mLedGpio;
    private Handler mHandler;
    private boolean mCurrentState = false;
    private int mInterval;//ms

    public LEDUtils(Handler handler)
    {
        mHandler = handler;
    }

    public void init() {
        PeripheralManagerService service = new PeripheralManagerService();
        try {
            String pinName = BoardDefaults.getGPIOForLED();
            Log.i(TAG, "getGPIOForLED:"+pinName);
            mLedGpio = service.openGpio(pinName);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            offLED();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    public void unInit() {
        // Remove pending blink Runnable from the handler.
        mHandler.removeCallbacks(mBlinkRunnable);
        // Close the Gpio pin.
        Log.i(TAG, "Closing LED GPIO pin");
        try {
            mLedGpio.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        } finally {
            mLedGpio = null;
        }
    }

    public boolean startBlink(int interval) {
        mInterval = interval;
        // Post a Runnable that continuously switch the state of the GPIO, blinking the
        // corresponding LED
        mHandler.post(mBlinkRunnable);
        return true;
    }

    public void setBlinkInterval(int interval) {
        mInterval = interval;
    }

    public boolean stopBlink() {
        mHandler.removeCallbacks(mBlinkRunnable);
        offLED();
        return true;
    }

    public boolean onLED() {
        try {
            mCurrentState = true;
            mLedGpio.setValue(mCurrentState);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean offLED() {
        try {
            mCurrentState = false;
            mLedGpio.setValue(mCurrentState);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if the GPIO is already closed
            if (mLedGpio == null) {
                return;
            }
            // Toggle the GPIO state
            if(mCurrentState)
                offLED();
            else
                onLED();

            // Reschedule the same runnable in {#INTERVAL_BETWEEN_BLINKS_MS} milliseconds
            mHandler.postDelayed(mBlinkRunnable, mInterval);

        }
    };
}





