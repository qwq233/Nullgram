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

public interface OutputSerializedData {

    void writeInt32(int x);
    void writeInt64(long x);
    void writeBool(boolean value);
    void writeBytes(byte[] b);
    void writeBytes(byte[] b, int offset, int count);
    void writeByte(int i);
    void writeByte(byte b);
    void writeString(String s);
    void writeByteArray(byte[] b, int offset, int count);
    void writeByteArray(byte[] b);
    void writeFloat(float f);
    void writeDouble(double d);
    void writeByteBuffer(NativeByteBuffer buffer);

    void skip(int count);
    int getPosition();

}
