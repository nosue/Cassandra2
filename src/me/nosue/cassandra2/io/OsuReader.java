package me.nosue.cassandra2.io;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Class to read data sent through by osu!
 * @author Markus Jarderot (http://stackoverflow.com/questions/28788616)
 */
public class OsuReader {
   private DataInputStream reader;

    public OsuReader(String filename) throws IOException {
        this(new FileInputStream(filename));
    }

    public OsuReader(InputStream source ){
        this.reader = new DataInputStream(source);
    }

    // --- Primitive values ---

    public byte readByte() throws IOException {
        // 1 byte
        return this.reader.readByte();
    }

    public short readShort() throws IOException {
        // 2 bytes, little endian
        byte[] bytes = new byte[2];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }

    public int readInt() throws IOException {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    public long readLong() throws IOException {
        // 8 bytes, little endian
        byte[] bytes = new byte[8];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();
    }

    public int readULEB128() throws IOException {
        // variable bytes, little endian
        // MSB says if there will be more bytes. If cleared,
        // that byte is the last.
        int value = 0;
        for (int shift = 0; shift < 32; shift += 7)
        {
            byte b = this.reader.readByte();
            value |= ((int) b & 0x7F) << shift;

            if (b >= 0) return value; // MSB is zero. End of value.
        }
        throw new IOException("ULEB128 too large");
    }

    public float readSingle() throws IOException {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getFloat();
    }

    public double readDouble() throws IOException {
        // 8 bytes little endian
        byte[] bytes = new byte[8];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getDouble();
    }

    public boolean readBoolean() throws IOException {
        // 1 byte, zero = false, non-zero = true
        return this.reader.readBoolean();
    }

    public String readString() throws IOException {
        // variable length
        // 00 = empty string
        // 0B <length> <char>* = normal string
        // <length> is encoded as an LEB, and is the byte length of the rest.
        // <char>* is encoded as UTF8, and is the string content.
        byte kind = this.reader.readByte();
        if (kind == 0) return "";
        if (kind != 11)
        {
            throw new IOException(String.format("String format error: Expected 0x0B or 0x00, found 0x%02X", (int) kind & 0xFF));
        }
        int length = readULEB128();
        if (length == 0) return "";
        byte[] utf8bytes = new byte[length];
        this.reader.readFully(utf8bytes);
        return new String(utf8bytes, "UTF-8");
    }

    public Date readDate() throws IOException {
        long ticks = readLong();
        long TICKS_AT_EPOCH = 621355968000000000L;
        long TICKS_PER_MILLISECOND = 10000;

        return new Date((ticks - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND);
    }
}
