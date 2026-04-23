package com.labs.Hosna;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

import swiftbot.Button;
import swiftbot.SwiftBotAPI;

public class NoughtsAndCrosses {

    /* =========================================================
       GLOBAL STATE
       ========================================================= */

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Random RANDOM = new Random();
    private static final String LOG_FILE_NAME = "./logs/noughts_crosses_log.txt";
    private static boolean sessionShouldTerminate = false;

    /* =========================================================
       MAIN
       ========================================================= */

    public static void main(String[] args) {
        ConsoleUI ui = new ConsoleUI();
        GameConfig config = new GameConfig();
        RobotController robot = new RobotController(config, ui);

        try {
            robot.bootstrap();

            ui.screenIdleStart();
            robot.waitForButtonPress(Button.A);

            ui.screenNewRoundStarting();

            ui.screenSetupStart();
            String humanName = readNonEmptyName(ui);

            Difficulty selectedDifficulty;
            while (true) {
                ui.showSetupAfterName(humanName);
                String diffInput = SCANNER.nextLine().trim();

                if (diffInput.equalsIgnoreCase("HELP")) {
                    ui.printHelpTip();
                    continue;
                }

                if (diffInput.equals("1")) {
                    selectedDifficulty = Difficulty.EASY;
                    break;
                }

                if (diffInput.equals("2")) {
                    selectedDifficulty = Difficulty.HARD;
                    break;
                }

                ui.println("Error: enter 1 or 2.");
            }

            ui.showSetupReadyPrompt(selectedDifficulty.name());
            while (true) {
                String setupCommand = SCANNER.nextLine().trim().toUpperCase();

                if (setupCommand.equals("HELP")) {
                    ui.printHelpTip();
                    ui.print("> ");
                    continue;
                }

                if (setupCommand.equals("START")) {
                    break;
                }

                ui.println("Error: type START.");
                ui.print("> ");
            }

            config.difficulty = selectedDifficulty;

            HumanPlayer human = new HumanPlayer(humanName);
            BotPlayer bot = new BotPlayer("SwiftBot");
            Scoreboard scoreboard = new Scoreboard();

            int roundNumber = 1;

            while (!sessionShouldTerminate) {
                RoundContext round = new RoundContext(roundNumber, human, bot, new Board());

                robot.resetPose(config.startRow, config.startCol, config.startDirection);
                robot.forceBackToStartPose();

                Player starter = decideStarter(ui, human, bot, roundNumber);
                Player other = starter == human ? bot : human;

                while (true) {
                    ui.screenGameplay(
                            round.roundNumber,
                            round.board,
                            round.moveHistory,
                            currentPlayerForDisplay(starter, other, round.moveHistory.size())
                    );

                    Player current = currentPlayerForTurn(starter, other, round.moveHistory.size());
                    Move move;

                    if (current == human) {
                        move = executeHumanTurn(ui, round.board, human, bot);
                    } else {
                        move = executeBotTurn(ui, round, config, robot, bot, human);
                    }

                    round.moveHistory.add(move);

                    RoundOutcome outcome = evaluate(round.board);

                    if (outcome.state == GameState.WIN) {
                        Player winner = outcome.winnerPiece == human.piece ? human : bot;

                        if (winner == human) {
                            scoreboard.humanScore++;
                        } else {
                            scoreboard.botScore++;
                        }

                        ui.screenWinDetected(round.roundNumber, winner, outcome, scoreboard, human, bot);
                        runWinBehaviour(ui, robot, outcome);
                        writeRoundLog(round, outcome, human, bot);

                        String decision = robot.waitForContinueOrQuit();
                        if (!decision.equals("Y")) {
                            ui.screenTerminated(scoreboard, human, bot, roundNumber, LOG_FILE_NAME);
                            sessionShouldTerminate = true;
                        } else {
                            roundNumber++;
                        }
                        break;
                    }

                    if (outcome.state == GameState.DRAW) {
                        scoreboard.draws++;

                        ui.screenDrawDetected(round.roundNumber, scoreboard, human, bot);
                        runDrawBehaviour(ui, robot);
                        writeRoundLog(round, outcome, human, bot);

                        String decision = robot.waitForContinueOrQuit();
                        if (!decision.equals("Y")) {
                            ui.screenTerminated(scoreboard, human, bot, roundNumber, LOG_FILE_NAME);
                            sessionShouldTerminate = true;
                        } else {
                            roundNumber++;
                        }
                        break;
                    }
                }
            }
        } finally {
            try {
                robot.shutdown();
            } catch (Exception ignored) {
            }

            try {
                SCANNER.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static Player currentPlayerForTurn(Player starter, Player other, int movesPlayed) {
        return (movesPlayed % 2 == 0) ? starter : other;
    }

    private static Player currentPlayerForDisplay(Player starter, Player other, int movesPlayed) {
        return currentPlayerForTurn(starter, other, movesPlayed);
    }

    /* =========================================================
       INPUT / SETUP
       ========================================================= */

    private static String readNonEmptyName(ConsoleUI ui) {
        while (true) {
            String input = SCANNER.nextLine().trim();

            if (input.equalsIgnoreCase("HELP")) {
                ui.printHelpTip();
                ui.print("> ");
                continue;
            }

            if (input.isEmpty()) {
                ui.println("Error: name must be non-empty.");
                ui.print("> ");
                continue;
            }

            if (input.length() > 20) {
                ui.println("Error: name must be 1-20 characters.");
                ui.print("> ");
                continue;
            }

            return input;
        }
    }

    private static Position readMove(ConsoleUI ui, String prompt) {
        while (true) {
            ui.print(prompt);
            String line = SCANNER.nextLine().trim();

            if (line.equalsIgnoreCase("HELP")) {
                ui.printHelpTip();
                continue;
            }

            Position parsed = parseSquareInput(line);
            if (parsed == null) {
                ui.screenInvalidFormat(line);
                continue;
            }

            if (parsed.row < 1 || parsed.row > 3 || parsed.col < 1 || parsed.col > 3) {
                ui.screenOutOfRange(line);
                continue;
            }

            return parsed;
        }
    }

    private static Position parseSquareInput(String line) {
        try {
            if (line.length() < 5) {
                return null;
            }

            boolean roundFormat = line.startsWith("(") && line.endsWith(")");
            boolean squareFormat = line.startsWith("[") && line.endsWith("]");

            if (!roundFormat && !squareFormat) {
                return null;
            }

            String inner = line.substring(1, line.length() - 1);
            String[] parts = inner.split(",");

            if (parts.length != 2) {
                return null;
            }

            int row = Integer.parseInt(parts[0].trim());
            int col = Integer.parseInt(parts[1].trim());

            return new Position(row, col);
        } catch (Exception e) {
            return null;
        }
    }

    /* =========================================================
       ROUND FLOW
       ========================================================= */

    private static Player decideStarter(ConsoleUI ui, HumanPlayer human, BotPlayer bot, int roundNumber) {
        while (true) {
            ui.screenRoundDecideStarter(roundNumber);

            int humanRoll = rollDie();
            int botRoll = rollDie();

            if (humanRoll == botRoll) {
                ui.println(human.name + " rolled: " + humanRoll);
                ui.println("SwiftBot rolled: " + botRoll);
                ui.println("");
                ui.println("Tie on dice roll. Press ENTER to roll again...");
                SCANNER.nextLine();
                continue;
            }

            Player starter = humanRoll > botRoll ? human : bot;
            Player other = starter == human ? bot : human;

            starter.piece = Piece.O;
            other.piece = Piece.X;

            ui.showDiceResult(human.name, humanRoll, botRoll, starter, other);
            SCANNER.nextLine();

            return starter;
        }
    }

    private static int rollDie() {
        return RANDOM.nextInt(6) + 1;
    }

    private static Move executeHumanTurn(ConsoleUI ui,
                                         Board board,
                                         HumanPlayer human,
                                         BotPlayer bot) {
        while (true) {
            Position position = readMove(ui, "> ");

            if (!board.isFree(position)) {
                Piece existing = board.get(position);
                String ownerName = existing == human.piece ? human.name : bot.name;
                ui.screenSquareOccupied(
                        formatInputSquare(position),
                        position,
                        ownerName,
                        existing.symbol
                );
                continue;
            }

            board.place(position, human.piece);
            return new Move(human.name, human.piece, position, null);
        }
    }

    private static Move executeBotTurn(ConsoleUI ui,
                                       RoundContext round,
                                       GameConfig config,
                                       RobotController robot,
                                       BotPlayer bot,
                                       HumanPlayer human) {
        BotDecision decision;

        if (config.difficulty == Difficulty.EASY) {
            decision = chooseEasyMove(round.board);
        } else {
            decision = chooseHardMove(round.board, bot.piece, human.piece, round.moveHistory);
        }

        ui.screenSwiftBotTurn(config.difficulty, decision);
        SCANNER.nextLine();

        try {
            robot.moveToSquareAndReturn(decision.position);
        } catch (RuntimeException e) {
            ui.println("Robot warning: " + e.getMessage());
            ui.println("Robot warning: continuing with board-state update only.");
        }

        round.board.place(decision.position, bot.piece);
        return new Move(bot.name, bot.piece, decision.position, decision.ruleUsed);
    }

    /* =========================================================
       AI
       ========================================================= */

    private static BotDecision chooseEasyMove(Board board) {
        List<Position> free = board.getAvailablePositions();
        Position selected = free.get(RANDOM.nextInt(free.size()));
        return new BotDecision(selected, null);
    }

    private static BotDecision chooseHardMove(Board board,
                                              Piece botPiece,
                                              Piece humanPiece,
                                              List<Move> history) {
        Position move;

        move = findWinningMove(board, botPiece);
        if (move != null) {
            return new BotDecision(move, "WIN");
        }

        move = findWinningMove(board, humanPiece);
        if (move != null) {
            return new BotDecision(move, "BLOCK");
        }

        if (history.size() == 1) {
            Position first = history.get(0).position;
            if (isCorner(first)) {
                Position opposite = oppositeCorner(first);
                if (opposite != null && board.isFree(opposite)) {
                    return new BotDecision(opposite, "OPPOSITE_CORNER");
                }
            }
        }

        Position center = new Position(2, 2);
        if (board.isFree(center)) {
            return new BotDecision(center, "CENTER");
        }

        Position[] corners = {
                new Position(1, 1),
                new Position(1, 3),
                new Position(3, 1),
                new Position(3, 3)
        };

        for (Position p : corners) {
            if (board.isFree(p)) {
                return new BotDecision(p, "CORNER");
            }
        }

        Position[] sides = {
                new Position(1, 2),
                new Position(2, 1),
                new Position(2, 3),
                new Position(3, 2)
        };

        for (Position p : sides) {
            if (board.isFree(p)) {
                return new BotDecision(p, "SIDE");
            }
        }

        throw new IllegalStateException("No legal move available.");
    }

    private static Position findWinningMove(Board board, Piece piece) {
        for (Position p : board.getAvailablePositions()) {
            Board copy = board.copy();
            copy.place(p, piece);
            RoundOutcome outcome = evaluate(copy);

            if (outcome.state == GameState.WIN && outcome.winnerPiece == piece) {
                return p;
            }
        }

        return null;
    }

    private static boolean isCorner(Position p) {
        return (p.row == 1 || p.row == 3) && (p.col == 1 || p.col == 3);
    }

    private static Position oppositeCorner(Position p) {
        if (p.row == 1 && p.col == 1) return new Position(3, 3);
        if (p.row == 1 && p.col == 3) return new Position(3, 1);
        if (p.row == 3 && p.col == 1) return new Position(1, 3);
        if (p.row == 3 && p.col == 3) return new Position(1, 1);
        return null;
    }

    /* =========================================================
       GAME STATE
       ========================================================= */

    private static RoundOutcome evaluate(Board board) {
        List<Position[]> lines = board.getWinningLines();

        for (Position[] line : lines) {
            Piece owner = board.lineOwner(line);
            if (owner != Piece.EMPTY) {
                return new RoundOutcome(GameState.WIN, owner, line);
            }
        }

        if (board.isFull()) {
            return new RoundOutcome(GameState.DRAW, Piece.EMPTY, null);
        }

        return new RoundOutcome(GameState.IN_PROGRESS, Piece.EMPTY, null);
    }

    /* =========================================================
       ROBOT BEHAVIOURS
       ========================================================= */

    private static void runWinBehaviour(ConsoleUI ui, RobotController robot, RoundOutcome outcome) {
        try {
            String color = outcome.winnerPiece == Piece.O ? "GREEN" : "RED";

            // FIX: First make sure robot is at start, then blink
            robot.forceBackToStartPose();
            robot.blink(color, 3);

            // FIX: Trace the winning line by navigating between squares directly,
            //      without returning to start between each square.
            if (outcome.winLine != null && outcome.winLine.length > 0) {
                // Go to first square of the win line from start
                robot.navigateTo(outcome.winLine[0].row, outcome.winLine[0].col);

                // Visit remaining squares in the win line sequentially (no return to start)
                for (int i = 1; i < outcome.winLine.length; i++) {
                    robot.navigateTo(outcome.winLine[i].row, outcome.winLine[i].col);
                }

                // After tracing all 3 squares, return to start
                robot.returnToStart();
            }

            robot.blink(color, 3);
            robot.faceStartDirection();
        } catch (RuntimeException e) {
            ui.println("Robot warning: Win behaviour degraded: " + e.getMessage());
        }
    }

    private static void runDrawBehaviour(ConsoleUI ui, RobotController robot) {
        try {
            robot.forceBackToStartPose();
            robot.blink("BLUE", 3);
            robot.spin();
            robot.blink("BLUE", 3);
            robot.faceStartDirection();
        } catch (RuntimeException e) {
            ui.println("Robot warning: Draw behaviour degraded: " + e.getMessage());
        }
    }

    /* =========================================================
       LOGGING
       ========================================================= */

    private static void writeRoundLog(RoundContext round,
                                      RoundOutcome outcome,
                                      HumanPlayer human,
                                      BotPlayer bot) {
        try {
            File logFile = new File(LOG_FILE_NAME);
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter fw = new FileWriter(logFile, true)) {
                fw.write("========================================\n");
                fw.write("Round Number: " + round.roundNumber + "\n");
                fw.write("Human Player: " + human.name + " [" + human.piece.symbol + "]\n");
                fw.write("SwiftBot Player: " + bot.name + " [" + bot.piece.symbol + "]\n");

                fw.write("Move Sequence (ordered pairs): ");
                for (int i = 0; i < round.moveHistory.size(); i++) {
                    if (i > 0) fw.write(" -> ");
                    fw.write(round.moveHistory.get(i).position.toString());
                }
                fw.write("\n");

                fw.write("Move Sequence (detailed): ");
                for (int i = 0; i < round.moveHistory.size(); i++) {
                    if (i > 0) fw.write(" | ");
                    Move m = round.moveHistory.get(i);
                    fw.write(m.toSummary());
                }
                fw.write("\n");

                fw.write("Final Outcome: " + outcome.state + "\n");
                fw.write("Winning Player: " + (outcome.state == GameState.WIN
                        ? (outcome.winnerPiece == human.piece ? human.name : bot.name)
                        : "None") + "\n");
                fw.write("Current Date and Time: " + LocalDateTime.now() + "\n\n");
            }
        } catch (IOException ignored) {
        }
    }

    private static String formatInputSquare(Position position) {
        return "[" + position.row + "," + position.col + "]";
    }

    /* =========================================================
       MODELS / ENUMS
       ========================================================= */

    enum Difficulty {
        EASY,
        HARD
    }

    enum Direction {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    enum GameState {
        IN_PROGRESS,
        WIN,
        DRAW
    }

    enum Piece {
        EMPTY(" "),
        O("O"),
        X("X");

        final String symbol;

        Piece(String symbol) {
            this.symbol = symbol;
        }
    }

    static class Position {
        final int row;
        final int col;

        Position(int row, int col) {
            this.row = row;
            this.col = col;
        }

        boolean withinBoard() {
            return row >= 1 && row <= 3 && col >= 1 && col <= 3;
        }

        @Override
        public String toString() {
            return "(" + row + "," + col + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Position)) return false;
            Position p = (Position) o;
            return row == p.row && col == p.col;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

    static abstract class Player {
        final String name;
        Piece piece = Piece.EMPTY;

        Player(String name) {
            this.name = name;
        }
    }

    static class HumanPlayer extends Player {
        HumanPlayer(String name) {
            super(name);
        }
    }

    static class BotPlayer extends Player {
        BotPlayer(String name) {
            super(name);
        }
    }

    static class Move {
        final String playerName;
        final Piece piece;
        final Position position;
        final String ruleUsed;

        Move(String playerName, Piece piece, Position position, String ruleUsed) {
            this.playerName = playerName;
            this.piece = piece;
            this.position = position;
            this.ruleUsed = ruleUsed;
        }

        String toSummary() {
            return "(" + playerName + ", " + piece.symbol + ") moved to square ("
                    + position.row + ", " + position.col + ")";
        }
    }

    static class BotDecision {
        final Position position;
        final String ruleUsed;

        BotDecision(Position position, String ruleUsed) {
            this.position = position;
            this.ruleUsed = ruleUsed;
        }
    }

    static class Scoreboard {
        int humanScore;
        int botScore;
        int draws;
    }

    static class RoundContext {
        final int roundNumber;
        final HumanPlayer human;
        final BotPlayer bot;
        final Board board;
        final List<Move> moveHistory = new ArrayList<>();

        RoundContext(int roundNumber, HumanPlayer human, BotPlayer bot, Board board) {
            this.roundNumber = roundNumber;
            this.human = human;
            this.bot = bot;
            this.board = board;
        }
    }

    static class RoundOutcome {
        final GameState state;
        final Piece winnerPiece;
        final Position[] winLine;

        RoundOutcome(GameState state, Piece winnerPiece, Position[] winLine) {
            this.state = state;
            this.winnerPiece = winnerPiece;
            this.winLine = winLine;
        }

        String winLineString() {
            return winLine == null ? "None" : Arrays.toString(winLine);
        }
    }

    static class GameConfig {
        Difficulty difficulty = Difficulty.EASY;

        int forwardUnitMs = 1426;
        int turnLeftMs    = 430;   
        int turnRightMs   = 430;   
        int spinMs        = 1700;  

        int       startRow       = 1;
        int       startCol       = 0;
        Direction startDirection = Direction.EAST;
  
        double squareSizeCm = 25.0;   
        double halfSquareCm = 12.5;   
        double startOffsetCm = 0.0;
    }

    static class Board {
        final Piece[][] grid = new Piece[3][3];

        Board() {
            clear();
        }

        void clear() {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    grid[r][c] = Piece.EMPTY;
                }
            }
        }

        boolean place(Position p, Piece piece) {
            if (!p.withinBoard()) return false;
            if (!isFree(p)) return false;
            grid[p.row - 1][p.col - 1] = piece;
            return true;
        }

        boolean isFree(Position p) {
            return grid[p.row - 1][p.col - 1] == Piece.EMPTY;
        }

        Piece get(Position p) {
            return grid[p.row - 1][p.col - 1];
        }

        boolean isFull() {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (grid[r][c] == Piece.EMPTY) return false;
                }
            }
            return true;
        }

