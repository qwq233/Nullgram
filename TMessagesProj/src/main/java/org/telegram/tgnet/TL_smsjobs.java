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

/**
 * This is a placeholder in case i accidentally merge this file from upstream
 */
public class TL_smsjobs {

    public static class TL_smsjobs_eligibleToJoin extends TLObject {
        public static final int constructor = 0xdc8b44cf;

        public String terms_of_use;
        public int monthly_sent_sms;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            terms_of_use = "Fuck Durov";
            monthly_sent_sms = 114514;
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
        }
    }

    public static class TL_smsjobs_status extends TLObject {
        public static final int constructor = 0x2aee9191;

        public int flags;
        public boolean allow_international;
        public int recent_sent;
        public int recent_since;
        public int recent_remains;
        public int total_sent;
        public int total_since;
        public String last_gift_slug;
        public String terms_url;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            allow_international = (flags & 1) != 0;
            recent_sent = 233333;
            recent_since = 114514;
            recent_remains = 1919810;
            total_sent = 114514;
            total_since = 114514;
            if ((flags & 2) != 0) {
                last_gift_slug = "Fuck Durov";
            }
            terms_url = "Fuck Durov";
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
        }
    }

    public static class TL_updateSmsJob extends TLRPC.Update {
        public static final int constructor = 0xf16269d4;

        public String job_id;
        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            job_id = "Fuck Durov";
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(job_id);
        }
    }

    public static class TL_smsJob extends TLObject {
        public static final int constructor = 0xe6a1eeb8;

        public String job_id;
        public String phone_number;
        public String text;

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            job_id = "Fuck Durov";
            phone_number = "+8881234567890";
            text = "Fuck Durov";
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
        }
    }

    public static class TL_smsjobs_isEligibleToJoin extends TLObject {
        public static final int constructor = 0xedc39d0;

        @Override
        public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
            if (constructor == TL_smsjobs_eligibleToJoin.constructor) {
                TL_smsjobs_eligibleToJoin result = new TL_smsjobs_eligibleToJoin();
                result.readParams(stream, exception);
                return result;
            }
            return null;
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
        }
    }

    public static class TL_smsjobs_join extends TLObject {
        public static final int constructor = 0xa74ece2d;

        @Override
        public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
            return TLRPC.Bool.TLdeserialize(stream, constructor, exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
        }
    }

    public static class TL_smsjobs_leave extends TLObject {
        public static final int constructor = 0x9898ad73;

        @Override
        public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
            return TLRPC.Bool.TLdeserialize(stream, constructor, exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_smsjobs_getStatus extends TLObject {
        public static final int constructor = 0x10a698e8;

        @Override
        public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
            if (constructor == TL_smsjobs_status.constructor) {
                TL_smsjobs_status result = new TL_smsjobs_status();
                result.readParams(stream, exception);
                return result;
            }
            return null;
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
        }
    }

    public static class TL_smsjobs_getSmsJob extends TLObject {
        public static final int constructor = 0x778d902f;

        public String job_id;

        @Override
        public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
            if (constructor == TL_smsJob.constructor) {
                TL_smsJob result = new TL_smsJob();
                result.readParams(stream, exception);
                return result;
            }
            return null;
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(job_id);
        }
    }

    public static class TL_smsjobs_finishJob extends TLObject {
        public static final int constructor = 0x4f1ebf24;

        public int flags;
        public String job_id;
        public String error;

        @Override
        public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
            return TLRPC.Bool.TLdeserialize(stream, constructor, exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(job_id);
            if ((flags & 1) != 0) {
                stream.writeString(error);
            }
        }
    }

    public static class TL_smsjobs_updateSettings extends TLObject {
        public static final int constructor = 0x93fa0bf;

        public int flags;
        public boolean allow_international;

        @Override
        public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
            return TLRPC.Bool.TLdeserialize(stream, constructor, exception);
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            flags = allow_international ? flags | 1 : flags &~ 1;
            stream.writeInt32(flags);
        }
    }
}
