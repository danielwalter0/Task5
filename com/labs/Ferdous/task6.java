package com.labs.Ferdous;

import swiftbot.Button;
import swiftbot.SwiftBotAPI;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Task 6: Draw Shape (Square + Triangle)
 * QR format:
 *   Square:   S:16
 *   Triangle: T:16:30:24
 *   Multiple: S:16&T:16:30:24  (max 5 shapes)
 *
 * Terminates when SwiftBot X button is pressed.
 */
public class Assignment3 {

    // -------- SwiftBot + UI colors --------
    static SwiftBotAPI swiftBot;

    static final String PURPLE = "\u001B[35m";
    static final String CYAN   = "\u001B[36m";
    static final String GREEN  = "\u001B[32m";
    static final String RED    = "\u001B[31m";
    static final String YELLOW = "\u001B[33m";
    static final String RESET  = "\u001B[0m";

    // -------- SPEED TABLE (from your calibration image) --------
    // motor percentage -> actual forward speed (cm/s)
    static final Map<Integer, Double> SPEED_CM_PER_S = new HashMap<>();
    static {
        SPEED_CM_PER_S.put(30, 16.8);
        SPEED_CM_PER_S.put(40, 21.1);
        SPEED_CM_PER_S.put(50, 23.9);
        SPEED_CM_PER_S.put(60, 25.35);
        SPEED_CM_PER_S.put(70, 28.0);
        SPEED_CM_PER_S.put(80, 28.9);
        SPEED_CM_PER_S.put(90, 28.6);
        SPEED_CM_PER_S.put(100, 30.15);
    }

    // Default “low speed” for drawing (task says low speed)
    static int drawSpeedPercent = 50;

    /**
     * TURN calibration:
     * You still need one turning calibration constant unless you also measure turn speed.
     * MS_PER_DEG = milliseconds per degree at TURN_SPEED (spin turn).
     */
    static final double MS_PER_DEG = 6.5;   // <-- keep/adjust after testing turns
    static final int TURN_SPEED = 60;       // spin speed for turns

    // -------- Control flag for terminating with X --------
    static volatile boolean xPressed = false;

    // -------- Records for logging --------
    static class ShapeRecord {
        String name;               // "Square" or "Triangle"
        List<Integer> sizesCm;     // side lengths
        List<Double> anglesDeg;    // triangles only
        double area;               // cm^2
        long timeMs;               // time to draw this shape
        LocalDateTime timestamp;

        ShapeRecord(String name, List<Integer> sizesCm, List<Double> anglesDeg, double area, long timeMs) {
            this.name = name;
            this.sizesCm = sizesCm;
            this.anglesDeg = anglesDeg;
            this.area = area;
            this.timeMs = timeMs;
            this.timestamp = LocalDateTime.now();
        }
    }

    static final List<ShapeRecord> drawnShapes = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // Initialise SwiftBot
        try {
            swiftBot = SwiftBotAPI.INSTANCE;
        } catch (Exception e) {
            System.out.println(RED + "I2C disabled or SwiftBot not connected." + RESET);
            System.exit(1);
            return;
        }

        // Enable X button termination
        try {
            swiftBot.enableButton(Button.X, () -> xPressed = true);
        } catch (Exception ignored) {}

        Scanner scanner = new Scanner(System.in);

        printHeader();

        // Optional: let user choose speed percent (uses your measured table)
        chooseSpeed(scanner);

