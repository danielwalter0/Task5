package com.labs.Daniel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;


public class GameLogger {
    private final String FILE_PATH = "mastermind_log.txt";
    private BufferedWriter bufferedWriter;
    private File logFile;


    public GameLogger() {
        //creating a file object which is going to be the game log file
        logFile = new File(FILE_PATH);
        try {
            //checking if the log file already exists, if not, creating one
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            //append is true so every time the program starts previous logs would not be erased
            bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
            //writing the date and time to differentiate between different logs from different games
            String dateTime = DateFormat.getDateTimeInstance().format(new Date());
            //if the file is not empty the file writer is going to separate the sessions with a clear line
            if(logFile.length() != 0){
                bufferedWriter.newLine();
            }
            bufferedWriter.write("Game started at: " + dateTime);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
        } catch (IOException e) {
            ConsoleUI.printErrorMessage("There was a problem with log file. Please try again and check the logs directory.");
            System.exit(1);
        }

    }

    public void log(String logLine){
        try {
            bufferedWriter.write(logLine);
            bufferedWriter.newLine(); // goes to a new line
            bufferedWriter.flush(); // flushes characters from the write buffer
        } catch (IOException e) {
            ConsoleUI.printErrorMessage("There was a problem with log file. Please try again and check the logs directory.");
        }
    }

    public String getLogFilePath(){
        return logFile.getAbsolutePath();
    }

}
