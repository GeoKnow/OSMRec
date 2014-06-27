package gr.athenainnovation.imis.classification;

import gr.athenainnovation.imis.OSMRec.OSMRec;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

/**
 * Provides functionality for executing the clustering process.
 * 
 * @author imis-nkarag
 */

public class VClustering {
    
    public void executeClusteringProcess(String path, int clusters, boolean isLinux){
        System.out.println("Executing clustering process...");
        String clusteringCommand;
        boolean isExecutable;
        if(isLinux){
            if(new File(path + "/src/main/resources/vcluster").canExecute()){
                isExecutable = true;
            }
            else{
                isExecutable = new File(path + "/src/main/resources/vcluster").setExecutable(true,false);
            }    
            //new File(path + "/src/main/resources/vcluster").
            clusteringCommand = path + "/src/main/resources/vcluster " 
            + "-clmethod=graph -sim=dist -mincomponent=1 -clustfile="+ path +"/target/classes/output/vmatrix.mat.clustering." + clusters + " " 
            + path + "/target/classes/output/vmatrix.mat "
            + clusters; //number of desired clusters based on the average instance per cluster. 
        }
        else{
            if(new File(path + "/src/main/resources/vcluster.exe").canExecute()){
                isExecutable = true;
            }
            else{
                isExecutable = new File(path + "/src/main/resources/vcluster").setExecutable(true,false);
            }  
            
            clusteringCommand = path + "/src/main/resources/vcluster.exe " 
            + "-clmethod=graph -sim=dist -mincomponent=1 -clustfile="+ path +"/target/classes/output/vmatrix.mat.clustering." + clusters + " " 
            + path + "/target/classes/output/vmatrix.mat "
            + clusters; //number of desired clusters based on the average instance per cluster.
        }
        
        CommandLine commandLineClustering = CommandLine.parse(clusteringCommand);
        DefaultExecutor classificationExecutor = new DefaultExecutor();
        classificationExecutor.setExitValue(0);
        try {
            classificationExecutor.execute(commandLineClustering);
        } catch (IOException ex) {
            if(!isExecutable){
                System.out.println("OSMRec could not grant permission to execute the vcluster process.\n "
                        + "Please set src/main/resources/vcluster process execute permission and try again.");
                System.exit(0);
            }
            Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }
}
