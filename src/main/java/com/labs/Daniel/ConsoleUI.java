package src.main.java.com.labs.Daniel;

//A separate class to handle all console print statements e.g. win/lose screen, error message
// and making them coloured
public class ConsoleUI {
    //enum includes the colours that are needed for the console UI
    // and each colour code value corresponds to colour name
    public enum ConsoleColor {
        RED("\033[31m"),
        VIOLET("\033[35m"),
        GREEN("\033[32m"),
        CYAN("\033[36m"),
        YELLOW("\033[33m"),
        RESET("\033[0m");

        private String colourCode;

        ConsoleColor(String colourCode) {
            this.colourCode = colourCode;
        }

        public String getColourCode() {
            return colourCode;
        }
    }
    //just to make all borders universal I declare it once
    static String border = "============================================================";

    public static String colorText(String text, ConsoleColor color){
        return color.getColourCode() + text + ConsoleColor.RESET.getColourCode();
    }


    //this method prints the MasterMind logo and a welcome message
    public static void printWelcomeMessage(){

        System.out.println(colorText(
                "__  __    _    ____ _____ _____ ____  __  __ ___ _   _ ____  \n" +
                        "|  \\/  |  / \\  / ___|_   _| ____|  _ \\|  \\/  |_ _| \\ | |  _ \\ \n" +
                        "| |\\/| | / _ \\ \\___ \\ | | |  _| | |_) | |\\/| || ||  \\| | | | |\n" +
                        "| |  | |/ ___ \\ ___) || | | |___|  _ <| |  | || || |\\  | |_| |\n" +
                        "|_|  |_/_/   \\_\\____/ |_| |_____|_| \\_\\_|  |_|___|_| \\_|____/ ",
                ConsoleColor.CYAN
        ));

        System.out.println(colorText("WELCOME TO MASTERMIND", ConsoleColor.GREEN));
        System.out.println();
    }

    //this method is used to print a box with a heading and a text inside
    public static void printBox(String text1, ConsoleColor color1, String text2, ConsoleColor color2){
        System.out.println(border);
        System.out.println(colorText(text1, color1));
        System.out.println(border);
        System.out.println(colorText(text2, color2));
        System.out.println(border);

    }
    public static void printBox(String text1, ConsoleColor color1, String text2, ConsoleColor color2,  String text3,  ConsoleColor color3){
        System.out.println(border);
        System.out.println(colorText(text1, color1));
        System.out.println(border);
        System.out.println(colorText(text2, color2));
        System.out.println();
        System.out.println(colorText(text3, color3));
        System.out.println(border);

    }

    // prints error message with the text you put in
    public static void printErrorMessage(String errorMessage) {

        System.out.println(border);

        // ERROR title line
        System.out.println(colorText("ERROR", ConsoleColor.RED));
        System.out.println(border);

        // Message line
        System.out.println(colorText(errorMessage, ConsoleColor.RED));

        System.out.println(border);
    }

    public static void printWinMessage() {
        System.out.println(ConsoleUI.colorText(
                " __   __  ___   _   _     __        ___  _   _ \n" +
                        " \\ \\ / / / _ \\ | | | |    \\ \\      / / || \\ | |\n" +
                        "  \\ V / | | | || | | |     \\ \\ /\\ / /| ||  \\| |\n" +
                        "   | |  | |_| || |_| |      \\ V  V / | || |\\  |\n" +
                        "   |_|   \\___/  \\___/        \\_/\\_/  |_||_| \\_|\n",
                ConsoleColor.GREEN
        ));
    }

    public static void printGameOverMessage(){
        System.out.println(ConsoleUI.colorText(
                "  ____    _    __  __ _____    _____     _______ ____  \n" +
                        " / ___|  / \\  |  \\/  | ____|  / _ \\ \\   / / ____|  _ \\ \n" +
                        "| |  _  / _ \\ | |\\/| |  _|   | | | \\ \\ / /|  _| | |_) |\n" +
                        "| |_| |/ ___ \\| |  | | |___  | |_| |\\ V / | |___|  _ < \n" +
                        " \\____/_/   \\_\\_|  |_|_____|  \\___/  \\_/  |_____|_| \\_\\",
                ConsoleColor.RED
        ));
    }





}
