package src.main.java.com.labs.Ainesh;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class SnakesAndLadders {

    static Map<Integer, Integer> snakes = new HashMap<>();
    static Map<Integer, Integer> ladders = new HashMap<>();
    static Random rand = new Random();
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        System.out.println("========================================");
        System.out.println("        SNAKES AND LADDERS GAME         ");
        System.out.println("========================================");

        // setting up the board
        snakes.put(14, 4);
        snakes.put(24, 16);
        ladders.put(3, 12);
        ladders.put(10, 21);

        System.out.println("Snakes at: 14->4, 24->16");
        System.out.println("Ladders at: 3->12, 10->21");

        System.out.println("Press 'Y' (or type 'Y' and press Enter) to start the game.");
        String start = scanner.nextLine();
        while (!start.equalsIgnoreCase("Y")) {
            System.out.println("Waiting for 'Y' to start...");
            start = scanner.nextLine();
        }

        System.out.print("Enter your preferred name: ");
        String player_name = scanner.nextLine();
        if (player_name.trim().isEmpty()) {
            player_name = "Player";
        }
        String bot_name = "SwiftBot";

        System.out.println("Select Game Mode:");
        System.out.println("A - Normal Mode");
        System.out.println("B - Override Mode (You can override the bot's dice roll)");
        System.out.print("Choice: ");
        String mode = scanner.nextLine().toUpperCase();
        while (!mode.equals("A") && !mode.equals("B")) 
        {
            System.out.print("Invalid mode. Enter A or B: ");
            mode = scanner.nextLine().toUpperCase();
        }

        int player_position = 1;
        int bot_position = 1;

        System.out.println("\nRolling to see who goes first...");
        int player_roll = rand.nextInt(6) + 1;
        int bot_roll = rand.nextInt(6) + 1;
        System.out.println("[" + player_name + "] rolled: " + player_roll);
        System.out.println("[" + bot_name + "] rolled: " + bot_roll);

        boolean player_turn = player_roll >= bot_roll;
        System.out.println((player_turn ? player_name : bot_name) + " goes first!");

        boolean game_active = true;

        // main game loop
        while (game_active) {
            if (player_turn) {
                System.out.println("\n--- " + player_name + "'s Turn ---");
                System.out.println("Press 'A' (or type 'A' and press Enter) to roll the die.");
                String cmd = scanner.nextLine();
                while (!cmd.equalsIgnoreCase("A")) {
                    System.out.println("Waiting for 'A' to roll...");
                    cmd = scanner.nextLine();
                }

                int dice = rand.nextInt(6) + 1;
                int new_pos = player_position + dice;

                if (new_pos > 25) {
                    System.out.println("[" + player_name + "] rolled a [" + dice + "]. Roll too high! Must land exactly on 25. Staying at " + player_position + ".");
                } 
                else 
                {
                    System.out.println("[" + player_name + "] rolled a [" + dice + "] and moved from square [" + player_position + "] to [" + new_pos + "].");
                    player_position = new_pos;
                    
                    // check for snakes
                    if (snakes.containsKey(player_position)) {
                        int slide = snakes.get(player_position);
                        System.out.println("Bitten by a snake! Sliding down to [" + slide + "].");
                        player_position = slide;
                    } 
                    // check for ladders
                    else if (ladders.containsKey(player_position)) {
                        int climb = ladders.get(player_position);
                        System.out.println("Found a ladder! Climbing up to [" + climb + "].");
                        player_position = climb;
                    }
                }

                if (player_position == 25) {
                    System.out.println("\n*** " + player_name.toUpperCase() + " WINS THE GAME! ***");
                    game_active = false;
                }
            } else {
                System.out.println("\n--- " + bot_name + "'s Turn ---");
                int dice = rand.nextInt(6) + 1;

                // override mode
                if (mode.equals("B")) {
                    System.out.println(bot_name + " rolled a " + dice + ".");
                    System.out.print("Mode B Active: Enter an override roll (1-5), or 0 to keep original roll: ");
                    String input = scanner.nextLine();
                    // System.out.println("input was: " + input);
                    int override = Integer.parseInt(input);
                    if (override >= 1 && override <= 5) {
                        dice = override;
                        System.out.println("Roll overridden to: " + dice);
                    } else if (override != 0) {
                        System.out.println("Invalid override (must be 1-5). Keeping original roll.");
                    }
                }

                int new_pos = bot_position + dice;

                if (new_pos > 25) {
                    System.out.println("[" + bot_name + "] rolled a [" + dice + "]. Roll too high! Must land exactly on 25. Staying at " + bot_position + ".");
                } else {
                    System.out.println("[" + bot_name + "] rolled a [" + dice + "] and moved from square [" + bot_position + "] to [" + new_pos + "].");
                    bot_position = new_pos;

                    if (snakes.containsKey(bot_position)) {
                        int slide = snakes.get(bot_position);
                        System.out.println("Bitten by a snake! Sliding down to [" + slide + "].");
                        bot_position = slide;
                    } else if (ladders.containsKey(bot_position)) {
                        int climb = ladders.get(bot_position);
                        System.out.println("Found a ladder! Climbing up to [" + climb + "].");
                        bot_position = climb;
                    }
                }

                if (bot_position == 25) {
                    System.out.println("\n*** " + bot_name.toUpperCase() + " WINS THE GAME! ***");
                    game_active = false;
                }
            }

            // checkpoint at square 5
            if (game_active && (player_position == 5 || bot_position == 5)) {
                System.out.print("\nSquare 5 reached! Press 'X' (or type 'X') to quit, or any other key to continue: ");
                String quit = scanner.nextLine();
                if (quit.equalsIgnoreCase("X")) {
                    System.out.println("Game aborted by user.");
                    game_active = false;
                }
            }

            player_turn = !player_turn;
        }

        System.out.print("\nGame Over! Press 'X' (or type 'X') to exit and save the log: ");
        String exit = scanner.nextLine();
        while (!exit.equalsIgnoreCase("X")) {
             System.out.println("Waiting for 'X' to exit...");
             exit = scanner.nextLine();
        }

        // saving game log
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss");
        String time_stamp = LocalDateTime.now().format(formatter);
        String file_name = "SnakesAndLadders_Log_" + time_stamp + ".txt";

        FileWriter writer = new FileWriter(file_name);
        writer.write("--- Snakes and Ladders Game Log ---\n");
        writer.write("Date/Time: " + time_stamp + "\n");
        writer.write("Final User Position: " + player_position + "\n");
        writer.write("Final SwiftBot Position: " + bot_position + "\n");
        writer.write("Snake Locations: 14->4, 24->16\n");
        writer.write("Ladder Locations: 3->12, 10->21\n");
        writer.close();
        System.out.println("Log saved successfully to: " + file_name);
        // System.out.println("game ended");
    }
}