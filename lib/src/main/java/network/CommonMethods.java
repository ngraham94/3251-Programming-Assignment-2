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

    private ArrayList<String> spamWordDictionary, foundSpamWords;//--------spamWordDictionary holds known spam words. found spam words holds words found in text
    private String fullText = "";
    private float spamScore;

    public CommonMethods() {//-------Initializes dictionary and found words list.
        spamWordDictionary = new ArrayList<>();
        foundSpamWords = new ArrayList<>();
    }

    public void computeSpamScore(String[] message ) {//-------Computes spam score as #spamWords/#wordsInMessage
        float spamCount = 0;
        float length = message.length;
        float result;
        for(String word : message) {
            if (spamWordDictionary.contains(word)) {
                spamCount++;
                if (!foundSpamWords.contains(word)) {
                    foundSpamWords.add(word);
                }
            }
        }
        this.spamScore = spamCount/length;
    }

    public int getDictionarySize() {
        return spamWordDictionary.size();
    }

    public int readWordFile(String spamWordTxt) {//-------Reads spam word file and adds words to the dictionary
        int successFlag = 0;
        FileReader fileReader;

        try (BufferedReader bufferReader = new BufferedReader(new FileReader(spamWordTxt))){
            String word = bufferReader.readLine();
            while ((word != null) && (word.length() > 0)) {
                spamWordDictionary.add(word);
                word = bufferReader.readLine();
            }
            successFlag = 1;
        } catch (FileNotFoundException e) {//-------Invalid spam word file
            System.out.println("File " + spamWordTxt + " not found!");
            successFlag = -1;
        } catch (IOException e) {//-------Could not read supplied spam word file
            System.out.println("Could not read file " + spamWordTxt);
            successFlag = -1;
        }
        return successFlag;
    }

    public int readTextMessage(String txtMsgFile) {//-------Read the text message file and save it to local variable
        int successFlag = 0;

        try (BufferedReader bufferReader = new BufferedReader(new FileReader(txtMsgFile))){
            String message = bufferReader.readLine();
            while ((message != null) && (message.length() > 0)) {
                this.fullText += message +"\n";
                message = bufferReader.readLine();
            }
            this.fullText += "\n";
            successFlag = 1;
        } catch (FileNotFoundException e) {//--------If file doesn't exist, return
            System.out.println("Text message file " + txtMsgFile + " not found!");
            successFlag = -1;
        } catch (IOException e) {//-------If file could not be read, return
            System.out.println("Could not read text message file " + txtMsgFile);
            successFlag = -1;
        }
        return successFlag;
    }

    public String generateReply(int successFlag) {//------Generate server reply with spam score and found words
        String reply = "";
        String foundWords = "";
        if (successFlag != 1) {
            reply = "0 -1 ERROR";
        } else {
            for (String wordFound: foundSpamWords) {
                foundWords += wordFound += " ";
            }
            reply += spamWordDictionary.size() + " " + spamScore +
                    " " + foundWords;
        }
        return reply + "\n";
    }

    public String processMessage(String message) {//-------Server checks message for length requirements and computes spam score.
        String[] wordsInMessage = message.split("\\W+");
        if (message.length() > 1000 || message.length() < 1) {
            return generateReply(-1);
        }
        computeSpamScore(wordsInMessage);
        return generateReply(1);
    }

    public String getFullText(){return this.fullText;}//-------Returns the text message for debugging

    public int getStringLength(String message) {
        return message.length();
    }//-------Returns the number of characters in the text for debugging

    public float getSpamScore() {
        return this.spamScore;
    }//-------Returns the spam score
}
