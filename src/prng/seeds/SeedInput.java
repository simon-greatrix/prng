package prng.seeds;

import java.io.DataInput;
import java.io.EOFException;
import java.io.UTFDataFormatException;

/**
 * Read previously stored data back from a byte array. As the data has already
 * been read into memory, the only possible IOException is an EOFException or a
 * UTFEncodingException. Both exceptions would indicate the data had been
 * corrupted in storage.
 * 
 * @author Simon Greatrix
 *
 */
public class SeedInput implements DataInput {
    /** Input data */
    private final byte[] input_;

    /** Input length */
    private final int length_;

    /** Current position in input */
    private int position_ = 0;


    /**
     * A new data input reading from the byte array.
     * 
     * @param input
     *            the input bytes
     */
    public SeedInput(byte[] input) {
        input_ = input.clone();
        length_ = input.length;
    }


    /**
     * Read a seed from the input. The seed bits will be scrambled on read.
     * 
     * @return the seed bits
     * @throws IOException
     */
    public byte[] readSeed() throws EOFException {
        int len = readUnsignedShort();
        byte[] data = new byte[len];
        readFully(data);
        return SeedStorage.scramble(data);
    }


    @Override
    public void readFully(byte[] b) throws EOFException {
        readFully(b, 0, b.length);
    }


    @Override
    public void readFully(byte[] b, int off, int len) throws EOFException {
        int endPos = position_ + len;
        if( endPos > length_ )
            throw new EOFException("Only " + (length_ - position_)
                    + " bytes remain. Required " + len);
        System.arraycopy(input_, position_, b, off, len);
        position_ = endPos;
    }


    @Override
    public int skipBytes(int n) {
        int rem = length_ - position_;
        int skip = Math.min(n, rem);
        position_ += skip;
        return skip;
    }


    @Override
    public boolean readBoolean() throws EOFException {
        if( position_ == length_ ) throw new EOFException("0 bytes remaining");
        byte b = input_[position_];
        position_++;
        return b != 0;
    }


    @Override
    public byte readByte() throws EOFException {
        if( position_ == length_ ) throw new EOFException("0 bytes remaining");
        byte b = input_[position_];
        position_++;
        return b;
    }


    @Override
    public int readUnsignedByte() throws EOFException {
        if( position_ == length_ ) throw new EOFException("0 bytes remaining");
        byte b = input_[position_];
        position_++;
        return b & 0xff;
    }


    @Override
    public short readShort() throws EOFException {
        return (short) readUnsignedShort();
    }


    @Override
    public int readUnsignedShort() throws EOFException {
        int endPos = position_ + 2;
        if( endPos > length_ )
            throw new EOFException("Only " + (length_ - position_)
                    + " bytes remain. Required 2");
        int b0 = 0xff & input_[position_];
        int b1 = 0xff & input_[position_ + 1];
        position_ = endPos;
        return (b0 << 8) | b1;
    }


    @Override
    public char readChar() throws EOFException {
        return (char) readUnsignedShort();
    }


    @Override
    public int readInt() throws EOFException {
        int endPos = position_ + 4;
        if( endPos > length_ )
            throw new EOFException("Only " + (length_ - position_)
                    + " bytes remain. Required 4");
        int b0 = 0xff & input_[position_];
        int b1 = 0xff & input_[position_ + 1];
        int b2 = 0xff & input_[position_ + 2];
        int b3 = 0xff & input_[position_ + 3];
        position_ = endPos;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }


    @Override
    public long readLong() throws EOFException {
        int endPos = position_ + 8;
        if( endPos > length_ )
            throw new EOFException("Only " + (length_ - position_)
                    + " bytes remain. Required 8");

        long v = 0;
        for(int p = position_;p < endPos;p++) {
            v = (v << 8) | (0xff & input_[p]);
        }
        position_ = endPos;
        return v;
    }


    @Override
    public float readFloat() throws EOFException {
        return Float.intBitsToFloat(readInt());
    }


    @Override
    public double readDouble() throws EOFException {
        return Double.longBitsToDouble(readLong());
    }


    /**
     * This method is not supported.
     * 
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public String readLine() {
        throw new UnsupportedOperationException(
                "DataInput.readLine() is deprecated and not supported");
    }


    @Override
    public String readUTF() throws EOFException, UTFDataFormatException {
        int utflen = readUnsignedShort();
        char[] chars = new char[utflen];

        int endPos = position_ + utflen;
        if( endPos > length_ )
            throw new EOFException("Only " + (length_ - position_)
                    + " bytes remain. Required " + utflen);

        int p = position_;
        int c = 0;

        while( p < endPos ) {
            int x = (int) input_[p] & 0xff;

            if( x == 0 ) {
                // modified UTF-8 stores zero as 0xC0 0x80
                throw new UTFDataFormatException(
                        "Malformed input. Saw byte 0x00 at position " + p);
            } else if( x < 0b1000_0000 ) {
                // regular ASCII character
                chars[c] = (char) x;
                c++;
                p++;
            } else if( x < 0b1100_0000 ) {
                // A byte like 0b10.. must be a second or third byte, not the
                // first
                throw new UTFDataFormatException("Malformed input. Saw byte 0x"
                        + Integer.toHexString(x) + " at position " + p);
            } else if( x < 0b1110_0000 ) {
                // two byte character, check 2nd byte
                p++;
                if( p == endPos )
                    throw new UTFDataFormatException(
                            "Missing byte at end of input. Last byte was 0x"
                                    + Integer.toHexString(x) + " at position "
                                    + (p - 1));
                int y = (int) input_[p] & 0xff;
                if( y < 0b1000_0000 || 0b1100_000 <= y )
                    throw new UTFDataFormatException(
                            "Malformed input. Saw bytes was 0x"
                                    + Integer.toHexString((x << 8) | y)
                                    + " at position " + (p - 1));
                chars[c] = (char) (((x & 0x1f) << 6) | (y & 0x3f));
                c++;
                p++;
            } else if( x < 0b1111_0000 ) {
                // three byte character, check 2nd byte
                p++;
                if( p == endPos )
                    throw new UTFDataFormatException(
                            "Missing bytes at end of input. Last byte was 0x"
                                    + Integer.toHexString(x) + " at position "
                                    + (p - 1));
                int y = (int) input_[p] & 0xff;
                if( y < 0b1000_0000 || 0b1100_000 <= y )
                    throw new UTFDataFormatException(
                            "Malformed input. Saw bytes was 0x"
                                    + Integer.toHexString((x << 8) | y)
                                    + " at position " + (p - 1));
                
                // check 3rd byte
                p++;
                if( p == endPos )
                    throw new UTFDataFormatException(
                            "Missing byte at end of input. Last bytes were 0x"
                                    + Integer.toHexString((x << 8) | y)
                                    + " at position " + (p - 1));
                int z = (int) input_[p] & 0xff;
                if( z < 0b1000_0000 || 0b1100_000 <= z )
                    throw new UTFDataFormatException(
                            "Malformed input. Saw bytes was 0x"
                                    + Integer.toHexString((x << 16) | (y << 8)
                                            | z) + " at position " + (p - 2));

                chars[c] = (char) (((x & 0xf) << 12) | ((y & 0x3f) << 6) | (z & 0x3f));
                c++;
                p++;
            } else {
                // 4-byte or more character, which is outside of Java's range
                throw new UTFDataFormatException("Malformed input. Saw byte 0x"
                        + Integer.toHexString(x) + " at position " + p);
            }
        }

        // The number of chars produced may be less than utflen
        return new String(chars, 0, c);
    }
}