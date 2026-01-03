package me.oleniuk.clipboard_sharing.target;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class MultilineEncoder extends ProtocolEncoderAdapter {
    private static final String DELIMITER = MultilineDecoder.DELIMITER;

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        String msg = message.toString() + DELIMITER;
        byte[] bytes = msg.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        IoBuffer buffer = IoBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        out.write(buffer);
    }
}
