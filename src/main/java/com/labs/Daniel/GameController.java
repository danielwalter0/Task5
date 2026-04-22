package src.main.java.com.labs.Daniel;

import swiftbot.Button;
import swiftbot.SwiftBotAPI;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import static swiftbot.ImageSize.*;
import static swiftbot.ImageSize.SQUARE_720x720;

public class GameController {
    private SwiftBotAPI swiftBot;
    private int playerWins = 0;
    private int botWins = 0;
    private int roundNumber = 0;
    private boolean continueGame = true;
    private int codeLength;
    private int maxGuesses;
    private int guessCount;
    private Colour[] guessArray;
    private boolean pressed = false;
    private boolean hintsEnabledThisRound;
    private Scanner sc = new Scanner(System.in);
    private GameLogger gameLogger;
    private String playerInputForLog; // storing user guesses for game logging
    private boolean hintUsed;
    private SecretCode secretCode; // the secrete code that the player should guess
    private int feedbackPlusCount;
    private int cardNumber; // card number for capturing guess


    public void startGame() throws InterruptedException {
        try{
            swiftBot = SwiftBotAPI.INSTANCE;
            gameLogger = new GameLogger();
        } catch (Exception e) {
            System.err.println("There was a problem, please try again. \n" + e.getMessage());
            System.exit(1);
        }


        ConsoleUI.printWelcomeMessage();
        System.out.println("Place SwiftBot 40 cm from the card scanning area.");

        //this loop keeps running until the player selects to quit and continue game gets false
        while(continueGame){
            playOneRound();
        }


        //just before the program terminates, the log file location is displayed
        ConsoleUI.printBox("THANK YOU FOR PLAYING!",ConsoleUI.ConsoleColor.CYAN,"Log file saved at:",ConsoleUI.ConsoleColor.RESET,gameLogger.getLogFilePath(),ConsoleUI.ConsoleColor.YELLOW);

        System.exit(0);
    }
    public void selectMode() throws InterruptedException {
        System.out.println("Select Game Mode:");
        //These print statements use ConsoleUI class methods to make them colourful
        System.out.println(ConsoleUI.colorText("Press Button [A] for Default Mode", ConsoleUI.ConsoleColor.YELLOW));
        System.out.println(ConsoleUI.colorText("Press Button [B] for Custom Mode", ConsoleUI.ConsoleColor.YELLOW));
        System.out.println();

        //reset pressed variable
        pressed = false;
        //reset all buttons to avoid any errors
        clearButtons();
        swiftBot.enableButton(Button.A, () -> {
            swiftBot.disableButton(Button.A);
            startDefaultMode();
            pressed = true;
        });
        swiftBot.enableButton(Button.B, () -> {
            swiftBot.disableButton(Button.B);
            startCustomMode();
            pressed = true;
        });
        //this loop waits until the button is pressed so that program wouldn't go forward
        while(!pressed){
            Thread.sleep(50);
        }

    }


    public void startDefaultMode(){
        codeLength = 4;
        maxGuesses = 6;
        ConsoleUI.printBox("DEFAULT MODE SELECTED", ConsoleUI.ConsoleColor.CYAN,"Code length    : 4\nColours        : R G B Y O P\nMax Guesses    : 6",ConsoleUI.ConsoleColor.RESET);
    }

    public void startCustomMode(){
        ConsoleUI.colorText("CUSTOM MODE SELECTED", ConsoleUI.ConsoleColor.CYAN);
        System.out.println("Please enter the desired code length (3-6): ");
        //validation loop - will loop while the valid is false
        boolean valid = false;
        while(!valid){
            try{
                codeLength = sc.nextInt();
                sc.nextLine();
                if(codeLength >= 3 && codeLength <= 6){
                    valid = true;
                }
                else{
                    ConsoleUI.printErrorMessage("Please enter a valid code length (3-6): ");
                }

            }catch (Exception e){
                ConsoleUI.printErrorMessage("Please enter a valid integer (3 <= code length <= 6)");
                //to clear the input
                sc.nextLine();
            }
        }
        System.out.println("Please enter the desired amount of allowed guesses (>=1): ");
        valid = false;
        while(!valid){
            try{
                maxGuesses = sc.nextInt();
                sc.nextLine();
                if(maxGuesses >= 1){
                    valid = true;
                }
                else{
                    ConsoleUI.printErrorMessage("Please enter a valid amount of guesses (Max Guesses >= 1): ");
                }
            }catch (Exception e){
                ConsoleUI.printErrorMessage("Please enter a valid integer (Max Guesses >= 1)");
                //to clear the previous input
                sc.nextLine();
            }
        }
        ConsoleUI.printBox("CUSTOM MODE CONFIRMED",ConsoleUI.ConsoleColor.CYAN,"Code length    : "+codeLength+"\nColours        : R G B Y O P\nMax Guesses    : "+maxGuesses,ConsoleUI.ConsoleColor.RESET);

    }

