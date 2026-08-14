package me.oleniuk.clipboard_sharing.source;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        String targetHost = getParameters().getNamed().getOrDefault("targetHost", "localhost");
        InetSocketAddress address = new InetSocketAddress(targetHost, 11099);
        connector.setDefaultRemoteAddress(address);
        connector.addListener(new ReconnectingListener(connector));
        ConnectFuture future = connector.connect();
        future.awaitUninterruptibly();
        if (future.isConnected()) {
            System.out.println("Connected to target at " + targetHost + ":11099");
        } else {
            Throwable cause = future.getException();
            String reason = cause != null ? cause.getMessage() : "unknown error";
            System.out.println("Failed to connect to " + targetHost + ":11099: " + reason);
        }
    }

    private static class ReconnectingListener implements IoServiceListener {
        private final NioSocketConnector connector;

        private final ScheduledExecutorService reconnectingExecutor;


        public ReconnectingListener(NioSocketConnector connector) {
            this.connector = connector;
            reconnectingExecutor = Executors.newSingleThreadScheduledExecutor();
        }



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
        }

        private void reconnect() {
            System.out.println("reconnecting...");
            ConnectFuture connectFuture = this.connector.connect();
            connectFuture.addListener(future -> {
                ConnectFuture connectResult = (ConnectFuture) future;
                if (connectResult.isConnected()) {
                    System.out.println("Reconnected to target at " + connectResult.getSession().getRemoteAddress());
                } else {
                    Throwable cause = connectResult.getException();
                    String reason = cause != null ? cause.getMessage() : "unknown error";
                    System.out.println("Reconnect failed: " + reason + "; retrying in 5 seconds");
                    reconnectingExecutor.schedule(ReconnectingListener.this::reconnect, 5, TimeUnit.SECONDS);
                }
            });
        }

        @Override
        public void sessionDestroyed(IoSession ioSession) throws Exception {
            System.out.println("Disconnected from target; reconnecting in 5 seconds");
            reconnectingExecutor.schedule(this::reconnect, 5, TimeUnit.SECONDS);
        }
    }

}