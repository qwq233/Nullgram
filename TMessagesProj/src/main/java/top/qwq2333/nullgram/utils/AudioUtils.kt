/*
 * Copyright (C) 2019-2023 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

package top.qwq2333.nullgram.utils

import android.media.AudioRecord
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import top.qwq2333.nullgram.config.ConfigManager

object AudioUtils {
    var automaticGainControl: AutomaticGainControl? =
        null
    var acousticEchoCanceler: AcousticEchoCanceler? =
        null
    var noiseSuppressor: NoiseSuppressor? =
        null

    @JvmStatic
    fun initVoiceEnhance(
        audioRecord: AudioRecord
    ) {
        if (!ConfigManager.getBooleanOrFalse(Defines.enchantAudio)) return
        if (AutomaticGainControl.isAvailable()) {
            automaticGainControl =
                AutomaticGainControl.create(
                    audioRecord.audioSessionId
                )
            automaticGainControl?.enabled =
                true
        }
        if (AcousticEchoCanceler.isAvailable()) {
            acousticEchoCanceler =
                AcousticEchoCanceler.create(
                    audioRecord.audioSessionId
                )
            acousticEchoCanceler?.enabled =
                true
        }
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor =
                NoiseSuppressor.create(
                    audioRecord.audioSessionId
                )
            noiseSuppressor?.enabled =
                true
        }
    }

    @JvmStatic
    fun releaseVoiceEnhance() {
        if (automaticGainControl != null) {
            automaticGainControl?.release()
            automaticGainControl =
                null
        }
        if (acousticEchoCanceler != null) {
            acousticEchoCanceler?.release()
            acousticEchoCanceler =
                null
        }
        if (noiseSuppressor != null) {
            noiseSuppressor?.release()
            noiseSuppressor =
                null
        }
    }

    @JvmStatic
    fun isAvailable(): Boolean {
        return AutomaticGainControl.isAvailable() || NoiseSuppressor.isAvailable() || AcousticEchoCanceler.isAvailable()
    }
}
