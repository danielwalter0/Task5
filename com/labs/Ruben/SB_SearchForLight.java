package com.labs.Ruben;

import swiftbot.*;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;

public class SB_SearchForLight {
	static SwiftBotAPI swiftBot;
	// ANSI color codes
	static final String RESET = "\u001B[0m";
	static final String CYAN = "\u001B[36m";
	static final String YELLOW = "\u001B[33m";
	static final String GREEN = "\u001B[32m";
	static final String WHITE = "\u001B[37m";
	static final String BOLD = "\u001B[1m";
	static final String RED = "\u001B[31m";
	static final String PINK = "\u001B[95m";
	
	//VARIABLES
	enum Mode 
	{
	    CAUTIOUS, NORMAL, AGGRESSIVE
	}
	
	// Behaviour settings
	static Mode currentMode;
	static int speed;
	static float obstacleDistance;
	static long delay;

	// Tracking variables
	static int detectedObjects = 0;
	static int distanceTravelled = 0;
	
	static double thresholdIntensity = 0;
	static double previousIntensity = 0;
	static double highestIntensity = 0;
	
	static double currentLeftIntensity = 0;
	static double currentCenterIntensity = 0;
	static double currentRightIntensity = 0;
	
	static String currentDirection = "";
	
	static long startTime;
	static boolean initialRun = true;
	
	static boolean buttonPressed = false;
	static boolean modeSelected = false;
	static boolean terminated = false;

	static ArrayList<Double> lightIntensityHistory = new ArrayList<>();
	static ArrayList<String> movementHistory = new ArrayList<>();
	
	//METHODS
	public static void main(String[] args) throws InterruptedException 
	{	
		//SearchForLight();
	}
	
	public static void SearchForLight()
	{
		try 
		{
	        swiftBot = SwiftBotAPI.INSTANCE;
	    } 
		catch (Exception e) 
		{
	        System.out.println("ERROR: SwiftBot not accessible.");
	        System.exit(5);
	    }

	    Scanner reader = new Scanner(System.in);

	    // UI HEADER
	    System.out.println(PINK + BOLD
	            + "  _________       .__  _____  __ ___.           __   \r\n"
	            + " /   _____/_  _  _|__|/ ____\\/  |\\_ |__   _____/  |_ \r\n"
	            + " \\_____  \\\\ \\/ \\/ /  \\   __\\\\   __\\ __ \\ /  _ \\   __\\\r\n"
	            + " /        \\\\     /|  ||  |   |  | | \\_\\ (  <_> )  |  \r\n"
	            + "/_______  / \\/\\_/ |__||__|   |__| |___  /\\____/|__|  \r\n"
	            + "        \\/                            \\/             " + RESET);
	    
	    System.out.println(CYAN
	            + "\n========================================\n"
	            + "        SWIFTBOT LIGHT SEEKER\n"
	            + "========================================" + RESET);
	    
	    System.out.println(CYAN + "\nPress Button A on the SwiftBot to start..." + RESET);

	    // Wait for Button A
	    
	    swiftBot.enableButton(Button.A, () -> {
			buttonPressed = true;
			swiftBot.disableButton(Button.A);
		});
	    
	    while (!buttonPressed) 
	    { 
	    	try { Thread.sleep(50); } catch (InterruptedException e) {}  	
	    }
	    	
    	System.out.println(WHITE + BOLD + "\nSelect Exploration Mode:" + RESET);
    	System.out.println(CYAN + "1. CAUTIOUS - Slow, careful movement" + RESET);
    	System.out.println(CYAN + "2. NORMAL - Default behaviour" + RESET);
    	System.out.println(CYAN + "3. AGGRESSIVE - Fast, bold movement" + RESET);
    	System.out.print(WHITE + BOLD + "Enter your choice (1-3): " + RESET);
	    
	    // MODE SELECTION
	    while (!modeSelected) 
	    {
	        String input = reader.next();

	        if (input.equals("1")) {
	            currentMode = Mode.CAUTIOUS;
	            speed = 40;
	            obstacleDistance = 60;
	            delay = 2000;
	            modeSelected = true;
	        } 
	        else if (input.equals("2")) {
	            currentMode = Mode.NORMAL;
	            speed = 50;
	            obstacleDistance = 50;
	            delay = 750;
	            modeSelected = true;
	        } 
	        else if (input.equals("3")) {
	            currentMode = Mode.AGGRESSIVE;
	            speed = 100;
	            obstacleDistance = 40;
	            delay = 100;
	            modeSelected = true;
	        } 
	        else 
	        {
	        	System.out.println(RED + "ERROR: Invalid input. Please try again." + RESET);
	        }
	    }

	    // INITIALISE VARIABLES
	    detectedObjects = 0;
	    distanceTravelled = 0;
	    startTime = System.currentTimeMillis();
	    initialRun = true;

	    System.out.println(GREEN + BOLD + "\nMode selected: " + currentMode + RESET);
	    System.out.println(GREEN + "System initialised. Beginning search...\n" + RESET);
	    
	    // Proceed to next stage
	    CaptureImage();
	}
	
