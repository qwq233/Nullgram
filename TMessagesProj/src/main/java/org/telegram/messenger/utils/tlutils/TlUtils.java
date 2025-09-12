/*
 * Copyright (C) 2019-2025 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.telegram.messenger.utils.tlutils;

import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;

public class TlUtils {
    public static boolean isInstance(Object obj, final Class<?>... classes) {
        if (obj == null || classes == null) return false;

        for (Class<?> cls : classes) {
            if (cls.isInstance(obj)) {
                return true;
            }
        }
        return false;
    }

    public static TLRPC.Document getGiftDocument(TL_stars.StarGift gift) {
        TLRPC.Document document = gift.sticker;
        if (gift.attributes != null && document == null)  {
            for (TL_stars.StarGiftAttribute attribute : gift.attributes) {
                if (attribute instanceof TL_stars.starGiftAttributeModel) {
                    document = ((TL_stars.starGiftAttributeModel) attribute).document;
                    break;
                }
            }
        }
        return document;
    }

    public static TLRPC.Document getGiftDocumentPattern(TL_stars.StarGift gift) {
        TLRPC.Document document = gift.sticker;
        if (gift.attributes != null && document == null)  {
            for (TL_stars.StarGiftAttribute attribute : gift.attributes) {
                if (attribute instanceof TL_stars.starGiftAttributePattern) {
                    document = ((TL_stars.starGiftAttributePattern) attribute).document;
                    break;
                }
            }
        }
        return document;
    }

    public static String getThemeEmoticonOrGiftTitle(TLRPC.ChatTheme chatTheme) {
        if (chatTheme instanceof TLRPC.TL_chatTheme) {
            return ((TLRPC.TL_chatTheme) chatTheme).emoticon;
        } else if (chatTheme instanceof TLRPC.TL_chatThemeUniqueGift) {
            return ((TLRPC.TL_chatThemeUniqueGift) chatTheme).gift.title;
        }
        return null;
    }
}