        // Main loop: keep scanning QR codes until X pressed
        while (!xPressed) {
            System.out.println(CYAN + "Press X on SwiftBot to terminate and write log." + RESET);

            // "Camera scanning animation" (CLI-only)
          String qr = scanQRCodeReal();

            // Parse up to 5 shapes separated by '&'
            String[] commands = qr.split("&");
            if (commands.length > 5) {
                System.out.println(RED + "ERROR: Max 5 shapes per QR code." + RESET);
                continue;
            }

            // Validate all commands first (fail fast if any invalid)
            List<ParsedCommand> parsed = new ArrayList<>();
            boolean ok = true;
            for (String cmd : commands) {
                ParsedCommand pc = parseAndValidateCommand(cmd.trim());
                if (pc == null) {
                    ok = false;
                    break;
                }
                parsed.add(pc);
            }
            if (!ok) continue;

            // Execute each shape in sequence
            for (int i = 0; i < parsed.size(); i++) {
                if (xPressed) break;

                ParsedCommand pc = parsed.get(i);

                // Display what will be drawn (brief requirement)
                System.out.println(PURPLE + "\n----------------------------------------" + RESET);
                System.out.println(PURPLE + "Next shape: " + pc.describe() + RESET);
                System.out.println(PURPLE + "Speed: " + drawSpeedPercent + "% (" + SPEED_CM_PER_S.get(drawSpeedPercent) + " cm/s)" + RESET);
                System.out.println(PURPLE + "----------------------------------------" + RESET);

                long start = System.nanoTime();

                if (pc.type == ShapeType.SQUARE) {
                    drawSquare(pc.squareSide);
                    double area = pc.squareSide * pc.squareSide;
                    long timeMs = (System.nanoTime() - start) / 1_000_000;
                    drawnShapes.add(new ShapeRecord(
                            "Square",
                            List.of(pc.squareSide),
                            Collections.emptyList(),
                            area,
                            timeMs
                    ));
                } else {
                    TriangleAngles ta = computeTriangleAngles(pc.a, pc.b, pc.c);
                    drawTriangle(pc.a, pc.b, pc.c, ta);
                    double area = triangleAreaHeron(pc.a, pc.b, pc.c);
                    long timeMs = (System.nanoTime() - start) / 1_000_000;
                    drawnShapes.add(new ShapeRecord(
                            "Triangle",
                            List.of(pc.a, pc.b, pc.c),
                            List.of(ta.A, ta.B, ta.C),
                            area,
                            timeMs
                    ));
                }

                // Blink green to indicate shape completed (brief requirement)
                blinkGreen(3, 200);

                // If more shapes in same QR, move 15cm backwards before next
                if (i < parsed.size() - 1 && !xPressed) {
                    System.out.println(CYAN + "Moving back 15cm to start next shape..." + RESET);
                    moveBackwardCm(15);
                    safeStop();
                }
            }

            System.out.println(GREEN + "\n✔ Shapes completed." + RESET);
            System.out.println(CYAN + "Scan a new QR code OR press X on SwiftBot to exit." + RESET);
            System.out.println(CYAN + "Press ENTER to continue..." + RESET);

// Wait for user before scanning again
try {
    System.in.read();
} catch (Exception ignored) {};
        }

        // Termination path when X pressed
        safeStop();
        disableUnderlights();

        File logFile = writeLogFile();
        System.out.println(PURPLE + "\n==================== SUMMARY ====================" + RESET);
        printSummaryToConsole(logFile);
        System.out.println(PURPLE + "=================================================\n" + RESET);

