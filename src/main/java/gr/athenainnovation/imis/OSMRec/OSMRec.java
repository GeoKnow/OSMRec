/*
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *     Read the COPYING file for more information. 
 */    

package gr.athenainnovation.imis.OSMRec;    

import gr.athenainnovation.imis.classification.TrainSVM;
import gr.athenainnovation.imis.classification.TestSVM;
import gr.athenainnovation.imis.OSMContainer.OSMNode;
import gr.athenainnovation.imis.OSMContainer.OSMWay;
import gr.athenainnovation.imis.scoring.ClusteringScorer;
import gr.athenainnovation.imis.classification.VClustering;
import gr.athenainnovation.imis.generator.Cluster;
import gr.athenainnovation.imis.parsers.MappingsParser;
import gr.athenainnovation.imis.parsers.OccurrencesParser;
import gr.athenainnovation.imis.generator.InstanceVectors;
import gr.athenainnovation.imis.parsers.OSMParser;
import gr.athenainnovation.imis.parsers.OntologyParser;
import gr.athenainnovation.imis.generator.BalancedVectorsMatrix;
import gr.athenainnovation.imis.generator.ClusterVectors;
import com.hp.hpl.jena.ontology.OntClass;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Entrance of the program
 *  Creates objects for parsing mappings file, ontology, name occurrences and OSM file
 *  Based on the provided arguments trains/tests using SVM or Clustering methodology.
 * 
 *  @author imis-nkarag
 */

public class OSMRec {   
    
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static Map<String,String> mappings;
    private static Map<String,Integer> mappingsWithIDs;
    private static Map<String, List<String>> indirectClasses;
    private static List<OSMNode> nodeList;
    private static List<OSMWay> wayList;
    private static Map<String, Integer> indirectClassesWithIDs;
    private static List<OntClass> listHierarchy;
    private static List<String> namesList;  
    private static int trainAlgorithm;
    private static boolean trainMode;
    private static String path;
    private static boolean testMode;
    //private static String matrixFilePath; //adjacency matrix
    
