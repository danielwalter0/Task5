package src.main.java.com.labs.Daniel;

//enum is a data type where the values it can take are specified beforehand,
//like so, the colour can only be the specified 6 colours
public enum Colour {
    RED('R', 138, 8, 29),
    GREEN('G', 19, 102, 72),
    BLUE('B',0,87,138),
    YELLOW('Y',150,134,6),
    ORANGE('O',166,59,9),
    PINK('P',99,64,114);

    private char colourLetter;
    private int red;
    private int green;
    private int blue;

    //constructor
    Colour(char colourLetter, int red, int green, int blue) {
        this.colourLetter = colourLetter;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public char getColourLetter() {
        return colourLetter;
    }
    public int getRed() {
        return red;
    }
    public int getGreen() {
        return green;
    }
    public int getBlue() {
        return blue;
    }

    public static Colour fromChar(char input) {
        for (Colour colour : Colour.values()) {
            if (colour.getColourLetter() == input) {
                return colour;
            }
        }
        throw new IllegalArgumentException("Invalid colour letter: " + input);
    }

}
