import swiftbot.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.awt.image.BufferedImage;

/**
 * Author: Miyelangelo Dell'Ovo Mujica
 * /255694, Task 9
 */

enum DanceStyle {
    SMOOTH("Smooth"),
    ENERGETIC("Energetic"),
    STROBE("Strobe");

    private final String name;
    DanceStyle(String name) { this.name = name; }
    public String getName() { return name; }

    static DanceStyle fromCode(int code) {
        if (code == 2) return ENERGETIC;
        if (code == 3) return STROBE;
        return SMOOTH;
    }
}

class HexToken {
    private static final int MAX_WHEEL_SPEED = 100;
    private static final int MIN_WHEEL_SPEED = 50;
    private static final int MAX_RGB         = 255;

    final String hex;
    final int    decimal;
    final String binary;
    final int    octal;
    final int    wheelSpeed;
    final int[]  rgb;
    final int    score;

    HexToken(String hex) {
        this.hex        = hex;
        this.decimal    = hexToDecimal(hex);
        this.binary     = decimalToBinary(decimal);
        this.octal      = decimalToOctal(decimal);
        this.wheelSpeed = deriveWheelSpeed(octal);
        this.rgb        = deriveRGB(decimal);
        this.score      = computeScore(binary, wheelSpeed);
    }

    private int hexToDecimal(String h) {
        int result = 0, power = 1;
        for (int i = h.length() - 1; i >= 0; i--) {
            char c = h.charAt(i);
            int v  = (c >= '0' && c <= '9') ? c - '0' : c - 'A' + 10;
            result += v * power;
            power  *= 16;
        }
        return result;
    }

    private String decimalToBinary(int n) {
        if (n == 0) return "0";
        StringBuilder sb = new StringBuilder();
        while (n > 0) { sb.insert(0, n % 2); n /= 2; }
        return sb.toString();
    }

    private int decimalToOctal(int n) {
        if (n == 0) return 0;
        StringBuilder sb = new StringBuilder();
        while (n > 0) { sb.insert(0, n % 8); n /= 8; }
        try { return Integer.parseInt(sb.toString()); }
        catch (NumberFormatException e) { return 0; }
    }

    private int deriveWheelSpeed(int oct) {
        int s = oct;
        if (s < MIN_WHEEL_SPEED) s += MIN_WHEEL_SPEED;
        return Math.min(s, MAX_WHEEL_SPEED);
    }

    private int[] deriveRGB(int dec) {
        int r = clamp(dec);
        int g = clamp((dec % 80) * 3);
        int b = clamp(Math.max(r, g));
        return new int[]{r, g, b};
    }

    private int clamp(int v) { return Math.max(0, Math.min(MAX_RGB, v)); }

    private int computeScore(String bin, int spd) {
        int fwd = 0, spins = 0;
        for (char c : bin.toCharArray()) { if (c == '1') fwd++; else spins++; }
        return (fwd * spd) - (spins * 10);
    }

    double forwardDuration() { return hex.length() == 1 ? 1.0 : 0.5; }
}

class SessionLog {
    private final ArrayList<String>  labels = new ArrayList<>();
    private final ArrayList<Integer> scores = new ArrayList<>();
    private final ArrayList<String>  allHex = new ArrayList<>();

    void addHex(String hex) { allHex.add(hex); }

    void commitScan(int scanNum, String rawQR, int score) {
        labels.add("Scan " + scanNum + " [" + rawQR.trim() + "]");
        scores.add(score);
    }

    ArrayList<String>  getLabels() { return labels; }
    ArrayList<Integer> getScores() { return scores; }
    ArrayList<String>  getAllHex() { return allHex; }
    boolean            isEmpty()   { return scores.isEmpty(); }
    int totalScore()               { return scores.stream().mapToInt(Integer::intValue).sum(); }

    int bestIndex() {
        int best = 0;
        for (int i = 1; i < scores.size(); i++)
            if (scores.get(i) > scores.get(best)) best = i;
        return best;
    }

