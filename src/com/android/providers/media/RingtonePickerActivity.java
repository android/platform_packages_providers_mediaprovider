/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.ContentProvider;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

//[TCT-ROM][Sound]Begin added by yang.sun for XRP23276 on 18-9-7
import android.content.pm.PackageManager;
import java.util.List;
import android.content.pm.ResolveInfo;
import android.content.ContentResolver;
import android.content.ContentValues;
//[TCT-ROM][Sound]End added by yang.sun for XRP23276 on 18-9-7
/**
 * The {@link RingtonePickerActivity} allows the user to choose one from all of the
 * available ringtones. The chosen ringtone's URI will be persisted as a string.
 *
 * @see RingtoneManager#ACTION_RINGTONE_PICKER
 */
public final class RingtonePickerActivity extends AlertActivity implements
        AdapterView.OnItemSelectedListener, Runnable, DialogInterface.OnClickListener,
        AlertController.AlertParams.OnPrepareListViewListener {

    private static final int POS_UNKNOWN = -1;

    private static final String TAG = "RingtonePickerActivity";

    private static final int DELAY_MS_SELECTION_PLAYED = 300;

    private static final String COLUMN_LABEL = MediaStore.Audio.Media.TITLE;

    private static final String SAVE_CLICKED_POS = "clicked_pos";

    private static final String SOUND_NAME_RES_PREFIX = "sound_name_";

    private static final int ADD_FILE_REQUEST_CODE = 300;
/[TCT-ROM][Sound]Begin added by yang.sun for XRXRP23276 on 18-9-6
    ///@}
    /// M: Request codes to MusicPicker for add more ringtone
    private static final int ADD_MORE_RINGTONES = 1;
    //[TCT-ROM][Sound]End added by yang.sun for XRXRP23276 on 18-9-6

    private RingtoneManager mRingtoneManager;
    private int mType;

    private Cursor mCursor;
    private Handler mHandler;
    private BadgedRingtoneAdapter mAdapter;

    /** The position in the list of the 'Silent' item. */
    private int mSilentPos = POS_UNKNOWN;

    /** The position in the list of the 'Default' item. */
    private int mDefaultRingtonePos = POS_UNKNOWN;

    /** The position in the list of the ringtone to sample. */
    private int mSampleRingtonePos = POS_UNKNOWN;
//[TCT-ROM][Sound]Begin add by yang.sun for XRP23276 on 18-9-6
    /** M: The position in the list of the 'More Ringtongs' item. */
    private int mMoreRingtonesPos = POS_UNKNOWN;

   /** M: The position in the list of the latest click.*/
    private int mLatestClickedPos =  POS_UNKNOWN;
    /** M: Whether this list has the 'More Ringtongs' item. */
    private boolean mHasMoreRingtonesItem = true;
//[TCT-ROM][Sound]End add by yang.sun for XRP23276 on 18-9-6

    /** Whether this list has the 'Silent' item. */
    private boolean mHasSilentItem;

    /** The Uri to place a checkmark next to. */
    private Uri mExistingUri;

    /** The number of static items in the list. */
    private int mStaticItemCount;

    /** Whether this list has the 'Default' item. */
    private boolean mHasDefaultItem;

    /** The Uri to play when the 'Default' item is clicked. */
    private Uri mUriForDefaultItem;

    /** Id of the user to which the ringtone picker should list the ringtones */
    private int mPickerUserId;

    /** Context of the user specified by mPickerUserId */
    private Context mTargetContext;

    /**
     * A Ringtone for the default ringtone. In most cases, the RingtoneManager
     * will stop the previous ringtone. However, the RingtoneManager doesn't
     * manage the default ringtone for us, so we should stop this one manually.
     */
    private Ringtone mDefaultRingtone;

    /**
     * The ringtone that's currently playing, unless the currently playing one is the default
     * ringtone.
     */
    private Ringtone mCurrentRingtone;

    /**
     * Stable ID for the ringtone that is currently checked (may be -1 if no ringtone is checked).
     */
    private long mCheckedItemId = -1;

    private int mAttributesFlags;

    private boolean mShowOkCancelButtons;

    /**
     * Keep the currently playing ringtone around when changing orientation, so that it
     * can be stopped later, after the activity is recreated.
     */
    private static Ringtone sPlayingRingtone;

    private DialogInterface.OnClickListener mRingtoneClickListener =
            new DialogInterface.OnClickListener() {

        /*
         * On item clicked
         */
        public void onClick(DialogInterface dialog, int which) {
            //[TCT-ROM][sound]Begin added by ronghui.yi for XR6628337 on 2018/09/05
 if (which == mMoreRingtonesPos) {

                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("vnd.android.cursor.dir/audio");
                        intent.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                        PackageManager packageManager = getApplicationContext().getPackageManager();
                        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
                        boolean isIntentSafe = activities.size() > 0;
                        if (isIntentSafe) {
                            startActivityForResult(intent, ADD_MORE_RINGTONES);
                        } else {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
//[TCT-ROM][Sound]End added by yang.sun for XRP23276 on 18-9-6
            }else {
                if (which == mCursor.getCount() + mStaticItemCount) {
                    // The "Add new ringtone" item was clicked. Start a file picker intent to select
                    // only audio files (MIME type "audio/*")
                    final Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                    chooseFile.setType("audio/*");
                    chooseFile.putExtra(Intent.EXTRA_MIME_TYPES,
                            new String[]{"audio/*", "application/ogg"});
                    chooseFile.putExtra(EXTRA_DRM_LEVEL, DRM_LEVEL_FL);
                    startActivityForResult(chooseFile, ADD_FILE_REQUEST_CODE);
                    return;
                }

                // Save the position of most recently clicked item
                setCheckedItem(which);
                //[TCT-ROM][Sound]Begin added by yang.sun for XRP23276 on 18-9-6
                mLatestClickedPos = which;
                //[TCT-ROM][Sound]End added by yang.sun for XRP23276 on 18-9-6
                // In the buttonless (watch-only) version, preemptively set our result since we won't
                // have another chance to do so before the activity closes.
                if (!mShowOkCancelButtons) {
                    //[TCT ROM][build error]begin modify by shie for can not find setResultFromSelection
                    //setResultFromSelection();
                    setSuccessResultWithRingtone(getCurrentlySelectedRingtoneUri());
                    //[TCT ROM][build error]begin modify by shie for can not find setResultFromSelection
                }

                // Play clip
                playRingtone(which, 0);
            }
            //[TCT-ROM][sound]End added by ronghui.yi for XR6628337 on 2018/09/05
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        Intent intent = getIntent();
        mPickerUserId = UserHandle.myUserId();
        mTargetContext = this;

        // Get the types of ringtones to show
        mType = intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1);
        initRingtoneManager();

        /*
         * Get whether to show the 'Default' item, and the URI to play when the
         * default is clicked
         */
        mHasDefaultItem = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        mUriForDefaultItem = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI);
        if (mUriForDefaultItem == null) {
            if (mType == RingtoneManager.TYPE_NOTIFICATION) {
                mUriForDefaultItem = Settings.System.DEFAULT_NOTIFICATION_URI;
            } else if (mType == RingtoneManager.TYPE_ALARM) {
                mUriForDefaultItem = Settings.System.DEFAULT_ALARM_ALERT_URI;
            } else if (mType == RingtoneManager.TYPE_RINGTONE) {
                mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
            } else {
                // or leave it null for silence.
                mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
            }
        }

        // Get whether to show the 'Silent' item
        mHasSilentItem = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        // AudioAttributes flags
        mAttributesFlags |= intent.getIntExtra(
                RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS,
                0 /*defaultValue == no flags*/);

        mShowOkCancelButtons = getResources().getBoolean(R.bool.config_showOkCancelButtons);

        // The volume keys will control the stream that we are choosing a ringtone for
        setVolumeControlStream(mRingtoneManager.inferStreamType());

        // Get the URI whose list item should have a checkmark
        mExistingUri = intent
                .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
        //[TCT-ROM][Calendar] Added by nanbing.zou for P23591 on 2018-09-14 begin
        if (mExistingUri != null) {
            if (mExistingUri.toString().contains("android.resource://com.google.android.calendar/raw")) {
                mExistingUri = Settings.System.DEFAULT_NOTIFICATION_URI;
            }
        }
        //[TCT-ROM][Calendar] Added by nanbing.zou for P23591 on 2018-09-14 end

        // Create the list of ringtones and hold on to it so we can update later.
        mAdapter = new BadgedRingtoneAdapter(this, mCursor,
                /* isManagedProfile = */ UserManager.get(this).isManagedProfile(mPickerUserId));
        if (savedInstanceState != null) {
            setCheckedItem(savedInstanceState.getInt(SAVE_CLICKED_POS, POS_UNKNOWN));
        }

        final AlertController.AlertParams p = mAlertParams;
        p.mAdapter = mAdapter;
        p.mOnClickListener = mRingtoneClickListener;
        p.mLabelColumn = COLUMN_LABEL;
        p.mIsSingleChoice = true;
        p.mOnItemSelectedListener = this;
        if (mShowOkCancelButtons) {
            p.mPositiveButtonText = getString(com.android.internal.R.string.ok);
            p.mPositiveButtonListener = this;
            p.mNegativeButtonText = getString(com.android.internal.R.string.cancel);
            p.mPositiveButtonListener = this;
        }
        p.mOnPrepareListViewListener = this;

        p.mTitle = intent.getCharSequenceExtra(RingtoneManager.EXTRA_RINGTONE_TITLE);
        if (p.mTitle == null) {
          if (mType == RingtoneManager.TYPE_ALARM) {
              p.mTitle = getString(com.android.internal.R.string.ringtone_picker_title_alarm);
          } else if (mType == RingtoneManager.TYPE_NOTIFICATION) {
              p.mTitle =
                  getString(com.android.internal.R.string.ringtone_picker_title_notification);
          } else {
              p.mTitle = getString(com.android.internal.R.string.ringtone_picker_title);
          }
        }

        setupAlert();
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_CLICKED_POS, getCheckedItem());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //[TCT-ROM][Sound]Begin added by yang.sun for XRP23276 on 18-9-6
        if (requestCode == ADD_MORE_RINGTONES && resultCode == RESULT_OK) {
        //[TCT-ROM][Sound]End added by yang.sun for XRP23276 on 18-9-6
            // Add the custom ringtone in a separate thread
            final AsyncTask<Uri, Void, Uri> installTask = new AsyncTask<Uri, Void, Uri>() {
                @Override
                protected Uri doInBackground(Uri... params) {
                    //[TCT-ROM][Sound]Begin added by yang.sun for XRP23276 on 18-9-6
                    try {

                        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            throw new IOException("External storage is not mounted. Unable to install ringtones.");
                        }
                        Uri uri = (null == data ? null : params[0]);
                        if (uri != null) {

                            // Sanity-check: are we actually being asked to install an audio file?
                            final String mimeType = mTargetContext.getContentResolver().getType(uri);
                            if (mimeType == null ||
                                    !(mimeType.startsWith("audio/") || mimeType.equals("application/ogg"))) {
                                throw new IllegalArgumentException("Ringtone file must have MIME type \"audio/*\"."
                                        + " Given file has MIME type \"" + mimeType + "\"");
                            }
                            Uri mRingtoneUri = setRingtone(mTargetContext.getContentResolver(), uri);

                            return mRingtoneUri;
                            //[TCT-ROM][Sound]End added by yang.sun for XRP23276 on 18-9-6
                        }
                    } catch (IOException | UnsupportedOperationException | IllegalArgumentException e) {
                        Log.e(TAG, "Unable to add new ringtone", e);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Uri ringtoneUri) {
                    //[TCT-ROM][Sound]Begin add by yang.sun for XRP23276 on 18-9-6
                            if (ringtoneUri != null ) {
                                requeryForAdapter(ringtoneUri);
                                Log.v(TAG, "onActivityResult: RESULT_OK,ringtoneUri so set to be ringtone! " + ringtoneUri);
        
                            } else {
                                // Ringtone was not added, display error Toast
                                Toast.makeText(RingtonePickerActivity.this, R.string.unable_to_add_ringtone,
                                        Toast.LENGTH_SHORT).show();
                            }
                    //[TCT-ROM][Sound]End add by yang.sun for XRP23276 on 18-9-6
                }
            };
            installTask.execute(data.getData());
        //[TCT-ROM][Sound]Begin add by yang.sun for XRP23276 on 18-9-6
                }else if (requestCode == ADD_MORE_RINGTONES && resultCode == RESULT_CANCELED) {
                    setLatestRingtoneClickPos(); //Added by ronghui.yi for defect 6334857 on 2018/5/28
                }
        //[TCT-ROM][Sound]End add by yang.sun for XRP23276 on 18-9-6
    }
   //[TCT-ROM][Sound]Begin add by yang.sun for XRP23276 on 18-9-6
            private void setLatestRingtoneClickPos(){
                if ((mLatestClickedPos >= mStaticItemCount || (mHasSilentItem && (mLatestClickedPos == 1
                        || mLatestClickedPos == 2))) && (POS_UNKNOWN != mLatestClickedPos)) {
                    //Added by nanbing.zou for D8679886 on 2019-12-12 begin
                    //just call mRingtoneManager.getRingtonePosition for call cursor.requery, do nothing
                    if (mExistingUri != null)
                        getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri));
                    //Added by nanbing.zou for D8679886 on 2019-12-12 end
                    setCheckedItem(mLatestClickedPos);
                } else if (mLatestClickedPos == POS_UNKNOWN) {
                    //[TCT-ROM][Sound]Begin modified by nanbing.zou for D8312660 on 2019-09-05
                    if (mExistingUri != null && !RingtoneManager.isDefault(mExistingUri)) {
                        //[TCT-ROM][Sound]End modified by nanbing.zou for D8312660 on 2019-09-05
                        mLatestClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri));
                        setCheckedItem(mLatestClickedPos);
                    } else if (mExistingUri == null || mExistingUri.toString().equals("") ) {
                        mLatestClickedPos = mSilentPos;
                        setCheckedItem(mLatestClickedPos);
                    }
                }
                setupAlert();
            }
   //[TCT-ROM][Sound]End add by yang.sun for XRP23276 on 18-9-6

    // Disabled because context menus aren't Material Design :(
    /*
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        int position = ((AdapterContextMenuInfo) menuInfo).position;

        Ringtone ringtone = getRingtone(getRingtoneManagerPosition(position));
        if (ringtone != null && mRingtoneManager.isCustomRingtone(ringtone.getUri())) {
            // It's a custom ringtone so we display the context menu
            menu.setHeaderTitle(ringtone.getTitle(this));
            menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.delete_ringtone_text);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Menu.FIRST: {
                int deletedRingtonePos = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
                Uri deletedRingtoneUri = getRingtone(
                        getRingtoneManagerPosition(deletedRingtonePos)).getUri();
                if(mRingtoneManager.deleteExternalRingtone(deletedRingtoneUri)) {
                    requeryForAdapter();
                } else {
                    Toast.makeText(this, R.string.unable_to_delete_ringtone, Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
            }
            default: {
                return false;
            }
        }
    }
    */

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        super.onDestroy();
    }

    public void onPrepareListView(ListView listView) {
        // Reset the static item count, as this method can be called multiple times
        mStaticItemCount = 0;
    
      //[TCT-ROM][Sound]Begin added by yang.sun for XRP23276 on 18-9-6
        if (mHasMoreRingtonesItem && mType != -1) {//[TCT-ROM][Sound]modified by junliang.liu for XR8120098 on 20190809
            mMoreRingtonesPos = addMoreRingtonesItem(listView);
        }
      //[TCT-ROM][Sound]End added by yang.sun for XRP23276 on 18-9-6

        if (mHasDefaultItem) {
            mDefaultRingtonePos = addDefaultRingtoneItem(listView);

            if (getCheckedItem() == POS_UNKNOWN && RingtoneManager.isDefault(mExistingUri)) {
                setCheckedItem(mDefaultRingtonePos);
            }
        }

        if (mHasSilentItem) {
            mSilentPos = addSilentItem(listView);

            // The 'Silent' item should use a null Uri
            if (getCheckedItem() == POS_UNKNOWN && mExistingUri == null) {
                setCheckedItem(mSilentPos);
            }
        }

        if (getCheckedItem() == POS_UNKNOWN) {
            setCheckedItem(getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri)));
        }

        // In the buttonless (watch-only) version, preemptively set our result since we won't
        // have another chance to do so before the activity closes.
        if (!mShowOkCancelButtons) {
            setSuccessResultWithRingtone(getCurrentlySelectedRingtoneUri());
        }
        // If external storage is available, add a button to install sounds from storage.
        //[TCT-ROM][Sound]Begin deleted by yang.sun for XRP23276 on 18-9-7
        /*if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            addNewRingtoneItem(listView);
        }*/
        //[TCT-ROM][Sound]End deleted by yang.sun for XRP23276 on 18-9-7

        // Enable context menu in ringtone items
        registerForContextMenu(listView);
    }

    /**
     * Re-query RingtoneManager for the most recent set of installed ringtones. May move the
     * selected item position to match the new position of the chosen sound.
     *
     * This should only need to happen after adding or removing a ringtone.
     */
   //[TCT-ROM][Sound]Begin modified by yang.sun for XRP23276 on 18-9-6
    private void requeryForAdapter(Uri ringtoneUri) {
        // Refresh and set a new cursor, closing the old one.
        initRingtoneManager();
        mAdapter.changeCursor(mCursor);

        // Update checked item location.
        int checkedPosition = POS_UNKNOWN;
       /* for (int i = 0; i < mAdapter.getCount(); i++) {
            if (mAdapter.getItemId(i) == mCheckedItemId) {
                checkedPosition = getListPosition(i);
                break;
            }
        }*/
        checkedPosition = getListPosition(mRingtoneManager.getRingtonePosition(ringtoneUri));
        if (mHasSilentItem && checkedPosition == POS_UNKNOWN) {
            checkedPosition = mSilentPos;
        }
        setCheckedItem(checkedPosition);
        setupAlert();
    }
      /**
       * M: Set the given uri to be ringtone
       *
       * @param resolver content resolver
       * @param uri the given uri to set to be ringtones
       */
      private Uri setRingtone(ContentResolver resolver, Uri uri) {
          /// Set the flag in the database to mark this as a ringtone
          try {
              ContentValues values = new ContentValues(1);
              if (RingtoneManager.TYPE_RINGTONE == mType) {
                  values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
              } else if (RingtoneManager.TYPE_ALARM == mType) {
                  values.put(MediaStore.Audio.Media.IS_ALARM, "1");
              } else if (RingtoneManager.TYPE_NOTIFICATION == mType) {
                  values.put(MediaStore.Audio.Media.IS_NOTIFICATION, "1");
              } else if (RingtoneManager.TYPE_SIM2_RINGTONE == mType) {
                  values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
              } else {
                  Log.e(TAG, "Unsupport ringtone type =  " + mType);
                  return null;
              }
              Log.d(TAG, "setRingtone() uri = " + uri);
              resolver.update(uri, values, null, null);
          } catch (UnsupportedOperationException ex) {
              /// most likely the card just got unmounted
              Log.e(TAG, "couldn't set ringtone flag for uri " + uri);

          }
          return uri;
      }
   //[TCT-ROM][Sound]End modified by yang.sun for XRP23276 on 18-9-6

    /**
     * Adds a static item to the top of the list. A static item is one that is not from the
     * RingtoneManager.
     *
     * @param listView The ListView to add to.
     * @param textResId The resource ID of the text for the item.
     * @return The position of the inserted item.
     */
    private int addStaticItem(ListView listView, int textResId) {
        TextView textView = (TextView) getLayoutInflater().inflate(
                com.android.internal.R.layout.select_dialog_singlechoice_material, listView, false);
        textView.setText(textResId);
        listView.addHeaderView(textView);
        mStaticItemCount++;
        return listView.getHeaderViewsCount() - 1;
    }

    private int addDefaultRingtoneItem(ListView listView) {
        if (mType == RingtoneManager.TYPE_NOTIFICATION) {
            return addStaticItem(listView, R.string.notification_sound_default);
        } else if (mType == RingtoneManager.TYPE_ALARM) {
            return addStaticItem(listView, R.string.alarm_sound_default);
        }

        return addStaticItem(listView, R.string.ringtone_default);
    }

    private int addSilentItem(ListView listView) {
        return addStaticItem(listView, com.android.internal.R.string.ringtone_silent);
    }

    private void addNewRingtoneItem(ListView listView) {
        listView.addFooterView(getLayoutInflater().inflate(R.layout.add_ringtone_item, listView,
                false /* attachToRoot */));
    }


    /**
     * M: Add more ringtone item to given listview and return it's position.
     *
     * @param listView The listview which need to add more ringtone item.
     * @return The position of more ringtone item in listview
     * */
   //[TCT-ROM][Sound]Begin added by yang.sun for XRP23276 on 18-9-6
   private int addMoreRingtonesItem(ListView listView) {
       //Modified by nanbing.zou for P23885 at 2018-09-20 begin
       View add_ringtone_view = getLayoutInflater().inflate(R.layout.add_ringtone_item, listView,
               false /* attachToRoot */);
        TextView textView = add_ringtone_view.findViewById(R.id.add_ringtone_text);
        textView.setText(R.string.add_ringtone_text);
        listView.addHeaderView(add_ringtone_view);
       //Modified by nanbing.zou for P23885 at 2018-09-20 end
        mStaticItemCount++;
        return listView.getHeaderViewsCount() - 1;
        }
   //[TCT-ROM][Sound]End added by yang.sun for XRP23276 on 18-9-6
    private void initRingtoneManager() {
        // Reinstantiate the RingtoneManager. Cursor.requery() was deprecated and calling it
        // causes unexpected behavior.
        mRingtoneManager = new RingtoneManager(this, /* includeParentRingtones */ true);
        if (mType != -1) {
            mRingtoneManager.setType(mType);
        }
        mCursor = new LocalizedCursor(mRingtoneManager.getCursor(), getResources(), COLUMN_LABEL);
    }

    private Ringtone getRingtone(int ringtoneManagerPosition) {
        if (ringtoneManagerPosition < 0) {
            return null;
        }
        return mRingtoneManager.getRingtone(ringtoneManagerPosition);
    }

    private int getCheckedItem() {
        return mAlertParams.mCheckedItem;
    }

    private void setCheckedItem(int pos) {
        mAlertParams.mCheckedItem = pos;
        mCheckedItemId = mAdapter.getItemId(getRingtoneManagerPosition(pos));
    }

    /*
     * On click of Ok/Cancel buttons
     */
    public void onClick(DialogInterface dialog, int which) {
        boolean positiveResult = which == DialogInterface.BUTTON_POSITIVE;

        // Stop playing the previous ringtone
        mRingtoneManager.stopPreviousRingtone();

        if (positiveResult) {
            setSuccessResultWithRingtone(getCurrentlySelectedRingtoneUri());
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    /*
     * On item selected via keys
     */
    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        // footer view
        if (position >= mCursor.getCount() + mStaticItemCount) {
            return;
        }

        playRingtone(position, DELAY_MS_SELECTION_PLAYED);

        // In the buttonless (watch-only) version, preemptively set our result since we won't
        // have another chance to do so before the activity closes.
        if (!mShowOkCancelButtons) {
            setSuccessResultWithRingtone(getCurrentlySelectedRingtoneUri());
        }
    }

    public void onNothingSelected(AdapterView parent) {
    }

    private void playRingtone(int position, int delayMs) {
        mHandler.removeCallbacks(this);
        mSampleRingtonePos = position;
        mHandler.postDelayed(this, delayMs);
    }

    public void run() {
        stopAnyPlayingRingtone();
        if (mSampleRingtonePos == mSilentPos) {
            return;
        }

        Ringtone ringtone;
        if (mSampleRingtonePos == mDefaultRingtonePos) {
            if (mDefaultRingtone == null) {
                mDefaultRingtone = RingtoneManager.getRingtone(this, mUriForDefaultItem);
            }
           /*
            * Stream type of mDefaultRingtone is not set explicitly here.
            * It should be set in accordance with mRingtoneManager of this Activity.
            */
            if (mDefaultRingtone != null) {
                mDefaultRingtone.setStreamType(mRingtoneManager.inferStreamType());
            }
            ringtone = mDefaultRingtone;
            mCurrentRingtone = null;
        } else {
            ringtone = mRingtoneManager.getRingtone(getRingtoneManagerPosition(mSampleRingtonePos));
            mCurrentRingtone = ringtone;
        }

        if (ringtone != null) {
            if (mAttributesFlags != 0) {
                ringtone.setAudioAttributes(
                        new AudioAttributes.Builder(ringtone.getAudioAttributes())
                                .setFlags(mAttributesFlags)
                                .build());
            }
            ringtone.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!isChangingConfigurations()) {
            stopAnyPlayingRingtone();
        } else {
            saveAnyPlayingRingtone();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isChangingConfigurations()) {
            stopAnyPlayingRingtone();
        }
    }

    private void setSuccessResultWithRingtone(Uri ringtoneUri) {
      setResult(RESULT_OK,
          new Intent().putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, ringtoneUri));
    }

    private Uri getCurrentlySelectedRingtoneUri() {
      if (getCheckedItem() == mDefaultRingtonePos) {
        // Use the default Uri that they originally gave us.
        return mUriForDefaultItem;
      } else if (getCheckedItem() == mSilentPos) {
        // Use a null Uri for the 'Silent' item.
        return null;
      } else {
        Uri uri = mRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(getCheckedItem()));
            if ("1".equals(uri.getQueryParameter(CANONICAL))) {
                return mTargetContext.getContentResolver().uncanonicalize(uri);
            } else {
                return uri;
            }
      }
    }

    private void saveAnyPlayingRingtone() {
        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            sPlayingRingtone = mDefaultRingtone;
        } else if (mCurrentRingtone != null && mCurrentRingtone.isPlaying()) {
            sPlayingRingtone = mCurrentRingtone;
        }
    }

    private void stopAnyPlayingRingtone() {
        if (sPlayingRingtone != null && sPlayingRingtone.isPlaying()) {
            sPlayingRingtone.stop();
        }
        sPlayingRingtone = null;

        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            mDefaultRingtone.stop();
        }

        if (mRingtoneManager != null) {
            mRingtoneManager.stopPreviousRingtone();
        }
    }

    private int getRingtoneManagerPosition(int listPos) {
        return listPos - mStaticItemCount;
    }

    private int getListPosition(int ringtoneManagerPos) {

        // If the manager position is -1 (for not found), return that
        if (ringtoneManagerPos < 0) return ringtoneManagerPos;

        return ringtoneManagerPos + mStaticItemCount;
    }

    private Intent getMediaFilePickerIntent() {
        final Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("audio/*");
        chooseFile.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[] { "audio/*", "application/ogg" });
        return chooseFile;
    }

    private boolean resolvesMediaFilePicker() {
        return getMediaFilePickerIntent().resolveActivity(getPackageManager()) != null;
    }

    private static class LocalizedCursor extends CursorWrapper {

        final int mTitleIndex;
        final Resources mResources;
        String mNamePrefix;
        final Pattern mSanitizePattern;

        LocalizedCursor(Cursor cursor, Resources resources, String columnLabel) {
            super(cursor);
            mTitleIndex = mCursor.getColumnIndex(columnLabel);
            mResources = resources;
            mSanitizePattern = Pattern.compile("[^a-zA-Z0-9]");
            if (mTitleIndex == -1) {
                Log.e(TAG, "No index for column " + columnLabel);
                mNamePrefix = null;
            } else {
                try {
                    // Build the prefix for the name of the resource to look up
                    // format is: "ResourcePackageName::ResourceTypeName/"
                    // (the type name is expected to be "string" but let's not hardcode it).
                    // Here we use an existing resource "notification_sound_default" which is
                    // always expected to be found.
                    mNamePrefix = String.format("%s:%s/%s",
                            mResources.getResourcePackageName(R.string.notification_sound_default),
                            mResources.getResourceTypeName(R.string.notification_sound_default),
                            SOUND_NAME_RES_PREFIX);
                } catch (NotFoundException e) {
                    mNamePrefix = null;
                }
            }
        }

        /**
         * Process resource name to generate a valid resource name.
         * @param input
         * @return a non-null String
         */
        private String sanitize(String input) {
            if (input == null) {
                return "";
            }
            return mSanitizePattern.matcher(input).replaceAll("_").toLowerCase();
        }

        @Override
        public String getString(int columnIndex) {
            final String defaultName = mCursor.getString(columnIndex);
            if ((columnIndex != mTitleIndex) || (mNamePrefix == null)) {
                return defaultName;
            }
            TypedValue value = new TypedValue();
            try {
                // the name currently in the database is used to derive a name to match
                // against resource names in this package
                mResources.getValue(mNamePrefix + sanitize(defaultName), value, false);
            } catch (NotFoundException e) {
                // no localized string, use the default string
                return defaultName;
            }
            if ((value != null) && (value.type == TypedValue.TYPE_STRING)) {
                Log.d(TAG, String.format("Replacing name %s with %s",
                        defaultName, value.string.toString()));
                return value.string.toString();
            } else {
                Log.e(TAG, "Invalid value when looking up localized name, using " + defaultName);
                return defaultName;
            }
        }
    }

    private class BadgedRingtoneAdapter extends CursorAdapter {
        private final boolean mIsManagedProfile;

        public BadgedRingtoneAdapter(Context context, Cursor cursor, boolean isManagedProfile) {
            super(context, cursor);
            mIsManagedProfile = isManagedProfile;
        }

        @Override
        public long getItemId(int position) {
            if (position < 0) {
                return position;
            }
            return super.getItemId(position);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.radio_with_work_badge, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Set text as the title of the ringtone
            ((TextView) view.findViewById(R.id.checked_text_view))
                    .setText(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));

            boolean isWorkRingtone = false;
            if (mIsManagedProfile) {
                /*
                 * Display the work icon if the ringtone belongs to a work profile. We can tell that
                 * a ringtone belongs to a work profile if the picker user is a managed profile, the
                 * ringtone Uri is in external storage, and either the uri has no user id or has the
                 * id of the picker user
                 */
                Uri currentUri = mRingtoneManager.getRingtoneUri(cursor.getPosition());
                int uriUserId = ContentProvider.getUserIdFromUri(currentUri, mPickerUserId);
                Uri uriWithoutUserId = ContentProvider.getUriWithoutUserId(currentUri);

                if (uriUserId == mPickerUserId && uriWithoutUserId.toString()
                        .startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                    isWorkRingtone = true;
                }
            }

            ImageView workIcon = (ImageView) view.findViewById(R.id.work_icon);
            if(isWorkRingtone) {
                workIcon.setImageDrawable(getPackageManager().getUserBadgeForDensityNoBackground(
                        UserHandle.of(mPickerUserId), -1 /* density */));
                workIcon.setVisibility(View.VISIBLE);
            } else {
                workIcon.setVisibility(View.GONE);
            }
        }
    }
}
