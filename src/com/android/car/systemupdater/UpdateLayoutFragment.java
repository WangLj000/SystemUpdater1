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
package com.android.car.systemupdater;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItem;
import androidx.fragment.app.Fragment;

//import com.android.car.ui.core.CarUi;
//import com.android.car.ui.toolbar.MenuItem;
//import com.android.car.ui.toolbar.ProgressBarController;
//import com.android.car.ui.toolbar.ToolbarController;
import com.android.internal.util.Preconditions;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/** Display update state and progress. */
public class UpdateLayoutFragment extends Fragment implements UpFragment {
    public static final String EXTRA_RESUME_UPDATE = "resume_update";

    private static final String TAG = "UpdateLayoutFragment";
    private static final String EXTRA_UPDATE_FILE = "extra_update_file";
    private static final int PERCENT_MAX = 100;
    private static final String REBOOT_REASON = "reboot-ab-update";
    private static final String NOTIFICATION_CHANNEL_ID = "update";
    private static final int NOTIFICATION_ID = 1;

    private TextView mContentTitle;
    private TextView mContentInfo;
    private TextView mContentDetails;
    private File mUpdateFile;
    private Toolbar mToolbar;
    private ProgressBar mProgressBar;
    private PowerManager mPowerManager;
    private NotificationManager mNotificationManager;
    private final UpdateVerifier mPackageVerifier = new UpdateVerifier();
    private final UpdateEngine mUpdateEngine = new UpdateEngine();
    private boolean mInstallationInProgress = false;

    private final CarUpdateEngineCallback mCarUpdateEngineCallback = new CarUpdateEngineCallback();

    /** Create a {@link UpdateLayoutFragment}. */
    public static UpdateLayoutFragment getInstance(File file) {
        UpdateLayoutFragment fragment = new UpdateLayoutFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_UPDATE_FILE, file.getAbsolutePath());
        fragment.setArguments(bundle);
        return fragment;
    }

    /** Create a {@link UpdateLayoutFragment} showing an update in progress. */
    public static UpdateLayoutFragment newResumedInstance() {
        UpdateLayoutFragment fragment = new UpdateLayoutFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_RESUME_UPDATE, true);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getArguments().getBoolean(EXTRA_RESUME_UPDATE)) {
            mUpdateFile = new File(getArguments().getString(EXTRA_UPDATE_FILE));
        }
        mPowerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        mNotificationManager =
                (NotificationManager) getContext().getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(
                new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        getContext().getString(R.id.system_update_auto_content_title),
                        NotificationManager.IMPORTANCE_DEFAULT));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.system_update_auto_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mContentTitle = view.findViewById(R.id.system_update_auto_content_title);
        mContentInfo = view.findViewById(R.id.system_update_auto_content_info);
        mContentDetails = view.findViewById(R.id.system_update_auto_content_details);
        mProgressBar = view.findViewById(R.id.progress);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
