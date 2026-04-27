package com.labs.Janika;

import swiftbot.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


public class ZigzagTask {

   
    private static final SwiftBotAPI robot = SwiftBotAPI.INSTANCE;

    
    private static final double CM_PER_SEC_PER_UNIT = 0.42;
    private static final int TURN_90_MS  = 820;
    private static final int TURN_SPEED  = 40;

    // speed range
    private static final int MIN_SPEED = 50;
    private static final int MAX_SPEED = 54;

   //colours
    private static final int[] GREEN = {0, 255, 0};
    private static final int[] BLUE  = {0, 0, 255};

    // log file 
    private static final String LOG_FILE = "zigzag_log.txt";

    // journey data
    private static class Journey {
        final int    sectionLength;
        final int    sectionCount;
        final int    wheelSpeed;
        final double totalDistance;       // cm  (start → end of zigzag)
        final double durationSecs;        // seconds
        final double straightLineDist;    // cm  (straight line start → end)

        Journey(int sl, int sc, int ws, double td, double dur, double sld) {
            sectionLength    = sl;
            sectionCount     = sc;
            wheelSpeed       = ws;
            totalDistance    = td;
            durationSecs     = dur;
            straightLineDist = sld;
        }

        // Returns the QR input label
        String inputLabel() {
            return sectionLength + "-" + sectionCount;
        }
    }

 // PROGRAM STATE
    private static final List<Journey> journeys = new ArrayList<>();
    private static final Scanner       scanner  = new Scanner(System.in);
 
