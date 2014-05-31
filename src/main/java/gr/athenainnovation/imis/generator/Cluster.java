package gr.athenainnovation.imis.generator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains instances of the cluster average vectors 
 * @author imis-nkarag
 */
public class Cluster implements Serializable{
    private final Integer clusterID;
    private final Collection<Integer> clusterInstances;
    private ArrayList<Integer> clusterVector;
    private List<DistinctiveClasses> averageVectorClasses;
    
    
    public Cluster(Integer clusterID, Collection<Integer> clusterInstances){
        this.clusterID = clusterID;
        this.clusterInstances = clusterInstances;
    }
    
    public Integer getClusterID(){
        return clusterID;
    }
    
    public void  setVector(ArrayList<Integer> vector){
        this.clusterVector = vector;
    }
    
    public ArrayList getClusterVector(){
        return clusterVector;
    }
    
    public void setSortedClusterClasses(List<DistinctiveClasses> majorityClasses){
        this.averageVectorClasses = majorityClasses;
    }
    
    public List<DistinctiveClasses> getSortedClusterClasses(){
        return averageVectorClasses;
    }
}
