package me.oleniuk.clipboard_sharing.source;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class MultilineCodecFactory implements ProtocolCodecFactory {
    private final ProtocolEncoder encoder = new MultilineEncoder();
    private final ProtocolDecoder decoder = new MultilineDecoder();

    @Override public ProtocolEncoder getEncoder(IoSession s) { return encoder; }
    @Override public ProtocolDecoder getDecoder(IoSession s) { return decoder; }
}