    // MAIN
    public static void main(String[] args) {
 
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            robot.stopMove();
            robot.disableUnderlights();
            robot.disableAllButtons();
        }));
 
        printWelcome();
 
        boolean running = true;
        while (running) {
 
            // 1. Scan QR code and validate input
            int[] input       = getValidInput();
            int sectionLength = input[0];
            int sectionCount  = input[1];
 
            // 2. Generate a safe random wheel speed
            int speed = generateWheelSpeed();
 
            // 3. Show the user what is about to happen
            printMovementSetup(sectionLength, sectionCount, speed);
 
            // 4. Work out how long to drive for each section
            int moveMs = calcMoveMs(sectionLength, speed);
 
            // 5. Run the zigzag forward
            long startTime = System.currentTimeMillis();
            doZigzag(sectionCount, speed, moveMs);
 
            // 6. Retrace the path back to start
            doRetrace(sectionCount, speed, moveMs);
            long endTime = System.currentTimeMillis();
 
            // 7. Calculate stats
            double totalDist    = (double) sectionLength * sectionCount;
            double duration     = (endTime - startTime) / 1000.0;
            double straightDist = calcStraightLine(sectionLength, sectionCount);
 
            Journey j = new Journey(sectionLength, sectionCount, speed,
                                    totalDist, duration, straightDist);
            journeys.add(j);
 
            // 8. Log and display journey results
            logAndPrint(j);
 
            // 9. Ask to do another journey or exit
            running = askToContinue();
        }
 
        // 10. Final summary before closing
        printFinalSummary();
    }
 
    // STEP 1 — QR SCANNING & VALIDATION
    private static int[] getValidInput() {
        while (true) {
            printHeader("QR CODE INPUT");
            System.out.println("Hold the QR code in front of the camera, then press ENTER.");
            scanner.nextLine();
 
            BufferedImage img = robot.getQRImage();
            if (img == null) {
                printError("Could not capture image. Please try again.");
                continue;
            }
 
            String qrText;
            try {
                qrText = robot.decodeQRImage(img).trim();
            } catch (IllegalArgumentException e) {
                printError("Image error: " + e.getMessage());
                continue;
            }
 
            if (qrText.isEmpty()) {
                printError("No QR code detected. Make sure it is centred and well lit.");
                continue;
            }
 
            System.out.println("QR code scanned: " + qrText);
 
            int[] result = validate(qrText);
            if (result != null) {
                System.out.println("Input is valid.");
                System.out.println("  Section length : " + result[0] + " cm");
                System.out.println("  Section count  : " + result[1]);
                return result;
            }
        }
    }
 
    private static int[] validate(String text) {
        String[] parts = text.split("-");
        if (parts.length != 2) {
            printError("Wrong format. Expected: Length-Sections  (e.g. 20-6)");
            return null;
        }
 
        int v1, v2;
        try {
            v1 = Integer.parseInt(parts[0].trim());
            v2 = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            printError("Both values must be whole numbers. (e.g. 20-6)");
            return null;
        }
 
        boolean lengthOk   = (v1 >= 15 && v1 <= 85);
        boolean sectionsOk = (v2 >= 2 && v2 <= 12 && v2 % 2 == 0);
 
        if (!lengthOk || !sectionsOk) {
            printHeader("ERROR");
            System.out.println("Invalid QR code input. What went wrong:");
            if (!lengthOk)
                System.out.println("  - Section length must be 15-85 cm. You entered: " + v1);
            if (!sectionsOk)
                System.out.println("  - Sections must be even, min 2, max 12. You entered: " + v2);
            System.out.println("Please scan a new QR code. Example: 20-6");
            return null;
        }
 
        return new int[]{v1, v2};
    }
 
    // STEP 2 — WHEEL SPEED GENERATION (additional feature)
    // Keeps speed in a safe range so robot doesn't go too fast or too slow
    private static int generateWheelSpeed() {
        printHeader("WHEEL SPEED");
        int speed = MIN_SPEED + new Random().nextInt((MAX_SPEED - MIN_SPEED) + 1);
        System.out.println("Random wheel speed generated: " + speed + "  (safe range: " + MIN_SPEED + "-" + MAX_SPEED + ")");
        return speed;
    }
 
    // STEP 3 — CALCULATIONS
    // time(ms) = (sectionLength / (speed x CM_PER_SEC_PER_UNIT)) x 1000
    private static int calcMoveMs(int sectionLength, int speed) {
        // calibrated at speed 50, correction adjusts for any speed in range
        double correction = 50.0 / speed;
        double cmPerSec   = 50 * CM_PER_SEC_PER_UNIT;
        double timeSecs   = (sectionLength / cmPerSec) * correction;
        return (int)(timeSecs * 1000);
    }
 
    // straightLine = root(horizontal squared + vertical squared) = root(2) x side
    private static double calcStraightLine(int sectionLength, int sectionCount) {
        double side = (sectionCount / 2.0) * sectionLength;
        return Math.sqrt(2.0) * side;
    }
 
    // STEP 4 — ZIGZAG MOVEMENT (start to end)
    private static void doZigzag(int sectionCount, int speed, int moveMs) {
        printHeader("ZIGZAG MOVEMENT");
        System.out.println("Starting zigzag movement...");

        try {
            for (int i = 1; i <= sectionCount; i++) {
                boolean isOdd = (i % 2 == 1);

                // set LED
                robot.fillUnderlights(isOdd ? GREEN : BLUE);
                System.out.println("[" + i + "/" + sectionCount + "] Moving  |  LED: " + (isOdd ? "GREEN" : "BLUE"));

                // move forward
                robot.move(speed, speed, moveMs);
                robot.stopMove();
                Thread.sleep(1000);

                // turn after every section except the last
                if (i < sectionCount) {
                    if (isOdd) {
                        // odd sections turn RIGHT
                        robot.move(TURN_SPEED, -TURN_SPEED, TURN_90_MS);
                    } else {
                        // even sections turn LEFT
                        robot.move(-TURN_SPEED, TURN_SPEED, TURN_90_MS);
                    }
                    robot.stopMove();
                    Thread.sleep(500);
                }
            }
            System.out.println("Zigzag completed.");

        } catch (InterruptedException e) {
            System.out.println("[ERROR] Zigzag interrupted.");
            robot.stopMove();
        }
    }

    private static void doRetrace(int sectionCount, int speed, int moveMs) {
        printHeader("RETURNING TO START");
        System.out.println("Turning 180 degrees...");

        try {
            // turn 180 to face back the way we came
            robot.move(TURN_SPEED, -TURN_SPEED, TURN_90_MS * 2);
            robot.stopMove();
            Thread.sleep(500);

            // now retrace each section in reverse using FORWARD movement
            for (int i = sectionCount; i >= 1; i--) {
                boolean isOdd = (i % 2 == 1);

                // same LED colours as forward
                robot.fillUnderlights(isOdd ? GREEN : BLUE);
                System.out.println("[" + i + "/" + sectionCount + "] Retracing  |  LED: " + (isOdd ? "GREEN" : "BLUE"));

                // move forward (robot is now facing the return direction)
                robot.move(speed, speed, moveMs);
                robot.stopMove();
                Thread.sleep(1000);

                // turn between sections
                if (i > 1) {
                    // the previous section (i-1) determines the turn
                    boolean prevIsOdd = ((i - 1) % 2 == 1);
                    if (prevIsOdd) {
                        // forward was RIGHT after odd, so retrace turns LEFT
                        robot.move(-TURN_SPEED, TURN_SPEED, TURN_90_MS);
                    } else {
                        // forward was LEFT after even, so retrace turns RIGHT
                        robot.move(TURN_SPEED, -TURN_SPEED, TURN_90_MS);
                    }
                    robot.stopMove();
                    Thread.sleep(500);
                }
            }

            robot.stopMove();
            robot.disableUnderlights();
            System.out.println("Back at START. LEDs off.");

        } catch (InterruptedException e) {
            System.out.println("[ERROR] Retrace interrupted.");
            robot.stopMove();
        }
    }
 
    // STEP 6 — JOURNEY LOGGING
    private static void logAndPrint(Journey j) {
        printHeader("JOURNEY RESULTS");
        System.out.printf("  Section length         : %d cm%n",       j.sectionLength);
        System.out.printf("  Number of sections     : %d%n",           j.sectionCount);
        System.out.printf("  Wheel speed            : %d%n",           j.wheelSpeed);
        System.out.printf("  Total zigzag distance  : %.0f cm%n",      j.totalDistance);
        System.out.printf("  Time taken             : %.1f seconds%n", j.durationSecs);
        System.out.printf("  Straight-line distance : %.2f cm%n",      j.straightLineDist);
 
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write("=== JOURNEY " + journeys.size() + " ===\n");
            fw.write("Section length         : " + j.sectionLength + " cm\n");
            fw.write("Number of sections     : " + j.sectionCount  + "\n");
            fw.write("Wheel speed            : " + j.wheelSpeed    + "\n");
            fw.write(String.format("Total zigzag distance  : %.0f cm%n",      j.totalDistance));
            fw.write(String.format("Duration               : %.1f seconds%n", j.durationSecs));
            fw.write(String.format("Straight-line distance : %.2f cm%n",      j.straightLineDist));
            fw.write("\n");
            System.out.println("  Log saved to: " + new File(LOG_FILE).getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[ERROR] Could not save log: " + e.getMessage());
        }
    }
 
    // STEP 7 — CONTINUE OR EXIT
    private static boolean askToContinue() {
        printHeader("REPEAT OR EXIT");
        System.out.println("Press 'Y' to scan another QR code");
        System.out.println("Press 'X' to exit the program");

        final boolean[] answered = {false};
        final boolean[] pressedY = {false};

        robot.enableButton(Button.Y, () -> {
            pressedY[0] = true;
            answered[0] = true;
        });
        robot.enableButton(Button.X, () -> {
            pressedY[0] = false;
            answered[0] = true;
        });
        robot.fillButtonLights();

        while (!answered[0]) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        robot.disableButton(Button.Y);
        robot.disableButton(Button.X);
        robot.disableButtonLights();

        return pressedY[0];
    }

    private static void printFinalSummary() {
        System.out.println();
        System.out.println("====================================================");
        System.out.println("                 PROGRAM SUMMARY");
        System.out.println("====================================================");
        System.out.println("Journeys completed: " + journeys.size());

        if (!journeys.isEmpty()) {
            Journey longest  = journeys.get(0);
            Journey shortest = journeys.get(0);

            for (Journey j : journeys) {
                if (j.straightLineDist > longest.straightLineDist)  longest  = j;
                if (j.straightLineDist < shortest.straightLineDist) shortest = j;
            }

            System.out.println();
            System.out.println("Longest straight-line distance:");
            System.out.println("  Input: " + longest.inputLabel() + "  |  " + String.format("%.2f", longest.straightLineDist) + " cm");
            System.out.println("Shortest straight-line distance:");
            System.out.println("  Input: " + shortest.inputLabel() + "  |  " + String.format("%.2f", shortest.straightLineDist) + " cm");
        }

        System.out.println();
        System.out.println("Log file: " + new File(LOG_FILE).getAbsolutePath());
        System.out.println("====================================================");
        System.out.println("             Thank you! Goodbye.");
        System.out.println("====================================================");

        // clean up everything before exiting
        
        robot.stopMove();
        robot.disableUnderlights();
        robot.disableAllButtons();
        robot.disableButtonLights();

       
        
    }
    // UI HELPERS
    private static void printWelcome() {
        System.out.println("====================================================");
        System.out.println("        SwiftBot Zigzag Movement Program");
        System.out.println("====================================================");
        System.out.println("This program makes the SwiftBot move in a zigzag.");
        System.out.println();
        System.out.println("QR code format:  Length-Sections  (e.g. 20-6)");
        System.out.println("  Length  : 15 to 85 cm");
        System.out.println("  Sections: even number, max 12");
        System.out.println("====================================================");
        System.out.println();
    }
 
    private static void printHeader(String title) {
        System.out.println();
        System.out.println("---- " + title + " ----");
    }
 
    private static void printError(String message) {
        System.out.println("[ERROR] " + message);
        System.out.println("        Please try again. Example format: 20-6");
    }
 
    private static void printMovementSetup(int sl, int sc, int ws) {
        System.out.println();
        System.out.println("Ready to move:");
        System.out.println("  Section length : " + sl + " cm");
        System.out.println("  Sections       : " + sc);
        System.out.println("  Wheel speed    : " + ws);
        System.out.println("Starting now...");
        System.out.println();
    }
}