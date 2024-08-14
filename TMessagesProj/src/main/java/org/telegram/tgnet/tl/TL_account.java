/*
 * Copyright (C) 2019-2024 qwq233 <qwq233@qwq2333.top>
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

package org.telegram.tgnet.tl;

import org.telegram.tgnet.AbstractSerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public class TL_account {

    public static class contentSettings extends TLObject {
        public static final int constructor = 0x57e28221;

        public int flags;
        public boolean sensitive_enabled;
        public boolean sensitive_can_change;

        public static contentSettings TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (contentSettings.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_account.contentSettings", constructor));
                } else {
                    return null;
                }
            }
            contentSettings result = new contentSettings();
            result.readParams(stream, exception);
            return result;
        }

        @Override
        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            sensitive_enabled = (flags & 1) != 0;
            sensitive_can_change = (flags & 2) != 0;
        }

        @Override
        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = sensitive_enabled ? (flags | 1) : (flags &~ 1);
            flags = sensitive_can_change ? (flags | 2) : (flags &~ 2);
            stream.writeInt32(flags);
        }
    }

    public static class setContentSettings extends TLObject {
        public static final int constructor = 0xb574b16b;

        public int flags;
        public boolean sensitive_enabled;

        @Override
        public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
            return TLRPC.Bool.TLdeserialize(stream, constructor, exception);
        }

        @Override
        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = sensitive_enabled ? (flags | 1) : (flags &~ 1);
            stream.writeInt32(flags);
        }
    }

    public static class getContentSettings extends TLObject {
        public static final int constructor = 0x8b9b4dae;

        @Override
        public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
            return contentSettings.TLdeserialize(stream, constructor, exception);
        }

        @Override
        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

}
