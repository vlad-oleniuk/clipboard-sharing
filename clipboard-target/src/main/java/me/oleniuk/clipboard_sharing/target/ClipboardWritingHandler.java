package me.oleniuk.clipboard_sharing.target;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import java.util.Objects;

public class ClipboardWritingHandler extends IoHandlerAdapter {

    private String lastContent = "";
    private IoSession currentSession;
    private Timeline clipboardPoller;

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        String contentStr = message.toString();
        System.out.println("RECEIVED CLIPBOARD CONTENT: " + contentStr);
        if (Objects.equals(contentStr, lastContent)) return;
        Platform.runLater(() -> {
            // 1. Get the System Clipboard
            Clipboard clipboard = Clipboard.getSystemClipboard();

            // 2. Create the content container
            ClipboardContent content = new ClipboardContent();

            // 3. Put the string into the container
            content.putString(contentStr);

            // 4. Set the clipboard content
            clipboard.setContent(content);

            this.lastContent=contentStr;

            System.out.println("Copied to clipboard: " + contentStr);
        });
    }


    @Override
    public void sessionOpened(IoSession session) throws Exception {
        this.currentSession = session;
        if (clipboardPoller == null) {
            // 2. Start the monitoring loop (polls every 200ms)
            // We use Timeline because it automatically runs on the JavaFX Application Thread
            this.clipboardPoller = new Timeline(
                    new KeyFrame(Duration.millis(200), event -> {
                        if (currentSession != null)
                            writeClipboardIfChanged(currentSession);
                    })
            );
            clipboardPoller.setCycleCount(Timeline.INDEFINITE);
            clipboardPoller.play();
        }
    }

    private void writeClipboardIfChanged(IoSession session) {

        // Access the system clipboard
        Clipboard clipboard = Clipboard.getSystemClipboard();

        // Check if there is a string and if it has changed
        if (clipboard.hasString()) {
            String currentContent = clipboard.getString();

            if (!currentContent.equals(lastContent)) {
                System.out.println("new content determined: " + currentContent);
                session.write(currentContent);
                lastContent = currentContent;
            }
        }
    }

    @Override
    public void sessionClosed(IoSession session) {
        currentSession = null;
    }

}