    void sortHexAscending() {
        for (int i = 0; i < allHex.size() - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < allHex.size(); j++)
                if (hexVal(allHex.get(j)) < hexVal(allHex.get(minIdx))) minIdx = j;
            String tmp = allHex.get(minIdx);
            allHex.set(minIdx, allHex.get(i));
            allHex.set(i, tmp);
        }
    }

    private int hexVal(String h) {
        int result = 0, power = 1;
        for (int i = h.length() - 1; i >= 0; i--) {
            char c = h.charAt(i);
            result += ((c >= '0' && c <= '9') ? c - '0' : c - 'A' + 10) * power;
            power *= 16;
        }
        return result;
    }

    void writeToFile(DanceStyle style) {
        String filename = "dance_session_log.txt";
        try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
            w.println("=== SwiftBot Dance Session Log ===");
            w.println("Dance Style: " + style.getName());
            w.println("Total valid values: " + allHex.size());
            for (String h : allHex) {
                HexToken t = new HexToken(h);
                int fwd = 0, spins = 0;
                for (char c : t.binary.toCharArray()) { if (c == '1') fwd++; else spins++; }
                w.println("  " + h + " (dec: " + t.decimal + ", forward: " + fwd + ", spins: " + spins + ")");
            }
            java.io.File f = new java.io.File(filename);
            System.out.println("\u001B[92m  [+]  Log saved: \u001B[97m" + f.getAbsolutePath() + "\u001B[0m");
        } catch (IOException e) {
            System.out.println("\u001B[91m  [x]  Log write failed: " + e.getMessage() + "\u001B[0m");
        }
    }
}

public class SwiftBotDance {

    private static final int    MAX_TOKENS_PER_SCAN = 5;
    private static final int    QR_RETRY_DELAY_MS   = 5000;
    private static final double OBSTACLE_CM         = 30.0;
    private static final int    SPIN_360_MS         = 1300;

    private static final String C_RESET  = "\u001B[0m";
    private static final String C_GREEN  = "\u001B[92m";
    private static final String C_CYAN   = "\u001B[96m";
    private static final String C_PINK   = "\u001B[95m";
    private static final String C_YELLOW = "\u001B[93m";
    private static final String C_RED    = "\u001B[91m";
    private static final String C_WHITE  = "\u001B[97m";
    private static final String C_GREY   = "\u001B[90m";
    private static final String C_BOLD   = "\u001B[1m";

    private final SwiftBotAPI swiftBot;
    private final Scanner     scanner;
    private final SessionLog  sessionLog;
    private DanceStyle        danceStyle;
    private boolean           obstacleDetected;
    private int               scanCount;
    private int               qrFailCount;

    public SwiftBotDance() {
        swiftBot         = SwiftBotAPI.INSTANCE;
        scanner          = new Scanner(System.in);
        sessionLog       = new SessionLog();
        danceStyle       = DanceStyle.SMOOTH;
        obstacleDetected = false;
        scanCount        = 0;
        qrFailCount      = 0;
        line(C_GREY, "  Initialising SwiftBot hardware...");
    }

    public static void main(String[] args) {
        SwiftBotDance program = new SwiftBotDance();
        program.run();
    }

    private void run() {
        displayWelcomeScreen();
        danceStyle = DanceStyle.fromCode(selectDanceStyleCode());
        displayStyleConfirmation(danceStyle);

        boolean continueRunning = true;
        while (continueRunning) {
            displayScanPrompt();
            String qrText = scanQRCode();
            if (qrText == null || qrText.trim().isEmpty()) {
                qrFailCount++;
                displayScanFailure(qrFailCount);
                if (qrFailCount >= 10) {
                    qrFailCount = 0;
                    continueRunning = promptContinueOrExit();
                } else {
                    sleep(QR_RETRY_DELAY_MS);
                }
                continue;
            }
            qrFailCount = 0;
            displayScanSuccess();
            obstacleDetected = false;
            scanCount++;

            int scanScore = processQRText(qrText);
            sessionLog.commitScan(scanCount, qrText, scanScore);

            if (obstacleDetected) {
                continueRunning = promptRetryAfterObstacle();
                if (continueRunning) continue;
            }
            continueRunning = promptContinueOrExit();
        }
        terminateProgram();
    }

