package network;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;



/**
 * Created by Nick on 2/8/2017.
 */
public class CommonMethods {
    private String fullText = "";

    public CommonMethods() {//-------Initializes dictionary and found words list.

    }

    public int readTextFile(String messageFile) {//-------Read the text message file and save it to local variable
        int successFlag = 0;

        try (BufferedReader bufferReader = new BufferedReader(new FileReader(messageFile))){
            String message = bufferReader.readLine();
            while ((message != null) && (message.length() > 0)) {
                this.fullText += message +"\n";
                message = bufferReader.readLine();
            }
            this.fullText += "\n";
            successFlag = 1;
        } catch (FileNotFoundException e) {                                                                             //--------If file doesn't exist, return
            System.out.println("Text message file " + messageFile + " not found!");
            successFlag = -1;
        } catch (IOException e) {                                                                                       //-------If file could not be read, return
            System.out.println("Could not read text message file " + messageFile);
            successFlag = -1;
        }
        return successFlag;
    }

    public String processMessage(String message) {                                                                      //-------Server checks message for length requirements and transforms message to uppercase.
        String result = message.toUpperCase();
        return result;
    }

    public String getFullText(){return this.fullText;}                                                                  //-------Returns the message for debugging

    public int getStringLength(String message) {
        return message.length();
    }                                                                                                                   //-------Returns the number of characters in the text for debugging

}