	public static void CaptureImage()
	{
		try 
		{
	        // Capture grayscale image
	        BufferedImage img = swiftBot.takeGrayscaleStill(ImageSize.SQUARE_720x720);
	        
	        if (img == null) 
	        {
	            System.out.println(RED + "ERROR: Could not capture image." + RESET);
	            return;
	        }
	              
	        int width = img.getWidth();
	        int height = img.getHeight();
	        
	        // Columns: left, center, right
	        double leftSum = 0;
	        double centerSum = 0;
	        double rightSum = 0;
	        double totalSum = 0;
	        
	        for (int y = 0; y < height; y++) 
	        {
	            for (int x = 0; x < width; x++) 
	            {
	                int pixel = img.getRGB(x, y); //Get RGB value of pixel
	                int gray = (pixel >> 16) & 0xFF; //Get red channel (shift 16 bits to right then mask to get exclusively red value)
	                
	                totalSum += gray;
	                
	                if (x < width / 3) //x < 240 
	                {
	                    leftSum += gray;
	                } 
	                else if (x < 2 * width / 3)  //x < 480
	                {
	                    centerSum += gray;
	                } 
	                else //x < 720
	                {
	                    rightSum += gray;
	                }
	            }           
	        }
	        
            int columnPixels = height * (width / 3); //Get the number of pixels in each column
            double leftAvg = leftSum / columnPixels;
            double centerAvg = centerSum / columnPixels;
            double rightAvg = rightSum / columnPixels;
            double avgOverall = totalSum / (width * height);

            // Display averages in CLI
            System.out.printf(PINK + "\nLeft: %.1f | Center: %.1f | Right: %.1f | Overall: %.1f\n" + RESET, leftAvg, centerAvg, rightAvg, avgOverall);

            currentLeftIntensity = leftAvg;
            currentCenterIntensity = centerAvg;
            currentRightIntensity = rightAvg;
            
            lightIntensityHistory.add(leftAvg);
            lightIntensityHistory.add(centerAvg);
            lightIntensityHistory.add(rightAvg);
            
            if (leftAvg > highestIntensity) highestIntensity = leftAvg;
            if (centerAvg > highestIntensity) highestIntensity = centerAvg;
            if (rightAvg > highestIntensity) highestIntensity = rightAvg;
            
            if (initialRun) 
            {
                thresholdIntensity = avgOverall;
                previousIntensity = thresholdIntensity;
                initialRun = false;
                
                System.out.println(GREEN + "Initial light threshold set to: " + thresholdIntensity + RESET);
                ChooseDirection();
            } 
            else 
            {
                // Check if overall light intensity changed by >=5%
                double changePercent = Math.abs(avgOverall - previousIntensity) / previousIntensity * 100;
                
                if (changePercent >= 5) 
                {
                	previousIntensity = avgOverall;
                	ChooseDirection();
                } 
                else 
                {
                	Wander();
                }
            }

		} 
		catch (Exception e) 
		{
	        e.printStackTrace();
	    }
	}

