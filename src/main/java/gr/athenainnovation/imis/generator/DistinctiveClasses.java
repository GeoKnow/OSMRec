
package gr.athenainnovation.imis.generator;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator class to compare the frequency between classes of a cluster
 * This was implemented because the frequency values are not unique and we cannot use multimap as it is not serializable.
 * 
 * @author imis-nkarag
 */

public class DistinctiveClasses implements Comparator<DistinctiveClasses>, Serializable {
    protected Integer classID;
    protected Integer frequency;
    
    public DistinctiveClasses(){        
    }
    
    public DistinctiveClasses(Integer classID, Integer frequency){
       this.classID = classID; 
       this.frequency = frequency; 
    }
    
    @Override
    public int compare(DistinctiveClasses avgCl1, DistinctiveClasses avgCl2){
       return avgCl2.frequency - avgCl1.frequency;
    }
   
    public int getClassID(){
        return classID;
    }
}
