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

package org.telegram.ui.Stars;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BagRandomizer<T> {

    private final List<T> bag;
    private final List<T> shuffledBag;
    private int currentIndex;
    private final Random random;

    private T next;

    public BagRandomizer(List<T> items) {
        if (items == null) items = new ArrayList<>();
        this.bag = new ArrayList<>(items);
        this.shuffledBag = new ArrayList<>(this.bag);
        this.currentIndex = 0;
        this.random = new Random();
        reshuffle();
        next();
    }

    @Nullable
    public T next() {
        if (this.bag.isEmpty()) return null;
        T result = next;
        if (currentIndex >= shuffledBag.size()) {
            reshuffle();
        }
        next = shuffledBag.get(currentIndex++);
        return result;
    }

    @Nullable
    public T getNext() {
        return next;
    }

    private void reshuffle() {
        Collections.shuffle(shuffledBag, random);
        currentIndex = 0;
    }

}