	public static void ChooseDirection() throws InterruptedException 
	{
        Thread.sleep(delay);
		
		int obstacleAhead = CheckObstacle();
	    
	    if (obstacleAhead == 3)
	    {
	    	return;
	    }
	    
	    ArrayList<String> availableColumns = new ArrayList<>();
	    availableColumns.add("LEFT");
	    availableColumns.add("CENTER");
	    availableColumns.add("RIGHT");

	    // If obstacle detected in a column, remove that column
	    if (obstacleAhead == 2) 
	    {
	        String blockedColumn = "CENTER";
	        availableColumns.remove(blockedColumn);
	        System.out.println(YELLOW + "Avoiding column: " + blockedColumn + RESET);
	    }

	    // Determine the next column with the highest intensity among available
	    double maxIntensity = -1;
	    ArrayList<String> candidates = new ArrayList<>();

	    for (String col : availableColumns) 
	    {
	        double intensity = 0;
	        
	        switch (col) 
	        {
	            case "LEFT": intensity = currentLeftIntensity; break;
	            case "CENTER": intensity = currentCenterIntensity; break;
	            case "RIGHT": intensity = currentRightIntensity; break;
	        }

	        if (intensity > maxIntensity) 
	        {
	            maxIntensity = intensity;
	            candidates.clear();
	            candidates.add(col);
	        } 
	        else if (intensity == maxIntensity) 
	        {
	            candidates.add(col);
	        }
	    }

	    // Randomly choose if there are multiple candidates
	    Random rand = new Random();
	    String chosenDirection;
	    
	    if (candidates.size() > 1) 
	    {
	        chosenDirection = candidates.get(rand.nextInt(candidates.size()));
	    }
	    else 
	    {
	        chosenDirection = candidates.get(0);
	    }

	    System.out.println(GREEN + "Chosen direction: " + chosenDirection + RESET);

	    currentDirection = chosenDirection;

	    // Call Move
	    Move();
	}
	
	public static int CheckObstacle() 
	{    
	    try 
	    {
	        // Measure distance to obstacle using ultrasound
	        double distance = swiftBot.useUltrasound();
	        
	        if (distance < obstacleDistance) 
	        {  
	            detectedObjects++;  // Increment obstacle counter
	            
	            // Display distance in CLI
	            System.out.printf(RED + BOLD + "Obstacle detected! Distance: %.1f cm\n" + RESET, distance);

	            // Blink underlights red 3 times
	            int[][] redColor = { {255, 0, 0} };  
	            
	            for (int i = 0; i < 3; i++) 
	            { 
	                swiftBot.fillUnderlights(redColor[0]);
	                Thread.sleep(300);
	                swiftBot.disableUnderlights();
	                Thread.sleep(300);
	            }

	            // Capture image of the obstacle
	            BufferedImage obsImg = swiftBot.takeGrayscaleStill(ImageSize.SQUARE_720x720);
	            String filename = "/data/home/pi/Obstacle_" + System.currentTimeMillis() + ".png";
	            ImageIO.write(obsImg, "png", new File(filename));
	            
	            System.out.println(GREEN + "Obstacle image saved: " + filename + RESET);

	            // Check if too many obstacles have been encountered
	            if (detectedObjects >= 5) 
	            {
	            	System.out.println(RED + BOLD + "Attention: 5 objects detected within 5 minutes." + RESET);
	                End();
	                return 3;
	            }

	            return 2;
	        } 
	    } 
	    catch (Exception e) 
	    {
	        System.out.println(RED + "ERROR: Obstacle detection failed!" + RESET);
	        e.printStackTrace();
	    }

	    // No obstacle detected
	    return 1;
	}
	
	public static void Move() throws InterruptedException 
	{
	    // Display current speed and direction
	    System.out.println(GREEN + BOLD + "\nMoving at speed: " + speed + " | Direction: " + currentDirection + RESET);
	       
	    try 
	    {
	    	// Set underlights to green
		    int[][] greenColor = { {0, 255, 0} }; 
		    swiftBot.fillUnderlights(greenColor[0]);
		    
		    boolean rotated = false;

		    if (currentDirection.equals("LEFT")) 
		    {
		    	swiftBot.move(0, 100, 1000);
		        rotated = true;	        
		        System.out.println(CYAN + "Rotating 30 degrees left..." + RESET);
		    } 
		    else if (currentDirection.equals("RIGHT")) 
		    {
		    	swiftBot.move(100, 0, 1000);
		        rotated = true;
		        System.out.println(CYAN + "Rotating 30 degrees right..." + RESET);
		    }

		    if (rotated) 
		    {
		        int obstacleAhead = CheckObstacle();
		    	
		    	if (obstacleAhead == 2) 
		        {
		            System.out.println(RED + "Obstacle detected after rotation." + RESET);
		            Wander();
		            return; 
		        }
		        else if (obstacleAhead == 3)
		        {
		        	return;
		        }
		        
		        Thread.sleep(500);
		    }

		    // Move forward for 1 second
		    String distance = "";
		    
		    switch (speed) 
		    {
			case 40:
				distance = "19";
				break;
			case 50:
				distance = "24";
				break;
			case 100:
				distance = "30";
				break;
		    }
				
		    movementHistory.add(currentDirection + " " + distance + " cm");
		    distanceTravelled += Integer.parseInt(distance);
		    
		    swiftBot.move(speed, speed, 1000);
            swiftBot.disableUnderlights();
            
            Thread.sleep(250);
            CaptureImage();
	    }
	    catch (Exception e) 
	    {
	        e.printStackTrace();
	        System.out.println(RED + "ERROR: Failed during Move()" + RESET);
	    }    
	}
	
