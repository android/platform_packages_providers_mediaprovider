/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.providers.media.tests;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.ServiceManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import com.android.providers.media.MtpService;

/**
 * Verify that MediaProvider has required permissions.
 *
 * The test does so by attempting to run with the same key,
 * shared id, and process. This grants it identical selinux
 * permissions, but not necessarily platform permissions.
 */
public class MediaProviderPermissionsTest extends AndroidTestCase {

    Context mContext;

    @Override
    protected void setUp() throws Exception {
        mContext = getContext();
        super.setUp();
    }

    /**
     * Test that MediaProvider can access surfaceflinger services.
     */
    @MediumTest
    public void testAccessSurfaceFlinger() {
        try {
            IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
            if (surfaceFlinger == null) {
                fail("Unable to find surfaceflinger service");
            }
        } catch (SecurityException e) {
            fail("Unable to find surfaceflinger - securityexception");
        }
    }

    /**
     * Test that MediaProvider can access the mtp service.
     */
    @MediumTest
    public void testAccessMtpService() {
        try {
            Intent intent = new Intent(mContext, MtpService.class);
            // TODO: If this test is granted MANAGE_USERS, restore this case
            /* ComponentName cName = mContext.startService(intent);
            if (cName == null) {
                fail("Unable to find mtp service");
            }
            mContext.stopService(new Intent(mContext, MtpService.class));*/
        } catch (SecurityException e) {
            fail("Unable to find mtp service - securityexception");
        }
    }

    /**
     * Test that MediaProvider can access MTP driver files.
     */
    @MediumTest
    public void testAccessMtp() {
        File dev_mtp = new File("/dev/mtp_usb");
        File ffs_mtp = new File("/dev/usb-ffs/mtp");

        // TODO: If this test is granted ACCESS_MTP,
        // add tests to check if files are writable.
        if (dev_mtp.exists()) {
            return;
        } else if (ffs_mtp.exists()) {
            return;
        }
        fail("Unable to access at least one type of MTP driver");
    }
}

