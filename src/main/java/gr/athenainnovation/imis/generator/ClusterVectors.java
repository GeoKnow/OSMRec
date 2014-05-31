package gr.athenainnovation.imis.generator;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Collections;

/**
 *
 * @author imis-nkarag
 */

public class ClusterVectors {
    private final List<OSMWay> wayList;
    private final String clusterSolution;
    private static final int numberOfFeatures = 3748;
    private ArrayList<Cluster> averageClusterVectors;

    public ClusterVectors(List<OSMWay> wayList, String clusterSolution){
        //Map<Integer, List<
        this.clusterSolution = clusterSolution;
        this.wayList = wayList;
        averageClusterVectors = new ArrayList();
        //initialize cluster vector
        //get cluster output file
    }   
    
    public void produceClusterVectors(){

        //HashMap<Integer, Integer> clusterToInstanceID = new HashMap<>();
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
            System.out.println("Something went wrong, producing the clustering solution. Check your input file");
        }
        
        System.out.println("producing average vectors..");              
        
        for(Entry<Integer, List<Integer>> clusterInstancesEntry : clusterToInstances.entrySet()){
        //iterate and construct average vectors for every cluster
            
            List<Integer> clusterInstances = clusterInstancesEntry.getValue(); //get instances of current vector
            ArrayList<Integer> averageVector = new ArrayList<>(Collections.nCopies(numberOfFeatures, 0)); //initialise average vector
            Cluster averageClusterVector = new Cluster(clusterInstancesEntry.getKey(), clusterInstances);
            
            List<Integer> majorityClasses = new ArrayList();
            for(Integer instanceID : clusterInstances){
            //for every instance of this cluster, get instances's vectors and compute the average cluster vector    
                ArrayList<Integer> instanceVector = wayList.get(instanceID).getVector();
                
                majorityClasses.addAll(wayList.get(instanceID).getClassIDs()); //we add all the classes that the instance belong 

                computeAverageVector(averageVector, instanceVector);
            }
            
            
            HashMap<Integer, Integer> frequencyMap = new HashMap<>();
            
            for (Integer classID : majorityClasses) {
                Integer count = frequencyMap.get(classID);
                frequencyMap.put(classID, (count == null) ? 1 : count + 1); //put classID and a frequency value 
            }

            List<DistinctiveClasses> averageClusterClassesList = new ArrayList<>();
            
            for(Entry<Integer, Integer> classIDWithFrequency : frequencyMap.entrySet()){
                DistinctiveClasses averageClusterClassesComparator = new DistinctiveClasses(classIDWithFrequency.getKey(), classIDWithFrequency.getValue());               
                averageClusterClassesList.add(averageClusterClassesComparator);
            }
            Collections.sort(averageClusterClassesList,new DistinctiveClasses());
            
            averageClusterVector.setSortedClusterClasses(averageClusterClassesList); //these are sorted in descending order based on frequency in the cluster
            averageClusterVector.setVector(averageVector); //average vector
            averageClusterVectors.add(averageClusterVector); //add instance of cluster average vector
            
        }
    }
    
    private void computeAverageVector(ArrayList<Integer> averageVector, ArrayList<Integer> ve){
        
        for(int i=0; i<1421; i++){
            //class features

          if (ve.get(i).equals(1)){
             averageVector.set(i, 1);
          }  
        }
        
        for(int i=1421; i<1425; i++){ 
        //geometry type features. 
        //The average vector features take an integer value of the sum of the present geometry type of each instance  
            
            if (ve.get(i).equals(1)){
                int increment  = averageVector.get(i) + 1;
                averageVector.set(i, increment);
            }
        }
        
        for(int i=1425; i<1465; i++){ 
        //rest of geometry features: rectangle, number of points, area, circle 
        //the features here are treated as boolean values and the averaage vector takes the OR of the instance features  
            
            if (ve.get(i).equals(1)){
                averageVector.set(i, 1);
            }
        }
        
        for(int i=1465; i<3748; i++){ 
        //textual features. The average vector features take an integer value of the sum of the present textual features     
            if (ve.get(i).equals(1)){
                int increment  = averageVector.get(i) + 1;
                averageVector.set(i, increment);
            }
        }
  
    }
    
    public void setAverageClusterVectors(ArrayList<Cluster> averageClusterVectors){
        this.averageClusterVectors = averageClusterVectors;
    }
    
    public ArrayList<Cluster> getAverageClusterVectors(){
        return averageClusterVectors;
    }
    
}
