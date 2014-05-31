package gr.athenainnovation.imis.classification;

import gr.athenainnovation.imis.OSMRec.OSMRec;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

/**
 * Provides functionality for executing the scluster process
 * This process uses the adjacency matrix 
 * The clustering process uses the VClustering class and vcluster process respectively for performance reasons.
 * 
 * @author imis-nkarag
 */

public class SClustering {
    
    public void executeClusteringProcess(String path, int clusters){
        String clusteringCommand = path + "/src/main/resources/scluster " //for cl
                + path + "/target/classes/output/matrix.graph " //for cl
                + clusters; //scluster 

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
