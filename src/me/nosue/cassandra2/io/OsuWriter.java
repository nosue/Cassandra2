package me.nosue.cassandra2.io;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Class to write data to be read by osu!
 */
public class OsuWriter {
    private DataOutputStream writer;

    public OsuWriter(String filename) throws IOException {
        this(new FileOutputStream(filename));
    }

    public OsuWriter(OutputStream source ){
        this.writer = new DataOutputStream(source);
    }

    // --- Primitive values ---

    public void writeByte(byte value) throws IOException {
        // 1 byte
        this.writer.writeByte(value);
    }

    public void writeByte(int value) throws IOException {
        // 1 byte
        this.writer.writeByte(value);
    }

    public void writeShort(short value) throws IOException {
        // 2 bytes, little endian
        byte[] bytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
        this.writer.write(bytes);
    }

    public void writeShort(int value) throws IOException {
        // 2 bytes, little endian
        this.writeShort((short)value);
    }

    public void writeInt(int value) throws IOException {
        // 4 bytes, little endian
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
        this.writer.write(bytes);
    }

    public void writeLong(long value) throws IOException {
        // 8 bytes, little endian
        byte[] bytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
        this.writer.write(bytes);
    }

    public void writeULEB128(int value) throws IOException {
        do {
            byte b = (byte) (value & 0x7F);
            value >>= 7;
            if (value != 0)
                b |= (1 << 7);
            writer.writeByte(b);
        } while (value != 0);
    }

    public void writeSingle(float value) throws IOException {
        // 4 bytes, little endian
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array();
        this.writer.write(bytes);
    }

    public void writeDouble(double value) throws IOException {
        // 8 bytes little endian
        byte[] bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array();
        this.writer.write(bytes);
    }

    public void writeBoolean(boolean value) throws IOException {
        // 1 byte, zero = false, non-zero = true
        this.writer.writeBoolean(value);
    }

    public void writeSring(String value) throws IOException {
        // 00 = empty string
        // 0B <length> <char>* = normal string
        // <length> is encoded as an LEB, and is the byte length of the rest.
        // <char>* is encoded as UTF8, and is the string content.
        if (value == null || value.length() == 0)
            writer.writeByte(0x00);
        else {
            writer.writeByte(0x0B);
            writeULEB128(value.length());
            writer.writeBytes(value);
        }
    }

    public void writeDate(Date date) throws IOException {
        final long TICKS_AT_EPOCH = 621355968000000000L;
        final long TICKS_PER_MILLISECOND = 10000;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        long ticks = TICKS_AT_EPOCH + calendar.getTimeInMillis() * TICKS_PER_MILLISECOND;
        this.writer.writeLong(ticks);
    }

    public void writeStringBytes(String value) throws IOException {
        this.writer.writeBytes(value);
    }
}
