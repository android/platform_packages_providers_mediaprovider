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
import android.support.test.filters.SmallTest;
import android.support.test.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verify that MediaProvider has required permissions.
 *
 * The test does so by attempting to run with the same key,
 * shared id, and process. This grants it identical selinux
 * permissions, but not necessarily platform permissions.
 */
@RunWith(JUnit4.class)
@SmallTest
public class MediaProviderPermissionsTest {

    Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getContext();
    }

    /**
     * Test that MediaProvider can access surfaceflinger services.
     */
    @Test
    @SmallTest
    public void testAccessSurfaceFlinger() {
        try {
            IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
            Assert.assertNotNull(surfaceFlinger);
        } catch (SecurityException e) {
            Assert.fail("Unable to find surfaceflinger - securityexception");
        }
    }

    /**
     * Test that MediaProvider can access MTP driver files.
     */
    @Test
    @SmallTest
    public void testAccessMtp() {
        File dev_mtp = new File("/dev/mtp_usb");
        File ffs_mtp = new File("/dev/usb-ffs/mtp");

        // TODO: If this test is granted ACCESS_MTP,
        // add tests to check if files are writable.
        Assert.assertTrue(dev_mtp.exists() || ffs_mtp.exists());
    }
}

