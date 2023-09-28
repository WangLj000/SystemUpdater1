/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.car.systemupdater.UpdateLayoutFragment.EXTRA_RESUME_UPDATE;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//import com.android.car.ui.core.CarUi;
//import com.android.car.ui.toolbar.Toolbar;
//import com.android.car.ui.toolbar.ToolbarController;

import java.io.File;

/**
 * Apply a system update using an ota package on internal or external storage.
 */
public class SystemUpdaterActivity extends AppCompatActivity
        implements DeviceListFragment.SystemUpdater {

    private static final String FRAGMENT_TAG = "FRAGMENT_TAG";
    private static final int STORAGE_PERMISSIONS_REQUEST_CODE = 0;
    private static final String[] REQUIRED_STORAGE_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_MEDIA_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUIRED_STORAGE_PERMISSIONS,
                    STORAGE_PERMISSIONS_REQUEST_CODE);
        }

        setContentView(R.layout.activity_main);


//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setTitle(getString(R.string.title));
        //toolbar.setState(Toolbar.State.SUBPAGE);

        if (savedInstanceState == null) {
            Bundle intentExtras = getIntent().getExtras();
            if (intentExtras != null && intentExtras.getBoolean(EXTRA_RESUME_UPDATE)) {
                UpdateLayoutFragment fragment = UpdateLayoutFragment.newResumedInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.device_container, fragment, FRAGMENT_TAG)
                        .commitNow();
            } else {
//                DeviceListFragment fragment = new DeviceListFragment();
//                getSupportFragmentManager().beginTransaction()
//                        .replace(R.id.device_container, fragment, FRAGMENT_TAG)
//                        .commitNow();
            }
        }
        intoFileManager();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            UpFragment upFragment =
                    (UpFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
            if (!upFragment.goUp()) {
                onBackPressed();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (STORAGE_PERMISSIONS_REQUEST_CODE == requestCode) {
            if (grantResults.length == 0) {
                finish();
            }
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
            }
        }
    }

    @Override
    public void applyUpdate(File file) {
        UpdateLayoutFragment fragment = UpdateLayoutFragment.getInstance(file);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.device_container, fragment, FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    private void intoFileManager() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//无类型限制
//        有类型限制是这样的:
//        intent.setType(“image/*”);//选择图片
//        intent.setType(“audio/*”); //选择音频
//        intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
//        intent.setType(“video/*;image/*”);//同时选择视频和图片

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }

    String path;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())){//使用第三方应用打开
                path = uri.getPath();
                Log.d("queryfilepath", "返回结果1: " + path);
                return;
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                path = FileChooseUtil.getPath(this, uri);
                Log.d("queryfilepath", "返回结果2: " + path);

            } else {//4.4以下下系统调用方法
                path = FileChooseUtil.getRealPathFromURI(uri);
                Log.d("queryfilepath", "返回结果3: " + path);

            }
            applyUpdate(new File(path));
        }

    }
}
