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

package com.frederikam.godnd.physics;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.frederikam.godnd.GoDND;

import java.util.LinkedList;
import java.util.Queue;

import static com.frederikam.godnd.MainActivity.TAG;

class MotionTracker implements SensorEventListener {

    private static final double G = 9.82;

    private final Queue<Double> motion = new LinkedList<>();
    private final int minHistory;
    private final int maxHistory;
    private long lastEventSavedTime = 0;
    private long sleepInterval;

    private double[] curVelocity = {0, 0, 0}; // 0m/s is a good guess
    private long lastUpdate = System.nanoTime();
    private long start = System.currentTimeMillis();

    MotionTracker(int sleepInterval, int maxHistory, int sensorType) {
        this.sleepInterval = sleepInterval;
        this.minHistory = maxHistory/4;
        this.maxHistory = maxHistory;

        SensorManager mSensorManager = (SensorManager) GoDND.getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensor = mSensorManager.getDefaultSensor(sensorType);

        mSensorManager.registerListener(this, mSensor, sleepInterval);
    }

    double getAverageVelocity() {
        // Returns a negative number if we have too little history
        if (motion.size() < minHistory) {
            return Double.MIN_VALUE;
        }

        double total = 0;
        LinkedList<Double> list = new LinkedList<>(motion);
        for (Double d : list) {
            total += d;
        }

        return total / motion.size();
    }

    private void addMotion(double magnitude) {
        motion.add(magnitude);

        Log.i(TAG, "test " + magnitude);

        if(motion.size() > maxHistory) {
            // Clear old data
            motion.remove();
        }

        lastEventSavedTime = System.currentTimeMillis();

        // Debug!
        /*try {
            MainActivity activity = MainActivity.getInstance();

            if(activity != null) {
                TextView tv = (TextView) activity.findViewById(R.id.debugText);
                tv.setVisibility(View.VISIBLE);
                tv.setText(Math.floor(magnitude*100)/100 + ":" + Math.floor(getAverageVelocity()*100)/100);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error while debugging motion", ex);
        }*/
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long nanoTime = System.nanoTime();
        double nanoDiffInSeconds = ((double)(nanoTime - lastUpdate))/1000000000;

        curVelocity[0] = curVelocity[0] + (double)(event.values[0]) * nanoDiffInSeconds;
        curVelocity[1] = curVelocity[1] + (double)(event.values[1]) * nanoDiffInSeconds;
        curVelocity[2] = curVelocity[2] + (double)(event.values[2]) * nanoDiffInSeconds;

        lastUpdate = nanoTime;

        // Make sure we're not adding to the queue too fast
        if(System.currentTimeMillis() - lastEventSavedTime < sleepInterval)
            return;

        double vel = Math.sqrt(
                Math.pow(curVelocity[0], 2)
                + Math.pow(curVelocity[1], 2)
                + Math.pow(curVelocity[2], 2)
        );

        // Attempt to eliminate gravity
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double secondsSinceStart = ((double) (start - System.currentTimeMillis()))/1000;
            vel -= G * secondsSinceStart;
            Log.i(TAG, "VEL "+(G * secondsSinceStart));
        }

        addMotion(vel);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }
}
