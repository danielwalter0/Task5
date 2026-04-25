package com.labs.Laur;

import swiftbot.SwiftBotAPI;

import java.awt.image.BufferedImage;
import java.util.Scanner;

public class Utility
{
    public static void clearConsole()
    {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void printSpyBot(String color)
    {
        final String RESET = "\u001B[0m";

        System.out.println();
        System.out.println(color + "  ____              ____        _      ");
        System.out.println(" / ___| _ __  _   _| __ )  ___ | |_    ");
        System.out.println(" \\___ \\| '_ \\| | | |  _ \\ / _ \\| __|   ");
        System.out.println("  ___) | |_) | |_| | |_) | (_) | |_    ");
        System.out.println(" |____/| .__/ \\__, |____/ \\___/ \\__|   ");
        System.out.println("       |_|    |___/                    " + RESET);
        System.out.println();
    }

    public static void print(String line, String textColor, boolean header)
    {
        String color = "";
        String reset = "\u001B[0m";

        if (textColor.equals("cyan"))
            color = "\u001B[36m";
        else if (textColor.equals("red"))
            color = "\u001B[31m";
        else if (textColor.equals("yellow"))
            color = "\u001B[33m";
        else if (textColor.equals("green"))
            color = "\u001B[32m";

        if (header)
            printSpyBot(color);

        int padding = 5;
        int contentWidth = line.length() + padding * 2;

        String border = "+" + "-".repeat(contentWidth) + "+";

        System.out.println();
        System.out.println(color + border);

        System.out.println("|" + " ".repeat(padding) + line
                + " ".repeat(padding) + "|");

        System.out.println(border + reset);
        System.out.println();
    }

    public static void print(String line1, String line2, String textColor, boolean header)
    {

        String color = "";
        String reset = "\u001B[0m";

        if (textColor.equals("cyan"))
        {
            color = "\u001B[36m";
        }
        else if (textColor.equals("red"))
        {
            color = "\u001B[31m";
        }
        else if (textColor.equals("yellow"))
        {
            color = "\u001B[33m";
        }
        else if (textColor.equals("green"))
        {
            color = "\u001B[32m";
        }

        if (header) printSpyBot(color);


        int padding = 5;

        int maxLength = Math.max(line1.length(), line2.length());
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

        System.out.println(border + reset);
        System.out.println();


    };

    public static void print(String line1, String line2, String line3, String textColor, boolean header)
    {
        String color = "";
        String reset = "\u001B[0m";

        if (textColor.equals("cyan"))
        {
            color = "\u001B[36m";
        }
        else if (textColor.equals("red"))
        {
            color = "\u001B[31m";
        }
        else if (textColor.equals("yellow"))
        {
            color = "\u001B[33m";
        }
        else if (textColor.equals("green"))
        {
            color = "\u001B[32m";
        }

        if (header) printSpyBot(color);

        int padding = 5;

        int maxLength = Math.max(
                line1.length(),
                Math.max(line2.length(), line3.length())
        );

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

        System.out.println(border + reset);
        System.out.println();

    };

    public static byte validateOption(String option1, String option2) throws InterruptedException
    {
        byte option;
        while(true)
        {
            Scanner scan = new Scanner(System.in);
            print(option1, option2,"cyan", true);


            System.out.print("Enter Option: ");
            String input = scan.nextLine();

            try
            {
                option = (byte) Integer.parseInt(input);
                if (option == 1 || option == 2)  break;
                else
                {
                    print("Invalid input. Please try again entering number 1 or 2", "red", false);
                    Thread.sleep(2000);
                }

            }
            catch (Exception e)
            {
                print("Invalid input. Please try again entering number 1 or 2", "red", false);
                Thread.sleep(2000);
            }
        }
        return option;
    }

    public static byte validateOption(String option1) throws InterruptedException
    {
        byte option;
        while(true)
        {
            Scanner scan = new Scanner(System.in);
            print(option1,"cyan", true);


            System.out.print("Enter Option: ");
            String input = scan.nextLine();

            try
            {
                option = (byte) Integer.parseInt(input);
                if (option == 1 || option == 2)  break;
                else
                {
                    print("Invalid input. Please try again entering number 1", "red", false);
                    Thread.sleep(2000);
                }

            }
            catch (Exception e)
            {
                print("Invalid input. Please try again entering number 1", "red", false);
                Thread.sleep(2000);
            }
        }
        return option;
    }

    public static byte validateOption(String option1, String option2, String option3) throws InterruptedException
    {
        byte option;
        while(true)
        {
            Scanner scan = new Scanner(System.in);
            print(option1, option2, option3, "cyan", true);


            System.out.print("Enter Option: ");
            String input = scan.nextLine();

            try
            {
                option = (byte) Integer.parseInt(input);
                if (option == 1 || option == 2 || option == 3)  break;
                else
                {
                    print("Invalid input. Please try again entering number 1, 2 or 3", "red", false);
                    Thread.sleep(2000);
                }

            }
            catch (Exception e)
            {
                print("Invalid input. Please try again entering number 1, 2 or 3", "red", false);
                Thread.sleep(2000);
            }
        }
        return option;
    }


}
