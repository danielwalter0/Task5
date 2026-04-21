package src.main.java.com.labs;

import com.labs.Laur.SpyBot;
import src.main.java.com.labs.Daniel.GameController;

import java.io.IOException;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Task5{
    public static void main(String[] args) throws InterruptedException {
        printWelcomeMessage();
        printChooseMessage();
        printSelectMenu();
        while(!validate());
        startGame();

        sc.close();
    }
    static Scanner sc = new Scanner(System.in);
    static int choice;

    public static void printWelcomeMessage() {
        String violet = "\u001B[35m";  // ANSI purple (violet)
        String reset = "\u001B[0m";

        System.out.println(violet +
                " _       __________    __________  __  _________\n" +
                "| |     / / ____/ /   / ____/ __ \\/  |/  / ____/\n" +
                "| | /| / / __/ / /   / /   / / / / /|_/ / __/   \n" +
                "| |/ |/ / /___/ /___/ /___/ /_/ / /  / / /___   \n" +
                "|__/|__/_____/_____/\\____/\\____/_/  /_/_____/   \n" +
                reset);

        //used https://patorjk.com/software/taag
        //Slant font
    }
    public static void printChooseMessage() {
        String cyan = "\u001B[36m";  // bright cyan
        String reset = "\u001B[0m";

        System.out.println(cyan +
                "   ________                             __  __                                        \n" +
                "  / ____/ /_  ____  ____  ________     / /_/ /_  ___     ____ _____ _____ ___  ___  _ \n" +
                " / /   / __ \\/ __ \\/ __ \\/ ___/ _ \\   / __/ __ \\/ _ \\   / __ `/ __ `/ __ `__ \\/ _ \\(_)\n" +
                "/ /___/ / / / /_/ / /_/ (__  )  __/  / /_/ / / /  __/  / /_/ / /_/ / / / / / /  __/   \n" +
                "\\____/_/ /_/\\____/\\____/____/\\___/   \\__/_/ /_/\\___/   \\__, /\\__,_/_/ /_/ /_/\\___(_)  \n" +
                "                                                      /____/                            "
                + reset);
    }

    public static void printSelectMenu() {
        String yellow = "\u001B[33m"; // yellow
        String reset = "\u001B[0m";

        System.out.println(yellow +
                "=========================================\n" +
                "               GAME MENU                 \n" +
                "=========================================\n" +
                "  1) Master Mind\n" +
                "  2) Zigzag\n" +
                "  3) Snakes and Ladders\n" +
                "  4) Traffic Light\n" +
                "  5) SpyBot\n" +
                "  6) Draw Shape\n" +
                "  7) Noughts and Crosses\n" +
                "  8) Search for Light\n" +
                "  9) Dance\n" +
                " 10) Detect Object\n" +
                "-----------------------------------------\n" +
                " Enter the game number (1-10): \n" +
                "=========================================\n"
                + reset);
    }
    public static boolean validate(){
        try{
            choice = sc.nextInt();
            if(choice < 1 || choice > 10){
                System.out.println("\u001B[31mError. Enter the correct number (1-10)\u001B[0m");
                return false;
            }
        }catch(InputMismatchException e){
            System.out.println("\u001B[31mError. Enter the correct number (1-10)\u001B[0m");
            sc.nextLine();
            return false;
        }
        return true;
    }

    public static void startGame() throws InterruptedException {
        switch(choice){
            case 1:
                GameController gameController = new GameController();
                gameController.startGame();
                break;
            case 2:
                //code to start game 2
                break;
            case 3:
                //code to start game 3
                break;
            case 4:
                //code to start game 4
                break;
            case 5:
                //code to start game 5
                break;
            case 6:
                //code to start game 6
                break;
            case 7:
                //code to start game 7
                break;
            case 8:
                SB_SearchForLight.SearchForLight();
                break;
            case 9:
                //code to start game 9
                break;
            case 10:
                //code to start game 10
                break;

        }
    }
}




