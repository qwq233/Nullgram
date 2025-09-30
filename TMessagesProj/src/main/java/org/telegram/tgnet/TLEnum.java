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

package org.telegram.tgnet;

public class TLEnum {
    public interface Constructor {
        int getConstructor();

        default void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(getConstructor());
        }
    }

    public static <E extends Enum<E> & Constructor> E TLdeserialize(Class<E> enumClass, int constructor, boolean exception) {
        final E result = fromConstructor(enumClass, constructor);

        if (result == null) {
            if (exception) {
                throw new RuntimeException(String.format("can't parse magic %x in %s", constructor, enumClass.getName()));
            }

            return null;
        }

        return result;
    }

    public static <E extends Enum<E> & Constructor> E fromConstructor(Class<E> enumClass, int constructor) {
        E[] enums = enumClass.getEnumConstants();
        if (enums == null) {
            return null;
        }

        for (E e : enums) {
            if (e.getConstructor() == constructor) {
                return e;
            }
        }
        return null;
    }
}
