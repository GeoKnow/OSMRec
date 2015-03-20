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
    private final int numberOfClassesToScore;
    private float scoreCosine = 100f;
    private float scoreEuclidian;
    private int unclassifiedInstances = 0;

    public ClusteringScorer(List<OSMWay> wayList, ArrayList<Cluster> averageVectors, 
                                                Map<String,Integer> mappingsWithIDs, int numberOfClassesToScore) {
        
       this.wayList = wayList;
       this.averageVectors = averageVectors;
       this.mappingsWithIDs = mappingsWithIDs; 
       this.numberOfClassesToScore = numberOfClassesToScore;
       
    }    
    
    public void score(){
        
        int scoresCosine = 0;
        int scoresEuclidian = 0;
        
        
        int instances = 0;
            Map<Integer,String> reverseMappings = new HashMap();

            for(Map.Entry<String, Integer> map : mappingsWithIDs.entrySet()){
                reverseMappings.put(map.getValue(), map.getKey());
            }
            System.out.print("Scoring clustering process...");
            for (OSMWay node : wayList){
                
                int tempScoreCosine = 0;//cosine
                int tempScoreEuclidian = 0;//euclidian
                
                TreeMap<Integer,Double> nodeVector = node.getIndexVector();
                ArrayList<Double> similarities = new ArrayList();
                ArrayList<Double> distances = new ArrayList();
                Map<Cluster, Double> clusterSimilarities = new HashMap<>();
                Map<Cluster, Double> clusterDistances = new HashMap<>();

                for(Cluster averageClusterVector : averageVectors){

                    TreeMap<Integer,Double> averageIndexVector = averageClusterVector.getClusterIndexVector();
                    Double similarity = SimilarityComputingUtils.cosineSimilarity(nodeVector, averageIndexVector);
                    Double distance = SimilarityComputingUtils.euclidianDistance(nodeVector, averageIndexVector);
                    distances.add(distance);
                    similarities.add(similarity);

                    //this map contains all average cluster vectors with the corresponding similarities with current instance                    
                    //cluster vector and similarity of the cluster vector and current instance vector
                    clusterSimilarities.put(averageClusterVector, similarity); 
                    clusterDistances.put(averageClusterVector, distance);
                }
                
                Collections.sort(similarities, Collections.reverseOrder());
                Collections.sort(distances);
                Cluster bestClusterForInstanceFromCosine = null;
                Cluster bestClusterForInstanceFromEuclidian = null;
                //cosine similarity score
                for(Map.Entry<Cluster, Double> clusterSimilarity : clusterSimilarities.entrySet()){ 
                    
                      if(clusterSimilarity.getValue().equals(similarities.get(0))){ 
                          //there is a chance that there are more than one cluster with same 
                          //similarity. In this case we might get a slightly different score by picking the first one.                                                             
                          bestClusterForInstanceFromCosine = clusterSimilarity.getKey();
                          break; //found best cluster
                      } 
                }
                
                //euclidian distance
                for(Map.Entry<Cluster, Double> clusterDistance : clusterDistances.entrySet()){ 
                    
                      if(clusterDistance.getValue().equals(distances.get(0))){ 
                          //there is a chance that there are more than one cluster with same 
                          //similarity. In this case we might get a slightly different score by picking the first one.                                                             
                          bestClusterForInstanceFromEuclidian = clusterDistance.getKey();
                          break; //found best cluster
                      } 
                }

                List<DistinctiveClasses> computedClasses = bestClusterForInstanceFromCosine.getSortedClusterClasses();
                List<DistinctiveClasses> computedClassesEuclidian = bestClusterForInstanceFromEuclidian.getSortedClusterClasses();
                
                Set<Integer> actualClassList = node.getClassIDs();
                //actualClassList.add(node.getClassID());
                if(actualClassList.isEmpty()){
                    unclassifiedInstances++;
                }
                
                //cosine score
                int i = 0;                                              
                for(DistinctiveClasses computedClass : computedClasses){            
                    if(i == numberOfClassesToScore){break;} 
                    //System.out.println("same? (cosine) " + computedClass.getClassID() + " " + actualClassList);

                    if (actualClassList.contains(computedClass.getClassID())){      
                        tempScoreCosine = 1;
                    }
                i++;    
                }
                
                //euclidian score
                for(DistinctiveClasses computedClassEuclidian : computedClassesEuclidian){            
                    if(i == numberOfClassesToScore){break;} 
                    //System.out.println("same? (euclidian) " + computedClassEuclidian.getClassID() + " " + actualClassList);

                    if (actualClassList.contains(computedClassEuclidian.getClassID())){      
                        tempScoreEuclidian = 1;
                    }
                i++;    
                }
                
                //System.out.println("~~");
                scoresCosine = scoresCosine + tempScoreCosine;
                scoresEuclidian = scoresEuclidian + tempScoreEuclidian;
                instances++;  
            }

            System.out.println("correct cosine: " + scoresCosine + " of total: " + instances + ", unclassified instances" + unclassifiedInstances);
            System.out.println("correct euclidian: " + scoresEuclidian + " of total: " + instances + ", unclassified instances" + unclassifiedInstances);

            
            scoreCosine = 100 - ((float)scoresCosine*100/ (float)(instances-unclassifiedInstances));
            scoreEuclidian = 100 - ((float)scoresEuclidian*100/ (float)(instances-unclassifiedInstances));
            
            setScore(scoreCosine);
            System.out.println("COSINE: total score (error) for the first "+ numberOfClassesToScore + " classes\n" + scoreCosine); 
            System.out.println("EUCLIDIAN: total score (error) for the first "+ numberOfClassesToScore + " classes\n" + scoreEuclidian); 
    }    
    
    private void setScore(float score){
        this.scoreCosine = score;
    }
    
    public float getScore(){
        return scoreCosine;
    }    
}