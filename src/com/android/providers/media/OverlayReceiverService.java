/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import java.util.List;

public class OverlayReceiverService extends Service {
    private final static String TAG = "OverlayReceiverService";
    private static final boolean DEBUG = false;

    private static final String CATEGORY = "customization/aboveTheLine";
    private static final int MSG_START = 1;
    private static final int MSG_OVERLAY_CHANGED = 2;

    private IOverlayManager mOverlayManager;

    private volatile Handler mServiceHandler;
    private volatile Looper mServiceLooper;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "onReceive: " + intent != null ? intent.getAction() : "null");

            if (intent != null && Intent.ACTION_OVERLAY_CHANGED.equals(intent.getAction())) {
                Message msg = mServiceHandler.obtainMessage(MSG_OVERLAY_CHANGED);
                mServiceHandler.sendMessage(msg);
            }
        }
    };

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "created");

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        // Reduce priority below other background threads to avoid interfering
        // with other services at boot time.
        HandlerThread thread = new HandlerThread("RingtoneInitializerService",
                Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_LESS_FAVORABLE);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        if (DEBUG) Log.d(TAG, "started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_OVERLAY_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mBroadcastReceiver, filter);

        Message msg = mServiceHandler.obtainMessage(MSG_START);
        mServiceHandler.sendMessage(msg);

        // Try again later if we are killed before we finish.
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
        if (mServiceLooper != null) {
            mServiceLooper.quit();
        }

        if (DEBUG) Log.d(TAG, "destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START:
                    if (DEBUG) Log.d(TAG, "MSG_START");
                    onStartCheck();
                    break;
                case MSG_OVERLAY_CHANGED:
                    if (DEBUG) Log.d(TAG, "MSG_OVERLAY_CHANGED");
                    onOverlayChanged();
                    break;
            }
        }
    }

    private void stop() {
        mServiceLooper.quit();
        mServiceLooper = null;

        unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;

        stopSelf();
        if (DEBUG) Log.d(TAG, "stopped");
    }

    private void onStartCheck() {
        DefaultSoundResourceInitializer initializer = new DefaultSoundResourceInitializer(this);

        // Initializer will copy resources that are larger than zero bytes. Call it directly on
        // start to copy and static overlay we might have, and then we'll copy any RRO later if we
        // get ACTION_OVERLAY_CHANGED.
        if (!initializer.areSettingsSet()) {
            initializer.copyAndSetResources();
        } else {
            stop();
        }
    }

    private void onOverlayChanged() {
        if (isOverlayEnabled()) {
            DefaultSoundResourceInitializer initializer = new DefaultSoundResourceInitializer(this);
            initializer.copyAndSetResources();
            stop();
        }
    }

    private boolean isOverlayEnabled() {
        if (mOverlayManager == null) {
            mOverlayManager = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
            if (mOverlayManager == null) {
                Log.w(TAG, "Failed to get OverlayManager interface");
            }
        }

        if (mOverlayManager != null) {
            try {
                String pkg = getPackageName();
                int userId = UserHandle.myUserId();
                List<OverlayInfo> infos = mOverlayManager.getOverlayInfosForTarget(pkg, userId);
                for (OverlayInfo info : infos) {
                    if (info.isEnabled() && CATEGORY.equals(info.category)) {
                        if (DEBUG) Log.d(TAG, "Overlay enabled");
                        return true;
                    }
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to get overlay infos");
            }
        }
        if (DEBUG) Log.d(TAG, "No overlay enabled");
        return false;
    }
}
