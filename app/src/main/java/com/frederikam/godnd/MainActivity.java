/*
 *  Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.frederikam.godnd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.frederikam.godnd.dnd.DNDHandler;
import com.frederikam.godnd.physics.MotionManager;
import com.frederikam.godnd.physics.MotionManagerEmulator;

import java.lang.ref.WeakReference;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, Button.OnClickListener {

    public static final String TAG = "com.frederikam.godnd";
    public static final int PERMISSION_REQUEST_DND_POLICY = 1000;

    // http://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in-the-emulator
    public static final boolean IS_EMULATOR = Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk".equals(Build.PRODUCT);

    private static WeakReference<MainActivity> instance;
    private DNDHandler dndHandler = new DNDHandler();
    private TextView textStatus = null;
    private ToggleButton toggleButton = null;
    private Button passengerButton = null;
    private TextView passengerText = null;
    private MotionManager motionManager = null;
    private boolean isPassengerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        PackageManager PM= this.getPackageManager();
        if(PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            Toast toast = Toast.makeText(getApplicationContext(), "This device does not support the required sensors", Toast.LENGTH_LONG);
            toast.show();
        }

        Log.i(TAG, "Creating activity: " + toString());

        instance = new WeakReference<>(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textStatus = (TextView) findViewById(R.id.textStatus);

        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(this);

        passengerButton = (Button) findViewById(R.id.passengerButton);
        passengerButton.setOnClickListener(this);

        passengerText = (TextView) findViewById(R.id.passengerText);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            findViewById(R.id.warningMuteText).setVisibility(View.VISIBLE);
        }



        toggleButton.setPressed(getPreferences(MODE_PRIVATE).getBoolean("isEnabled", true));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            handlePermissions();
        } else {
            startMotionSensors();
        }

        render();
    }

    public void onMotionChanged(boolean inMotion) {
        isPassengerMode = false;
        render();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Toggle button
        getPreferences(MODE_PRIVATE).edit().putBoolean("isEnabled", isChecked).apply();
        render();
    }

    @Override
    public void onClick(View v) {
        // Passenger button
        isPassengerMode = !isPassengerMode;
        ((Button) v).setText(isPassengerMode ? "Exit passenger mode" : "Enter passenger mode");
        render();
    }

    @SuppressLint("SetTextI18n")
    private void render() {
        // Passenger mode, motion change or disable/disable
        boolean inMotion = motionManager.isInMotion();
        boolean isEnabled = toggleButton.isChecked();

        boolean enableDnd = false;

        Log.i(TAG, "Render: inMotion:"+inMotion + " isEnabled:"+isEnabled + " isPassengerMode"+isPassengerMode);

        if (isEnabled) {
            textStatus.setVisibility(View.VISIBLE);
            passengerButton.setVisibility(inMotion ? View.VISIBLE : View.INVISIBLE);
            passengerText.setVisibility(inMotion ? View.VISIBLE : View.INVISIBLE);

            if(!inMotion) {
                textStatus.setText("You are not in motion. Move around for a few seconds and you" +
                        " the app will enable do not disturb mode.");
            } else if (!isPassengerMode) {
                textStatus.setText("You are now in motion and incoming SMS and calls have been" +
                        " disabled. Turn on passenger to temporarily disable the app.");
                enableDnd = true;
            } else {
                textStatus.setText("Passenger mode enabled.");
            }
        } else {
            textStatus.setVisibility(View.INVISIBLE);
            passengerButton.setVisibility(View.INVISIBLE);
            passengerText.setVisibility(View.INVISIBLE);
        }

        dndHandler.handle(enableDnd);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void handlePermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            startMotionSensors();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, PERMISSION_REQUEST_DND_POLICY);

        }
    }

    private void startMotionSensors() {
        if(motionManager != null) return;

        // The emulator does not support our sensors :/
        if(!IS_EMULATOR) {
            motionManager = new MotionManager();
            motionManager.start();
        } else {
            motionManager = new MotionManagerEmulator((Button) findViewById(R.id.emulatorMotion));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.i(TAG, "Permission results: " + requestCode + " - " + Arrays.asList(permissions) + ":"
            + Arrays.asList(grantResults));

        /*
        boolean cont = false;

        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];

            switch (perm) {
                case Manifest.permission.ACCESS_NOTIFICATION_POLICY:
                    if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        cont = true;
                    } else {
                        Log.e(TAG, "Not granted DND permission! Result: " + grantResults[i]);
                    }
                    break;
                default:
                    Log.w(TAG, "Got unexpected permission result:" + perm + ":" + grantResults[i])
                    break;
            }
        }*/

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            startMotionSensors();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "GoDND needs permission to change your DND setting to work.", Toast.LENGTH_LONG);
            toast.show();

            finish();
        }
    }

    @Nullable
    public static MainActivity getInstance() {
        return instance.get();
    }
}
