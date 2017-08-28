import javax.swing.*;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * Created by neil on 7/28/17.
 */
public class scrabble {

    public static String rack;                                          // represents the players rack
    public String[] board = new String[13];                             // represents scrabble board
    public Set<String> dictionary = new HashSet<>();                    // scrabble dictionary.  Hashset for O(1) lookup time
    public ArrayList<word> wordsGenerated = new ArrayList();            // record of words generated from rack
    public ArrayList<word> fivePlusWords = new ArrayList<>();           // used to keep track of words than can reach a 2x square
    public Hashtable<String, Integer> tileValues = new Hashtable<>();   // stores (tile --> value) pairs
    public int numberOfBlanks;
    public String[] alphabet = {"A", "B", "C", "D", "E", "F", "G", "H"
            , "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V"
            , "W", "X", "Y", "Z"};

    public static void main(String[] args) {

        scrabble myScrabble = new scrabble();
        myScrabble.new JFileChooser();
        final long startTime = System.currentTimeMillis();
        myScrabble.importDicionary();
        myScrabble.initTileValues();

        if (myScrabble.numberOfBlanks == 1) {
            for (String letter : myScrabble.alphabet) {

                String wildcardRack = rack.replace("-", letter);
                myScrabble.generateWords("", wildcardRack);
            }
        } else if (myScrabble.numberOfBlanks == 2) {

            for (String letter : myScrabble.alphabet) {

                String wildcardRack = rack.replace("-", letter);

                for (String otherLetter : myScrabble.alphabet) {
                    wildcardRack = rack.replace("-", otherLetter);
                    myScrabble.generateWords("", wildcardRack);
                }
            }
        } else {
            myScrabble.generateWords("", rack);
        }

        if (myScrabble.wordsGenerated == null) {
            myScrabble.exchangeTiles();
        }
        myScrabble.wordPlacement();

        final long duration = System.currentTimeMillis();
        NumberFormat formatter = new DecimalFormat("#0.00000");
        System.out.print("Execution time is " + formatter.format((duration - startTime) / 1000d) + " seconds");
    }

    public void printWords() {
        System.out.println(wordsGenerated.toString());
    }


    /***************************************************************
     *  Finds the highest scoring placement based on generated words
     ***************************************************************/
    public void wordPlacement() {

        boolean doesFivePlusExist = fivePlusHeuristic();

        if (doesFivePlusExist == false) {                // if no 5+ word, placement doesn't matter since no word can reach to double letter score
            // find highest base score word, and place it at square 5
            word max = null;
            for (word word : wordsGenerated) {
                if (word.score > max.score) {
                    max = word;
                }
            }

            System.out.println("PLACE WORD <" + max.word + "><5><" + max.score + ">");

        } else {

            ArrayList<word> placementList = new ArrayList<>();

            for (word word : fivePlusWords) {

                for (int j = 0; j < word.getLength(); j++) {                       // place each word a # of times equal to its length.  Testing different placements

                    for (int i = 0; i < word.getLength(); i++) {
                        board[i + j] = Character.toString(word.word.charAt(i));    // place each word on the board letter by letter
                    }

                    word tempWord;

                    if (board[2] != null) {
                        tempWord = word;
                        tempWord.score += tileValues.get(board[2].toLowerCase());
                        tempWord.startingSquare = j;
                        if (board[6] != null) {
                            placementList.add(tempWord);
                        }
                    } else if (board[10] != null) {
                        tempWord = word;
                        tempWord.score += tileValues.get(board[10].toLowerCase());
                        tempWord.startingSquare = j;
                        if (board[6] != null) {
                            placementList.add(tempWord);
                        }
                    }


                    for (int i = 0; i < board.length; i++) {
                        board[i] = null;
                    }                                                 // wipe board clean between each word placement
                }
            }

            word max = placementList.get(0);
            for (word word : placementList) {                                      // find the highest scoring word based on all possible placements
                if (word.score > max.score) {
                    max = word;
                }
            }

            System.out.println("The board is represented in an array as follows");
            System.out.println("[0] [1] [2 DOUBLELETTER] [3] [4] [5] [6 DOUBLEWORD] [7] [8] [9] [10 DOUBLE WORD] [11] [12] [13]");
            System.out.println();
            System.out.println("PLACE WORD<" + max.word + ">    STARTING SQUARE<" + max.startingSquare + ">    POINTS<" + (max.score * 2) + ">");
        }
    }


    /****************************************************************************************
     *  If there are no words to be generated, exchange 5 tiles with least value for new ones
     ****************************************************************************************/
    public void exchangeTiles() {

        ArrayList<word> leastValuable = new ArrayList<>();

        word highest = null;
        highest.score = 0;
        word secondHighest = null;
        secondHighest.score = 0;
        for (int i = 0; i < 7; i++) {
            if (tileValues.get(rack.charAt(i)) > highest.score) {
                secondHighest.score = highest.score;
                secondHighest.word = highest.word;
                highest.score = tileValues.get(rack.charAt(i));
                highest.word = Character.toString(rack.charAt(i));
            }
        }

        System.out.println("<TILES> Player will keep: " + highest.word + " " + secondHighest.word + " and exchange the rest");
    }


