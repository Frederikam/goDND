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
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.frederikam.godnd.physics.EmulatorMotionManager;
import com.frederikam.godnd.physics.LinearMotionManager;
import com.frederikam.godnd.physics.MotionManager;
import com.frederikam.godnd.physics.NonlinearMotionManager;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, Button.OnClickListener, Button.OnLongClickListener {

    public static final String TAG = "com.frederikam.godnd";
    public static final int PERMISSION_REQUEST_DND_POLICY = 100;

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
    public static boolean hasRequestedDndAccess = false;
    private DNDHandler dndHandler = new DNDHandler();
    private TextView textStatus = null;
    private ToggleButton toggleButton = null;
    private Button passengerButton = null;
    private TextView passengerText = null;
    private MotionManager motionManager = null;
    private Button emulatorButton = null;
    private TextView warningMuteText = null;
    private boolean isPassengerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Creating activity: " + toString());

        instance = new WeakReference<>(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textStatus = (TextView) findViewById(R.id.textStatus);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        passengerButton = (Button) findViewById(R.id.passengerButton);
        passengerText = (TextView) findViewById(R.id.passengerText);
        emulatorButton = (Button) findViewById(R.id.emulatorMotion);
        warningMuteText = (TextView) findViewById(R.id.warningMuteText);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            handlePermissions();
        } else {
            startMotionSensors();
        }

        render();
    }

    @Override
    protected void onStart() {
        super.onStart();

        PackageManager pm = this.getPackageManager();
        if(!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            Toast toast = Toast.makeText(getApplicationContext(), "This device does not support the required sensors", Toast.LENGTH_LONG);
            toast.show();
        }

        passengerButton.setOnClickListener(this);
        toggleButton.setOnCheckedChangeListener(this);
        toggleButton.setPressed(getPreferences(MODE_PRIVATE).getBoolean("isEnabled", true));
        toggleButton.setChecked(getPreferences(MODE_PRIVATE).getBoolean("isEnabled", true));
        emulatorButton.setOnLongClickListener(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            warningMuteText.setVisibility(View.VISIBLE);
        } else {
            NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

            // Check if the notification policy access has been granted for the app.
            if (!mNotificationManager.isNotificationPolicyAccessGranted() && !hasRequestedDndAccess) {
                hasRequestedDndAccess = true;
                Toast toast = Toast.makeText(getApplicationContext(), "GoDND must have permission to access do not disturb.", Toast.LENGTH_LONG);
                toast.show();

                // Wait a moment to let the toast show, and to not confuse the user
                synchronized (this) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Got interrupted while sleeping", e);
                    }
                }

                Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
            }
        }


    }

    public void onMotionChanged(boolean inMotion) {
        isPassengerMode = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                render();
            }
        });
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
        // Passenger mode, motion change or enable/disable
        boolean inMotion = motionManager != null && motionManager.isInMotion();
        boolean isEnabled = toggleButton.isChecked();

        boolean enableDnd = false;

        if (isEnabled) {
            textStatus.setVisibility(View.VISIBLE);
            passengerButton.setVisibility(inMotion ? View.VISIBLE : View.INVISIBLE);
            passengerText.setVisibility(inMotion ? View.VISIBLE : View.INVISIBLE);

            if(!inMotion) {
                textStatus.setText("You are not in motion. Move around for a few seconds and" +
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
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED) {
            startMotionSensors();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY, Manifest.permission.WRITE_SETTINGS}, PERMISSION_REQUEST_DND_POLICY);
        }
    }

    private void startMotionSensors() {
        if(motionManager != null) return;

        // The emulator does not support our sensors :/
        boolean emulatePhysics = false;

        if(!IS_EMULATOR || !emulatePhysics) {
            PackageManager pm = this.getPackageManager();
            if(pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
                motionManager = new LinearMotionManager();
            } else {
                motionManager = new NonlinearMotionManager();
            }

        } else {
            motionManager = new EmulatorMotionManager(emulatorButton);
        }

        motionManager.start();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; i++) {
            Log.i(TAG, "Permission request " + requestCode + " received: " + permissions[i] + " = " + grantResults[i]);
        }

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

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED) {
            startMotionSensors();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "GoDND needs permission to change your notification settings to work.", Toast.LENGTH_LONG);
            toast.show();

            finish();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        //Long press on the emulator button. Clears to UI so we can take screenshots =D

        emulatorButton.setVisibility(View.INVISIBLE);
        warningMuteText.setVisibility(View.INVISIBLE);

        return true;
    }

    @Nullable
    public static MainActivity getInstance() {
        return instance.get();
    }

}