    public void playOneRound() throws InterruptedException {
        //select mode after each game in case the user wants to try a different game mode
        selectMode();
        //increment round number
        roundNumber++;
        secretCode = new SecretCode(codeLength);
        hintUsed = false;

        //make a little pause before the round starts
        Thread.sleep(2000);
        ConsoleUI.printBox("ROUND "+roundNumber+" START",ConsoleUI.ConsoleColor.CYAN,"Settings: Code length - "+codeLength+" | Max Guesses - "+maxGuesses+"\nScore   : Player "+playerWins+" - SwiftBot "+botWins,ConsoleUI.ConsoleColor.RESET, "Enable ONE hint for this round? (Y/N)",ConsoleUI.ConsoleColor.YELLOW);

        //hint enabling answer validation loop
        boolean validInput = false;
        while(!validInput) {
            String userInput = sc.nextLine();
            //validating user input, also making sure the lower case values can be validated
            if (userInput.equalsIgnoreCase("y")) {
                hintsEnabledThisRound = true;
                validInput = true;
            } else if (userInput.equalsIgnoreCase("n")) {
                hintsEnabledThisRound = false;
                validInput = true;
            } else {
                ConsoleUI.printErrorMessage("Invalid selection. please enter Y or N.");
            }
        }


        playerInputForLog = "";
        guessCount = 0;

        clearButtons();
        //enabling the hint button so that a user can request a hint at any point during an active round
        if(hintsEnabledThisRound && !hintUsed){
            swiftBot.enableButton(Button.B, () -> {
                try {
                    requestHint();
                } catch (InterruptedException e) {
                    ConsoleUI.printErrorMessage("There was a problem with requesting a hint.");
                }
            });
        }

        //looping while the player has not run out of guesses
        //main loop that captures the guess and shows the feedback
        while(guessCount < maxGuesses){
            captureGuess();
            computeFeedback();
            if (feedbackPlusCount == codeLength) {
                playerWins++;
                ConsoleUI.printWinMessage();
                ConsoleUI.printBox("CONGRATULATIONS! YOU WON!", ConsoleUI.ConsoleColor.GREEN,"SCOREBOARD",ConsoleUI.ConsoleColor.YELLOW,"Player   :" + playerWins+"\nSwiftBot : "+botWins,ConsoleUI.ConsoleColor.RESET);
                //to exit the loop when the player won
                break;
            }

            guessCount++;

            //this if statement check, if the player has any guesses left in order to ask if the player wants to continue to the next guess
            if(guessCount < maxGuesses){
                waitForContinue();
            }
        }

        //disabling hint button if not used
        swiftBot.disableButton(Button.B);


        //to ensure the game over message only shows when a player has run out of guesses
        if(guessCount == maxGuesses){
            botWins++;
            ConsoleUI.printGameOverMessage();
            ConsoleUI.printBox("ROUND LOST",ConsoleUI.ConsoleColor.RED,"You have used all your available guesses.\nThe correct code was: "+secretCode.toString()+"\nScore: Player "+playerWins+" - SwiftBot "+botWins,ConsoleUI.ConsoleColor.RESET);
        }


        //logging round results to the log file
        gameLogger.log("");
        gameLogger.log("Round " + roundNumber + " results:");
        gameLogger.log("Secret Code: " + secretCode.toString());
        gameLogger.log("Player Input: " + playerInputForLog);
        gameLogger.log("Score: Player " + playerWins+" - SwiftBot " + botWins);
        gameLogger.log("Total Number of Guesses: " + guessCount);
        gameLogger.log("Remaining Guesses: " + (maxGuesses - guessCount));

        replayDecision();
    }

