package me.oleniuk.clipboard_sharing.source;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.Clipboard;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class BackgroundClipboardWatcher extends Application {

    private String lastContent = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Prevent JavaFX from closing when there are no visible windows
        Platform.setImplicitExit(false);

        // 2. Start the monitoring loop (polls every 200ms)
        // We use Timeline because it automatically runs on the JavaFX Application Thread
        Timeline clipboardPoller = new Timeline(
                new KeyFrame(Duration.millis(200), event -> checkClipboard())
        );
        clipboardPoller.setCycleCount(Timeline.INDEFINITE);
        clipboardPoller.play();

        System.out.println("Clipboard watcher is running in the background...");
    }

    private void checkClipboard() {
        // Access the system clipboard
        Clipboard clipboard = Clipboard.getSystemClipboard();

        // Check if there is a string and if it has changed
        if (clipboard.hasString()) {
            String currentContent = clipboard.getString();

            if (!currentContent.equals(lastContent)) {
                System.out.println("new content determined: " + currentContent);
                lastContent = currentContent;
                try {
                    sendContentClipboard(currentContent);
                } catch (IOException e) {
                    System.out.println("Failed handling clipboard content");
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendContentClipboard(String currentContent) throws IOException {
        Parameters params = getParameters();
        String targetHost = params.getNamed().get("targetHost");
        try (var socket=new Socket(targetHost, 11099);
                var out = socket.getOutputStream()){
            out.write(currentContent.getBytes(StandardCharsets.UTF_8));
        }
    }


}