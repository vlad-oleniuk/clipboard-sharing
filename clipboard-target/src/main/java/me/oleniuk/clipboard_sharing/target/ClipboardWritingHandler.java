package me.oleniuk.clipboard_sharing.target;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Objects;

public class ClipboardWritingHandler extends IoHandlerAdapter {

    private String lastContent = "";
    private String lastImageHash = null;
    private IoSession currentSession;
    private Timeline clipboardPoller;

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        String msg = message.toString();
        if (Objects.equals(msg, lastContent)) return;

        if (msg.startsWith("I:")) {
            String encoded = msg.substring(2);
            System.out.println("RECEIVED IMAGE (" + encoded.length() + " base64 chars)");
            try {
                Image image = decodeImage(encoded);
                Platform.runLater(() -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putImage(image);
                    Clipboard.getSystemClipboard().setContent(content);
                    lastContent = msg;
                    lastImageHash = quickHash(image);
                    System.out.println("Image copied to clipboard");
                });
            } catch (Exception e) {
                System.err.println("Failed to decode image: " + e.getMessage());
            }
        } else if (msg.startsWith("T:")) {
            String text = msg.substring(2);
            System.out.println("RECEIVED CLIPBOARD CONTENT: " + text);
            Platform.runLater(() -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                Clipboard.getSystemClipboard().setContent(content);
                lastContent = msg;
                lastImageHash = null;
                System.out.println("Copied to clipboard: " + text);
            });
        }
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        System.out.println("Clipboard sync active with source at " + session.getRemoteAddress());
        this.currentSession = session;
        if (clipboardPoller == null) {
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
        Clipboard clipboard = Clipboard.getSystemClipboard();

        if (clipboard.hasImage()) {
            Image image = clipboard.getImage();
            String hash = quickHash(image);
            if (!hash.equals(lastImageHash)) {
                try {
                    String encoded = encodeImage(image);
                    String msg = "I:" + encoded;
                    if (!msg.equals(lastContent)) {
                        System.out.println("Sending image to peer (" + encoded.length() + " base64 chars)");
                        if (session != null) {
                            session.write(msg);
                            lastContent = msg;
                            lastImageHash = hash;
                        } else {
                            System.out.println("No connection is active!!! Reconnect the source");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to encode image: " + e.getMessage());
                }
            }
        } else if (clipboard.hasString()) {
            String currentContent = "T:" + clipboard.getString();
            if (!currentContent.equals(lastContent)) {
                System.out.println("new content determined: " + clipboard.getString());
                if (session != null) {
                    session.write(currentContent);
                    lastContent = currentContent;
                    lastImageHash = null;
                } else {
                    System.out.println("No connection is active!!! Reconnect the source");
                }
            }
        }
    }

    private String encodeImage(Image image) throws Exception {
        BufferedImage bi = SwingFXUtils.fromFXImage(image, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private Image decodeImage(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
        return SwingFXUtils.toFXImage(bi, null);
    }

    private String quickHash(Image image) {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        if (w == 0 || h == 0) return w + "x" + h;
        PixelReader reader = image.getPixelReader();
        int hash = w * 31 + h;
        int[][] points = {{0, 0}, {w - 1, 0}, {0, h - 1}, {w - 1, h - 1}, {w / 2, h / 2}};
        for (int[] p : points) {
            if (p[0] < w && p[1] < h) {
                hash = hash * 31 + reader.getArgb(p[0], p[1]);
            }
        }
        return w + "x" + h + ":" + hash;
    }

    @Override
    public void sessionClosed(IoSession session) {
        System.out.println("Clipboard sync inactive — source disconnected");
        currentSession = null;
    }

}
