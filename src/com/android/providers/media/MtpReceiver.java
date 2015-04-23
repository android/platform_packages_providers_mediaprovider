/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class MtpReceiver extends BroadcastReceiver {
    private static final String TAG = MtpReceiver.class.getSimpleName();
    private static final boolean DEBUG = false;

/* >> Create Message Looper to avoid receiver encoutner ANR.
    (database opertion-insert/delete is I/O)
    (And MediaProvider has global lock for "delete" to ensure data correctness)
*/

    private HandlerThread mHandlerThread;
    private ReceiveHandler mRecvHandler;

    public MtpReceiver() {
        super();

        mHandlerThread = new HandlerThread("MtpReceiverThread");
        mHandlerThread.start();
        mRecvHandler = new ReceiveHandler(mHandlerThread.getLooper());
    }

    @Override
    protected void finalize() throws Throwable {
        if (mHandlerThread != null)
            mHandlerThread.quitSafely();
        //quitSafely has not been tested, the purpose is to prevent receiver drop USB event -> MTP can't exit.

        super.finalize();
    }

    private final class ReceiveHandler extends Handler {
        public ReceiveHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            handleUsbState(msg);
            Log.d(TAG, "[MTP][handleMessage]-");
        }
    }

    private void handleUsbStateAsync(Context context, Intent intent) {
        Log.d(TAG, "[MTP][handleUsbStateAsync]+");
        Bundle extras = intent.getExtras();
        Message msg = mRecvHandler.obtainMessage();
        msg.setData(extras);
        msg.obj = context;
        mRecvHandler.sendMessage(msg);
        Log.d(TAG, "[MTP][handleUsbStateAsync]-");
    }

/* << - Create Message Looper */

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            final Intent usbState = context.registerReceiver(
                    null, new IntentFilter(UsbManager.ACTION_USB_STATE));
            if (usbState != null) {
                handleUsbStateAsync(context, usbState);
            }
        } else if (UsbManager.ACTION_USB_STATE.equals(action)) {
            handleUsbStateAsync(context, intent);
        }
    }

    private void handleUsbState(Message msg) {
        Log.d(TAG, "[MTP][handleUsbState]+");
        Context context = (Context) msg.obj;
        Bundle extras = msg.getData();
        boolean connected = extras.getBoolean(UsbManager.USB_CONFIGURED);
        boolean mtpEnabled = extras.getBoolean(UsbManager.USB_FUNCTION_MTP);
        boolean ptpEnabled = extras.getBoolean(UsbManager.USB_FUNCTION_PTP);
        boolean unlocked = extras.getBoolean(UsbManager.USB_DATA_UNLOCKED);

        Log.d(TAG, "connected = " + connected + ", mtpEnabled = " + mtpEnabled + ", ptpEnabled = " + ptpEnabled + ", unlocked = " + unlocked);

        // Start MTP service if USB is connected and either the MTP or PTP function is enabled
        if (connected && (mtpEnabled || ptpEnabled)) {
             Intent intent = new Intent(context, MtpService.class);
            intent.putExtra(UsbManager.USB_DATA_UNLOCKED, unlocked);
            if (ptpEnabled) {
                intent.putExtra(UsbManager.USB_FUNCTION_PTP, true);
            }
            if (DEBUG) { Log.d(TAG, "handleUsbState startService"); }
            context.startService(intent);
            // tell MediaProvider MTP is connected so it can bind to the service
            context.getContentResolver().insert(Uri.parse(
                    "content://media/none/mtp_connected"), null);
        } else {
            boolean status = context.stopService(new Intent(context, MtpService.class));
            if (DEBUG) { Log.d(TAG, "handleUsbState stopService status=" + status); }
            // tell MediaProvider MTP is disconnected so it can unbind from the service
            context.getContentResolver().delete(Uri.parse(
                    "content://media/none/mtp_connected"), null, null);
        }

        Log.d(TAG, "[MTP][handleUsbState]-");

    }
}
