package com.labs.Lawrence;
import swiftbot.Button;

public class Buttons {

    private static boolean pressed = false;
    private static String choiceOfUser = "";
    private static final Object lock = new Object();

    public static void waitForButtonPressed() {

        pressed = false;

        TrafficLights.api.setButtonLight(Button.A, true);

        TrafficLights.api.enableButton(Button.A, () -> {
            synchronized (lock) {
                pressed = true;
                lock.notifyAll();
            }

            TrafficLights.api.disableButtonLights();
            TrafficLights.api.disableButton(Button.A);
            System.out.println("\nStarting...");

        });

        synchronized (lock) {
            while (!pressed) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Something went terribly wrong!");
                }
            }
        }
    }

    public static void terminateProgram() {

        pressed = false;

        TrafficLights.api.setButtonLight(Button.X, true);
        TrafficLights.api.setButtonLight(Button.Y, true);

        TrafficLights.api.enableButton(Button.X, () -> {
            synchronized (lock) {
                pressed = true;
                choiceOfUser = "X";
                lock.notifyAll();
            }

            TrafficLights.api.disableButtonLights();
            TrafficLights.api.disableButton(Button.X);
            TrafficLights.api.disableButton(Button.Y);
            TrafficLights.api.disableUnderlights();
        });

        TrafficLights.api.enableButton(Button.Y, () -> {
            synchronized (lock) {
                pressed = true;
                choiceOfUser = "Y";
                lock.notifyAll();
            }
            TrafficLights.api.disableButtonLights();
            TrafficLights.api.disableUnderlights();
            TrafficLights.api.disableButton(Button.Y);
            TrafficLights.api.disableButton(Button.X);
        });

        synchronized (lock) {
            while (!pressed) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Something went terribly wrong!");
                }
            }
        }
    }

    public static String getUserChoice() {
        return choiceOfUser;
    }
}