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

package frederikam.com.godnd.physics;

import android.util.Log;

import frederikam.com.godnd.MainActivity;

import static frederikam.com.godnd.MainActivity.TAG;

public class MotionManager extends Thread {

    private static final int MOTION_THRESHOLD_HIGH = 2; // m/s
    private static final int MOTION_THRESHOLD_LOW = 1; // m/s
    private static final int SLEEP_INTERVAL = 500; // ms

    private MotionTracker tracker = new MotionTracker(500, 30);
    protected boolean inMotion = false;

    public MotionManager() {
        setDaemon(true);
        setName("MotionManager");
    }

    @Override
    public void run() {
        Log.i(TAG, "Started " + getName());
         while (true) {
             try {
                 sleep(SLEEP_INTERVAL);
             } catch (InterruptedException e) {
                 throw new RuntimeException(e);
             }
         }
    }

    private void tick() {
        if (!inMotion && tracker.getAverageMotion() > MOTION_THRESHOLD_HIGH) {
            inMotion = true;
            MainActivity.INSTANCE.onMotionChanged(true);
        } else if (inMotion && tracker.getAverageMotion() < MOTION_THRESHOLD_LOW) {
            inMotion = false;
            MainActivity.INSTANCE.onMotionChanged(false);
        }
    }

    public boolean isInMotion() {
        return inMotion;
    }
}
