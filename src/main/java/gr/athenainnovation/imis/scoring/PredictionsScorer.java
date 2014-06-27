package gr.athenainnovation.imis.scoring;

import static java.lang.Double.parseDouble;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Class containing the method for the SVM process evaluation.
 * 
 * @author imis-nkarag
 */

public class PredictionsScorer {

    public void computeScore(File predictionsFile, File testFile, Map<String,Integer> mappings){
        int correct = 0;
        try {
            Scanner predictionsInput = new Scanner(predictionsFile);            
            Scanner vectorInput = new Scanner(testFile);
            int scores = 0;
            int loops =0;
            System.out.print("computing score..");
            
            while(predictionsInput.hasNextLine() && vectorInput.hasNextLine()) {
                
                int score = 0;    
                Map<Double, Integer> classesWithIDs = new HashMap<>();               
                String predictionsNextLine = predictionsInput.nextLine();
                String testNextLine = vectorInput.nextLine();
                String testClasses[] = testNextLine.split("#",2);
                
                testClasses[1] = testClasses[1].replaceAll("[\\[,\\]]", "");
                String[] stringIDs = testClasses[1].trim().split("\\s+");

                Integer[] integerIDs = new Integer[stringIDs.length]; 
                List<Integer> classesFromDelimiter = new ArrayList();
                
                for(int i = 0;i < (stringIDs.length-1);i++){

                    integerIDs[i] = Integer.parseInt(stringIDs[i]); //this array contains the IDs of every class of each instance

                    classesFromDelimiter.add(integerIDs[i]);
                }
                String[] splitContent = predictionsNextLine.split("\\s+");
                
                for (int j=1; j<splitContent.length; j++){

                    if(!splitContent[j].equals("0.000000")){

                        double value = parseDouble(splitContent[j]); //e.g. value -> 103.142612  Id-> 1210
                        classesWithIDs.put(value,j);

                    }
                }
  
                TreeMap<Double, Integer> sortedMapOfClassValues = new TreeMap<>();
                sortedMapOfClassValues.putAll(classesWithIDs);                

                int k = 0;
                for (Double key : sortedMapOfClassValues.descendingKeySet()){
                    
                    if(k==4){break;} 
            
                    if(classesFromDelimiter.contains(sortedMapOfClassValues.get(key))){

                        score = 1;
                        correct++;

                    }
                k++;                    
                }
            scores = scores + score;    
            loops++;    
            }//end of while
            
            float result = 100-((float)scores*100/(float)loops);
            System.out.print(". done.\n");
            System.out.println("number of instances: " + loops + " and score (error): " + result + " correct: " + correct );           
        } 
        catch (FileNotFoundException ex) {
            System.err.println("something went wrong constructing the vectors file.. check your input file parameter");
            //Logger.getLogger(PredictionsScorer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }      
}