	public static void Wander() throws InterruptedException 
	{
		Thread.sleep(delay);
		
		// Choose a random direction: LEFT, or RIGHT
	    String[] directions = {"LEFT", "RIGHT"};
	    int randomIndex = (int)(Math.random() * directions.length);
	    currentDirection = directions[randomIndex];

	    System.out.println(YELLOW + BOLD + "\nWandering... chosen direction: " + currentDirection + RESET);

	    Move();
	}

	public static void End() 
	{
	    try 
	    {
	        Scanner reader = new Scanner(System.in);
        
	        System.out.println(CYAN + BOLD + "\nEnter 'TERMINATE' to safely end the program:");
	        
	        while (!terminated)
	        {
		        String input = reader.next().toLowerCase();
		        
		        if (input.equals("terminate")) 
		        {
		        	terminated = true;
		        }
		        else
		        {
		        	System.out.println(RED + "ERROR: Invalid input. Please try again." + RESET);
		        }
	        }

	        System.out.println(GREEN + "Terminating program and writing log..." + RESET);

	        // Build log 
	        StringBuilder log = new StringBuilder();
	        log.append("SWIFTBOT LIGHT SEEKER LOG\n");
	        log.append("=========================\n");
	        log.append("Threshold Light Intensity: ").append(thresholdIntensity).append("\n");
	        log.append("Brightest Light Source Detected: ").append(highestIntensity).append("\n");
	        
	        log.append("Number of Light Detections: ").append(lightIntensityHistory.size() / 3).append("\n");
	        log.append("\nLight Intensities Recorded:\n");

	        int count = 1;
	        
	        for (int i = 0; i < lightIntensityHistory.size(); i += 3) 
	        {
	            log.append(count).append(" :\n");
	            
	            double left = lightIntensityHistory.get(i);
	            double center = (i + 1 < lightIntensityHistory.size()) ? lightIntensityHistory.get(i + 1) : 0;
	            double right = (i + 2 < lightIntensityHistory.size()) ? lightIntensityHistory.get(i + 2) : 0;

	            log.append("  Column 1 (Left)   : ").append(String.format("%.1f", left)).append("\n");
	            log.append("  Column 2 (Center) : ").append(String.format("%.1f", center)).append("\n");
	            log.append("  Column 3 (Right)  : ").append(String.format("%.1f", right)).append("\n\n");
	            
	            count++;
	        }
	               
	        log.append("Execution Duration : ").append((System.currentTimeMillis() - startTime) / 1000.0).append(" seconds").append("\n");
	        log.append("Total Distance Travelled : ").append(distanceTravelled).append(" cm").append("\n");
	        
	        log.append("\nMovement History:\n");
	        for (String move : movementHistory) 
	        {
	            log.append(" - ").append(move).append("\n");
	        }
	        
	        log.append("\nNumber of Objects Detected: ").append(detectedObjects).append("\n");
	        
	        log.append("Images saved at : /data/home/pi/").append("\n");
	        log.append("Log saved at: /data/home/pi/");   
	        
	        FileWriter writer = new FileWriter("/data/home/pi/SwiftBot_Log.txt");
	        
	        writer.write(log.toString());
	        writer.close();

	        System.out.println(GREEN + "Log successfully written. Program terminated." + RESET);

	        swiftBot.move(0, 0, 100);
	        swiftBot.disableUnderlights();

	        reader.close();
	        System.exit(0);
	    } 
	    catch (IOException e) 
	    {
	        e.printStackTrace();
	        System.out.println(RED + "ERROR: Failed to write log." + RESET);
	    }
	}
}
