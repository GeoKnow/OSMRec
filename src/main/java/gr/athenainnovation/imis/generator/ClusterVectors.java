package gr.athenainnovation.imis.generator;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

/**
 *
 * @author imis-nkarag
 */

public class ClusterVectors {
    
    private final List<OSMWay> trainList;
    private final String clusterSolution;
    private ArrayList<Cluster> averageClusterVectors;
    private ArrayList<Cluster> averageClusterIndexVectors;

    public ClusterVectors(List<OSMWay> trainList, String clusterSolution){
        
        this.clusterSolution = clusterSolution;
        this.trainList = trainList;
        averageClusterVectors = new ArrayList();
        averageClusterIndexVectors = new ArrayList<>();
    }   
    
    public void produceClusterVectors(){

        HashMap<Integer, List<Integer>> clusterToInstances = new HashMap<>();
      
        try{
        Scanner input = new Scanner(new File(clusterSolution));
        Integer k = 0;
            while(input.hasNextInt()) {   
                Integer clusterID = input.nextInt();

                if(clusterID!=-1){ //exclude non clustered instances
                    if(clusterToInstances.containsKey(clusterID)){
                        List<Integer> lis = clusterToInstances.get(clusterID);
                        lis.add(k);
                        clusterToInstances.put(clusterID, lis);
                    }
                    else{
                        List<Integer> lis= new ArrayList();
                        lis.add(k);
                        clusterToInstances.put(clusterID, lis);
                    }
                }
                k++;    
            }
        }
        catch(FileNotFoundException e){
            System.out.println("Something went wrong, producing the clustering solution. Check your input file\n\n" + e);
        }
        
        System.out.print("producing average vectors..");              
        
        for(Entry<Integer, List<Integer>> clusterInstancesEntry : clusterToInstances.entrySet()){
        //iterate and construct average vectors for every cluster
            
            List<Integer> clusterInstances = clusterInstancesEntry.getValue(); //get instances of current vector
            TreeMap<Integer,Double> averageIndexVector  = new TreeMap<>();
            
            Cluster averageClusterVector = new Cluster(clusterInstancesEntry.getKey(), clusterInstances);
            
            List<Integer> majorityClasses = new ArrayList();
            for(Integer instanceID : clusterInstances){
            //for every instance of this cluster, get instances's vectors and compute the average cluster vector    
                //ArrayList<Integer> instanceVector = wayList.get(instanceID).getVector();
                TreeMap<Integer,Double> instanceIndexVevtor = trainList.get(instanceID).getIndexVector();
                //we add all the classes that the instance belong 
                majorityClasses.addAll(trainList.get(instanceID).getClassIDs()); 
                computeAverageVector(averageIndexVector, instanceIndexVevtor); 
            }           
            
            HashMap<Integer, Integer> frequencyMap = new HashMap<>();
            
            for (Integer classID : majorityClasses) {
                Integer count = frequencyMap.get(classID);
                frequencyMap.put(classID, (count == null) ? 1 : count + 1); //put classID and a frequency value 
            }

            List<DistinctiveClasses> averageClusterClassesList = new ArrayList<>();
            
            for(Entry<Integer, Integer> classIDWithFrequency : frequencyMap.entrySet()){
                DistinctiveClasses averageClusterClassesComparator = 
                                new DistinctiveClasses(classIDWithFrequency.getKey(), classIDWithFrequency.getValue());               
                averageClusterClassesList.add(averageClusterClassesComparator);
            }
            Collections.sort(averageClusterClassesList, new DistinctiveClasses());
            
            //these are sorted in descending order based on frequency in the cluster
            averageClusterVector.setSortedClusterClasses(averageClusterClassesList); 
            averageClusterVector.setIndexVector(averageIndexVector);
            averageClusterVectors.add(averageClusterVector); //add instance of cluster average vector
            averageClusterIndexVectors.add(averageClusterVector);
        }
        System.out.print(". done.\n");
    }
    