    private int processQRText(String qrText) {
        String[] rawTokens = qrText.split("&");
        if (rawTokens.length > MAX_TOKENS_PER_SCAN)
            displayError("Too many values (" + rawTokens.length + "). Processing first 5.");
        int toProcess = Math.min(rawTokens.length, MAX_TOKENS_PER_SCAN);
        boolean anyValid = false;
        int scanScore = 0;

        for (int i = 0; i < toProcess; i++) {
            String raw = rawTokens[i].trim().toUpperCase();
            line(C_GREY, "  -- Token " + (i + 1) + " of " + toProcess + " ----------------------------------------");
            if (!isValidHex(raw)) {
                line(C_RED, "  [x]  '" + raw + "' is not valid hex -- skipped.");
                continue;
            }
            anyValid = true;
            HexToken token = new HexToken(raw);
            sessionLog.addHex(token.hex);
            scanScore += token.score;
            displayTokenInfo(token);
            setAllUnderlights(token.rgb[0], token.rgb[1], token.rgb[2]);
            line(C_PINK, "  >  Dancing now...");
            executeDance(token);
            turnOffLights();
            line(C_GREEN, "  [+]  Routine complete.");
            System.out.println();
            if (obstacleDetected) break;
        }
        if (!anyValid) displayError("No valid hex values found in this scan.");
        return scanScore;
    }

    private boolean isValidHex(String token) {
        if (token == null || token.length() == 0 || token.length() > 2) return false;
        for (char c : token.toCharArray())
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'))) return false;
        return true;
    }

    private void executeDance(HexToken token) {
        double adjFwd = token.forwardDuration();
        int    spinMs = SPIN_360_MS;

        if (danceStyle == DanceStyle.SMOOTH)    { adjFwd *= 1.5; spinMs = (int)(SPIN_360_MS * 1.4); }
        if (danceStyle == DanceStyle.ENERGETIC) { adjFwd *= 0.7; }

        for (int i = token.binary.length() - 1; i >= 0; i--) {
            if (danceStyle == DanceStyle.STROBE) {
                setAllUnderlights(255, 255, 255); sleep(100);
                setAllUnderlights(0, 0, 0);       sleep(100);
            }
            if (token.binary.charAt(i) == '1') {
                if (obstacleNearby()) {
                    swiftBot.stopMove();
                    turnOffLights();
                    obstacleDetected = true;
                    line(C_RED + C_BOLD, "  [!]  OBSTACLE DETECTED -- dance stopped!");
                    return;
                }
                int durationMs = (int)(adjFwd * 1000);
                swiftBot.move(token.wheelSpeed, token.wheelSpeed, durationMs);
                sleep(durationMs);
            } else {
                swiftBot.move(token.wheelSpeed, -token.wheelSpeed, spinMs);
                sleep(spinMs);
                swiftBot.stopMove();
            }
            if (danceStyle == DanceStyle.SMOOTH) sleep(200);
        }
        swiftBot.stopMove();
    }

    private String scanQRCode() {
        try {
            BufferedImage img = swiftBot.getQRImage();
            if (img == null) return null;
            String result = swiftBot.decodeQRImage(img);
            return (result == null || result.isEmpty()) ? null : result;
        } catch (Exception e) {
            displayError("Camera error: " + e.getMessage());
            return null;
        }
    }

    private boolean obstacleNearby() {
        try {
            double d1 = swiftBot.useUltrasound();
            if (d1 <= 0 || d1 >= OBSTACLE_CM) return false;
            sleep(150);
            double d2 = swiftBot.useUltrasound();
            return d2 > 0 && d2 < OBSTACLE_CM;
        } catch (Exception e) {
            return false;
        }
    }

    private void setAllUnderlights(int r, int g, int b) {
        try { swiftBot.fillUnderlights(new int[]{r, g, b}); }
        catch (Exception e) { line(C_YELLOW, "  [!]  LED error: " + e.getMessage()); }
    }

