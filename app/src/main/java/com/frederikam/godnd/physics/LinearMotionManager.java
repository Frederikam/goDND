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

import com.frederikam.godnd.MainActivity;

public class LinearMotionManager extends MotionManager {

    private static final double MOTION_THRESHOLD_HIGH = 3; // m/s
    private static final double MOTION_THRESHOLD_LOW = 1; // m/s

    private MotionTracker tracker = new LinearMotionTracker(500, 30);
    private boolean inMotion = false;

    public LinearMotionManager() {
        super("LinearMotionManager");
    }

    @Override
    void tick() {
        MainActivity activity = MainActivity.getInstance();

        if (!inMotion && tracker.getAverageMotion() > MOTION_THRESHOLD_HIGH) {
            inMotion = true;
            if(activity == null) return;
            activity.onMotionChanged(true);
        } else if (inMotion && tracker.getAverageMotion() < MOTION_THRESHOLD_LOW) {
            inMotion = false;
            if(activity == null) return;
            activity.onMotionChanged(false);
        }
    }

    public boolean isInMotion() {
        return inMotion;
    }
}
