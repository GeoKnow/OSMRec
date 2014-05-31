package gr.athenainnovation.imis.classification;

import gr.athenainnovation.imis.OSMRec.OSMRec;
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
    
    public void executeClusteringProcess(String path, int clusters){
        System.out.println("Executing clustering process...");
        String clusteringCommand = path + "/src/main/resources/vcluster " 
                + "-clmethod=graph -sim=dist -mincomponent=1 -clustfile="+ path +"/target/classes/output/vmatrix.mat.clustering." + clusters + " " 
                + path + "/target/classes/output/vmatrix.mat "
                + clusters; //number of desired clusters based on the average instance per cluster. 

        CommandLine commandLineClustering = CommandLine.parse(clusteringCommand);
        DefaultExecutor classificationExecutor = new DefaultExecutor();
        classificationExecutor.setExitValue(0);
        try {
            classificationExecutor.execute(commandLineClustering);
        } catch (IOException ex) {
            
            Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }
}
