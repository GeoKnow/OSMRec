
package gr.athenainnovation.imis.OSMRec;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import gr.athenainnovation.imis.generator.DistinctiveClasses;
import gr.athenainnovation.imis.generator.Cluster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class provides functionality for recommending classes to new osm instances of the provided OSM file
 * This recommendation is based on the clustering training algorithm.
 * 
 * @author imis-nkarag
 */

public class ClusteringRecommender {
    
    private final List<OSMWay> wayList;
    private final ArrayList<Cluster> averageVectors;
    private final Map<String, Integer> mappingsWithIDs;
    private final String path;
    private final String recommendationsFileClustering;  

    public ClusteringRecommender(List<OSMWay> wayList, ArrayList<Cluster> averageVectors, Map<String,Integer> mappingsWithIDs, String path, String recommendationsFileClustering) {
       this.wayList = wayList;
       this.averageVectors = averageVectors;
       this.path = path;
       this.recommendationsFileClustering = recommendationsFileClustering;
       this.mappingsWithIDs = mappingsWithIDs;               
    }    
    
    public void recommendClasses(){

        new File(path + "/classes/output/" + recommendationsFileClustering).delete(); //delete file from previous recommendations

        try{
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter
                                    (new FileOutputStream(path + "/classes/output/" + recommendationsFileClustering),"UTF-8"));
            
            Map<Integer,String> reverseMappings = new HashMap();

            for(Map.Entry<String, Integer> map : mappingsWithIDs.entrySet()){
                reverseMappings.put(map.getValue(), map.getKey());
            }
            
            System.out.print("computing recommendations...");
            for (OSMWay node : wayList){
                
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

                bufferedWriter.write("\nNode ID: " + node.getID());
                bufferedWriter.newLine();

                int i =0;               
                List<String> recommendationClasses = new ArrayList();
                for(DistinctiveClasses computedClass : computedClasses){            
                if(i==5){break;} //recommendation for the first 5 classes. 

                   if (actualClassList.contains(computedClass.getClassID())){     
                        recommendationClasses.add(reverseMappings.get(computedClass.getClassID()));
                   }
                i++;    
                }
                
                if(computedClasses.isEmpty()){
                    bufferedWriter.write("Clustering process could not produce a good clustering solution to classify this instance. \n"
                                            + "You could try again by changing the -k parameter in train mode.");
                }
                else{
                    if(recommendationClasses.isEmpty()){
                        bufferedWriter.write("Could not recommend a suitable class for this instance.\n");
                    }
                    else{
                        //write recomendation classes list iunder this instance
                        String temp = recommendationClasses.toString();
                        String text = temp.substring(1, temp.length()-1);

                        bufferedWriter.write(text);
                        bufferedWriter.newLine();
                    }
                } 
            }
            bufferedWriter.close();
       }
       catch(IOException ex){
           System.out.println("Something went wrong computing the recommendations.. Please try again.");
           //System.out.println(ex);
       }
    }
    
    private static double cosineSimilarity(ArrayList<Integer> vectorA, ArrayList<Integer> vectorB){
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i)*vectorB.get(i);
    //          normA += Math.pow(vectorA.get(i), 2);
    //          normB += Math.pow(vectorB.get(i), 2);
            normA += vectorA.get(i)*vectorA.get(i);
            normB += vectorB.get(i)*vectorB.get(i);
        }  
        double cosSim =  (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
        return cosSim;
    }
    
}
