
package gr.athenainnovation.imis.OSMRec;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import static java.lang.Double.parseDouble;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * This class provides functionality for recommending classes to new osm instances of the provided OSM file
 * This recommendation is based on the SVM training algorithm.
 * 
 * @author imis-nkarag
 */

public class SVMRecommender {    
    
    private static final String SEP = System.lineSeparator();
    
    public void recommend(String recommendationsFile, File svmPredictionsOutputFile, File testFile, Map<String,Integer> mappingsWithIDs, List<OSMWay> wayList, String path){
        
        Map<Integer,String> reverseMappings = new HashMap();
        
        for(Map.Entry<String, Integer> map : mappingsWithIDs.entrySet()){
            reverseMappings.put(map.getValue(), map.getKey());
        }
        
        int instance = 0;
        try {
            Scanner predictionsInput = new Scanner(svmPredictionsOutputFile);
            new File(path + "/classes/output/" + recommendationsFile).delete(); //delete recommendations file if exists from previous execution
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path + "/classes/output/" + recommendationsFile))) {
                System.out.print("computing recommendations..");
                
                while(predictionsInput.hasNextLine()){
                    String predictionsNextLine = predictionsInput.nextLine();
                    Map<Double, Integer> classesWithIDs = new HashMap<>();
                    String[] splitContent = predictionsNextLine.split("\\s+");
                    for (int j=1; j<splitContent.length; j++){
                        
                        if(!splitContent[j].equals("0.000000")){
                            
                            double value = parseDouble(splitContent[j]); //e.g. value -> 103.142612  Id-> 1210
                            classesWithIDs.put(value,j);
                            
                        }
                    }                 
                    
                    TreeMap<Double, Integer> sortedMapOfClassValues = new TreeMap<>();
                    sortedMapOfClassValues.putAll(classesWithIDs);
                    
                    int k =0;
                    bufferedWriter.write(SEP +"Node ID: " + wayList.get(instance).getID());
                    bufferedWriter.newLine();
                    for (Double key : sortedMapOfClassValues.descendingKeySet()){
                        
                        if(k==5){break;}
                        bufferedWriter.write(reverseMappings.get(sortedMapOfClassValues.get(key)));        
                        bufferedWriter.newLine();
                        
                        k++;
                    }
                    instance++;
                }
            }
        System.out.println("Recommendations computed! Check the output file.");
        }
        catch(IOException ex ){
            System.out.println("Something went wrong computing the recommendations.. check the input file parameters" + ex);
        } 

    }
}
