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

public interface InputSerializedData {

    boolean readBool(boolean exception);
    int readInt32(boolean exception);
    long readInt64(boolean exception);
    byte readByte(boolean exception);
    void readBytes(byte[] b, boolean exception);
    byte[] readData(int count, boolean exception);
    String readString(boolean exception);
    byte[] readByteArray(boolean exception);
    float readFloat(boolean exception);
    double readDouble(boolean exception);
    NativeByteBuffer readByteBuffer(boolean exception);

    int length();
    void skip(int count);
    int getPosition();
    int remaining();

}
