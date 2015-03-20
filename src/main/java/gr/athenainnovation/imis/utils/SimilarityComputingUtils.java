package gr.athenainnovation.imis.utils;

//import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Cosine Similarity and Euclidian Distance calculation.
 * 
 * @author imis-nkarag
 */

public final class SimilarityComputingUtils {
    
    private SimilarityComputingUtils() {
        //preventing instantiation.                   
    }
    
    public static double cosineSimilarity(TreeMap<Integer,Double> vectorA, TreeMap<Integer,Double> vectorB){
    //calculates cosine similarity for the two provided vectors given as Treemaps   
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for ( Integer id : vectorA.keySet()){
            if(vectorB.containsKey(id)){
                dotProduct += vectorA.get(id)*vectorB.get(id);//1 for boolean features
                normA += vectorA.get(id)*vectorA.get(id);
                normB += vectorB.get(id)*vectorB.get(id);
            }
            else{
                normA += vectorA.get(id)*vectorA.get(id);
            }
        }
         
        return (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
    
    /*
    public static double cosineSimilarityArray(ArrayList<Integer> vectorA, ArrayList<Integer> vectorB){
    //computes cosine similarity for the two provided vectors given as ArrayLists
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
    */
    
    public static double euclidianDistance(TreeMap<Integer, Double> vectorA, TreeMap<Integer, Double> vectorB){       
        double sum = 0.0;
        
        for ( Integer id : vectorA.keySet()){
            if(vectorB.containsKey(id)){
                double a = vectorA.get(id);
                double b = vectorB.get(id);
                sum = sum + (a-b)*(a-b);
            }
            else{
                double a = vectorA.get(id);
                sum = sum + a*a; //b = 0 => (a-b)*(a-b) = a^2
            }
        }
        return Math.sqrt(sum);
    }
    
    /*
    public static double euclidianDistanceArray(ArrayList<Integer> vectorA, ArrayList<Integer> vectorB){       
        double sum = 0.0;
        for(int i=0;i<vectorA.size();i++) {
            Integer a = vectorA.get(i);
            Integer b = vectorB.get(i);            
            sum = sum + (a-b)*(a-b);
        }
        return Math.sqrt(sum);
    }    
    */
}
