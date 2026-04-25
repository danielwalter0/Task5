package com.labs.Lawrence;

public class UI {

    public static void welcomeUI(){
        System.out.println("SwiftBot");
        System.out.println("==================================================");
        System.out.println("SwiftBot Traffic Light Navigation System v1.0");
        System.out.println("==================================================");
        System.out.println();
        System.out.println("CONTROLS: [A] Start");
        System.out.println("> Press Button A to begin...");
        System.out.println();
        System.out.println("[Button A detected]");
        System.out.println("[OK] Initialization complete");
        System.out.println("[OK] Camera system: READY");
        System.out.println("[OK] Underlight system: READY");
        System.out.println("[OK] Movement system: READY");
    }

    public static void yellowUI(){
        System.out.println("\n--------------------------------------------------");
        System.out.println("[MOVING] Speed: 16 cm/s | Underlights: YELLOW");
        System.out.println("--------------------------------------------------");
        System.out.println();
        System.out.println("Scanning for traffic lights...");
    }

    public static void redUI(){
        System.out.println("[15:24:15] [DETECTING] Scanning for traffic lights...");
        System.out.println();
        System.out.println("============================================");
        System.out.println("TRAFFIC LIGHT DETECTED - RED");
        System.out.println("============================================");
        System.out.println();
        System.out.println("ACTION: STOP");
        System.out.println("------------------------------------------------------------");
        System.out.println("Underlights set to RED");
        System.out.println("Stopping at traffic light...");
        System.out.println();
        System.out.println("[STOPPED] Waiting for 1 second...");
    }
    public static void greenUI(){
        System.out.println("================= TRAFFIC LIGHT DETECTED - GREEN =================");
        System.out.println();
        System.out.println("ACTION: PROCEED");
        System.out.println("------------------------------------------------------------");
        System.out.println("[OK] Underlights set to GREEN");
        System.out.println("[OR] Accelerating to pass...");
    }

    public static void blueUI(){
        System.out.println();
        System.out.println("====================================");
        System.out.println("TRAFFIC LIGHT DETECTED - BLUE (EMERGENCY)");
        System.out.println("====================================");
        System.out.println();
        System.out.println("ACTION: YIELD (Emergency Protocol)");
        System.out.println("------------------------------------------------------------");
        System.out.println("[STOPPED] Stopping for 1 second... | Underlights: BLUE (blinking)");
        System.out.println("[OK] Turning LEFT 90 degrees...");
        System.out.println("[MOVING] Moving forward for 1 second...");
        System.out.println("[STOPPED] Stopping |  Returning to original path");
        System.out.println("[OK] Turn RIGHT 90 degrees...");
    }

    public static void confirmationUI(){
        System.out.println("""
                3 LIGHTS DETECTED
                
                Would you like to continue?
                On the SwiftBot press:
                [Y] YES     [N] NO
                """);
    }
}