    /**********************************************************************************************
     *  If 5+ word exists, place and score.  Throw out all words of length < 5 and score < 5+ score
     **********************************************************************************************/
    public boolean fivePlusHeuristic() {

        boolean fivePlusExists = false;

        for (word word : wordsGenerated) {

            if (word.word.length() > 4) {            // if any 5+ words exist
                fivePlusExists = true;
                break;
            }
        }

        if (fivePlusExists == true) {
            for (word word : wordsGenerated) {
                if (word.getLength() > 4) {
                    fivePlusWords.add(word);
                }
            }

            for (word word : fivePlusWords) {
                if (word.getLength() == 7) {
                    word.score += 50;
                }
            }

            return true;


            // if 5+ words exist, you can throw out a lot of the short and low score words.  Greatly reducing the search space
            // if there aren't 5+ words, you can score immediately using base score generated earlier, placing the word anywhere on the board

        } else return false;
    }


    /**********************************************************
     *  Generates all possible words from a given rack of tiles
     **********************************************************/
    public void generateWords(String permutation, String word) {

        if (isWordValid(permutation) == true) {                       // if valid word, generateBaseScore() & create / store word object
            int wordScore = generateBaseScore(permutation);
            word myWord = new word(wordScore, permutation);
            wordsGenerated.add(myWord);
        }

        for (int i = 0; i < word.length(); i++) {

            generateWords(permutation + word.charAt(i), word.substring(0, i) + word.substring(i + 1, word.length()));
        }
    }


    /******************************************************
     *  Generates base score value for each valid word generated
     ******************************************************/
    public int generateBaseScore(String word) {

        int wordScore = 0;

        for (int i = 0; i < word.length(); i++) {
            String myLetter = Character.toString(word.charAt(i));
            myLetter = myLetter.toLowerCase();
            wordScore += tileValues.get(myLetter);
        }

        return wordScore;
    }


    /***************************************************
     *  Check if the word generated is in the dictionary
     ***************************************************/
    public boolean isWordValid(String word) {

        String theWord = new String(word);

        if (dictionary.contains(theWord)) {
            return true;
        } else {
            return false;
        }
    }


    /**************************************************************
     *  Reads in the dictionary file and places words in a hash set
     **************************************************************/
    public void importDicionary() {

        File dictionaryFile = new File("/home/neil/IdeaProjects/Scrabble/Dictionary");   //CHANGE AS NECESSARY TO FIT YOUR FILESTRUCTURE

        try {

            InputStream in_stream = new FileInputStream(dictionaryFile);
            InputStreamReader in_stream_reader = new InputStreamReader(in_stream, Charset.defaultCharset());
            BufferedReader buffered_reader = new BufferedReader(in_stream_reader);
            String line;

            while ((line = buffered_reader.readLine()) != null) {
                if (line.length() <= 7) {
                    dictionary.add(line);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /******************************
     *  Loads rack[] from text file
     ******************************/
    public void loadRack(File file) {

        File myFile = file;

        try {

            InputStream in_stream = new FileInputStream(myFile);
            InputStreamReader in_stream_reader = new InputStreamReader(in_stream, Charset.defaultCharset());
            BufferedReader buffered_reader = new BufferedReader(in_stream_reader);
            String line;

            while ((line = buffered_reader.readLine()) != null) {
                line.toUpperCase();
                rack = line;
                System.out.println("RACK: " + line);
                System.out.println();

                for (int i = 0; i < line.length(); i++) {
                    System.out.println(Character.toString(rack.charAt(i)));
                    if ((rack.charAt(i)) == '-') {
                        numberOfBlanks++;
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /***************************************************************
     *  Initializes tileValues hashtable with (tile --> value) pairs
     ***************************************************************/
    public void initTileValues() {
        tileValues.put("a", 1);
        tileValues.put("b", 3);
        tileValues.put("c", 3);
        tileValues.put("d", 2);
        tileValues.put("e", 1);
        tileValues.put("f", 4);
        tileValues.put("g", 2);
        tileValues.put("h", 4);
        tileValues.put("i", 1);
        tileValues.put("j", 8);
        tileValues.put("k", 5);
        tileValues.put("l", 1);
        tileValues.put("m", 3);
        tileValues.put("n", 1);
        tileValues.put("o", 1);
        tileValues.put("p", 3);
        tileValues.put("q", 10);
        tileValues.put("r", 1);
        tileValues.put("s", 1);
        tileValues.put("t", 1);
        tileValues.put("u", 1);
        tileValues.put("v", 4);
        tileValues.put("w", 4);
        tileValues.put("x", 8);
        tileValues.put("y", 4);
        tileValues.put("z", 10);
        tileValues.put("-", 0);
    }

    /*******************************************************************
     *  Allows user to open a selected file for importing racks of tiles
     *******************************************************************/
    public class JFileChooser {

        public File selectedFile;

        public JFileChooser() {

            javax.swing.JFileChooser jFileChooser = new javax.swing.JFileChooser();

            int result = jFileChooser.showOpenDialog(new JFrame());

            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                File selectedFile = jFileChooser.getSelectedFile();
                loadRack(selectedFile);
            }
        }

        public File getFile() {
            return selectedFile;
        }
    }

    /**********************************
     *  Class for creating word objects
     **********************************/
    class word {

        int score;
        String word;
        int startingSquare;

        public word(int score, String word) {
            this.score = score;
            this.word = word;
        }

        public int getLength() {
            return word.length();
        }

    }
}