    private void turnOffLights() {
        try { swiftBot.disableUnderlights(); } catch (Exception ignored) {}
    }

    private void line(String col, String content) {
        System.out.println(col + content + C_RESET);
    }

    private void displayWelcomeScreen() {
        System.out.println();
        line(C_YELLOW, "                              * . * . * . * . *");
        line(C_WHITE,  "                             .   \\ | /   \\ | /  .");
        line(C_YELLOW, "                           *----[DISCO BALL]----*");
        line(C_WHITE,  "                             '   / | \\   / | \\  '");
        line(C_YELLOW, "                              * ' * ' * ' * ' * ");
        System.out.println();
        line(C_PINK,   "  \\o/  o   \\o/  \\o   o/  \\o/  o   \\o/  o/  \\o   \\o/  o   \\o/  o/  \\o  \\o/  o   \\o/");
        line(C_CYAN,   "   |  /|\\   |   /|    |\\   |  /|\\   |   /|   |\\   |  /|\\   |   /|   |\\   |  /|\\   |  ");
        line(C_PINK,   "  / \\ | \\ / \\  / \\  / \\  / \\ | \\ / \\  / \\ / \\ / \\ | \\ / \\  / \\ / \\ / \\ | \\ / \\ ");
        System.out.println();
        line(C_YELLOW, "  o   \\o/  o/  \\o   \\o/  o   \\o/  o/  \\o  \\o/  o   \\o/  o/  \\o   \\o/  o   \\o/  o/ ");
        line(C_GREEN,  " /|\\   |  /|    |\\   |  /|\\   |  /|    |\\   |  /|\\   |  /|    |\\   |  /|\\   |  /|   ");
        line(C_YELLOW, " | \\ / \\ / \\ / \\ / \\ | \\ / \\ / \\ / \\ / \\ / \\ | \\ / \\ / \\ / \\ / \\ | \\ / \\ / \\ ");
        System.out.println();
        line(C_WHITE,  "    ######    ##      ##   ######    ######    ########  ######     ######     ##              ");
        line(C_CYAN,   "   ##         ##      ##     ##      ##           ##     ##   ##   ##    ##    ##              ");
        line(C_WHITE,  "   ##         ##  ##  ##     ##      ##           ##     ##   ##   ##    ##  ########          ");
        line(C_YELLOW, "    ######    ##  ##  ##     ##      ######       ##     ######    ##    ##    ##      ########");
        line(C_WHITE,  "         ##    ## ## ##      ##      ##           ##     ##   ##   ##    ##    ##              ");
        line(C_CYAN,   "         ##    ## ## ##      ##      ##           ##     ##   ##   ##    ##    ##              ");
        line(C_WHITE,  "   ##    ##     ##   ##      ##      ##           ##     ######     ######       ###           ");
        line(C_YELLOW, "    ######                 ######                                                              ");
        System.out.println();
        line(C_CYAN,   "  ######     ####    ##    ##   ######   ######      ##  ");
        line(C_WHITE,  "  ##   ##   ##  ##   ###   ##  ##        ##          ##  ");
        line(C_CYAN,   "  ##   ##   ##  ##   ## #  ##  ##        ##          ##  ");
        line(C_YELLOW, "  ##   ##   ######   ##  # ##  ##        #####       ##  ");
        line(C_WHITE,  "  ##   ##   ##  ##   ##   ###  ##        ##              ");
        line(C_CYAN,   "  ##   ##   ##  ##   ##    ##  ##        ##          ##  ");
        line(C_WHITE,  "  ######    ##  ##   ##    ##   ######   ######      ##  ");
        System.out.println();
        line(C_YELLOW, "  -*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-");
        System.out.println();
        line(C_PINK,   "  \\o/  o   \\o/  o/  \\o   \\o/  o   \\o/  o/  \\o   \\o/  o   \\o/  \\o   \\o/  o  ");
        line(C_CYAN,   "   |  /|\\   |  /|    |\\   |  /|\\   |   /|   |\\   |  /|\\   |   /|    |\\   |  ");
        line(C_PINK,   "  / \\ | \\ / \\ / \\ / \\ / \\ | \\ / \\  / \\ / \\ / \\ | \\ / \\  / \\ / \\ / \\ / \\ ");
        System.out.println();
        line(C_CYAN,   "  +-------------------------------------------------------+");
        line(C_WHITE,  "  |" + C_CYAN  + "    Task 9  --  CS1814 Software Implementation    " + C_WHITE + "    |");
        line(C_WHITE,  "  |" + C_GREEN + "      Scan QR codes  --  max 5 hex values each    " + C_WHITE + "    |");
        line(C_WHITE,  "  |" + C_GREY  + "             Example:  0A&1F&B7&03&FF              " + C_WHITE + "    |");
        line(C_CYAN,   "  +-------------------------------------------------------+");
        System.out.println();
    }

