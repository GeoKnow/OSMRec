
package gr.athenainnovation.imis.OSMRec;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import gr.athenainnovation.imis.generator.Cluster;
import gr.athenainnovation.imis.generator.DistinctiveClasses;
import gr.athenainnovation.imis.utils.SimilarityComputingUtils;
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
import java.util.TreeMap;

/**
 * This class provides functionality for recommending classes to new osm instances of the provided OSM file
 * This recommendation is based on the clustering training algorithm.
 * 
 * @author imis-nkarag
 */

public class ClusteringRecommender {
    
    private static final String SEP = System.lineSeparator();
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
            
            System.out.println("computing recommendations...");
            for (OSMWay node : wayList){
                
                //ArrayList<Integer> nodeVector = node.getVector();
                TreeMap<Integer,Double> nodeVector = node.getIndexVector();
                ArrayList<Double> similarities = new ArrayList();
                Map<Cluster, Double> clusterSimilarities = new HashMap<>();

                for(Cluster averageClusterVector : averageVectors){

                    //ArrayList<Integer> averageVector = averageClusterVector.getClusterVector(); 
                    TreeMap<Integer,Double> averageIndexVector = averageClusterVector.getClusterIndexVector();
                    
                    Double similarity = SimilarityComputingUtils.cosineSimilarity(nodeVector, averageIndexVector);
                    similarities.add(similarity);

                    //this map contains all average cluster vectors with the corresponding similarities with current instance
                    clusterSimilarities.put(averageClusterVector, similarity); //cluster vector and similarity of the cluster vector and current instance vector                    
                }
                
                Collections.sort(similarities, Collections.reverseOrder());
                Cluster bestClusterForInstance = null;
                for(Map.Entry<Cluster, Double> clusterSimilarity : clusterSimilarities.entrySet()){ 

                    if(clusterSimilarity.getValue().equals(similarities.get(0))){ //there is a chance that there are more than one cluster with same 
                                                                                      //similarity. In this case we might get a slightly different score by picking the first one.
                        bestClusterForInstance = clusterSimilarity.getKey();
                        break; //found best cluster
                    } 
                }

                List<DistinctiveClasses> computedClasses = new ArrayList<>();
                if(bestClusterForInstance != null){
                    computedClasses = bestClusterForInstance.getSortedClusterClasses();
                }
                Set<Integer> actualClassList = node.getClassIDs();
                actualClassList.add(node.getClassID());

                bufferedWriter.write(SEP + "Node ID: " + node.getID());
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
                    bufferedWriter.write("Clustering process could not produce a good clustering solution to classify this instance. "+ SEP
                                            + "You could try again by changing the -k parameter in train mode."+ SEP);
                }
                else{
                    if(recommendationClasses.isEmpty()){
                        bufferedWriter.write("Could not recommend a suitable class for this instance." + SEP);
                    }
                    else{
                        //write recomendation classes list under this instance
                        String temp = recommendationClasses.toString();
                        String text = temp.substring(1, temp.length()-1);

                        bufferedWriter.write(text);
                        bufferedWriter.newLine();
                    }
                } 
            }
        bufferedWriter.close();
        System.out.println("Recommendations computed! Check the output.txt in the target/classes directory" + SEP);
        }
        catch(IOException ex){
           System.out.println("Something went wrong computing the recommendations.. Please try again.\n\n" + ex);
        }
    }   
}
