package com.labs.Lawrence;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    private static final String LOG_FILE = "swiftbotlogger.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void write(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String entry = "[" + timestamp + "] [" + level + "] " + message;
        System.out.println(entry);
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println(entry);
        } catch (IOException e) {
            System.out.println("WARNING: Could not write to log file: " + e.getMessage());
        }
    }

    public static void info(String message) {
        write("INFO", message);
    }

    public static void error(String message) {
        write("ERROR", message);
    }

    public static void detection(String colour) {
        write("DETECTION", "Traffic light detected: " + colour.toUpperCase());
    }

    public static void movement(String action) {
        write("MOVEMENT", action);
    }

    public static void button(String button) {
        write("BUTTON", "Button pressed: " + button);
    }
}