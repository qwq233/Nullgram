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

package top.qwq2333.nullgram.translate;

import android.view.View;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LanguageDetector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class LanguageDetectorTimeout {

    public static void detectLanguage(View parent, String text, LanguageDetector.StringCallback onSuccess, LanguageDetector.ExceptionCallback onFail, AtomicBoolean waitForLangDetection, AtomicReference<Runnable> onLangDetectionDone) {
        waitForLangDetection.set(true);
        LanguageDetector.detectLanguage(
            text,
            (String lang) -> {
                onSuccess.run(lang);
                waitForLangDetection.set(false);
                if (onLangDetectionDone.get() != null) {
                    onLangDetectionDone.get().run();
                    onLangDetectionDone.set(null);
                }
            },
            (Exception e) -> {
                FileLog.e("mlkit: failed to detect language");
                if (onFail != null) onFail.run(e);
                waitForLangDetection.set(false);
                if (onLangDetectionDone.get() != null) {
                    onLangDetectionDone.get().run();
                    onLangDetectionDone.set(null);
                }
            }
        );
        parent.postDelayed(() -> {
            if (onLangDetectionDone.get() != null) {
                onLangDetectionDone.getAndSet(null).run();
            }
        }, 250);
    }
}
