/* //device/content/providers/media/src/com/android/providers/media/MediaScannerReceiver.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class MediaScannerReceiver extends BroadcastReceiver {
    private final static String TAG = "MediaScannerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final Uri uri = intent.getData();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Scan internal only.
            scan(context, MediaProvider.INTERNAL_VOLUME, null, Intent.ACTION_MEDIA_MOUNTED);
        } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            scanTranslatable(context);
        } else {
            if (uri.getScheme().equals("file")) {
                // handle intents related to external storage
                String path = uri.getPath();
                String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
                String legacyPath = Environment.getLegacyExternalStorageDirectory().getPath();

                try {
                    path = new File(path).getCanonicalPath();
                } catch (IOException e) {
                    Log.e(TAG, "couldn't canonicalize " + path);
                    return;
                }
                if (path.startsWith(legacyPath)) {
                    path = externalStoragePath + path.substring(legacyPath.length());
                }

                Log.d(TAG, "action: " + action + " path: " + path);
                if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                    // Scan internal before external
                    scan(context, MediaProvider.INTERNAL_VOLUME, null, Intent.ACTION_MEDIA_MOUNTED);
                    // scan whenever any volume is mounted
                    StorageVolume storage = (StorageVolume)intent.getParcelableExtra(
                            StorageVolume.EXTRA_STORAGE_VOLUME);
                    scan(context, MediaProvider.EXTERNAL_VOLUME, storage, action);
                } else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                    StorageVolume storage = (StorageVolume)intent.getParcelableExtra(
                            StorageVolume.EXTRA_STORAGE_VOLUME);
                    scan(context, MediaProvider.EXTERNAL_VOLUME, storage, action);
                } else if (Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action) &&
                        path != null && path.startsWith(externalStoragePath + "/")) {
                    scanFile(context, path);
                }
            }
        }
    }

    private void scan(Context context, String volume, StorageVolume storage, String action) {
        if (MediaProvider.EXTERNAL_VOLUME.equals(volume) && storage == null) {
            Log.e(TAG, "StorageVolume is mandatory for external storage scan");
            return;
        }
        Bundle args = new Bundle();
        args.putString("volume", volume);
        args.putParcelable("storage", storage);
        args.putString("action", action);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }

    private void scanFile(Context context, String path) {
        Bundle args = new Bundle();
        args.putString("filepath", path);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }

    private void scanTranslatable(Context context) {
        final Bundle args = new Bundle();
        args.putBoolean(MediaStore.RETRANSLATE_CALL, true);
        context.startService(new Intent(context, MediaScannerService.class).putExtras(args));
    }
}
