
package gr.athenainnovation.imis.OSMRec;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import gr.athenainnovation.imis.utils.SimilarityComputingUtils;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * KNN algorithm between trained instances and new instances.
 * 
 * @author imis-nkarag
 */

public class KNN {
    
    private final List<OSMWay> wayList;
    private final Map<String, Integer> mappingsWithIDs;
    private final ArrayList<OSMWay> trainedList;
    private int unclassifiedInstances;
    
    public KNN(List<OSMWay> wayList, Map<String,Integer> mappingsWithIDs, ArrayList<OSMWay> trainedList) {
       this.wayList = wayList;
       this.trainedList = trainedList;
       this.mappingsWithIDs = mappingsWithIDs; 
       unclassifiedInstances = 0;
    }    
    
    public void recommendClasses(){
        
        int scoresCos = 0;
        int scoresEu = 0;
        int instances = 0;
        
        Map<Integer,String> reverseMappings = new HashMap();
        for(Map.Entry<String, Integer> map : mappingsWithIDs.entrySet()){
            reverseMappings.put(map.getValue(), map.getKey());
        }

        System.out.println("computing recommendations...");
        for (OSMWay node : wayList){
            int correctInstanceCos = 0;
            int correctInstanceEu = 0;
            int arraySize = trainedList.size();
            TreeMap<Integer, Double> testVector = node.getIndexVector();
            
            Double[] cosineSimilarities = new Double[arraySize];
            Double[] euclidianDistances = new Double[arraySize];
            Double[] areaDis = new Double[arraySize];
            Double[] pointsDis = new Double[arraySize];
                     
            double testArea = node.getGeometry().getArea();
            if(testArea > 1000000){ //setting a max area for big areas to avoid very small values in similarity scores
                testArea = 1000000;
            }            
            int testPoints = node.getNumberOfNodes();            
            int k = 0;
            
            for(OSMWay trainedInstance : trainedList){
                TreeMap<Integer, Double> trainedVector = trainedInstance.getIndexVector();
                
                int trainedPoints = trainedInstance.getNumberOfNodes();               
                double trainedArea = trainedInstance.getGeometry().getArea(); 
                
                if(trainedArea > 1000000){
                    trainedArea = 1000000;
                }                             
                                               
                euclidianDistances[k] = SimilarityComputingUtils.euclidianDistance(testVector, trainedVector);                 
                cosineSimilarities[k] = SimilarityComputingUtils.cosineSimilarity(testVector, trainedVector); 
                areaDis[k] = abs(trainedArea - testArea)/(getMaxArea(trainedArea, testArea));
                pointsDis[k] = (abs(trainedPoints - testPoints)/((double)getMaxPoints(trainedPoints, testPoints)));

            k++;    
            }
            double maxEuclidian = Collections.max(Arrays.asList(euclidianDistances));
            Double[] totalEuclidianSimilarities = new Double[arraySize];
            Double[] totalCosineSimilarities = new Double[arraySize];
            
            int i=0;
            Map<OSMWay, Double> mapDistances = new HashMap<>();
            Map<OSMWay, Double> mapSimilarities = new HashMap<>();
            
            for(OSMWay trainedInstance : trainedList){  
                //String trainedID = trainedInstance.getID();
                double totalEuclidianSim = (1- euclidianDistances[i]/maxEuclidian) + 2*(1- areaDis[i]) + 2*(1-pointsDis[i]); //the bigger the better!
                totalEuclidianSimilarities[i] = totalEuclidianSim;
                double totalCosineSim = (cosineSimilarities[i]) + 2*(1-areaDis[i]) + 2*(1-pointsDis[i]); //the bigger the better also     
                totalCosineSimilarities[i] = totalCosineSim;
                mapDistances.put(trainedInstance, totalEuclidianSim);
                mapSimilarities.put(trainedInstance, totalCosineSim);
            i++;    
            }
            Collections.sort(Arrays.asList(totalEuclidianSimilarities), Collections.reverseOrder());
            Collections.sort(Arrays.asList(totalCosineSimilarities), Collections.reverseOrder());

            Set<Integer> actualClassList = node.getClassIDs();          

            //cosine             
            Set<Integer> totalClassesFromTrain = new HashSet();
            Set<String> recommendedInstancesCosine = new HashSet();
            i = 0;
            while(i<10){
            Set<Integer> classesFromTrainCos;    
                for(Map.Entry<OSMWay, Double> similarity : mapSimilarities.entrySet()){ 
                    Double similarityValue = similarity.getValue();
                    OSMWay tempNode = similarity.getKey();

                    if(similarityValue.equals(totalCosineSimilarities[i])){ //there is a chance that there are more than one instance with same cosine
                        classesFromTrainCos = tempNode.getClassIDs();//classes from train instance
                        recommendedInstancesCosine.add(tempNode.getID());
                        //actualClassList
                        totalClassesFromTrain.addAll(classesFromTrainCos);//add all trained instances classes to a set. check if this set contains the test inst class
                        break; //found best instance
                    }
                }
            i++;    
            }   

            i = 0;
            Set<Integer> totalClassesFromTrainEuclidian = new HashSet();
            Set<String> recommendedInstancesEuclidian = new HashSet();
            while(i<10){
                Set<Integer> classesFromTrainEu;
                for(Map.Entry<OSMWay, Double> distance : mapDistances.entrySet()){
                    Double  distanceValue = distance.getValue();
                    OSMWay tempNode = distance.getKey();
                    String nodeID = distance.getKey().getID();
                    //check the 10 first similarities
                    if(distanceValue.equals(totalEuclidianSimilarities[i])){ //comparing current euclidian distance 

                        classesFromTrainEu = tempNode.getClassIDs();
                        recommendedInstancesEuclidian.add(nodeID);
                        totalClassesFromTrainEuclidian.addAll(classesFromTrainEu);
                        break; //found best 
                    }
                }
            i++;    
            }
            
            for(Integer classID : actualClassList){
                if(classID.equals(0)){
                    unclassifiedInstances++;
                } 
                else{
                    if(totalClassesFromTrainEuclidian.contains(classID)){
                        correctInstanceEu = 1;
                    }
                    if(totalClassesFromTrain.contains(classID)){
                        correctInstanceCos = 1;
                    }
                }
            }
            scoresEu = scoresEu + correctInstanceEu;
            scoresCos = scoresCos + correctInstanceCos;
            
            instances++;
        }
    System.out.println("cosine similarity:    " + (double)scoresCos/(double)(instances-unclassifiedInstances));  
    System.out.println("euclidian distance: " + (double)scoresEu/(double)(instances-unclassifiedInstances)); 
    System.out.println("total instances: " + instances + " unclassified: " + unclassifiedInstances);
    System.out.println("cosine    correct instances: " + scoresCos);
    System.out.println("euclidian correct instances: " + scoresEu);

    }
    
    private double getMaxArea(double areaA, double areaB){
        double maxArea;
        if(areaA == areaB){
            return 1;
        }
        
        if(areaA > areaB){           
            maxArea = areaA;
        }
        else{    
            maxArea = areaB;
        }
        
        if(maxArea != 0){
           return maxArea; 
        }
        else{
            return 1;
        }       
    }
    
    private int getMaxPoints(int pointsA, int pointsB){
        int maxPoints;
        if(pointsA > pointsB){
            maxPoints = pointsA;
        }
        else{
            maxPoints = pointsB;
        }
        if(maxPoints != 0){
           return maxPoints; 
        }
        else{
           return 1;
        }       
    }    
}

