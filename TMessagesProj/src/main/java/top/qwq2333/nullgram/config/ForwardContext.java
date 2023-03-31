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

package top.qwq2333.nullgram.config;

import org.telegram.messenger.MessageObject;

import java.util.ArrayList;

public interface ForwardContext {
    ForwardParams forwardParams = new ForwardParams();

    ArrayList<MessageObject> getForwardingMessages();

    default boolean forceShowScheduleAndSound() {
        return false;
    }

    default ForwardParams getForwardParams() {
        return forwardParams;
    }

    default void setForwardParams(boolean noquote, boolean nocaption) {
        forwardParams.noQuote = noquote;
        forwardParams.noCaption = nocaption;
        forwardParams.notify = true;
        forwardParams.scheduleDate = 0;
    }

    default void setForwardParams(boolean noquote) {
        forwardParams.noQuote = noquote;
        forwardParams.noCaption = false;
        forwardParams.notify = true;
        forwardParams.scheduleDate = 0;
    }

    class ForwardParams {
        public boolean noQuote = false;
        public boolean noCaption = false;
        public boolean notify = true;
        public int scheduleDate = 0;
    }
}
