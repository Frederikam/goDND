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

package frederikam.com.godnd.dnd;

// Cite: http://stackoverflow.com/questions/14087322/enabling-and-disabling-vibration-in-android-programmatically

import android.media.AudioManager;

@SuppressWarnings("deprecation")
class AudioManagerCompat {
    final static int VIBRATE_TYPE_RINGER = AudioManager.VIBRATE_TYPE_RINGER;
    final static int VIBRATE_TYPE_NOTIFICATION = AudioManager.VIBRATE_TYPE_NOTIFICATION;
    final static int VIBRATE_SETTING_ON = AudioManager.VIBRATE_SETTING_ON;
    final static int VIBRATE_SETTING_OFF = AudioManager.VIBRATE_SETTING_OFF;
    final static int VIBRATE_SETTING_ONLY_SILENT = AudioManager.VIBRATE_SETTING_ONLY_SILENT;

    static int getVibrateSetting(AudioManager am, int vibrateType) {
        return am.getVibrateSetting(vibrateType);
    }

    static void setVibrateSetting(AudioManager am, int vibrateType, int vibrateSetting) {
        am.setVibrateSetting(vibrateType, vibrateSetting);
    }
}
