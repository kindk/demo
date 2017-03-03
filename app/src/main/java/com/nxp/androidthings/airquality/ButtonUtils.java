package com.nxp.androidthings.airquality;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Created by b07211 on 12/26/16.
 */

public class ButtonUtils {
    private static final String TAG = ButtonUtils.class.getSimpleName();

    private Gpio mButtonGpio;
    private ButtonPressedListener mButtonPressedListener;

    public ButtonUtils(ButtonUtils.ButtonPressedListener listener)
    {
        mButtonPressedListener = listener;
    }

    public void init() {
        PeripheralManagerService service = new PeripheralManagerService();
        try {
            String pinName = BoardDefaults.getGPIOForButton();
            Log.i(TAG, "getGPIOForButton:"+pinName);
            mButtonGpio = service.openGpio(pinName);
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mButtonGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    //Log.i(TAG, "GPIO changed, button pressed or released");
                    // Return true to continue listening to events
                    try {
                        if(!mButtonGpio.getValue())
                            mButtonPressedListener.onButtonPressed();
                        else
                            mButtonPressedListener.onButtonReleased();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    public void unInit() {
        if (mButtonGpio != null) {
            // Close the Gpio pin
            Log.i(TAG, "Closing Button GPIO pin");
            try {
                mButtonGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            } finally {
                mButtonGpio = null;
            }
        }
    }

    public interface ButtonPressedListener {
        public void onButtonPressed();
        public void onButtonReleased();
    }

    public boolean isButtonPressed() {
        try {
            return mButtonGpio.getValue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