    public void captureGuess() throws InterruptedException {
        guessArray = new Colour[codeLength];

        //adding a hint string which is only going to show up when hint is enabled, otherwise it is just going to be empty
        String hintString = "";
        if(hintsEnabledThisRound){
            hintString = "Hint Enabled: Yes | Hint used: " + (hintUsed ? "Yes" : "No") + (hintUsed ? "" : "\nTo request a hint press button [B]");
        }
        ConsoleUI.printBox("SCANNING GUESS", ConsoleUI.ConsoleColor.VIOLET, "Round: "+roundNumber+" | Guess "+(guessCount+1)+" of "+maxGuesses,ConsoleUI.ConsoleColor.RESET,hintString,ConsoleUI.ConsoleColor.YELLOW);

        cardNumber = 0;
        for(int i = 0; i < guessArray.length; i++){
            Thread.sleep(1000);
            captureCardUsingCamera();
            cardNumber++;
        }

        //add guess to the player guess for logging
        playerInputForLog = playerInputForLog + "Guess Number " + (guessCount+1) + ": " + Arrays.toString(guessArray);
    }

    //this method captures the image using swiftbot camera and calls the method to recognise the colour from this image
    public void captureCardUsingCamera() throws InterruptedException {
        boolean colourDetected = false;
        while(!colourDetected){
            Thread.sleep(2000);
            ConsoleUI.printBox("CAMERA SCAN", ConsoleUI.ConsoleColor.CYAN,"Hold colour card #"+(cardNumber+1)+" in front of the camera.",ConsoleUI.ConsoleColor.RESET,"[Scanning...]", ConsoleUI.ConsoleColor.YELLOW);
            System.out.println("1");
            Thread.sleep(2000);
            System.out.println("2");
            Thread.sleep(2000);
            System.out.println("3");
            Thread.sleep(1000);
            BufferedImage guessImage = swiftBot.takeStill(SQUARE_720x720);
            // crop the center of the image (slightly lower) 300x300 of 720x720
            guessImage = guessImage.getSubimage(210, 230, 300, 300);
            System.out.println("Picture taken!");
            colourDetected = detectColourFromImage(guessImage);
        }


    }

    //this method processes the image and recognises the colour from it
    public boolean detectColourFromImage(BufferedImage image) throws InterruptedException {
        //these variables will hold the total value for each colour in the pixels of the photo
        int redTotal = 0;
        int greenTotal = 0;
        int blueTotal = 0;

        //image processing
        for(int i = 0; i < image.getWidth(); i++){
            for(int j = 0; j < image.getHeight(); j++){
                //getting the rgb value of each pixel
                int rgb = image.getRGB(i,j);
                //bit shifting to get the value of each individual colour
                int red   = (rgb >> 16) & 0xff;
                int green = (rgb >> 8)  & 0xff;
                int blue  = rgb & 0xff;

                redTotal += red;
                greenTotal += green;
                blueTotal += blue;
            }
        }

        int pixelCount = image.getWidth() * image.getHeight();
        //calculating an average value of each colour
        int redAverage = redTotal / pixelCount;
        int greenAverage = greenTotal / pixelCount;
        int blueAverage = blueTotal / pixelCount;

        //this variable shows the threshold a maximum distance that will be accepted from colour detection
        int colourThreshold = 5000;

        //creating a variable with a very large value (so that if statement would run in the first place and store distance to first colour )
        //to find the smallest distance to a colour out of all
        Colour closestColour = null;
        int smallestDistance = Integer.MAX_VALUE;
        //colour detection loop, it finds the distance to each colour and leaves only the smallest distance colour
        for(Colour colour : Colour.values()){
            int diffRed = redAverage - colour.getRed();
            int diffGreen = greenAverage - colour.getGreen();
            int diffBlue = blueAverage - colour.getBlue();
            //squaring each difference value and summing them up to get the distance
            // Weighted Euclidean distance — green is weighted highest (x4) as the human eye is most sensitive to it,
            // red is weighted x2 and blue x3, this reduces misclassification between visually similar colours
            int distanceToColour = (2*diffRed*diffRed + 4*diffGreen*diffGreen + 3*diffBlue*diffBlue);
            //searching for the smallest distance
            if(distanceToColour < smallestDistance){
                smallestDistance = distanceToColour;
                closestColour = colour;
            }
        }

        //if a colour is successfully found, that is if the distance is smaller than the minimum threshold, detected = true
        if(smallestDistance <= colourThreshold){
            guessArray[cardNumber] = closestColour;
            return true;
        }
        Thread.sleep(2000);
        ConsoleUI.printErrorMessage("Colour not recognised. Please rescan the same card.");
        Thread.sleep(1000);
        return false;

    }

