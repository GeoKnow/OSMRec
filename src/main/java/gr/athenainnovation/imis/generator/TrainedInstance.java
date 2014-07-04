package gr.athenainnovation.imis.generator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Contains instances of the trained vectors 
 * @author imis-nkarag
 */

public class TrainedInstance implements Serializable{
    private final Integer trainedInstanceID;
    //private final Collection<Integer> trainedInstances;
    private ArrayList<Integer> clusterVector;
    //private List<DistinctiveClasses> averageVectorClasses;
    
    
    public TrainedInstance(Integer trainedInstanceID, Collection<Integer> trainedInstances){
        this.trainedInstanceID = trainedInstanceID;
        //this.trainedInstances = trainedInstances;
    }
    
    public Integer getTrainedInstanceID(){
        return trainedInstanceID;
    }
    
    public void  setVector(ArrayList<Integer> vector){
        this.clusterVector = vector;
    }
    
    public ArrayList getClusterVector(){
        return clusterVector;
    }    
}
