package gr.athenainnovation.imis.scoring;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import gr.athenainnovation.imis.generator.DistinctiveClasses;
import gr.athenainnovation.imis.generator.Cluster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                ArrayList<Integer> nodeVector = node.getVector();
                ArrayList<Double> similarities = new ArrayList();
                Map<Cluster, Double> clusterSimilarities = new HashMap<>();

                for(Cluster averageClusterVector : averageVectors){

                    ArrayList<Integer> averageVector = averageClusterVector.getClusterVector(); 
                    Double similarity = cosineSimilarity(nodeVector, averageVector);
                    similarities.add(similarity);

                    //this map contains all average cluster vectors with the corresponding similarities with current instance
                    clusterSimilarities.put(averageClusterVector, similarity); //cluster vector and similarity of the cluster vector and current instance vector
                    Collections.sort(similarities, Collections.reverseOrder());
                }

                Cluster bestClusterForInstance = null;
                for(Map.Entry<Cluster, Double> clusterSimilarity : clusterSimilarities.entrySet()){ 
                    
                      if(clusterSimilarity.getValue().equals(similarities.get(0))){ //there is a chance that there are more than one cluster with same 
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
                    if(i==5){break;} 

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
    
    private static double cosineSimilarity(ArrayList<Integer> vectorA, ArrayList<Integer> vectorB){
    //computes cosine similarity for the two provided vectors    
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i)*vectorB.get(i);
            normA += vectorA.get(i)*vectorA.get(i);
            normB += vectorB.get(i)*vectorB.get(i);
        }  
        double cosSim =  (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
        return cosSim;
    }
    
    private void setScore(float score){
        this.score = score;
    }
    
    public float getScore(){
        return score;
    }
    
}