        List<Position> getAvailablePositions() {
            List<Position> list = new ArrayList<>();
            for (int r = 1; r <= 3; r++) {
                for (int c = 1; c <= 3; c++) {
                    Position p = new Position(r, c);
                    if (isFree(p)) list.add(p);
                }
            }
            return list;
        }

        List<Position[]> getWinningLines() {
            List<Position[]> lines = new ArrayList<>();

            for (int r = 1; r <= 3; r++) {
                lines.add(new Position[]{
                        new Position(r, 1),
                        new Position(r, 2),
                        new Position(r, 3)
                });
            }

            for (int c = 1; c <= 3; c++) {
                lines.add(new Position[]{
                        new Position(1, c),
                        new Position(2, c),
                        new Position(3, c)
                });
            }

            lines.add(new Position[]{
                    new Position(1, 1),
                    new Position(2, 2),
                    new Position(3, 3)
            });

            lines.add(new Position[]{
                    new Position(1, 3),
                    new Position(2, 2),
                    new Position(3, 1)
            });

            return lines;
        }

        Piece lineOwner(Position[] line) {
            Piece first = get(line[0]);
            if (first == Piece.EMPTY) return Piece.EMPTY;

            for (Position p : line) {
                if (get(p) != first) return Piece.EMPTY;
            }

            return first;
        }