    private void displayScanPrompt() {
        System.out.println();
        line(C_CYAN,   "  +==============================================+");
        line(C_WHITE,  "  |" + C_GREEN + C_BOLD + "               >> READY TO SCAN <<              " + C_RESET + C_WHITE + "|");
        line(C_WHITE,  "  |" + C_GREY  + "      Hold your QR code up to the camera        " + C_WHITE + "|");
        line(C_WHITE,  "  |" + C_YELLOW + "                  Scanning...                   " + C_WHITE + "|");
        line(C_CYAN,   "  +==============================================+");
        System.out.println();
    }

    private void displayScanFailure(int attempt) {
        System.out.println();
        line(C_YELLOW, "  +==============================================+");
        line(C_WHITE,  "  |" + C_YELLOW + C_BOLD + "             [!]  NO QR CODE FOUND              " + C_RESET + C_WHITE + "|");
        String pad = attempt < 10 ? " " : "";
        line(C_WHITE,  "  |" + C_GREY   + "     Attempt " + pad + attempt + " of 10  --  retrying in 5s...    " + C_WHITE + "|");
        line(C_WHITE,  "  |" + C_GREY   + "     Reposition the code and hold it steady     " + C_WHITE + "|");
        line(C_YELLOW, "  +==============================================+");
        System.out.println();
    }

    private void displayScanSuccess() {
        System.out.println();
        line(C_GREEN,  "  +==============================================+");
        line(C_WHITE,  "  |" + C_GREEN + C_BOLD + "            [+]  QR CODE DETECTED!              " + C_RESET + C_WHITE + "|");
        line(C_WHITE,  "  |" + C_GREY  + "           Processing hex values...             " + C_WHITE + "|");
        line(C_GREEN,  "  +==============================================+");
        System.out.println();
    }

    private int selectDanceStyleCode() {
        System.out.println();
        line(C_PINK,  "  +==============================================+");
        line(C_WHITE, "  |" + C_PINK + C_BOLD + "              SELECT DANCE STYLE                " + C_RESET + C_WHITE + "|");
        line(C_PINK,  "  +==============================================+");
        line(C_WHITE, "  |                                              |");
        line(C_WHITE, "  |   " + C_CYAN + "[1]" + C_WHITE + "  Smooth      " + C_GREY + "slower, relaxed moves       " + C_WHITE + "|");
        line(C_WHITE, "  |   " + C_CYAN + "[2]" + C_WHITE + "  Energetic   " + C_GREY + "faster, intense moves       " + C_WHITE + "|");
        line(C_WHITE, "  |   " + C_CYAN + "[3]" + C_WHITE + "  Strobe      " + C_GREY + "flashing LED effects        " + C_WHITE + "|");
        line(C_WHITE, "  |                                              |");
        line(C_PINK,  "  +==============================================+");
        System.out.print(C_WHITE + "  >  Enter selection (1-3): " + C_RESET);
        int choice = 1;
        try {
            String input = scanner.nextLine().trim();
            int parsed = Integer.parseInt(input);
            if (parsed >= 1 && parsed <= 3) choice = parsed;
            else line(C_YELLOW, "  [!]  Invalid input -- defaulting to Smooth.");
        } catch (NumberFormatException e) {
            line(C_YELLOW, "  [!]  Invalid input -- defaulting to Smooth.");
        }
        return choice;
    }

