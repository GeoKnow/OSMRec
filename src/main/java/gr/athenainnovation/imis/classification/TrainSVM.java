
package gr.athenainnovation.imis.classification;

import gr.athenainnovation.imis.OSMRec.OSMRec;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

/**
 * Provides functionality for executing the SVM training process.
 * 
 * @author imis-nkarag
 */

public class TrainSVM {
    
    public void executeTrain(String path, double confParameter, String model){

            String trainLine = path + "/src/main/resources/svm_multiclass_learn "
                + "-c " + confParameter + " "
                + path + "/target/classes/output/vectors "     
                + path + "/target/classes/output/" + model;               
  
        CommandLine commandLine = CommandLine.parse(trainLine);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        try {
            executor.execute(commandLine);
            
        } catch (IOException ex) {
            
            Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }     
}