    public static void main(String[] args ) throws FileNotFoundException 
    {   
       
       Path currentPath = Paths.get("");
       path = currentPath.toAbsolutePath().toString();
       trainAlgorithm = 0;
       trainMode = false;
       testMode = false;

       Double confParameter = null;       
       String arg;
       String value;
       String osmFile = null;
       String testSvmOutputFile = "testSVM";
       String model = "model0";      
       String recommendationsFileSVM = "output.txt";
       String recommendationsFileClustering = "output.txt";
       
       int i =0;       
       int kClusters; //default value for the number of clusters is 10.
       int instancesSize;
       int averageInstancesPerCluster = -1; //default: no k parameter specified
       boolean wrongArguments = false;
       //matrixFilePath = path + "/classes/output/matrix.graph";   //adjacency matrix
       boolean isLinux =false;
       
       if(OS.indexOf("nux") >= 0){
           isLinux = true;
       }
       else if(OS.indexOf("win") >= 0){
           isLinux = false;
       }
       else{
           System.out.println("Your operating system is not supported yet :/");
           System.exit(0);
       }
       
       while (i < args.length){

           arg = args[i];
           if(arg.startsWith("-")){ 
	       if(arg.equals("-help")){
		   System.out.println("Usage:\n java -jar OSMRec-1.0.jar -train trainAlgorithm -i inputFile [-c confParameter] [-k averageSize] [-m model]\n or\n"
			   + "java -jar OSMRec-1.0.jar -test trainAlgorithm -i inputFile [-o outputFile]\n");
		   System.out.println("Train algorithm takes values 1,2 or 3: -train algorithm \n1    SVM training on spatial entities as items\n" +
						      //"3    Clustering of spatial entities and SVM training on clusters of entities\n" +
						      "2    Clustering of spatial entities and k-NN algorithm on clusters of entities\n"
			   + "-i requires a filename: [-i inputFile]\n"
			   + "-c requires a number parameter: [-c confParameter]\n"
			   + "-k requires the desired average number of items per cluster: [-k averageSize]\n"
			   + "-m requires a filename for the model: [-m model]\n");	
	       System.exit(0);	   
	       }
	       value = args[i+1]; 
	       if(arg.equals("-train")){
		   System.out.println("Train mode on!");
		   if(value.equals("1")){              
		       trainAlgorithm = 1;
		       System.out.println(" - SVM training on spatial entities as items");
		   }
		   else if(value.equals("2")){
		       trainAlgorithm = 2;
		   }
		   else if(value.equals("3")){
		       trainAlgorithm = 3;
                       wrongArguments = true;
		   }
		   else{
		       System.out.println("Train algorithm takes values 1,2 or 3: [-train algorithm] \n1    SVM training on spatial entities as items\n" +
						      //"2    Clustering of spatial entities and SVM training on clusters of entities\n" +
						      "2    Clustering of spatial entities and k-NN algorithm on clusters of entities");
                       wrongArguments = true;
		   }
		   trainMode = true;
	       }
	       else if(arg.equals("-test")){
		   System.out.println("Test mode on!");
		   if(value.equals("1")){              
		       trainAlgorithm = 1;
		   }
		   else if(value.equals("2")){
		       trainAlgorithm = 2;
		   }
		   else if(value.equals("3")){
		       trainAlgorithm = 3;
		   }
		   else{
		       System.out.println("Train algorithm takes values 1,2 or 3: [-test algorithm] \n1    SVM training on spatial entities as items\n" +
						      //"3    Clustering of spatial entities and SVM training on clusters of entities\n" +
						      "2    Clustering of spatial entities and k-NN algorithm on clusters of entities");
                       wrongArguments = true;
		   }
		   testMode = true;
	       }
	       else if(arg.equals("-i")){ 
		   if (i < args.length){
		     osmFile = value;  
		   }
		   else{
		     System.err.println("-i requires a filename: [-i inputFile]");
                     wrongArguments = true;
		   }                          
	       }
	       else if(arg.equals("-c")){
		    if (i < args.length){
			try{
			confParameter = Double.parseDouble(value);
			}
			catch(NumberFormatException e){
			    System.err.println("-c requires a number parameter: [-c confParameter]");
                            wrongArguments = true;
			}
		    }
		    else{
			    System.err.println("-c requires a number parameter: [-c confParameter]");
                            wrongArguments = true;
		    }
	       }
	       else if(arg.equals("-k")){
		   if (i < args.length){
		       try{
                        averageInstancesPerCluster = Integer.parseInt(value);
		       }
		       catch(NumberFormatException e){
			 System.out.println("-k requires the desired average number of items per cluster: [-k averageSize]");  
                         wrongArguments = true;
		       }
		   }
		   else{
		       System.out.println("-k requires the desired average number of items per cluster: [-k averageSize]");
                       wrongArguments = true;
		   }
	       }
	       else if(arg.equals("-m")){
		   if (i < args.length){
		     model = value;  
		   }
		   else{
		     System.out.println("-m requires a filename for the model: [-m model]");
                     wrongArguments = true;
		   } 
	       }
	       else if(arg.equals("-o")){
                   if (i < args.length){
		     recommendationsFileSVM = value;
                     recommendationsFileClustering = value;
		   }
		   else{
		     System.out.println("-o requires a filename: [-o inputFile]");
                     wrongArguments = true;
		   }   
	       }
	   }
	   i++;
       }
       if (args.length == 0 || osmFile == null || wrongArguments){
	   System.err.println("Usage:\n java -jar OSMRec-1.0.jar -train trainAlgorithm -i inputFile [-c confParameter] [-k averageSize] [-m model]\n or\n"
			   + "java -jar OSMRec-1.0.jar -test trainAlgorithm -i inputFile [-o outputFile]\n");
	   System.exit(0);
       } 
       
        try { //clear the vectors file if it exists from previous execution                 
            FileOutputStream writer = new FileOutputStream(path + "/classes/output/vectors"); 
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        File file = new File(path + "/classes/mappings/Map"); 

        MappingsParser mappingsParser = new MappingsParser();
        try {   
            mappingsParser.parseFile(file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
        }
        mappings = mappingsParser.getMappings();
        mappingsWithIDs = mappingsParser.getMappingsWithIDs();           
      
        OntologyParser ontologyParser = new OntologyParser(path);
        ontologyParser.parseOntology();
        indirectClasses = ontologyParser.getIndirectClasses();
        indirectClassesWithIDs = ontologyParser.getIndirectClassesIDs();
        listHierarchy = ontologyParser.getListHierarchy(); 

        File namesFile = new File(path + "/classes/mappings/names"); 
        
        OccurrencesParser nameOccurrencesParser = new OccurrencesParser();
        nameOccurrencesParser.parseNamesFile(namesFile);
        namesList = nameOccurrencesParser.getNamesList();
        
        String osmInputPath;
        if (osmFile.startsWith("/") || osmFile.startsWith("file:///")){  //system has already exited in case of a null osmFile

             osmInputPath = osmFile;
        }
        else{

            osmInputPath = path + "/"+ osmFile; 
        }
        
        OSMParser osmParser = new OSMParser(osmInputPath); // OSM FILE

        osmParser.parseDocument(); 
        nodeList = osmParser.getNodeList();
        wayList =osmParser.getWayList();
        instancesSize = wayList.size();
        if(instancesSize ==0){
            System.out.println("Something went wrong.. Please check the path of input osm file");
            System.exit(0);
        }
        else{
            System.out.println("The input file has " + instancesSize + " nodes.");
        }       
        
        kClusters = instancesSize/averageInstancesPerCluster; //number of clusters, based on average instances per cluster provided by the user       
        
        InstanceVectors instanceVectors = new InstanceVectors(mappings, mappingsWithIDs, indirectClasses, 
                indirectClassesWithIDs, listHierarchy, nodeList, namesList, path);
        
        instanceVectors.constructWayVectors(wayList);                  
        
        
        
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////        
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////        
///////////          Execution flow based on the provided arguments          //////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////        
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////        
        
        
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////      
//////////////////////////////////      Train Algorithm 1      /////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        
        if(confParameter == null && trainMode && trainAlgorithm == 1){ 
            chooseOptimalConfParameter(model, testSvmOutputFile, isLinux); //if conf parameter is not set, find best conf param 
            
        }
        else{ 
            //-train 1: training the model using SVM multiclass
            if(trainMode && trainAlgorithm == 1){
                
                String makePath;
                if(isLinux){
                    makePath = path.replace("/target", "");
                }
                else{
                    makePath = path.replace("\\target","");    
                }
                
                TrainSVM trainSVM = new TrainSVM();
                trainSVM.executeTrain(makePath, confParameter, model, isLinux);
            }

            //-test 1: testing the input osm file, using the model produced from the training process.
            if(testMode && trainAlgorithm == 1){
                
                String makePath;
                if(isLinux){
                makePath = path.replace("/target", "");
                }
                else{
                makePath = path.replace("\\target","");    
                }
                
                TestSVM testSVM = new TestSVM(makePath, model, testSvmOutputFile);
                testSVM.executeTest(isLinux);

                File vectorsOutputFile = new File(path + "/classes/output/vectors"); //this file contains the vectors produced from the test set       
                File svmPredictionsOutputFile = new File(path + "/classes/output/" + testSvmOutputFile); //this file contains the predictions from svm classify
                
                SVMRecommender recommender = new SVMRecommender();
                recommender.recommend(recommendationsFileSVM, svmPredictionsOutputFile, vectorsOutputFile, mappingsWithIDs, wayList, path);
            }
        }
        

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////        Train Algorithm 2      /////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        if(averageInstancesPerCluster == -1 && trainMode && trainAlgorithm == 2){
            chooseOptimalNumberOfClusters(isLinux);
        }
        else {           
        
            //-train -2: produces a vector matrix for the vcluster process
            if(trainMode && trainAlgorithm == 2){
                String vectorMatrixOutputFile = path + "/classes/output/vmatrix";

                BalancedVectorsMatrix balancedVectorsMatrix = new BalancedVectorsMatrix(wayList, vectorMatrixOutputFile);
                balancedVectorsMatrix.generateBalancedVectorsMatrix();  

                String makePath;
                if(isLinux){
                makePath = path.replace("/target", "");
                }
                else{
                makePath = path.replace("\\target","");    
                }
                
                VClustering vCluster = new VClustering();
                vCluster.executeClusteringProcess(makePath, kClusters, isLinux);

                //train average cluster vectors and save them to a file. This file will be used to classify new osm instances in a cluster
                String clusterSolution = path + "/classes/output/vmatrix.mat.clustering." + kClusters;

                ClusterVectors clusterVectors = new ClusterVectors(wayList, clusterSolution);
                clusterVectors.produceClusterVectors();

                //serialize average cluster vectors to file.
                try (FileOutputStream fileOut = new FileOutputStream(path + "/classes/mappings/averageClusterVectors.ser"); 
                    ObjectOutputStream out = new ObjectOutputStream(fileOut)) {

                ArrayList<Cluster> averageVectors = clusterVectors.getAverageClusterVectors();   

                    out.writeObject(averageVectors);

                }
                catch(IOException e){
                    System.out.println("serialize " + e);
                }
            }

            if(testMode && trainAlgorithm == 2){

                ArrayList<Cluster> trainedAverageVectors = null;
                
                //obtain serialized average cluster vectors from file.
                try{   
                    try (FileInputStream fileIn = new FileInputStream(path + "/classes/mappings/averageClusterVectors.ser"); 
                            ObjectInputStream in = new ObjectInputStream(fileIn)) {
                        //AverageClusterVector oneTest = (AverageClusterVector) clusterVectors.getAverageClusterVectors().get(1);

                        trainedAverageVectors = (ArrayList<Cluster>) in.readObject();
                        //System.out.println("cluster vector: " + trainedAverageVectors.get(1).getClusterVector());
                    }
                }
                catch(IOException | ClassNotFoundException e){//
                    //System.out.println("deserialize failure\n" + e);
                    System.err.println("Something went wrong.. Try to train a model first and then test it with the same average instances per cluster!");
                }

                ClusteringRecommender clusterRecommender = new ClusteringRecommender(wayList, trainedAverageVectors, mappingsWithIDs, path, recommendationsFileClustering);
                clusterRecommender.recommendClasses();

            }   
        }//end of else with no -k param 
        
    }//end of main
   
    private static void chooseOptimalConfParameter(String model, String testSvmOutputFile, boolean isLinux){
        Double[] confParams = new Double[] {5.0, 30.0, 1000.0, 40000.0, 100000.0, 200000.0}; // custom values for choosing optimal c parameter
        //Double[] confParams = new Double[] {3.0, 7.0}; //debugging
        int i =0;
        float bestScore = 100;
        String bestModel = model;
        double bestConfParam = 5.0;
        
        String makePath;
        if(isLinux){
        makePath = path.replace("/target", "");
        }
        else{
        makePath = path.replace("\\target","");    
        }
        
        for (Double confParam : confParams) {
                                  
            TrainSVM trainSVM = new TrainSVM();
            trainSVM.executeTrain(makePath, confParam, model+i, isLinux);

            TestSVM testSVM = new TestSVM(makePath, model+i, testSvmOutputFile);
            testSVM.executeTest(isLinux);             
            
            float score = testSVM.getScore();
            if(score < bestScore){
                bestScore = score;
                bestModel = model+i;
                bestConfParam = confParam;
            }            
        i++;    
        }             
        
        TrainSVM trainSVM = new TrainSVM();
        trainSVM.executeTrain(makePath, bestConfParam, bestModel, isLinux);  //final training with optimal c parameter
        
        System.out.println("Best model produced is the file \"" + bestModel +"\", with conf parameter \"-c " + bestConfParam + "\"");
        System.out.println("Define this parameter to use the best model for SVM test: \"-m " + bestModel + "\"");
        
    }
    
    private static void chooseOptimalNumberOfClusters(boolean isLinux){

        float bestScore = 100; //worst possible score, because score represents classification error
        int optimalClusters = 70;
        Integer[] averageInstances = new Integer[] {70, 65, 60, 55, 50, 45, 40, 35, 30, 25};
        //Integer[] averageInstances = new Integer[] {70, 50}; //debugging 
        
        int trainSize = 3*wayList.size()/5;
        int testSize = wayList.size()/5;
        
        List<OSMWay> trainList= new ArrayList<>();
        for(int g = 0; g<trainSize; g++){                   
            trainList.add(wayList.get(g));
        }
        List<OSMWay> testList= new ArrayList<>();
        for(int g = 4*testSize; g<5*testSize; g++){
            testList.add(wayList.get(g));
        }
        
        for(Integer k : averageInstances){       
            
            String vectorMatrixOutputFile = path + "/classes/output/vmatrix";
            BalancedVectorsMatrix balancedVectorsMatrix = new BalancedVectorsMatrix(trainList, vectorMatrixOutputFile);
            balancedVectorsMatrix.generateBalancedVectorsMatrix();  

            String makePath;
            if(isLinux){
            makePath = path.replace("/target", "");
            }
            else{
            makePath = path.replace("\\target","");    
            }             

            VClustering vCluster = new VClustering();
            vCluster.executeClusteringProcess(makePath, (trainSize/k), isLinux);

            //train average cluster vectors and save them to a file. This file will be used to classify new osm instances in a cluster
            String clusterSolution = path + "/classes/output/vmatrix.mat.clustering." + (trainSize/k);

            ClusterVectors clusterVectors = new ClusterVectors(trainList, clusterSolution);
            clusterVectors.produceClusterVectors();

            ArrayList<Cluster> trainedAverageVectors = clusterVectors.getAverageClusterVectors(); //avoiding serialization here                  
            ClusteringScorer cs = new ClusteringScorer(testList, trainedAverageVectors, mappingsWithIDs);    
            cs.score();

            float score = cs.getScore();               
            if(score < bestScore){ //the score represents the classification error
                bestScore = score;
                optimalClusters = (trainSize/k);
            }

            //serialize average vectors with the best k parameter
            try (FileOutputStream fileOut = new FileOutputStream(path + "/classes/mappings/averageClusterVectors.ser"); 
                ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
                out.writeObject(trainedAverageVectors);

            }
            catch(IOException e){
                System.out.println("serialize error:\n " + e);
            }
        }
    System.out.println("Best value for average instances per cluster is " + optimalClusters);
    System.out.println("Try to test defining -k " + optimalClusters);
    }
    //////////////////////       for sclustering, takes too long to execute for large training files   //////////////////////        
 
        /*        
        //-train 2, -train 3: producing the similarities adjacency matrix and executes clustering process with CLUTO 
        //if(trainMode && (trainAlgorithm == 2 || trainAlgorithm == 3)){
        if(false){    
            AdjacencyMatrix adjMatrix = new AdjacencyMatrix(wayList, wayList.size(),path);
            try {
                adjMatrix.constructMatrix();
            } catch (IOException ex) {
                Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
            } 
            String makePath = path.replace("/target", "");
            SClustering clustering = new SClustering();            
            clustering.executeClusteringProcess(makePath, kClusters);
       
        //produces the average cluster vectors from the computed clusters and train a new SVM on them or KNN on new instances
        //the produced vectors reside in the output directory.    
            String clusterSolution = path + "/classes/output/matrix.graph.clustering." + kClusters;
            ClusterVectors clusterVectors = new ClusterVectors(wayList, clusterSolution);
            clusterVectors.produceClusterVectors();
        }*/
}
