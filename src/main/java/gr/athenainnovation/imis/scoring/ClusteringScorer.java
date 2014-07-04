package gr.athenainnovation.imis.scoring;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import gr.athenainnovation.imis.generator.Cluster;
import gr.athenainnovation.imis.generator.DistinctiveClasses;
import gr.athenainnovation.imis.utils.SimilarityComputingUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Scoring the clustering process
 * The scoring mechanism executes when no k parameter is defined and scores the results from several values of k.
 * 
 * @author imis-nkarag
 */

public class ClusteringScorer {
    
    private final List<OSMWay> wayList;
    private final ArrayList<Cluster> averageVectors;
    private final Map<String, Integer> mappingsWithIDs; 
    private float score = 100f;

    public ClusteringScorer(List<OSMWay> wayList, ArrayList<Cluster> averageVectors, Map<String,Integer> mappingsWithIDs) {
        
       this.wayList = wayList;
       this.averageVectors = averageVectors;
       this.mappingsWithIDs = mappingsWithIDs;              
    }    
    
    public void score(){
        
        int scores = 0;
        int instance = 0;
            Map<Integer,String> reverseMappings = new HashMap();

            for(Map.Entry<String, Integer> map : mappingsWithIDs.entrySet()){
                reverseMappings.put(map.getValue(), map.getKey());
            }
            System.out.print("Scoring clustering process...");
            for (OSMWay node : wayList){
                
                int tempScore = 0;
                //ArrayList<Integer> nodeVector = node.getVector();
                
                TreeMap<Integer,Double> nodeVector = node.getIndexVector();
                ArrayList<Double> similarities = new ArrayList();
                Map<Cluster, Double> clusterSimilarities = new HashMap<>();

                for(Cluster averageClusterVector : averageVectors){

                    //ArrayList<Integer> averageVector = averageClusterVector.getClusterVector(); 
                    TreeMap<Integer,Double> averageIndexVector = averageClusterVector.getClusterIndexVector();
                    //Double similarity = cosineSimilarity(nodeVector, averageVector);
                    Double similarity = SimilarityComputingUtils.cosineSimilarity(nodeVector, averageIndexVector);
                    similarities.add(similarity);

                    //this map contains all average cluster vectors with the corresponding similarities with current instance                    
                    //cluster vector and similarity of the cluster vector and current instance vector
                    clusterSimilarities.put(averageClusterVector, similarity); 
                    Collections.sort(similarities, Collections.reverseOrder());
                }

                Cluster bestClusterForInstance = null;
                for(Map.Entry<Cluster, Double> clusterSimilarity : clusterSimilarities.entrySet()){ 
                    
                      if(clusterSimilarity.getValue().equals(similarities.get(0))){ 
                          //there is a chance that there are more than one cluster with same 
                          //similarity. In this case we might get a slightly different score by picking the first one.                                                             
                          bestClusterForInstance = clusterSimilarity.getKey();
                          break; //found best cluster
                      } 
                }

                List<DistinctiveClasses> computedClasses = bestClusterForInstance.getSortedClusterClasses();
                Set<Integer> actualClassList = node.getClassIDs();
                actualClassList.add(node.getClassID());
                
                int i =0;               
                for(DistinctiveClasses computedClass : computedClasses){            
                    if(i==1){break;} 

                    if (actualClassList.contains(computedClass.getClassID())){      
                        tempScore = 1;
                    }
                i++;    
                }
                scores = scores + tempScore;
                instance++;  
            }

            score = 100 - ((float)scores*100/ (float)instance);
            setScore(score);
            System.out.println("total score (error): "+ score); // + " loops" + instance + " scores " + scores);
    }    
    
    private void setScore(float score){
        this.score = score;
    }
    
    public float getScore(){
        return score;
    }    
}