        System.exit(0);
    }

    // -------------------- Speed Helpers --------------------

    static void chooseSpeed(Scanner scanner) {
        System.out.println(CYAN + "Choose drawing speed percentage from: 30,40,50,60,70,80,90,100" + RESET);
        System.out.print(CYAN + "Enter speed % (press Enter for default 50%): " + RESET);
        String sp = scanner.nextLine().trim();
        if (sp.isEmpty()) {
            drawSpeedPercent = 50;
            System.out.println(GREEN + "Using default speed: 50% (" + SPEED_CM_PER_S.get(50) + " cm/s)\n" + RESET);
            return;
        }
        try {
            int requested = Integer.parseInt(sp);
            if (SPEED_CM_PER_S.containsKey(requested)) {
                drawSpeedPercent = requested;
            } else {
                drawSpeedPercent = nearestSpeedPercent(requested);
                System.out.println(YELLOW + "Speed not in table. Using nearest: " + drawSpeedPercent + "%" + RESET);
            }
            System.out.println(GREEN + "Using speed: " + drawSpeedPercent + "% (" + SPEED_CM_PER_S.get(drawSpeedPercent) + " cm/s)\n" + RESET);
        } catch (Exception e) {
            drawSpeedPercent = 50;
            System.out.println(YELLOW + "Invalid input. Using default 50%.\n" + RESET);
        }
    }

    static int nearestSpeedPercent(int requested) {
        int best = 50;
        int bestDiff = Integer.MAX_VALUE;
        for (int k : SPEED_CM_PER_S.keySet()) {
            int diff = Math.abs(k - requested);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = k;
            }
        }
        return best;
    }

    // Convert cm distance → ms using measured speed table
    static int distanceToMs(int distanceCm, int speedPercent) {
        double v = SPEED_CM_PER_S.get(speedPercent); // cm/s
        int ms = (int) Math.round((distanceCm / v) * 1000.0);
        return Math.max(ms, 1); // must be > 0ms
    }

    // -------------------- Parsing & Validation --------------------

    enum ShapeType { SQUARE, TRIANGLE }

    static class ParsedCommand {
        ShapeType type;
        int squareSide;
        int a, b, c;

        String describe() {
            if (type == ShapeType.SQUARE) return "Square (side " + squareSide + " cm)";
            return "Triangle (sides " + a + ", " + b + ", " + c + " cm)";
        }
    }

    static ParsedCommand parseAndValidateCommand(String cmd) {
        if (cmd.isEmpty()) {
            System.out.println(RED + "ERROR: Empty command in QR." + RESET);
            return null;
        }

        String[] parts = cmd.split(":");
        if (parts.length < 2) {
            System.out.println(RED + "ERROR: Invalid format. Use S:len or T:a:b:c" + RESET);
            return null;
        }

        String shapeCode = parts[0].trim().toUpperCase(Locale.ROOT);

        try {
            if (shapeCode.equals("S")) {
                if (parts.length != 2) {
                    System.out.println(RED + "ERROR: Square format must be S:len" + RESET);
                    return null;
                }
                int side = Integer.parseInt(parts[1].trim());
                if (!inRange(side)) {
                    System.out.println(RED + "ERROR: Square side must be 15–85 cm." + RESET);
                    return null;
                }
                ParsedCommand pc = new ParsedCommand();
                pc.type = ShapeType.SQUARE;
                pc.squareSide = side;
                return pc;

            } else if (shapeCode.equals("T")) {
                if (parts.length != 4) {
                    System.out.println(RED + "ERROR: Triangle format must be T:a:b:c" + RESET);
                    return null;
                }
                int a = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                int c = Integer.parseInt(parts[3].trim());

                if (!inRange(a) || !inRange(b) || !inRange(c)) {
                    System.out.println(RED + "ERROR: Triangle sides must be 15–85 cm." + RESET);
                    return null;
                }
                if (!isValidTriangle(a, b, c)) {
                    System.out.println(RED + "ERROR: These values cannot form a triangle (triangle inequality failed)." + RESET);
                    return null;
                }

                ParsedCommand pc = new ParsedCommand();
                pc.type = ShapeType.TRIANGLE;
                pc.a = a; pc.b = b; pc.c = c;
                return pc;

            } else {
                System.out.println(RED + "ERROR: Shape code must be S or T (case-insensitive)." + RESET);
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println(RED + "ERROR: Side lengths must be integers." + RESET);
            return null;
        }
    }

    static boolean inRange(int x) {
        return x >= 15 && x <= 85;
    }

    static boolean isValidTriangle(int a, int b, int c) {
        return (a + b > c) && (a + c > b) && (b + c > a);
    }

    // -------------------- Triangle Maths --------------------

    static class TriangleAngles {
        double A, B, C;
        TriangleAngles(double A, double B, double C) {
            this.A = A; this.B = B; this.C = C;
        }
    }

    static TriangleAngles computeTriangleAngles(int a, int b, int c) {
        double A = Math.toDegrees(Math.acos(clamp((b*b + c*c - a*a) / (2.0*b*c))));
        double B = Math.toDegrees(Math.acos(clamp((a*a + c*c - b*b) / (2.0*a*c))));
        double C = 180.0 - A - B;
        return new TriangleAngles(round2(A), round2(B), round2(C));
    }

    static double triangleAreaHeron(int a, int b, int c) {
        double s = (a + b + c) / 2.0;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }

    static double clamp(double x) {
        if (x < -1) return -1;
        if (x > 1) return 1;
        return x;
    }

    static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    // -------------------- Drawing Behaviours --------------------

    static void drawSquare(int sideCm) throws InterruptedException {
        setUnderlights(new int[]{0, 0, 255}); // blue while drawing square
        for (int i = 0; i < 4; i++) {
            moveForwardCm(sideCm);
            safeStop();
            Thread.sleep(250);
            turnDegrees(90);
            safeStop();
            Thread.sleep(250);
        }
        disableUnderlights();
    }

    static void drawTriangle(int a, int b, int c, TriangleAngles angles) throws InterruptedException {
        setUnderlights(new int[]{255, 255, 0}); // yellow while drawing triangle

        double extA = 180.0 - angles.A;
        double extB = 180.0 - angles.B;
        double extC = 180.0 - angles.C;

        moveForwardCm(a);
        safeStop();
        Thread.sleep(250);
        turnDegrees(extA);

        moveForwardCm(b);
        safeStop();
        Thread.sleep(250);
        turnDegrees(extB);

        moveForwardCm(c);
        safeStop();
        Thread.sleep(250);
        turnDegrees(extC);

        disableUnderlights();
    }

    // -------------------- Motor Control Helpers --------------------

    static void moveForwardCm(int cm) throws InterruptedException {
        int ms = distanceToMs(cm, drawSpeedPercent);
        swiftBot.move(drawSpeedPercent, drawSpeedPercent, ms);
    }

    static void moveBackwardCm(int cm) throws InterruptedException {
        int ms = distanceToMs(cm, drawSpeedPercent);
        swiftBot.move(-drawSpeedPercent, -drawSpeedPercent, ms);
    }

    static void turnDegrees(double deg) throws InterruptedException {
        int ms = (int) Math.round(Math.abs(deg) * MS_PER_DEG);
        ms = Math.max(ms, 1);

        // Turn left for positive degrees (swap if your robot turns opposite)
        if (deg >= 0) {
            swiftBot.move(-TURN_SPEED, TURN_SPEED, ms);
        } else {
            swiftBot.move(TURN_SPEED, -TURN_SPEED, ms);
        }
    }

    static void safeStop() {
        try {
            swiftBot.move(0, 0, 1); // must be > 0ms
        } catch (Exception ignored) {}
    }

    // -------------------- LEDs --------------------

    static void setUnderlights(int[] rgb) {
        try { swiftBot.fillUnderlights(rgb); }
        catch (Exception ignored) {}
    }

    static void disableUnderlights() {
        try { swiftBot.disableUnderlights(); }
        catch (Exception ignored) {}
    }

    static void blinkGreen(int times, int delayMs) throws InterruptedException {
        for (int i = 0; i < times; i++) {
            setUnderlights(new int[]{0, 255, 0});
            Thread.sleep(delayMs);
            disableUnderlights();
            Thread.sleep(delayMs);
        }
    }

    // -------------------- Logging & Summary --------------------

    static File writeLogFile() {
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File file = new File("shape_log_" + ts + ".txt");

            try (FileWriter fw = new FileWriter(file, false)) {
                fw.write("SwiftBot Draw Shape Log\n");
                fw.write("Generated: " + LocalDateTime.now() + "\n");
                fw.write("Speed used: " + drawSpeedPercent + "% (" + SPEED_CM_PER_S.get(drawSpeedPercent) + " cm/s)\n\n");

                if (drawnShapes.isEmpty()) {
                    fw.write("No shapes were drawn.\n");
                    return file;
                }

                fw.write("1) Shapes drawn (in order):\n");
                for (ShapeRecord r : drawnShapes) {
                    if (r.name.equals("Square")) {
                        fw.write(String.format(
                                "Square: %d (time: %d ms)\n",
                                r.sizesCm.get(0), r.timeMs
                        ));
                    } else {
                        fw.write(String.format(
                                "Triangle: %d, %d, %d (angles: %.2f, %.2f, %.2f; time: %d ms)\n",
                                r.sizesCm.get(0), r.sizesCm.get(1), r.sizesCm.get(2),
                                r.anglesDeg.get(0), r.anglesDeg.get(1), r.anglesDeg.get(2),
                                r.timeMs
                        ));
                    }
                }

                ShapeRecord largest = Collections.max(drawnShapes, Comparator.comparingDouble(r -> r.area));
                fw.write("\n2) Largest shape by area:\n");
                fw.write(describeLargest(largest) + "\n");

                long squares = drawnShapes.stream().filter(r -> r.name.equals("Square")).count();
                long triangles = drawnShapes.stream().filter(r -> r.name.equals("Triangle")).count();
                fw.write("\n3) Most frequent shape:\n");
                if (squares > triangles) fw.write("Square: " + squares + " times\n");
                else if (triangles > squares) fw.write("Triangle: " + triangles + " times\n");
                else fw.write("Square and Triangle: " + squares + " times each\n");

                long totalMs = drawnShapes.stream().mapToLong(r -> r.timeMs).sum();
                long avgMs = totalMs / drawnShapes.size();
                fw.write("\n4) Average time to draw shapes:\n");
                fw.write(avgMs + " ms\n");
            }

            return file;
        } catch (Exception e) {
            System.out.println(RED + "ERROR: Failed to write log file: " + e.getMessage() + RESET);
            return new File("shape_log_FAILED.txt");
        }
    }

    static String describeLargest(ShapeRecord r) {
        if (r.name.equals("Square")) {
            return String.format("Square: %d (area: %.2f cm^2)", r.sizesCm.get(0), r.area);
        } else {
            return String.format("Triangle: %d,%d,%d (area: %.2f cm^2)",
                    r.sizesCm.get(0), r.sizesCm.get(1), r.sizesCm.get(2), r.area);
        }
    }

    static void printSummaryToConsole(File logFile) {
        if (drawnShapes.isEmpty()) {
            System.out.println(YELLOW + "No shapes were drawn." + RESET);
            System.out.println("Log file: " + logFile.getAbsolutePath());
            return;
        }

        long squares = drawnShapes.stream().filter(r -> r.name.equals("Square")).count();
        long triangles = drawnShapes.stream().filter(r -> r.name.equals("Triangle")).count();

        ShapeRecord largest = Collections.max(drawnShapes, Comparator.comparingDouble(r -> r.area));
        long totalMs = drawnShapes.stream().mapToLong(r -> r.timeMs).sum();
        long avgMs = totalMs / drawnShapes.size();

        System.out.println("Total shapes drawn: " + drawnShapes.size());
        System.out.println("Squares: " + squares + " | Triangles: " + triangles);
        System.out.println("Largest shape: " + describeLargest(largest));
        System.out.println("Average draw time: " + avgMs + " ms");
        System.out.println("Log file saved at: " + logFile.getAbsolutePath());
    }

    // -------------------- UI --------------------

    static void scanQRAnimation() throws InterruptedException {
        System.out.println(CYAN + "\n[Camera] Align QR code in front of the camera..." + RESET);
        Thread.sleep(600);

        String[] frames = {"|", "/", "-", "\\"};
        System.out.print(YELLOW + "[Camera] Scanning " + RESET);

        for (int i = 0; i < 20; i++) {
            System.out.print("\r" + YELLOW + "[Camera] Scanning " + frames[i % frames.length] + RESET);
            Thread.sleep(100);
        }
        System.out.println();

        System.out.println(CYAN + "[Decoder] Capturing image..." + RESET);
        Thread.sleep(700);
        System.out.println(CYAN + "[Decoder] Enhancing contrast..." + RESET);
        Thread.sleep(600);
        System.out.println(CYAN + "[Decoder] Decoding QR payload..." + RESET);
        Thread.sleep(800);

        System.out.print(PURPLE + "[Decode] [" + RESET);
        int steps = 25;
        for (int i = 0; i < steps; i++) {
            System.out.print(PURPLE + "=" + RESET);
            Thread.sleep(40);
        }
        System.out.println(PURPLE + "] 100%" + RESET);

        System.out.println(GREEN + "[Success] QR decoded. Please enter/confirm decoded text below.\n" + RESET);
    }

    static void printHeader() {
        System.out.println(PURPLE + "+---------------------------------------+" + RESET);
        System.out.println(PURPLE + "|       SWIFTBOT DRAW SHAPE (TASK 6)    |" + RESET);
        System.out.println(PURPLE + "+---------------------------------------+" + RESET);
        System.out.println(CYAN + "Supported QR formats:" + RESET);
        System.out.println("  " + GREEN + "S:16" + RESET + "              -> draw square side 16cm");
        System.out.println("  " + GREEN + "T:16:30:24" + RESET + "        -> draw triangle sides 16,30,24cm");
        System.out.println("  " + GREEN + "S:16&T:16:30:24" + RESET + "  -> multiple shapes (max 5)");
        System.out.println();
    }
  static String scanQRCodeReal() {

    System.out.println(CYAN + "\n========================================" + RESET);
    System.out.println(CYAN + "Scan QR code OR press X on SwiftBot to exit" + RESET);
    System.out.println("• Hold QR 15–30 cm from camera");
    System.out.println("========================================\n");

    // -------------------------
    // STEP 1: WAIT FOR QR PRESENCE
    // -------------------------
    while (true) {

        if (xPressed) return "";

        try {
            var img = swiftBot.getQRImage();

            // Try decode just to CHECK presence (don’t trust it yet)
            String test = swiftBot.decodeQRImage(img);

            if (test != null && !test.isEmpty()) {

                // -------------------------
                // STEP 2: QR DETECTED → SCANNING
                // -------------------------
                System.out.println(YELLOW + "[Camera] QR detected..." + RESET);

                String[] frames = {"|", "/", "-", "\\"};

                for (int i = 0; i < 20; i++) {
                    System.out.print("\r" + YELLOW + "[Camera] Scanning " + frames[i % 4] + RESET);
                    Thread.sleep(250); // ~5 seconds total
                }
                System.out.println();

                // -------------------------
                // STEP 3: FINAL DECODE (AFTER STABLE HOLD)
                // -------------------------
                var finalImg = swiftBot.getQRImage();
                String decoded = swiftBot.decodeQRImage(finalImg);

                if (decoded != null && !decoded.isEmpty()) {
                    System.out.println(GREEN + "[SUCCESS] QR Code: " + decoded + RESET);
                    return decoded;
                } else {
                    System.out.println(RED + "Scan failed. Hold steady and try again.\n" + RESET);
                }
            }

        } catch (Exception e) {
            System.out.println(RED + "Camera error..." + RESET);
        }

        try { Thread.sleep(300); } catch (Exception ignored) {}
    }
  }
}
