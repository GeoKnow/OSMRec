
package gr.athenainnovation.imis.classification;

import gr.athenainnovation.imis.OSMRec.OSMRec;
import java.io.File;
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
    
    public void executeTrain(String path, double confParameter, String model, boolean isLinux){

        String trainLine;
        boolean isExecutable;
        
        if(isLinux){
            if(new File(path + "/src/main/resources/svm_multiclass_learn").canExecute()){
                isExecutable = true;
            }
            else{
                isExecutable = new File(path + "/src/main/resources/svm_multiclass_learn").setExecutable(true,false);
            }
            
            trainLine = path + "/src/main/resources/svm_multiclass_learn "
            + "-c " + confParameter + " "
            + path + "/target/classes/output/vectors "     
            + path + "/target/classes/output/" + model; 
        }
        else{
            if(new File(path + "/src/main/resources/svm_multiclass_learn.exe").canExecute()){
                isExecutable = true;
            }
            else{
                isExecutable = new File(path + "/src/main/resources/svm_multiclass_learn.exe").setExecutable(true,false);
            } 
            
            trainLine = path + "/src/main/resources/svm_multiclass_learn.exe "
            + "-c " + confParameter + " "
            + path + "/target/classes/output/vectors "     
            + path + "/target/classes/output/" + model;
        }              
  
        CommandLine commandLine = CommandLine.parse(trainLine);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        try {
            executor.execute(commandLine);
            
        } catch (IOException ex) {
            if(!isExecutable){
                System.out.println("OSMRec could not grant permission to execute the svm_multiclass process.\n "
                        + "Please set src/main/resources/svm_multiclass_learn process execute permission and try again.");
                System.exit(0);
            }
            
            Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }     
}
