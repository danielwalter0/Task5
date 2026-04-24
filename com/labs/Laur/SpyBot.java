package com.labs.Laur;
import swiftbot.Button;
import swiftbot.SwiftBotAPI;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class SpyBot
{


    public record Agent(String callsign, String location)
    {
    }

    static SwiftBotAPI swiftBot;
    static String orientation = "AB";

    public static void main(String[] args) throws InterruptedException, IOException
    {

        swiftBot = SwiftBotAPI.INSTANCE;
        File logFile = new File("log.txt");
        FileWriter fileWriter = new FileWriter(logFile, true);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        try
        {
            if (!logFile.exists())
            {
                logFile.createNewFile();
                System.out.println("Log file created: " + logFile.getName());

            }
        }
        catch (Exception e)
        {
            System.out.println("Could not create log file.");
            System.exit(0);
        }

        LinkedHashMap<String, String> morseToChar = new LinkedHashMap<>();
        File morseCodeDictionary = new File("MorseCodeDictionary.txt");


        //Reading data from the dictionary and converting it into a hashtable for key value pairs
        //to later easily map the morse code to letters
        try (Scanner scan = new Scanner(morseCodeDictionary))
        {
            while (scan.hasNextLine())
            {
                String line = scan.nextLine();
                String letter = Character.toString(line.charAt(0));
                String morseChar = line.substring(1).trim();
                morseToChar.put(morseChar, letter);
            }
        }
        catch (Exception e)
        {
            System.out.println("Could not find MorseCodeDictionary.txt");
            System.exit(1);
        }


        final Agent[] agents =
                {
                        new Agent("Allmight", "A"),
                        new Agent("Hawks", "B"),
                        new Agent("Endeavour", "C")
                };




        Main:
        while (true)
        {
            Utility.clearConsole();

            Agent sender;
            ArrayList<String> wordsMorse = new ArrayList<>();
            String plainWords = "";
            String destination= "";

            byte option = Utility.validateOption("[1] Send Message", "[2] Exit");

            if (option == 1)
            {
                Utility.clearConsole();
                Utility.print("USER AUTHENTICATION REQUIRED - Please point the SwiftBot camera to the qr code and hold still", "yellow", true);

                option = Utility.validateOption("[1] Take Picture", "[2] Go Back");
                if (option == 1)
                {
                    sender = authenticateUser(swiftBot, agents);
                    if (sender == null)
                    {
                        Utility.print("AUTHENTICATION FILED - Please try again", "red", true);
                        printWriter.println("FAILED AUTHENTICATION | time=" + LocalTime.now());
                        continue;
                    }
                }
                else continue;

                if(sender.location().equals("A")) orientation = "AB";
                else if(sender.location().equals("B")) orientation = "BC";
                else orientation = "CA";


                Utility.print("Select Priority", "cyan", true);
                option = Utility.validateOption("[1] NORMAL", "[2] URGENT", "[3] CRITICAL");
                LocalTime recordedTime;
                while (true)
                {   recordedTime = LocalTime.now();
                    wordsMorse = recordMorse(swiftBot);
                    plainWords = decodeMorse(wordsMorse, morseToChar);
                    if(!plainWords.equals("Error"))
                    {
                        if(plainWords.length()==0)
                        {
                            Utility.print("Message empty", "red", true);
                            continue;
                        }

                        destination = Character.toString(plainWords.charAt(0));

                        if(!(destination.equals("A") || destination.equals("B") || destination.equals("C")))
                        {
                            Utility.print("Invalid destination. Must be A, B, or C.", "red", true);
                            continue;
                        }

                        break;
                    }
                }



                computeRouteAndMove(swiftBot, sender.location(), destination, option);


                deliverMessage(
                        swiftBot,
                        sender,
                        destination,
                        option,
                        wordsMorse,
                        morseToChar,
                        agents,
                        printWriter,
                        plainWords,
                        recordedTime
                );

                // turn around before returning
                turnRight(swiftBot);

                // return to sender
                computeRouteAndMove(swiftBot, destination, sender.location(), option);

            }
            else
            {
                System.out.println("Log file path:" + logFile.getAbsolutePath());
                System.exit(0);


            }


        }
    }

    public static Agent authenticateUser(SwiftBotAPI swiftBot, Agent[] agents) throws InterruptedException
    {
        Utility.clearConsole();

        Utility.print("Taking picture in: 3s", "green", false);
        Thread.sleep(1000);

        Utility.print("Taking picture in: 2s", "yellow", false);
        Thread.sleep(1000);

        Utility.print("Taking picture in: 1s", "red", false);
        Thread.sleep(1000);

        Utility.clearConsole();

        BufferedImage img = swiftBot.getQRImage();
        Utility.print("Picture taken", "cyan", false);

        String qr;

        try
        {
            qr = swiftBot.decodeQRImage(img);
        }
        catch (IllegalArgumentException e)
        {
            Utility.print("Error reading QR image", "red", false);
            return null;
        }


        System.out.println("Decoded QR: [" + qr + "]");

        // No QR detected
        if (qr == null || qr.length() == 0)
        {
            Utility.print("No QR code detected", "red", false);
            return null;
        }

        // Remove optional < >
        qr = qr.replace("<", "").replace(">", "");

        // Validate format
        if (!qr.contains(":"))
        {
            Utility.print("Invalid QR format. Expected callsign:location", "red", false);
            return null;
        }

        String[] parts = qr.split(":");

        if (parts.length != 2)
        {
            Utility.print("Invalid QR format", "red", false);
            return null;
        }

        String callSign = parts[0].trim();
        String location = parts[1].trim();

        System.out.println("Call Sign: " + callSign);
        System.out.println("Location: " + location);

        // Check against stored agents
        for (int i = 0; i < agents.length; i++)
        {
            if (agents[i].callsign().equals(callSign) &&
                    agents[i].location().equals(location))
            {
                return agents[i];
            }
        }

        Utility.print("Agent not recognised", "red", false);
        return null;
    }

    public static ArrayList<String> recordMorse(SwiftBotAPI swiftBot) throws InterruptedException
    {
        Utility.clearConsole();
        Utility.print("Please enter your message", "cyan", true);

        String color = "\u001B[36m";
        String reset = "\u001B[0m";
        int padding = 5;

        String line1 = "X = '.'";
        String line2 = "Y = '-'";
        String line3 = "A = end character";
        String line4 = "B = end word";
        String line5 = "End message = Morse for '0'";

        int maxLength = line5.length();
        int contentWidth = maxLength + padding * 2;

        String border = "+" + "-".repeat(contentWidth) + "+";

        System.out.println();
        System.out.println(color + border);

        System.out.println("|" + " ".repeat(padding) + line1
                + " ".repeat(maxLength - line1.length())
                + " ".repeat(padding) + "|");

        System.out.println("|" + " ".repeat(padding) + line2
                + " ".repeat(maxLength - line2.length())
                + " ".repeat(padding) + "|");

        System.out.println("|" + " ".repeat(padding) + line3
                + " ".repeat(maxLength - line3.length())
                + " ".repeat(padding) + "|");

        System.out.println("|" + " ".repeat(padding) + line4
                + " ".repeat(maxLength - line4.length())
                + " ".repeat(padding) + "|");

        System.out.println("|" + " ".repeat(padding) + line5
                + " ".repeat(padding) + "|");

        System.out.println(border + reset);
        System.out.println();

        ArrayList<String> wordsMorse = new ArrayList<>();

        AtomicReference<String> currentWordMorse = new AtomicReference<>("");
        AtomicReference<String> currentCharMorse = new AtomicReference<>("");

        AtomicBoolean endMessageFlag = new AtomicBoolean(false);

        // DOT
        swiftBot.enableButton(Button.X, () -> {
            System.out.println(".");
            currentCharMorse.set(currentCharMorse.get() + ".");
        });

        // DASH
        swiftBot.enableButton(Button.Y, () -> {
            System.out.println("-");
            currentCharMorse.set(currentCharMorse.get() + "-");
        });

        // END CHARACTER
        swiftBot.enableButton(Button.A, () -> {

            if (currentCharMorse.get().equals(""))
            {
                Utility.print("Error: No dot/dash entered. Please enter a dot or dash first", "red", false);
                return;
            }

            // End message detection (0 = -----)
            if (currentCharMorse.get().equals("-----"))
            {
                endMessageFlag.set(true);
                currentCharMorse.set("");
                return;
            }

            System.out.println("<End of Char>");

            if (currentWordMorse.get().equals(""))
            {
                currentWordMorse.set(currentCharMorse.get());
            }
            else
            {
                currentWordMorse.set(currentWordMorse.get() + " " + currentCharMorse.get());
            }

            currentCharMorse.set("");
        });

        // END WORD
        swiftBot.enableButton(Button.B, () -> {

            if (!currentWordMorse.get().equals(""))
            {
                wordsMorse.add(currentWordMorse.get());
                System.out.println("<End of Word>");
                currentWordMorse.set("");
            }
        });

        // Wait until end message
        while (!endMessageFlag.get())
        {
            Thread.sleep(100);
        }

        // Save last word if unfinished
        if (!currentWordMorse.get().equals(""))
        {
            wordsMorse.add(currentWordMorse.get());
        }

        // Disable buttons after recording
        swiftBot.disableButton(Button.X);
        swiftBot.disableButton(Button.Y);
        swiftBot.disableButton(Button.A);
        swiftBot.disableButton(Button.B);


        return wordsMorse;
    }

    public static String decodeMorse(ArrayList<String> wordsMorse, LinkedHashMap<String, String> morseToChar) throws InterruptedException
    {
        String plainWords = "";

        for (int i = 0; i < wordsMorse.size(); i++)
        {
            String plainWord = "";
            String wordMorse = wordsMorse.get(i);

            // Split characters by spaces
            String[] charsMorse = wordMorse.split(" ");

            for (int j = 0; j < charsMorse.length; j++)
            {
                String morseChar = charsMorse[j];

                String plainChar = morseToChar.get(morseChar);

                if (plainChar == null)
                {
                    Utility.print("Error: No such morse character found", "red", false);
                    Thread.sleep(3000);
                    return "Error";
                }

                if (plainChar.equals("0"))
                {
                    break;
                }

                plainWord += plainChar;
            }

            if (plainWords.length() == 0)
                plainWords = plainWord;
            else
                plainWords += " " + plainWord;
        }

        return plainWords;
    }

    public static void transmitMorseAsLED(ArrayList<String> wordsMorse, SwiftBotAPI swiftBot, boolean sendEndSignal) throws InterruptedException
    {
        int delay = 1100;

        for(int w = 0; w < wordsMorse.size(); w++)
        {
            String word = wordsMorse.get(w);
            String[] chars = word.split(" ");

            for(String morseChar : chars)
            {
                for(char symbol : morseChar.toCharArray())
                {
                    if(symbol == '.')
                        swiftBot.fillUnderlights(new int[]{255,255,255});
                    else
                        swiftBot.fillUnderlights(new int[]{0,0,255});

                    Thread.sleep(delay);
                    swiftBot.disableUnderlights();
                }

                // end character
                swiftBot.fillUnderlights(new int[]{255,191,0});
                Thread.sleep(delay);
                swiftBot.disableUnderlights();
            }

            if(w < wordsMorse.size()-1)
            {
                swiftBot.fillUnderlights(new int[]{255,0,0});
                Thread.sleep(delay);
                swiftBot.disableUnderlights();
            }
        }

        if(sendEndSignal)
        {
            swiftBot.fillUnderlights(new int[]{0,255,0});
            Thread.sleep(delay);
            swiftBot.disableUnderlights();
        }
    }

    public static void computeRouteAndMove(SwiftBotAPI swiftBot, String start, String destination, int priority) throws InterruptedException
    {
        int velocity;

        if(priority == 1) velocity = 70;
        else if(priority == 2) velocity = 90;
        else velocity = 100;

        int movementTime = 2000;

        String current = start;

        Thread blinkThread = startTravelBlink(swiftBot, priority);

        while(!current.equals(destination))
        {
            if(current.equals("A"))
            {
                if(destination.equals("B"))
                {
                    swiftBot.move(velocity, velocity, movementTime);
                    current = "B";
                    orientation = "AB";
                }
                else
                {
                    turnRight(swiftBot);
                    swiftBot.move(velocity, velocity, movementTime);
                    current = "C";
                    orientation = "AC";
                }
            }

            else if(current.equals("B"))
            {
                if(destination.equals("A"))
                {
                    swiftBot.move(velocity, velocity, movementTime);
                    current = "A";
                    orientation = "BA";
                }
                else
                {
                    turnLeft(swiftBot);
                    swiftBot.move(velocity, velocity, movementTime);
                    current = "C";
                    orientation = "BC";
                }
            }

            else if(current.equals("C"))
            {
                if(destination.equals("A"))
                {
                    turnLeft(swiftBot);
                    swiftBot.move(velocity, velocity, movementTime);
                    current = "A";
                    orientation = "CA";
                }
                else
                {
                    swiftBot.move(velocity, velocity, movementTime);
                    current = "B";
                    orientation = "CB";
                }
            }
        }

        blinkThread.interrupt();
        swiftBot.disableUnderlights();
    }

    public static Thread startTravelBlink(SwiftBotAPI swiftBot, int priority)
    {
        Thread blinkThread = new Thread(() -> {

            try
            {
                while(!Thread.currentThread().isInterrupted())
                {
                    if(priority == 1) // NORMAL
                    {
                        swiftBot.fillUnderlights(new int[]{0,255,0}); // green
                        Thread.sleep(500);
                        swiftBot.disableUnderlights();
                        Thread.sleep(500);
                    }

                    else if(priority == 2) // URGENT
                    {
                        swiftBot.fillUnderlights(new int[]{255,165,0}); // orange
                        Thread.sleep(200);
                        swiftBot.disableUnderlights();
                        Thread.sleep(200);
                    }

                    else // CRITICAL
                    {
                        swiftBot.fillUnderlights(new int[]{255,0,0}); // solid red
                        Thread.sleep(200);
                    }
                }
            }
            catch (InterruptedException ignored) {}
        });

        blinkThread.start();
        return blinkThread;
    }

    public static void moveWithPriority(SwiftBotAPI swiftBot, int velocity, int time) throws InterruptedException
    {
        swiftBot.move(velocity, velocity, time);
    }

    public static void moveForward(SwiftBotAPI swiftBot, int velocity, int time) throws InterruptedException
    {
        swiftBot.move(velocity, velocity, time);
        swiftBot.stopMove();
    }

    public static void turnLeft(SwiftBotAPI swiftBot) throws InterruptedException
    {
        swiftBot.move(-60, 60, 900);
        swiftBot.stopMove();

        // update orientation
        switch(orientation)
        {
            case "AB": orientation = "AC"; break;
            case "AC": orientation = "CB"; break;
            case "CB": orientation = "CA"; break;
            case "CA": orientation = "BA"; break;
            case "BA": orientation = "BC"; break;
            case "BC": orientation = "AB"; break;
        }
    }

    public static void turnRight(SwiftBotAPI swiftBot) throws InterruptedException
    {
        swiftBot.move(60, -60, 900);
        swiftBot.stopMove();

        switch(orientation)
        {
            case "AB": orientation = "BC"; break;
            case "BC": orientation = "BA"; break;
            case "BA": orientation = "CA"; break;
            case "CA": orientation = "CB"; break;
            case "CB": orientation = "AC"; break;
            case "AC": orientation = "AB"; break;
        }
    }

    public static String getSenderMorse(String location)
    {
        if(location.equals("A")) return ".-";
        if(location.equals("B")) return "-...";
        if(location.equals("C")) return "-.-.";
        return "";
    }

    public static void deliverMessage(
            SwiftBotAPI swiftBot,
            Agent sender,
            String destination,
            int priority,
            ArrayList<String> wordsMorse,
            LinkedHashMap<String,String> morseToChar,
            Agent[] agents,
            PrintWriter log,
            String plainMessage,
            LocalTime recordedTime
    ) throws InterruptedException
    {

        swiftBot.fillUnderlights(new int[]{255,0,0});
        Thread.sleep(2000);
        swiftBot.disableUnderlights();

        Utility.print("Receiver authentication required", "yellow", true);
        Thread.sleep(1000);

        Agent receiver = authenticateUser(swiftBot, agents);

        if(receiver == null)
        {
            Utility.print("Receiver authentication failed", "red", true);

            for(int i=0;i<10;i++)
            {
                swiftBot.fillUnderlights(new int[]{255,0,0});
                Thread.sleep(150);
                swiftBot.disableUnderlights();
                Thread.sleep(150);
            }

            log.println(
                    "FAILED DELIVERY | sender=" + sender.callsign() +
                            " senderLocation=" + sender.location() +
                            " destination=" + destination +
                            " message=" + plainMessage +
                            " time=" + LocalTime.now()
            );

            return; // robot returns in main
        }

        Utility.print("Transmitting message...", "green", true);

        // Send sender identifier first
        ArrayList<String> senderSignal = new ArrayList<>();
        senderSignal.add(getSenderMorse(sender.location()));

        transmitMorseAsLED(senderSignal, swiftBot, false); // no green
        transmitMorseAsLED(wordsMorse, swiftBot, true);    // real message end

        LocalTime deliveredTime = LocalTime.now();

        log.println(
                "MESSAGE DELIVERED | sender=" + sender.callsign() +
                        " senderLocation=" + sender.location() +
                        " receiver=" + receiver.callsign() +
                        " receiverLocation=" + destination +
                        " message=\"" + plainMessage + "\"" +
                        " priority=" + priority +
                        " recordedTime=" + recordedTime +
                        " deliveredTime=" + deliveredTime
        );

        Utility.print("Message delivered. Waiting 10 seconds...", "cyan", false);

        Thread.sleep(10000);
    }


}



