package com.nxp.androidthings.airquality;

import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;
import java.util.List;

/**
 * Created by yjd on 1/10/17.
 */

public class Sensor {
    private static final String TAG = Sensor.class.getSimpleName();
    private static final int PARCEL_SIZE = 32;
    private static final int DATA_COUNT = 12;
    private static final int PARCEL_DATA_START_INDEX = 4;
    private static final int PARCEL_DATA_END_INDEX = 27;
    private static final int DATA_PM2_5_INDEX = 1; //or 4
    private static final int DATA_PM10_INDEX = 2;  //or 5
    private UartDevice mDevice;
    //0x42, 0x4d, lenH, lenL, 24byte data, version, error, crcH, crcL
    private byte[] mParcel;
    private int[] mData;

    public Sensor() {
        mParcel = new byte[PARCEL_SIZE];
        mData = new int[DATA_COUNT];
    }

    public void init() {
        PeripheralManagerService manager = new PeripheralManagerService();
        List<String> deviceList = manager.getUartDeviceList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No UART port available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
            try {
                mDevice = manager.openUartDevice(deviceList.get(0));
                Log.i(TAG, "Open Uart device successful.");
                mDevice.setBaudrate(9600);
                mDevice.setDataSize(8);
                mDevice.setParity(UartDevice.PARITY_NONE);
                mDevice.setStopBits(1);
                //mDevice.setHardwareFlowControl(UartDevice.HW_FLOW_CONTROL_NONE);
                mDevice.registerUartDeviceCallback(mUartCallBack);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void unInit() {
        if (mDevice != null) {
            mDevice.unregisterUartDeviceCallback(mUartCallBack);
            try {
                mDevice.close();
                mDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close UART device", e);
            } finally {
                mDevice = null;
            }
        }
    }

    //TODO this method cannot call: case RuntimeException:Can't create handler inside thread that has not called Looper.prepare()
    public void startProcess() {
        try {
            mDevice.registerUartDeviceCallback(mUartCallBack);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopProcess() {
        mDevice.unregisterUartDeviceCallback(mUartCallBack);
    }

    public int getPM2_5() {
        return mData[DATA_PM2_5_INDEX];
    }

    public int getPM10() {
        return mData[DATA_PM10_INDEX];
    }


    private UartDeviceCallback mUartCallBack = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            try {
                readUartBuffer(uart);
            } catch (IOException e) {
                Log.i(TAG, "readUartBuffer error", e);
            }
            return true;
        }
    };

    private int byte2int(byte b) {
        return (int)b & 0xff;
    }

    private void dump() {
        for (int i=PARCEL_DATA_START_INDEX;i<= PARCEL_DATA_END_INDEX;i++) {
            Log.i(TAG, ""+mParcel[i]);
        }
    }

    private void readUartBuffer(UartDevice uart) throws IOException {
        int count;
        while ((count = uart.read(mParcel, mParcel.length)) > 0) {

            int dataIndex;
            for (int i = 0; i < DATA_COUNT; i++) {
                dataIndex = PARCEL_DATA_START_INDEX + i * 2;
                mData[i] = byte2int(mParcel[dataIndex]) * 256 + byte2int(mParcel[dataIndex+1]);
            }
            //Log.i(TAG, "1PM:1.0 2.5 10 1.0 2.5 10(ug/m3)  0.3 0.5 1.0 2.5 5.0 10: ");
            //Log.i(TAG, Arrays.toString(mData));
        }
    }
}
