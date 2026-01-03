package me.oleniuk.clipboard_sharing.source;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;

public class BackgroundClipboardWatcher extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Prevent JavaFX from closing when there are no visible windows
        Platform.setImplicitExit(false);

        NioSocketConnector connector = new NioSocketConnector();
        connector.getFilterChain().addLast("codec",
                new ProtocolCodecFilter(new MultilineCodecFactory()));
        connector.getFilterChain().addLast("logger", new LoggingFilter());
        connector.setHandler(new ClipboardWritingHandler());
        connector.addListener(new ReconnectingListener());
        String targetHost = getParameters().getNamed().getOrDefault("targetHost", "localhost");
        ConnectFuture future = connector.connect(new InetSocketAddress(targetHost, 11099));
        // Wait until the connection is established
        future.awaitUninterruptibly();
        System.out.println("Clipboard watcher is running in the background...");
    }

    private static class ReconnectingListener implements IoServiceListener {

        @Override
        public void serviceActivated(IoService ioService) throws Exception {

        }

        @Override
        public void serviceIdle(IoService ioService, IdleStatus idleStatus) throws Exception {
            // TODO add some ping, but chek if here is the right place
        }

        @Override
        public void serviceDeactivated(IoService ioService) throws Exception {

        }

        @Override
        public void sessionCreated(IoSession ioSession) throws Exception {

        }

        @Override
        public void sessionClosed(IoSession ioSession) throws Exception {
            // TODO reconnect
        }

        @Override
        public void sessionDestroyed(IoSession ioSession) throws Exception {

        }
    }

}