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

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.Settings.System;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;

import libcore.io.IoUtils;

public class DefaultSoundResourceInitializer {
    private final static String TAG = "DefaultSoundResourceInitializer";
    private static final boolean DEBUG = false;

    private final Context mContext;

    private static final String INTERNAL_DIR = Environment.getRootDirectory() + "/media";

    DefaultSoundResourceInitializer(Context context) {
        mContext = context;
    }

    void copyAndSetResources() {
        copyResourceAndSetAsSound(R.raw.default_ringtone,
                System.RINGTONE,
                Environment.DIRECTORY_RINGTONES);
        copyResourceAndSetAsSound(R.raw.default_notification_sound,
                System.NOTIFICATION_SOUND,
                Environment.DIRECTORY_NOTIFICATIONS);
        copyResourceAndSetAsSound(R.raw.default_alarm_alert,
                System.ALARM_ALERT,
                Environment.DIRECTORY_ALARMS);
    }

    boolean areSettingsSet() {
        return wasAlreadySet(System.RINGTONE) &&
                wasAlreadySet(System.NOTIFICATION_SOUND) &&
                wasAlreadySet(System.ALARM_ALERT);
    }

    /**
     * If the resource contains any data, copy a resource to the file system, scan it, and set the
     * file URI as the default for a sound.
     *
     * @param name Name of setting corresponding to sound.
     * @param type type of sound.
     */
    void copyResourceAndSetAsSound(int id, String name, String subPath) {
        File destDir = Environment.getExternalStoragePublicDirectory(subPath);
        if (!destDir.exists() && !destDir.mkdirs()) {
            Log.w(TAG, "can't create " + destDir.getAbsolutePath());
            return;
        }

        File dest = new File(destDir, "default_" + name + ".ogg");

        try (
            InputStream is = mContext.getResources().openRawResource(id);
            FileOutputStream os = new FileOutputStream(dest);
        ) {
            if (is.available() > 0) {
                if (DEBUG) Log.d(TAG, "copying resource " + name + " to " + dest.getAbsolutePath());
                FileUtils.copy(is, os);
                if (DEBUG) Log.d(TAG, "copied " + name + " to " + dest.getAbsolutePath());

                Uri uri = scanFile(dest);
                if (DEBUG) Log.d(TAG, "Scanned " + name + " to " + uri);

                if (uri != null) {
                    set(name, uri);
                    if (DEBUG) Log.d(TAG, "set " + uri + " as default " + name);
                }
            } else {
                if (DEBUG) Log.d(TAG, "Resource for " + name + " has no overlay");
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to copy resource for " + name + ": " + e);
        }
    }

    private Uri scanFile(File file) {
        SynchronousQueue<Uri> queue = new SynchronousQueue<>();

        if (DEBUG) Log.d(TAG, "Scanning " + file.getAbsolutePath());
        MediaScannerConnection.scanFile(mContext, new String[] { file.getAbsolutePath() }, null,
                new OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                if (uri == null) {
                    file.delete();
                    return;
                }
                try {
                    queue.put(uri);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Unable to put new Uri in queue", e);
                }
            }
        });

        try {
            return queue.take();
        } catch (InterruptedException e) {
            Log.e(TAG, "Unable to take new Uri from queue", e);
        }

        return null;
    }

    /**
     * Set the default URI for a sound.
     *
     * @param name Name of setting corresponding to sound.
     * @param URI content URI of the sound file.
     */
    private void set(String name, Uri uri) {
        final Uri settingUri = System.getUriFor(name);
        RingtoneManager.setActualDefaultRingtoneUri(mContext,
                RingtoneManager.getDefaultType(settingUri), uri);

        System.putInt(mContext.getContentResolver(), settingSetIndicatorName(name), 1);
    }

    private String settingSetIndicatorName(String base) {
        return base + "_set";
    }

    /** Check if the default URI for a sound has already been set.
     *
     * @param name Name of setting corresponding to sound.
     * @return true if default is already set.
     */
    private boolean wasAlreadySet(String name) {
        String indicatorName = settingSetIndicatorName(name);
        try {
            return System.getInt(mContext.getContentResolver(), indicatorName) != 0;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }
}