//        mToolbar = CarUi.requireToolbar(getActivity());
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.VISIBLE);
        showStatus(R.string.verify_in_progress);

        if (getArguments().getBoolean(EXTRA_RESUME_UPDATE)) {
            // Rejoin the update already in progress.
            showInstallationInProgress();
        } else {
            // Extract the necessary information and begin the update.
            mPackageVerifier.execute(mUpdateFile);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mPackageVerifier != null) {
            mPackageVerifier.cancel(true);
        }
    }

    /** Update the status information. */
    private void showStatus(@StringRes int status) {
        mContentTitle.setText(status);
        if (mInstallationInProgress) {
            mNotificationManager.notify(NOTIFICATION_ID, createNotification(getContext(), status));
        } else {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void showStatus(@StringRes int status,int errorCode) {
        mContentTitle.setText(getText(status)+" ErrorCode: " + errorCode);
        if (mInstallationInProgress) {
            mNotificationManager.notify(NOTIFICATION_ID, createNotification(getContext(), status));
        } else {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }

    /** Show the install now button. */
    private void showInstallNow(UpdateParser.ParsedUpdate update) {
//        mContentTitle.setText(R.string.install_ready);
//        mContentInfo.append(getString(R.string.update_file_name, mUpdateFile.getName()));
//        mContentInfo.append(System.getProperty("line.separator"));
//        mContentInfo.append(getString(R.string.update_file_size));
//        mContentInfo.append(Formatter.formatFileSize(getContext(), mUpdateFile.length()));
//        mContentDetails.setText(null);
//        MenuItem installButton = ActionMenuItem.builder(getActivity())
//                .setTitle(R.string.install_now)
//                .setOnClickListener(i -> installUpdate(update))
//                .build();
//        mToolbar.setMenuItems(Collections.singletonList(installButton));
    }

    /** Reboot the system. */
    private void rebootNow() {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "Rebooting Now.");
        }
        mPowerManager.reboot(REBOOT_REASON);
    }

    /** Attempt to install the update that is copied to the device. */
    private void installUpdate(UpdateParser.ParsedUpdate parsedUpdate) {
        showInstallationInProgress();
        String convertPath = null;
        if (Build.VERSION.SDK_INT >= 30) { // Build.VERSION_CODES.R
            Log.d(TAG, "installPackage SDK version >= Build.VERSION_CODES.R,should convert update file path.");
            if (parsedUpdate.mUrl.startsWith("file:///storage/emulated/0")) {
                convertPath = parsedUpdate.mUrl.replace("/storage/emulated/0", "/data/media/0");
            } else if (parsedUpdate.mUrl.startsWith("file:///storage")) {
                convertPath = parsedUpdate.mUrl.replace("/storage", "/mnt/media_rw");
            }
            Log.d(TAG, "installPackage update " + parsedUpdate.mUrl + " to " + convertPath);
            mUpdateEngine.applyPayload(
                    convertPath, parsedUpdate.mOffset, parsedUpdate.mSize, parsedUpdate.mProps);
        }else {
            mUpdateEngine.applyPayload(
                    parsedUpdate.mUrl, parsedUpdate.mOffset, parsedUpdate.mSize, parsedUpdate.mProps);
        }
    }

    /** Set the layout to show installation progress. */
    private void showInstallationInProgress() {
//        mInstallationInProgress = true;
        mProgressBar.setIndeterminate(false);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setMax(PERCENT_MAX);
//        mToolbar.setMenuItems(null); // Remove install button
//        showStatus(R.string.install_in_progress);

        mUpdateEngine.bind(mCarUpdateEngineCallback, new Handler(getContext().getMainLooper()));
    }

    /** Attempt to verify the update and extract information needed for installation. */
    private class UpdateVerifier extends AsyncTask<File, Void, UpdateParser.ParsedUpdate> {

        @Override
        protected UpdateParser.ParsedUpdate doInBackground(File... files) {
            Preconditions.checkArgument(files.length > 0);
            File file = files[0];
            try {
                return UpdateParser.parse(file);
            } catch (IOException e) {
                Log.e(TAG, String.format("For file %s", file), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(UpdateParser.ParsedUpdate result) {
            mProgressBar.setVisibility(View.GONE);
            if (result == null) {
                showStatus(R.string.verify_failure);
                return;
            }
            if (!result.isValid()) {
                showStatus(R.string.verify_failure);
                Log.e(TAG, String.format("Failed verification %s", result));
                return;
            }
            if (Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, result.toString());
            }

            //showInstallNow(result);
            installUpdate(result);
        }
    }

    /** Handles events from the UpdateEngine. */
    public class CarUpdateEngineCallback extends UpdateEngineCallback {

        @Override
        public void onStatusUpdate(int status, float percent) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, String.format("onStatusUpdate %d, Percent %.2f", status, percent));
            }
            switch (status) {
                case UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT:
                    rebootNow();
                    break;
                case UpdateEngine.UpdateStatusConstants.DOWNLOADING:
                    mProgressBar.setProgress((int) (percent * 100));
                    Log.e("longjiang","percent"+percent * 100);
                    break;
                default:
                    // noop
            }
        }

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            Log.w(TAG, String.format("onPayloadApplicationComplete %d", errorCode));
            mInstallationInProgress = false;
            showStatus(errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS
                    ? R.string.install_success
                    : R.string.install_failed,errorCode);
            mProgressBar.setVisibility(View.GONE);
//            mToolbar.setMenuItems(null); // Remove install now button
        }
    }

    /** Build a notification to show the installation status. */
    private static Notification createNotification(Context context, @StringRes int contents) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, SystemUpdaterActivity.class));
        intent.putExtra(EXTRA_RESUME_UPDATE, true);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        /* requestCode= */ 0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(context.getString(contents))
                .setSmallIcon(R.drawable.update)
                .setContentIntent(pendingIntent)
                .setShowWhen(false)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
    }
}