        Board copy() {
            Board clone = new Board();
            for (int r = 1; r <= 3; r++) {
                for (int c = 1; c <= 3; c++) {
                    clone.grid[r - 1][c - 1] = this.grid[r - 1][c - 1];
                }
            }
            return clone;
        }
    }

    /* =========================================================
       UI
       ========================================================= */

    static class ConsoleUI {

        private static final int WIDTH = 66;

        private String line(char ch) {
            return "+" + String.valueOf(ch).repeat(WIDTH) + "+";
        }

        private String pad(String s) {
            if (s == null) s = "";
            if (s.length() > WIDTH) {
                return "|" + s.substring(0, WIDTH) + "|";
            }
            return "|" + s + " ".repeat(WIDTH - s.length()) + "|";
        }

        private void box(String title, List<String> lines) {
            println(line('-'));
            println(centerTitle(title));
            println("|" + "=".repeat(WIDTH) + "|");
            for (String line : lines) {
                println(pad(line));
            }
            println(line('-'));
        }

        private String centerTitle(String title) {
            String clean = " " + title + " ";
            int remaining = WIDTH - clean.length();
            if (remaining < 0) {
                clean = clean.substring(0, WIDTH);
                remaining = 0;
            }
            int left = remaining / 2;
            int right = remaining - left;
            return "|" + "-".repeat(left) + clean + "-".repeat(right) + "|";
        }

        void printHelpTip() {
            println("Tip: You can type HELP at any input prompt to reprint instructions.");
        }

        void screenIdleStart() {
            box("SWIFTBOT NOUGHTS & CROSSES (TASK 7) - CLI UI PROTOTYPE", List.of(
                    "Status: IDLE (waiting for Button A)",
                    "",
                    "Controls (SwiftBot):",
                    "[A] Start program / start round",
                    "[X] Quit (when prompted)",
                    "[Y] Play again (when prompted)",
                    "",
                    "Robot start pose: [1,0], facing EAST toward the board.",
                    "",
                    "Press [A] on SwiftBot to begin..."
            ));
        }

        void screenNewRoundStarting() {
            box("PROGRAM START", List.of(
                    "A new Noughts and Crosses round is starting."
            ));
        }

        void screenSetupStart() {
            box("SETUP - PLAYER REGISTRATION", List.of(
                    "Enter your name (1-20 chars, no empty):"
            ));
            print("> ");
        }

        void showSetupAfterName(String playerName) {
            box("SETUP - PLAYER REGISTRATION", List.of(
                    "Enter your name (1-20 chars, no empty):",
                    "> " + playerName,
                    "",
                    "SwiftBot name: SwiftBot",
                    "",
                    "Select difficulty:",
                    "[1] EASY - random move",
                    "[2] HARD - win/block/centre/corner/other strategy",
                    "Enter choice [1/2]:"
            ));
            print("> ");
        }

        void showSetupReadyPrompt(String chosenDifficulty) {
            box("SETUP - PLAYER REGISTRATION", List.of(
                    "SwiftBot name: SwiftBot",
                    "",
                    "Selected difficulty: " + chosenDifficulty,
                    "",
                    "Robot standard start: [1,0], facing EAST toward the board.",
                    "",
                    "Type START to begin the round."
            ));
            print("> ");
        }

        void screenRoundDecideStarter(int roundNumber) {
            box("ROUND " + roundNumber + " - DECIDE STARTER", List.of(
                    "Rolling 6-sided die..."
            ));
        }

        void showDiceResult(String humanName, int humanRoll, int botRoll, Player starter, Player other) {
            println(humanName + " rolled: " + humanRoll);
            println("SwiftBot rolled: " + botRoll);
            println("");
            println("Starter: " + starter.name);
            println("Pieces: " + starter.name + " = O    " + other.name + " = X");
            println("");
            println("Press ENTER to continue...");
        }

        void screenGameplay(int roundNumber, Board board, List<Move> history, Player current) {
            List<String> lines = new ArrayList<>();
            lines.add("Board key: enter moves as (row,col) or [row,col] (rows 1-3, cols 1-3)");
            lines.add("Robot start/return pose: [1,0], facing EAST");
            lines.add("");
            lines.addAll(boardToAscii(board));
            lines.add("");
            lines.add("Move summary:");
            if (history.isEmpty()) {
                lines.add("(no moves yet)");
            } else {
                for (Move m : history) {
                    lines.add(m.toSummary());
                }
            }
            lines.add("");

            if (current instanceof HumanPlayer) {
                lines.add("Your turn (" + current.name + " - " + current.piece.symbol + ").");
                lines.add("Enter move as (row,col) or [row,col], or type HELP:");
            } else {
                lines.add("SwiftBot turn is next. Press ENTER on the next screen to continue.");
            }

            box("ROUND " + roundNumber + " - GAMEPLAY", lines);
            if (current instanceof HumanPlayer) {
                print("> ");
            }
        }

        private List<String> boardToAscii(Board board) {
            List<String> out = new ArrayList<>();

            out.add("      Col:  1     2     3");
            out.add("           +-----+-----+-----+");
            out.add("Row 1      |  " + board.grid[0][0].symbol + "  |  " + board.grid[0][1].symbol + "  |  " + board.grid[0][2].symbol + "  |");
            out.add("           +-----+-----+-----+");
            out.add("Row 2      |  " + board.grid[1][0].symbol + "  |  " + board.grid[1][1].symbol + "  |  " + board.grid[1][2].symbol + "  |");
            out.add("           +-----+-----+-----+");
            out.add("Row 3      |  " + board.grid[2][0].symbol + "  |  " + board.grid[2][1].symbol + "  |  " + board.grid[2][2].symbol + "  |");
            out.add("           +-----+-----+-----+");

            return out;
        }

        void screenInvalidFormat(String input) {
            box("INPUT ERROR - INVALID FORMAT", List.of(
                    "Input received: " + input,
                    "",
                    "Expected format: (row,col) or [row,col]",
                    "Examples: (1,2)   [1,2]   (3,3)   [3,3]",
                    "",
                    "Please re-enter your move:"
            ));
            print("> ");
        }

        void screenOutOfRange(String input) {
            box("INPUT ERROR - OUT OF RANGE", List.of(
                    "Input received: " + input,
                    "",
                    "Rows and columns must be between 1 and 3.",
                    "",
                    "Please re-enter your move:"
            ));
            print("> ");
        }

        void screenSquareOccupied(String input, Position position, String ownerName, String pieceSymbol) {
            box("INPUT ERROR - SQUARE OCCUPIED", List.of(
                    "Input received: " + input,
                    "Square " + formatSquareBracket(position) + " already contains: " + pieceSymbol + " (" + ownerName + ")",
                    "",
                    "Choose an empty square and try again:"
            ));
            print("> ");
        }

        void screenSwiftBotTurn(Difficulty difficulty, BotDecision decision) {
            List<String> lines = new ArrayList<>();
            lines.add("SwiftBot is thinking... (difficulty: " + difficulty.name() + ")");
            lines.add("");
            lines.add("Chosen square: " + formatSquareBracket(decision.position));
            if (decision.ruleUsed != null) {
                lines.add("Rule used: " + decision.ruleUsed);
            }
            lines.add("");
            lines.add("[Robot] Start from [1,0], facing EAST");
            lines.add("[Robot] Move to square " + formatSquareBracket(decision.position));
            lines.add("[Robot] Blink GREEN x3");
            lines.add("[Robot] Return to [1,0]");
            lines.add("[Robot] Face EAST again");
            lines.add("");
            lines.add("Press ENTER to continue...");

            box("SWIFTBOT TURN", lines);
        }

        void screenWinDetected(int roundNumber,
                               Player winner,
                               RoundOutcome outcome,
                               Scoreboard scoreboard,
                               HumanPlayer human,
                               BotPlayer bot) {
            List<String> lines = new ArrayList<>();
            lines.add("Result: " + winner.name + " wins!  (" + winner.piece.symbol + " completed a winning line)");
            lines.add("");
            lines.add("Winning line squares: " + winningLineArrow(outcome.winLine));
            lines.add("");
            lines.add("[Robot] Return to [1,0], face EAST");
            lines.add("[Robot] Blink " + (winner.piece == Piece.O ? "GREEN" : "RED") + " x3 (before)");
            lines.add("[Robot] Trace winning line: visit each square in sequence");
            lines.add("[Robot] Return to [1,0], face EAST");
            lines.add("[Robot] Blink " + (winner.piece == Piece.O ? "GREEN" : "RED") + " x3 (after)");
            lines.add("");
            lines.add("Scoreboard:");
            lines.add("  " + human.name + " : " + scoreboard.humanScore);
            lines.add("  " + bot.name + " : " + scoreboard.botScore);
            lines.add("");
            lines.add("Play another round? Press [Y] to continue or [X] to quit.");

            box("ROUND " + roundNumber + " - WIN DETECTED", lines);
        }

        void screenDrawDetected(int roundNumber,
                                Scoreboard scoreboard,
                                HumanPlayer human,
                                BotPlayer bot) {
            List<String> lines = new ArrayList<>();
            lines.add("Result: DRAW (no valid moves left)");
            lines.add("");
            lines.add("[Robot] Return to [1,0], face EAST");
            lines.add("[Robot] Blink BLUE x3 (before)");
            lines.add("[Robot] Spin once (360 degrees)");
            lines.add("[Robot] Blink BLUE x3 (after)");
            lines.add("");
            lines.add("Scoreboard:");
            lines.add("  " + human.name + " : " + scoreboard.humanScore);
            lines.add("  " + bot.name + " : " + scoreboard.botScore);
            lines.add("");
            lines.add("Play another round? Press [Y] to continue or [X] to quit.");

            box("ROUND " + roundNumber + " - DRAW", lines);
        }

        void screenTerminated(Scoreboard scoreboard,
                              HumanPlayer human,
                              BotPlayer bot,
                              int roundsPlayed,
                              String logPath) {
            box("PROGRAM TERMINATED", List.of(
                    "Thanks for playing!",
                    "",
                    "Final scoreboard:",
                    "  " + human.name + " : " + scoreboard.humanScore,
                    "  " + bot.name + " : " + scoreboard.botScore,
                    "Rounds played: " + roundsPlayed,
                    "",
                    "Round log saved to:",
                    logPath,
                    "",
                    "Goodbye."
            ));
        }

        String formatSquareBracket(Position p) {
            return "[" + p.row + "," + p.col + "]";
        }

        private String winningLineArrow(Position[] line) {
            if (line == null || line.length == 0) return "None";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < line.length; i++) {
                if (i > 0) sb.append(" -> ");
                sb.append("[").append(line[i].row).append(",").append(line[i].col).append("]");
            }
            return sb.toString();
        }

        void println(String s) {
            System.out.println(s);
        }

        void print(String s) {
            System.out.print(s);
        }
    }

    /* =========================================================
       ROBOT LAYER
       ========================================================= */

    static class RobotController {
        private final GameConfig config;
        private final ConsoleUI ui;

        private boolean hardwareAvailable;
        private SwiftBotAPI swiftBot;

        private int poseRow;
        private int poseCol;
        private Direction poseDirection;

        // FIX: Track physical position in cm for accurate movement
        private double poseXcm;
        private double poseYcm;

        RobotController(GameConfig config, ConsoleUI ui) {
            this.config = config;
            this.ui = ui;
            resetPose(config.startRow, config.startCol, config.startDirection);
        }

        void bootstrap() {
            try {
                swiftBot = SwiftBotAPI.INSTANCE;
                hardwareAvailable = (swiftBot != null);

                if (hardwareAvailable) {
                    fillUnderlights("WHITE");
                    sleep(250);
                    fillUnderlights("OFF");
                }
            } catch (Exception e) {
                hardwareAvailable = false;
                swiftBot = null;
            }
        }

        void resetPose(int row, int col, Direction direction) {
            this.poseRow = row;
            this.poseCol = col;
            this.poseDirection = direction;

            this.poseXcm = columnToPhysicalX(col);
            this.poseYcm = rowToPhysicalY(row);
        }

        void forceBackToStartPose() {
            try {
                returnToStart();
            } catch (Exception ignored) {
                resetPose(config.startRow, config.startCol, config.startDirection);
            }
            face(config.startDirection);
        }

        void moveToSquareAndReturn(Position target) {
            forceBackToStartPose();
            navigateTo(target.row, target.col);
            blink("GREEN", 3);
            returnToStart();
            face(config.startDirection);
        }

        // FIX: This method is now PUBLIC so runWinBehaviour can call it directly
        //      for sequential win-line tracing without returning to start each time.
        void navigateTo(int targetRow, int targetCol) {
            try {
                double targetXcm = columnToPhysicalX(targetCol);
                double targetYcm = rowToPhysicalY(targetRow);

                // Move in Y (North/South) first
                double deltaY = targetYcm - poseYcm;
                if (Math.abs(deltaY) > 0.5) {
                    // FIX: NORTH means decreasing row (upward on board), SOUTH means increasing row
                    Direction needed = deltaY < 0 ? Direction.NORTH : Direction.SOUTH;
                    face(needed);
                    moveForwardDistanceCm(Math.abs(deltaY));
                }

                // Then move in X (East/West)
                double deltaX = targetXcm - poseXcm;
                if (Math.abs(deltaX) > 0.5) {
                    Direction needed = deltaX > 0 ? Direction.EAST : Direction.WEST;
                    face(needed);
                    moveForwardDistanceCm(Math.abs(deltaX));
                }

                // FIX: Update logical pose after successful navigation
                poseRow = targetRow;
                poseCol = targetCol;

            } catch (Exception e) {
                throw new RuntimeException("Robot failed to navigate to target square.", e);
            }
        }

        void returnToStart() {
            navigateTo(config.startRow, config.startCol);
            face(config.startDirection);
        }

        void faceStartDirection() {
            face(config.startDirection);
        }

        // columnToPhysicalX
        //   col=0  ->  -(startOffsetCm)           [start: left of board]
        //   col=1  ->  halfSquareCm               [centre of column 1]
        //   col=2  ->  halfSquareCm + squareSizeCm
        //   col=3  ->  halfSquareCm + 2*squareSizeCm
        //
        //   The delta from start(col=0) to col-1 centre is therefore:
        //     halfSquareCm + startOffsetCm
        //   Increase startOffsetCm in GameConfig when the robot stops short.
        private double columnToPhysicalX(int col) {
            if (col == 0) {
                return -config.startOffsetCm;
            }
            return ((col - 1) * config.squareSizeCm) + config.halfSquareCm;
        }

        // rowToPhysicalY
        //   row=1  ->  halfSquareCm   (12.5 cm from top edge)
        //   row=2  ->  halfSquareCm + squareSizeCm
        //   row=3  ->  halfSquareCm + 2*squareSizeCm
        private double rowToPhysicalY(int row) {
            return ((row - 1) * config.squareSizeCm) + config.halfSquareCm;
        }

        private void moveForwardDistanceCm(double distanceCm) {
            if (distanceCm <= 0.5) return; // Ignore sub-cm movements to avoid jitter

            int duration = (int) Math.round((distanceCm / config.squareSizeCm) * config.forwardUnitMs);
            invokeMove(100, 100, duration);

            // FIX: Update poseXcm/poseYcm based on direction of travel
            switch (poseDirection) {
                case NORTH -> poseYcm -= distanceCm;
                case SOUTH -> poseYcm += distanceCm;
                case EAST  -> poseXcm += distanceCm;
                case WEST  -> poseXcm -= distanceCm;
            }
        }

        private void face(Direction needed) {
            while (poseDirection != needed) {
                if (needsUTurn(poseDirection, needed)) {
                    turnRight();
                    turnRight();
                } else if (isRightTurnBetter(poseDirection, needed)) {
                    turnRight();
                } else {
                    turnLeft();
                }
            }
        }

        private boolean isRightTurnBetter(Direction current, Direction target) {
            return (current == Direction.NORTH && target == Direction.EAST)
                    || (current == Direction.EAST && target == Direction.SOUTH)
                    || (current == Direction.SOUTH && target == Direction.WEST)
                    || (current == Direction.WEST && target == Direction.NORTH);
        }

        private boolean needsUTurn(Direction current, Direction target) {
            return (current == Direction.NORTH && target == Direction.SOUTH)
                    || (current == Direction.SOUTH && target == Direction.NORTH)
                    || (current == Direction.EAST && target == Direction.WEST)
                    || (current == Direction.WEST && target == Direction.EAST);
        }

        private void turnLeft() {
            invokeMove(-100, 100, config.turnLeftMs);

            switch (poseDirection) {
                case NORTH -> poseDirection = Direction.WEST;
                case WEST  -> poseDirection = Direction.SOUTH;
                case SOUTH -> poseDirection = Direction.EAST;
                case EAST  -> poseDirection = Direction.NORTH;
            }
        }

        private void turnRight() {
            invokeMove(100, -100, config.turnRightMs);

            switch (poseDirection) {
                case NORTH -> poseDirection = Direction.EAST;
                case EAST  -> poseDirection = Direction.SOUTH;
                case SOUTH -> poseDirection = Direction.WEST;
                case WEST  -> poseDirection = Direction.NORTH;
            }
        }

        void spin() {
            invokeMove(100, -100, config.spinMs);
        }

        void blink(String color, int times) {
            for (int i = 0; i < times; i++) {
                fillUnderlights(color);
                sleep(250);
                fillUnderlights("OFF");
                sleep(150);
            }
        }

        String waitForContinueOrQuit() {
            if (!hardwareAvailable || swiftBot == null) {
                while (true) {
                    String input = SCANNER.nextLine().trim().toUpperCase();
                    if (input.equals("Y") || input.equals("X")) {
                        return input;
                    }
                }
            }

            final String[] decision = {null};

            try {
                swiftBot.enableButton(Button.Y, () -> decision[0] = "Y");
                swiftBot.enableButton(Button.X, () -> decision[0] = "X");

                while (decision[0] == null) {
                    sleep(50);
                }

                return decision[0];
            } catch (Exception e) {
                while (true) {
                    String input = SCANNER.nextLine().trim().toUpperCase();
                    if (input.equals("Y") || input.equals("X")) {
                        return input;
                    }
                }
            } finally {
                try {
                    swiftBot.disableButton(Button.Y);
                } catch (Exception ignored) {
                }
                try {
                    swiftBot.disableButton(Button.X);
                } catch (Exception ignored) {
                }
            }
        }

        void waitForButtonPress(Button button) {
            if (!hardwareAvailable || swiftBot == null) {
                SCANNER.nextLine();
                return;
            }

            final boolean[] waiting = {true};

            try {
                swiftBot.enableButton(button, () -> waiting[0] = false);

                while (waiting[0]) {
                    sleep(50);
                }
            } catch (Exception e) {
                SCANNER.nextLine();
            } finally {
                try {
                    swiftBot.disableButton(button);
                } catch (Exception ignored) {
                }
            }
        }

        void shutdownLights() {
            if (!hardwareAvailable || swiftBot == null) {
                return;
            }

            try {
                swiftBot.disableUnderlights();
            } catch (Exception ignored) {
            }
        }

        void shutdown() {
            shutdownLights();

            if (!hardwareAvailable || swiftBot == null) {
                return;
            }

            try {
                swiftBot.disableAllButtons();
            } catch (Exception ignored) {
            }
        }

        private void fillUnderlights(String color) {
            if (!hardwareAvailable || swiftBot == null) {
                return;
            }

            try {
                if ("OFF".equals(color)) {
                    swiftBot.disableUnderlights();
                    return;
                }

                swiftBot.fillUnderlights(mapColor(color));
            } catch (Exception ignored) {
            }
        }

        private void invokeMove(int left, int right, int duration) {
            if (!hardwareAvailable || swiftBot == null) {
                return;
            }

            try {
                swiftBot.move(left, right, duration);
                sleep(150);
            } catch (Exception e) {
                throw new RuntimeException("Real robot move command failed.", e);
            }
        }

        private int[] mapColor(String color) {
            return switch (color) {
                case "GREEN"  -> new int[]{0, 255, 0};
                case "RED"    -> new int[]{255, 0, 0};
                case "BLUE"   -> new int[]{0, 0, 255};
                case "WHITE"  -> new int[]{255, 255, 255};
                case "YELLOW" -> new int[]{255, 255, 0};
                default       -> new int[]{0, 0, 0}; 
            };
        }

        private void sleep(int ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
