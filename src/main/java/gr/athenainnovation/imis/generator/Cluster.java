package gr.athenainnovation.imis.generator;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * Contains instances of the cluster average vectors 
 * @author imis-nkarag
 */

public class Cluster implements Serializable{
    
    private final Integer clusterID;
    //private final Collection<Integer> clusterInstances;
    private List<DistinctiveClasses> averageVectorClasses;
    private TreeMap<Integer, Double> clusterIndexVector;  
    
    public Cluster(Integer clusterID, Collection<Integer> clusterInstances){
        this.clusterID = clusterID;
        //this.clusterInstances = clusterInstances;
    }
    
    public Integer getClusterID(){
        return clusterID;
    }
    
    public void  setIndexVector(TreeMap<Integer,Double> indexVector){
        this.clusterIndexVector = indexVector;
    }
    
    public TreeMap<Integer,Double> getClusterIndexVector(){
        return clusterIndexVector;
    }
    
    public void setSortedClusterClasses(List<DistinctiveClasses> majorityClasses){
        this.averageVectorClasses = majorityClasses;
    }
    
    public List<DistinctiveClasses> getSortedClusterClasses(){
        return averageVectorClasses;
    }
}
