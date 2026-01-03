package me.oleniuk.clipboard_sharing.source;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import java.nio.charset.StandardCharsets;

public class MultilineDecoder extends CumulativeProtocolDecoder {
    // Define your unique delimiter
    public static final String DELIMITER = "019b853f-1ab1-77ce-bb5c-7cd9a518f7b1";

    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        // Search for the delimiter in the incoming buffer
        String currentData = in.getString(StandardCharsets.UTF_8.newDecoder());

        if (currentData.contains(DELIMITER)) {
            int delimiterIndex = currentData.indexOf(DELIMITER);

            // Extract the full multiline message (minus the delimiter)
            String message = currentData.substring(0, delimiterIndex);
            out.write(message);

            // Tell MINA how many bytes we actually consumed from the buffer
            // We move the buffer position past the delimiter
            int totalBytesConsumed = (message + DELIMITER).getBytes(StandardCharsets.UTF_8).length;
            in.position(totalBytesConsumed);

            return true; // There might be another message in the buffer
        }

        return false; // Not enough data yet; wait for more
    }
}