    public void computeFeedback(){
        feedbackPlusCount = 0;
        int feedbackMinusCount = 0;

        //code to count how many + and -
        for(int i = 0;i < codeLength;i++){
            //checks for colours in correct positions
            if(guessArray[i] == secretCode.getSecretCode()[i]){
                feedbackPlusCount++;
            }
            //checks for colours which are in an incorrect position but are in the secret code
            if(guessArray[i] != secretCode.getSecretCode()[i] && Arrays.asList(secretCode.getSecretCode()).contains(guessArray[i])){
                feedbackMinusCount++;
            }
        }

        String feedback = "";
        for(int i=0; i<feedbackPlusCount;i++){
            feedback = feedback + "+ ";
        }
        for(int i=0; i<feedbackMinusCount;i++){
            feedback = feedback + "- ";
        }

        ConsoleUI.printBox("FEEDBACK", ConsoleUI.ConsoleColor.VIOLET,"Result            : "+feedback+"\nGuesses remaining : "+(maxGuesses-guessCount-1)+"\n\nNote: '+' shown before '-'. Colours/Positions  not revealed.",ConsoleUI.ConsoleColor.RESET);

    }

    public void replayDecision() throws InterruptedException {
        ConsoleUI.printBox("PLAY AGAIN?",ConsoleUI.ConsoleColor.VIOLET,"Press [Y] to continue to next round\nPress [X] to quit game",ConsoleUI.ConsoleColor.YELLOW);
        //reset the pressed variable
        pressed = false;
        //reset all buttons to avoid any errors
        clearButtons();
        swiftBot.enableButton(Button.X, () -> {
            continueGame = false;
            pressed = true;
            swiftBot.disableButton(Button.X);
        });
        swiftBot.enableButton(Button.Y, () -> {
            continueGame = true;
            pressed = true;
            swiftBot.disableButton(Button.Y);
        });
        //this loop waits until the button is pressed so that program wouldn't go forward
        while(!pressed){
            Thread.sleep(50);
        }
    }

    //resetting buttons so that an error of a function not disabled from a button wouldn't show up
    public void clearButtons(){
        for(Button b : Button.values()){
            swiftBot.disableButton(b);
        }
    }




    public void requestHint() throws InterruptedException {
        //making sure the hint is only going to reveal the positions which the player has not yet guessed
        ArrayList<Integer> availableHintPositions = new ArrayList<>();
        for(int i=0; i<codeLength; i++){
            if(guessArray[i] != secretCode.getSecretCode()[i]){
                availableHintPositions.add(i);
            }
        }
        Random random = new Random();
        int randomIndex = random.nextInt(availableHintPositions.size());
        int hintPosition = availableHintPositions.get(randomIndex);

        Colour hintColour = secretCode.getSecretCode()[hintPosition];
        int[] hintUnderLightColour = new int[]{hintColour.getRed(),hintColour.getGreen(),hintColour.getBlue()};

        ConsoleUI.printBox("HINT REQUESTED", ConsoleUI.ConsoleColor.VIOLET, "Colour " + hintColour.getColourLetter() + " at position " + (hintPosition+1), ConsoleUI.ConsoleColor.RESET);
        swiftBot.fillUnderlights(hintUnderLightColour);
        Thread.sleep(4000);
        swiftBot.disableUnderlights();

        //disabling the button so that the user could not request the hint once again
        hintUsed = true;
        swiftBot.disableButton(Button.B);
    }

    //this method is enabling the program to wait while the user analyses feedback and then presses a button to continue capturing the next guess
    public void waitForContinue() throws InterruptedException {
        ConsoleUI.printBox("CONTINUE", ConsoleUI.ConsoleColor.CYAN,"Press button [A] to capture the next guess.",ConsoleUI.ConsoleColor.VIOLET,(hintUsed ? "" : "Press button [B] to request a hint"),ConsoleUI.ConsoleColor.YELLOW);

        pressed = false;

        swiftBot.enableButton(Button.A, () -> {
            pressed = true;
            swiftBot.disableButton(Button.A);
        });

        while(!pressed){
            Thread.sleep(50);
        }
    }
}