    private void displayStyleConfirmation(DanceStyle style) {
        System.out.println();
        String col = style == DanceStyle.SMOOTH ? C_CYAN : style == DanceStyle.ENERGETIC ? C_YELLOW : C_PINK;
        line(col, "  +----------------------------------------------+");
        line(col, "  |  [+]  Dance style locked in:  " + C_BOLD + C_WHITE + style.getName() + C_RESET + col + "              |");
        line(col, "  +----------------------------------------------+");
        System.out.println();
    }

    private void displayError(String message) {
        System.out.println();
        line(C_RED,   "  +==============================================+");
        line(C_WHITE, "  |  " + C_RED + C_BOLD + "[!]  ERROR                                    " + C_RESET + C_WHITE + "|");
        line(C_WHITE, "  |  " + C_YELLOW + message + C_WHITE);
        line(C_RED,   "  +==============================================+");
        System.out.println();
    }

    private void displayTokenInfo(HexToken token) {
        StringBuilder seq = new StringBuilder();
        for (int i = token.binary.length() - 1; i >= 0; i--) {
            if (seq.length() > 0) seq.append(C_GREY).append(" > ").append(C_RESET);
            seq.append(token.binary.charAt(i) == '1'
                ? C_GREEN + "forward" + C_RESET
                : C_PINK  + "spin"    + C_RESET);
        }
        System.out.println();
        line(C_CYAN,  "  +==============================================+");
        System.out.println(C_WHITE + "  |  "
            + C_CYAN + C_BOLD + "TOKEN  " + C_RESET
            + C_YELLOW + C_BOLD + token.hex + C_RESET
            + C_GREY + "  --  conversion results"
            + C_WHITE + "               |" + C_RESET);
        line(C_CYAN,  "  +==============================================+");
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-10s" + C_RESET + C_WHITE + "  " + C_BOLD + "%-10d" + C_RESET + "%n", "Decimal",  token.decimal);
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-10s" + C_RESET + C_WHITE + "  " + C_BOLD + "%-10s" + C_RESET + "%n", "Binary",   token.binary);
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-10s" + C_RESET + C_WHITE + "  " + C_BOLD + "%-10d" + C_RESET + "%n", "Octal",    token.octal);
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-10s" + C_RESET + C_YELLOW + C_BOLD + "  %d%%"    + C_RESET + "%n", "Speed",    token.wheelSpeed);
        System.out.println(C_WHITE + "  |  " + C_GREY + "  Colour    "
            + C_WHITE + "  R=" + C_RED   + C_BOLD + token.rgb[0] + C_RESET
            + C_WHITE + "  G=" + C_GREEN + C_BOLD + token.rgb[1] + C_RESET
            + C_WHITE + "  B=" + C_CYAN  + C_BOLD + token.rgb[2] + C_RESET);
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-10s" + C_RESET + C_PINK  + C_BOLD + "  %-10s" + C_RESET + "%n", "Style",  danceStyle.getName());
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-10s" + C_RESET + C_GREEN + C_BOLD + "  %d pts"  + C_RESET + "%n", "Score",  token.score);
        line(C_CYAN,  "  +----------------------------------------------+");
        System.out.println(C_WHITE + "  |  " + C_GREY + "  Sequence  " + C_WHITE + "  " + seq.toString());
        line(C_CYAN,  "  +==============================================+");
        System.out.println();
    }

