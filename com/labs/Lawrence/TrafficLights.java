package com.labs.Lawrence;
import swiftbot.ImageSize;
import swiftbot.SwiftBotAPI;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import static java.lang.System.out;

public class TrafficLights {
    public static void main(String[] args) throws InterruptedException {
        startProgram();
    }
    public final static SwiftBotAPI api = SwiftBotAPI.INSTANCE;

    public static int numOfDetectedLights = 0;

    static void startProgram() throws InterruptedException {
        UI.welcomeUI();
        Buttons.waitForButtonPressed();
        Thread.sleep(500);
        Movement.moveYellow();
    }

    // Takes a Picture
    static void camera() throws InterruptedException {

        //SwiftBot takes 240x240 image
        BufferedImage image = api.takeStill(ImageSize.SQUARE_240x240);

        if (image == null) {
            out.print("\nERROR: image is null");
            System.exit(5);
        }
        else {
            try {
                ImageIO.write(image, "png", new File("/data/home/pi/Image.png"));
            } catch (Exception e) {
                out.println("\nCamera not enabled!");
                out.println("Try running the following command: ");
                out.println("sudo raspi-config nonint do_camera 0\n");
                out.println("Then reboot using the following command: ");
                out.println("sudo reboot\n");
                System.exit(5);
            }
            // Saves the photo to a directory

            detectTrafficLight(image);

        }
    }


    // Detects the colour of the traffic light from the image
    private static void detectTrafficLight(BufferedImage image) throws InterruptedException {
        int width = image.getWidth();
        int height = image.getHeight();

        int redPixelCount = 0;
        int greenPixelCount = 0;
        int bluePixelCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int pixel = image.getRGB(x, y);

                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                // --- Red Detection ---
                if (r > 180 && g < 100 && b < 100) {
                    redPixelCount++;
                }

                // --- Green Detection ---
                if (g > 180 && r < 100 && b < 100) {
                    greenPixelCount++;
                }

                // --- Blue Detection ---
                if (b > 180 && r < 100 && g < 100) {
                    bluePixelCount++;
                }
            }
        }

        // Decide which colour dominates
        if (redPixelCount > greenPixelCount && redPixelCount > bluePixelCount) {
            numOfDetectedLights++;
            UI.redUI();
            Movement.moveRed();
        } else if (greenPixelCount > redPixelCount && greenPixelCount > bluePixelCount) {
            numOfDetectedLights++;
            UI.greenUI();
            Movement.moveGreen();
        } else if (bluePixelCount > redPixelCount && bluePixelCount > greenPixelCount) {
            numOfDetectedLights++;
            UI.blueUI();
            Movement.moveBlue();
        }

    }
}