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

package frederikam.com.godnd;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

import javax.vecmath.Vector3d;

public class MotionTracker implements SensorEventListener {

    private static final Logger log = LoggerFactory.getLogger(MotionTracker.class);

    private final Queue<Double> motion = new LinkedList<>();
    private final int minHistory;
    private final int maxHistory;
    private long lastEventSavedTime = 0;
    private long sleepInterval;

    public MotionTracker(int sleepInterval, int maxHistory) {
        this.sleepInterval = sleepInterval;
        this.minHistory = maxHistory/4;
        this.maxHistory = maxHistory;

        SensorManager mSensorManager = (SensorManager) MainActivity.INSTANCE.getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mSensorManager.registerListener(this, mSensor, sleepInterval);
    }

    public double getAverageMotion() {
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Make sure we're not getting events too fast
        if(lastEventSavedTime - System.currentTimeMillis() > sleepInterval)
            return;

        // Cast to doubles
        double[] doubles = new double[3];

        for (int i = 0; i < 3; i++) {
            doubles[i] = event.values[i];
        }
        
        Vector3d vec = new Vector3d(doubles);
        motion.add(vec.length());

        if(motion.size() > maxHistory) {
            // Clear old data
            motion.remove();
        }

        lastEventSavedTime = System.currentTimeMillis();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }
}
