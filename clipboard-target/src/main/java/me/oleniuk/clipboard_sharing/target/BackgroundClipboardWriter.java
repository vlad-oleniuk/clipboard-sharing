package me.oleniuk.clipboard_sharing.target;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

public class BackgroundClipboardWriter extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Prevent JavaFX from closing when there are no visible windows
        Platform.setImplicitExit(false);
        new Thread(this::startServer).start();
    }

    private void startServer() {
        try (var s = new ServerSocket(11099)) {
            System.out.println("Clipboard writer is listening to clipboard senders...");
            while (true) {
                try (Socket incoming = s.accept()) {
                    InputStream is = incoming.getInputStream();
                    handleNewContent(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void handleNewContent(String contentStr) {
        System.out.println("RECEIVED CLIPBOARD CONTENT: " + contentStr);
        Platform.runLater(() -> {
            // 1. Get the System Clipboard
            Clipboard clipboard = Clipboard.getSystemClipboard();

            // 2. Create the content container
            ClipboardContent content = new ClipboardContent();

            // 3. Put the string into the container
            content.putString(contentStr);

            // 4. Set the clipboard content
            clipboard.setContent(content);

            System.out.println("Copied to clipboard: " + contentStr);
        });
    }

}
