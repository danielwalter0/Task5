package src.main.java.com.labs.Daniel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SecretCode {
    private Colour[] secretCode;

    public SecretCode(int codeLength) {
        generateSecretCode(codeLength);
    }

    private void generateSecretCode(int codeLength){
        secretCode = new Colour[codeLength];
        //Storing all 6 possible colours as an array
        Colour[] allColours = Colour.values();
        //Converting them to list because it is a Collection
        List<Colour> listOfAllColours = Arrays.asList(allColours);
        //Performing shuffle function on a collection, it puts the values on random positions
        Collections.shuffle(listOfAllColours);
        //populating the secret code array with the required amount of elements from the shuffled list
        for(int i = 0; i < codeLength; i++){
            secretCode[i] = listOfAllColours.get(i);
        }

    }

    public Colour[] getSecretCode() {
        return secretCode;
    }

    public String toString(){
        return Arrays.toString(secretCode);
    }

}