    private void displayLeaderboard() {
        if (sessionLog.isEmpty()) return;
        int bestIdx = sessionLog.bestIndex();
        ArrayList<String>  labels = sessionLog.getLabels();
        ArrayList<Integer> scores = sessionLog.getScores();
        System.out.println();
        line(C_YELLOW, "  +==============================================+");
        line(C_WHITE,  "  |" + C_YELLOW + C_BOLD + "              DANCE LEADERBOARD                 " + C_RESET + C_WHITE + "|");
        line(C_YELLOW, "  +==============================================+");
        for (int i = 0; i < scores.size(); i++) {
            String crown = (i == bestIdx) ? C_YELLOW + C_BOLD + "  << BEST" + C_RESET : "";
            System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-22s" + C_WHITE + C_BOLD + "  %5d pts" + C_RESET + "%s%n",
                labels.get(i), scores.get(i), crown);
        }
        line(C_YELLOW, "  +----------------------------------------------+");
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-22s" + C_GREEN + C_BOLD + "  %5d pts%n" + C_RESET,
            "Total session score", sessionLog.totalScore());
        line(C_YELLOW, "  +==============================================+");
    }

    private boolean promptRetryAfterObstacle() {
        System.out.println();
        line(C_RED,   "  +==============================================+");
        line(C_WHITE, "  |  " + C_RED + C_BOLD + "[!]  OBSTACLE DETECTED                        " + C_RESET + C_WHITE + "|");
        line(C_WHITE, "  |  " + C_GREY + "  Move SwiftBot to a bigger open area          " + C_WHITE + "|");
        line(C_WHITE, "  |                                              |");
        line(C_WHITE, "  |  " + C_GREEN + "  [Y]" + C_WHITE + " Redo dance        " + C_RED + "[X]" + C_WHITE + " End session           |");
        line(C_RED,   "  +==============================================+");
        System.out.println();
        final int[] result = {0};
        swiftBot.enableButton(Button.Y, () -> result[0] = 1);
        swiftBot.enableButton(Button.X, () -> result[0] = 2);
        while (result[0] == 0) { sleep(100); }
        swiftBot.disableButton(Button.Y);
        swiftBot.disableButton(Button.X);
        if (result[0] == 1) { obstacleDetected = false; return true; }
        return false;
    }

    private boolean promptContinueOrExit() {
        System.out.println();
        line(C_CYAN,  "  +==============================================+");
        line(C_WHITE, "  |  " + C_GREEN + C_BOLD + "[Y]" + C_RESET + C_WHITE + "  Scan another QR code                    |");
        line(C_WHITE, "  |  " + C_RED   + C_BOLD + "[X]" + C_RESET + C_WHITE + "  End session and show results            |");
        line(C_CYAN,  "  +==============================================+");
        System.out.println();
        final int[] result = {0};
        swiftBot.enableButton(Button.Y, () -> result[0] = 1);
        swiftBot.enableButton(Button.X, () -> result[0] = 2);
        while (result[0] == 0) { sleep(100); }
        swiftBot.disableButton(Button.Y);
        swiftBot.disableButton(Button.X);
        return result[0] == 1;
    }

    private void terminateProgram() {
        try { swiftBot.stopMove(); turnOffLights(); } catch (Exception ignored) {}
        System.out.println();
        line(C_GREEN,  "  +==============================================+");
        line(C_WHITE,  "  |" + C_GREEN + C_BOLD + "               SESSION COMPLETE                 " + C_RESET + C_WHITE + "|");
        line(C_GREEN,  "  +==============================================+");
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-22s" + C_RESET + C_WHITE + C_BOLD + "  %s%n" + C_RESET, "Scans completed",  scanCount);
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-22s" + C_RESET + C_WHITE + C_BOLD + "  %s%n" + C_RESET, "Valid hex values",  sessionLog.getAllHex().size());
        System.out.printf(C_WHITE + "  |  " + C_GREY + "  %-22s" + C_RESET + C_PINK  + C_BOLD + "  %s%n" + C_RESET, "Dance style",       danceStyle.getName());
        line(C_GREEN,  "  +==============================================+");
        displayLeaderboard();
        if (!sessionLog.getAllHex().isEmpty()) {
            line(C_GREY, "  -- Sorting values...");
            sessionLog.sortHexAscending();
            line(C_GREY, "  -- Writing session log...");
            sessionLog.writeToFile(danceStyle);
        }
        scanner.close();
        System.out.println();
        line(C_GREY,           "  -----------------------------------------------");
        line(C_GREEN + C_BOLD, "       See you next time, dancer.  ~*~");
        line(C_GREY,           "  -----------------------------------------------");
        System.out.println();
        System.exit(0);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
