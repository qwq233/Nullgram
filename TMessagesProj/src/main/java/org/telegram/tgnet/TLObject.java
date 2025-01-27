/*
 * Copyright (C) 2019-2025 qwq233 <qwq233@qwq2333.top>
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

package org.telegram.tgnet;

public class TLObject {

    public int networkType;

    public boolean disableFree = false;
    private static final ThreadLocal<NativeByteBuffer> sizeCalculator = new ThreadLocal<NativeByteBuffer>() {
        @Override
        protected NativeByteBuffer initialValue() {
            return new NativeByteBuffer(true);
        }
    };

    public TLObject() {}

    public void readParams(InputSerializedData stream, boolean exception) {

    }

    public void serializeToStream(OutputSerializedData stream) {

    }

    public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
        return null;
    }

    public void freeResources() {

    }

    public int getObjectSize() {
        NativeByteBuffer byteBuffer = sizeCalculator.get();
        byteBuffer.rewind();
        serializeToStream(sizeCalculator.get());
        return byteBuffer.length();
    }
}
