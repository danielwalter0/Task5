package com.labs.Lawrence;

class Movement {

    static void moveYellow() throws InterruptedException {
        TrafficLights.api.fillUnderlights(Colours.yellow);

        Thread cameraThread = new Thread(() -> {
            try {
                while (true) {
                    TrafficLights.camera();
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                // Stops camera loop
            }
        });
        cameraThread.start();
        UI.yellowUI();
        TrafficLights.api.move(57, 57, 500000000);
        cameraThread.interrupt();
    }

    static void moveRed() throws InterruptedException {
        TrafficLights.api.fillUnderlights(Colours.red);
        TrafficLights.api.move(0, 0, 1000);

        if (TrafficLights.numOfDetectedLights % 3 == 0) {
            UI.confirmationUI();
            Buttons.terminateProgram();
            if (Buttons.getUserChoice().equals("X")) {
                System.exit(0);
            } else {
                moveYellow();
                return;
            }
        }
        moveYellow();
    }

    static void moveGreen() throws InterruptedException {
        TrafficLights.api.fillUnderlights(Colours.green);
        TrafficLights.api.move(100, 100, 2000);
        TrafficLights.api.move(0, 0, 1000);

        if (TrafficLights.numOfDetectedLights % 3 == 0) {
            UI.confirmationUI();
            Buttons.terminateProgram();
            if (Buttons.getUserChoice().equals("X")) {
                System.exit(0);
            } else {
                moveYellow();
                return;
            }
        }
        moveYellow();
    }

    static void moveBlue() throws InterruptedException {
        TrafficLights.api.move(0, 0, 1000);

        for (int i = 0; i < 2; i++) {
            TrafficLights.api.fillUnderlights(Colours.off);
            Thread.sleep(250);
            TrafficLights.api.fillUnderlights(Colours.blue);
            Thread.sleep(250);
        }

        TrafficLights.api.move(0, 100, 1000);
        TrafficLights.api.move(45, 45, 1000);
        TrafficLights.api.move(0, 0, 1000);
        TrafficLights.api.move(-45, -45, 1000);
        TrafficLights.api.move(0, -100, 1000);
        TrafficLights.api.move(0, 0, 1000);

        if (TrafficLights.numOfDetectedLights % 3 == 0) {
            UI.confirmationUI();
            Buttons.terminateProgram();
            if (Buttons.getUserChoice().equals("X")) {
                System.exit(0);
            } else {
                moveYellow();
                return;
            }
        }
        moveYellow();
    }
}