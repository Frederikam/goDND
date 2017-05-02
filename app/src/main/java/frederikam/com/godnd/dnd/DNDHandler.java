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

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

import frederikam.com.godnd.MainActivity;

public class DNDHandler {

    private boolean enabled = false;
    private int oldNotificationPolicy = -1;
    private int oldVolumeRing = -1;
    private int oldVolumeNotif = -1;
    private int oldVibrationSettingRinger = -1;
    private int oldVibrationSettingNotif = -1;

    public void handle(boolean enable) {
        if(enabled == enable) return;
        enabled = enable;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            handleModern(enable);
        } else {
            handleLegacy(enable);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void handleModern(boolean enable) {
        NotificationManager mNotificationManager = (NotificationManager) MainActivity.INSTANCE.getSystemService(Context.NOTIFICATION_SERVICE);
        if(enable) {
            oldNotificationPolicy = mNotificationManager.getCurrentInterruptionFilter();
            mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS);
        } else {
            mNotificationManager.setInterruptionFilter(oldNotificationPolicy);
        }
    }

    /**
     * Mute or unmute the ringer and notifications instead of going into "proper" DND
     * @param enable Whether to enable
     */
    private void handleLegacy(boolean enable) {
        AudioManager audioManager = (AudioManager) MainActivity.INSTANCE.getSystemService(Context.AUDIO_SERVICE);
        if (enable) {
            oldVolumeRing = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            oldVolumeNotif = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            oldVibrationSettingRinger = AudioManagerCompat.getVibrateSetting(audioManager, AudioManagerCompat.VIBRATE_TYPE_RINGER);
            oldVibrationSettingNotif = AudioManagerCompat.getVibrateSetting(audioManager, AudioManagerCompat.VIBRATE_TYPE_NOTIFICATION);

            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            audioManager.setStreamMute(AudioManager.STREAM_RING, true);
            audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
            AudioManagerCompat.setVibrateSetting(audioManager, AudioManagerCompat.VIBRATE_TYPE_RINGER, AudioManagerCompat.VIBRATE_SETTING_OFF);
            AudioManagerCompat.setVibrateSetting(audioManager, AudioManagerCompat.VIBRATE_TYPE_NOTIFICATION, AudioManagerCompat.VIBRATE_SETTING_OFF);
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, oldVolumeRing, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, oldVolumeNotif, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            audioManager.setStreamMute(AudioManager.STREAM_RING, false);
            audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
            AudioManagerCompat.setVibrateSetting(audioManager, AudioManagerCompat.VIBRATE_TYPE_RINGER, oldVibrationSettingRinger);
            AudioManagerCompat.setVibrateSetting(audioManager, AudioManagerCompat.VIBRATE_TYPE_NOTIFICATION, oldVibrationSettingNotif);
        }
    }
}
