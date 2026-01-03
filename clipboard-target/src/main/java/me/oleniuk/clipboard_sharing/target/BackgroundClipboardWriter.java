package me.oleniuk.clipboard_sharing.target;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;

public class BackgroundClipboardWriter extends Application {

    private static final int PORT = 11099;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Prevent JavaFX from closing when there are no visible windows
        Platform.setImplicitExit(false);
        this.startServer();
    }

    private void startServer() {
        IoAcceptor acceptor = new NioSocketAcceptor();
        try {
            acceptor.getFilterChain().addLast("logger", new LoggingFilter());
            acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MultilineCodecFactory()));
            acceptor.setHandler(new ClipboardWritingHandler());
            acceptor.bind(new InetSocketAddress(PORT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