    private void computeAverageVector(TreeMap<Integer,Double> averageIndexVector, TreeMap<Integer, Double> testVector){
        
        for(int i=0; i<1422; i++){
            //class features
            if(testVector.containsKey(i)){  
                averageIndexVector.put(i, 1.0); //OR between average vector and train vector
            }
        }
        
        for(int i=1422; i<1426; i++){        
        //geometry type features. 
        //The average vector features take an integer value of the sum of the present geometry type of each instance              
            if(testVector.containsKey(i) && averageIndexVector.containsKey(i)){
                averageIndexVector.put(i, averageIndexVector.get(i)+1.0);
            }
            else if(testVector.containsKey(i)){
                averageIndexVector.put(i, 1.0);
            }
        }
        
        for(int i=1426; i<1466; i++){ 
        //rest of geometry features: rectangle, number of points, area, circle 
        //the features here are treated as boolean values and the average vector takes the OR of the instance features             
            if(testVector.containsKey(i)){
                averageIndexVector.put(i,1.0);
            }
        }
        
        //the mean and variance values are the mean values of all means and variances of the instance vectors 
        int meanID = 1466;
        /*//double values for mean and variance
        
        
        if(testVector.containsKey(meanID)){
            if(averageIndexVector.containsKey(meanID)){
                averageIndexVector.put(meanID,(testVector.get(meanID) + averageIndexVector.get(meanID))/2);
            }
            else {
                averageIndexVector.put(meanID, testVector.get(meanID));
            }
        }
        
        int varianceID = 1467;
        
        if(testVector.containsKey(varianceID)){
            if(averageIndexVector.containsKey(varianceID)){
                averageIndexVector.put(varianceID,(testVector.get(varianceID) + averageIndexVector.get(varianceID))/2);
            }
            else {
                averageIndexVector.put(varianceID, testVector.get(varianceID));
            }
        }
        */
        
        //boolean mean and variance 23 + 36 = 59
        int meanEnd = meanID+60;
        for(int i = meanID; i<meanEnd; i++){           
            if(testVector.containsKey(i) && averageIndexVector.containsKey(i)){
                averageIndexVector.put(i, averageIndexVector.get(i)+1.0); //if the feature exists in testVector 
                                                                    //the value is added to the average
            }
            else if(testVector.containsKey(i)){
                averageIndexVector.put(i, 1.0); //first occurance of this feature. Put initial value in average
            }
        }
        //System.out.println("mean start " + meanID + " mean end " + (meanID+59));
        
        //rel
        int relEnd = meanEnd+6;
        //System.out.println("rel start " + relStart + " rel end " + (relEnd-1));
        for(int i = meanEnd; i<relEnd; i++){//boolean var and mean
            if(testVector.containsKey(i) && averageIndexVector.containsKey(i)){
                averageIndexVector.put(i, averageIndexVector.get(i)+1.0);
            }
            else if(testVector.containsKey(i)){
                averageIndexVector.put(i, 1.0); //initialize relation feature in average vector
            }
        }
        
        //textual features
        int texStart = relEnd;
        int texEnd = texStart + 2280;
        //System.out.println("tex start " + texStart + " tex end " + texEnd);
        for(int i=texStart; i<texEnd; i++){ //boolean var and mean
        //textual features. The average vector features take an integer value of the sum of the present textual features  
            if(testVector.containsKey(i) && averageIndexVector.containsKey(i)){
                averageIndexVector.put(i, averageIndexVector.get(i)+1.0);
            }            
            else if (testVector.containsKey(i)){
                averageIndexVector.put(i, 1.0); //initialize average vector feature
            }
        }
        
        //relations
        /*for(int i=1468; i<1473; i++){ //double var and mean
            if(testVector.containsKey(i)){
                averageIndexVector.put(i, 1.0);
            }
        }
        
        for(int i=1473; i<3752; i++){ //double var and mean
        //textual features. The average vector features take an integer value of the sum of the present textual features  
            if(testVector.containsKey(i)){
                averageIndexVector.put(i, testVector.get(i)+1.0);
            }
            //else{
            //    averageIndexVector.put(i,1.0);
            //}
        }*/
//        System.out.println("avg vec size: " + averageIndexVector.size());
//        for(Entry<Integer, Double> la : averageIndexVector.entrySet()){
//            System.out.println(la);
//        }
    }
    
//    public void setAverageClusterVectors(ArrayList<Cluster> averageClusterVectors){
//        this.averageClusterVectors = averageClusterVectors;
//    }
    
    public void setIndexAverageClusterVectors(ArrayList<Cluster> averageIndexClusterVectors){
        this.averageClusterIndexVectors = averageIndexClusterVectors;
    }
    
//    public ArrayList<Cluster> getAverageClusterVectors(){
//        return averageClusterVectors;
//    }    
    
    public ArrayList<Cluster> getIndexAverageClusterVectors(){
        return averageClusterIndexVectors;
    }
}
