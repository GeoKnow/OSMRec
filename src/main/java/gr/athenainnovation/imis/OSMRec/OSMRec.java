/*
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *     Read the COPYING file for more information. 
 */    

package gr.athenainnovation.imis.OSMRec;    

import com.hp.hpl.jena.ontology.OntClass;
import gr.athenainnovation.imis.OSMContainer.OSMNode;
import gr.athenainnovation.imis.OSMContainer.OSMRelation;
import gr.athenainnovation.imis.OSMContainer.OSMWay;
import gr.athenainnovation.imis.classification.TestSVM;
import gr.athenainnovation.imis.classification.TrainSVM;
import gr.athenainnovation.imis.classification.VClustering;
import gr.athenainnovation.imis.generator.BalancedVectorsMatrix;
import gr.athenainnovation.imis.generator.Cluster;
import gr.athenainnovation.imis.generator.ClusterVectors;
import gr.athenainnovation.imis.generator.InstanceVectors;
import gr.athenainnovation.imis.generator.TrainInstanceVectors;
import gr.athenainnovation.imis.parsers.MappingsParser;
import gr.athenainnovation.imis.parsers.OSMParser;
import gr.athenainnovation.imis.parsers.OccurrencesParser;
import gr.athenainnovation.imis.parsers.OntologyParser;
import gr.athenainnovation.imis.scoring.ClusteringScorer;
import gr.athenainnovation.imis.scoring.PredictionsScorer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengis.referencing.FactoryException;


//import org.openstreetmap.josm.plugins.Plugin;
//import org.openstreetmap.josm.plugins.PluginInformation;

/**
 *  Creates objects for parsing mappings file, class ontology, textual info, and the OSM input file
 *  Constructs Vectors for the OSM instances
 *  Trains/tests using SVM, Clustering or KNN methodology based on the provided arguments.
 * 
 *  @author imis-nkarag
 */

public class OSMRec{// extends Plugin{   
    
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String PATH = Paths.get("").toAbsolutePath().toString();
    private static final String TEST_SVM_OUTPUT = "testSVM";
    //private static final int COLUMN_SIZE = 3750; //vector features, mean var double values
    private static final int COLUMN_SIZE = 3811; //vector features, mean var boolean intervals
    private static int kClusters = 1;
    private static Integer averageInstancesPerCluster = null; //default: no k parameter specified
    private static int trainAlgorithm = 0;
    private static int instancesSize = 0;
    private static boolean trainMode = false;
    private static boolean testMode = false;
    private static boolean isLinux = false;
    private static boolean wrongArguments = false;
    private static Double confParameter = null;
    private static String osmFile = null;  
    private static String recommendationsFileSVM = "output.txt";
    private static String recommendationsFileClustering = "output.txt";
    private static String model = "model0";
    private static Map<String,String> mappings;
    private static Map<String,Integer> mappingsWithIDs;
    private static Map<String, List<String>> indirectClasses;
    private static Map<String, Integer> indirectClassesWithIDs;
    private static List<OSMNode> nodeList;
    private static List<OSMWay> wayList;
    private static List<OSMRelation> relationList;
    private static List<OntClass> listHierarchy;
    private static List<String> namesList;   
    private static boolean validation = true;
    
//    public OSMRec(PluginInformation info){
//        super(info);
//    }
    
    public static void main(String[] args) throws FileNotFoundException 
    {   
        
        defineOS(); //check OS type
        parseArguments(args);       
        clearFiles(); //clear files from previous execution if exist
        parseFiles(); //parse ontology, tags-to-class mappings, textual info, osm file                    
        //constructVectors(); //construct vectors for the instances of the osm input file
        
    //////////////////      Algorithm 1      //////////////////      
        if(confParameter == null && trainMode && trainAlgorithm == 1){ 
            chooseOptimalConfParameter(); //if conf parameter is not set, find best conf param            
        }
        else{  
            if(trainMode && trainAlgorithm == 1){
                trainSVM();
            }
            if(testMode && trainAlgorithm == 1){
                if(model.startsWith("/") || model.startsWith("file:///")){
                    if(new File(model).exists()){
                        testSVM();
                    }
                    else{
                        System.out.println(model + " not found");
                        System.out.println("\nTry to train a model first and then run test "
                                + "or define another path for the model to use.\n");
                        System.exit(0);
                    }
                }
                else{
                    if(new File(PATH +"/classes/output/"+ model).exists()){
                        testSVM();
                    }
                    else {
                        System.out.println(PATH +"/classes/output/"+ model + " not found");
                        System.out.println("\nTry to train a model first and then run test "
                                + "or define another path for the model to use.\n");
                        System.exit(0);
                    }
                }
            }    
        }       

    /////////////////       Algorithm 2      /////////////////
        if(averageInstancesPerCluster == null && trainMode && trainAlgorithm == 2){
            chooseOptimalNumberOfClusters();
        }
        else {  
            if(trainMode && trainAlgorithm == 2){
                trainClustering();
            }
            if(testMode && trainAlgorithm == 2){
                testClustering();
            }
        } 
        
    /////////////////       Algorithm 3      /////////////////  
        if(trainMode && trainAlgorithm == 3){
            //trainKNN();
            evaluateKNN(); //for evaluation
        }
        
        if(testMode && trainAlgorithm == 3){
            testKNN();            
        }   
        
    }
    
    private static void defineOS() {
       if(OS.contains("nux")){
           isLinux = true;
       }
       else if(OS.contains("win")){
           isLinux = false;
       }
       else{
           System.out.println("Your operating system is not supported yet :/");
           System.exit(0);
       }
    }
    
    private static void parseArguments(String[] args){
        //Double confParameter = null;
        //osmFile = null;
        String arg;
        String value;
        int i =0;
        while (i < args.length){

           arg = args[i];
           if(arg.startsWith("-")){ 
	       if(arg.equals("-help")){
		   System.out.println("Usage:\n java -jar OSMRec-1.0.jar -train trainAlgorithm -i inputFile "
                           + "[-c confParameter] [-k averageSize] [-m model]\n or\n"
			   + "java -jar OSMRec-1.0.jar -test trainAlgorithm -i inputFile [-o outputFile]\n");
		   System.out.println("Train algorithm takes values 1,2 or 3: -train algorithm \n"
                            + "1    SVM training on spatial entities as items\n"		      
			    + "2    Clustering of spatial entities and k-NN algorithm on clusters of entities\n"
                            + "3    KNN of spatial entities as items\n"
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
                       System.out.println(" - Clustering of spatial entities and k-NN algorithm on clusters of entities");
		   }
		   else if(value.equals("3")){
		       trainAlgorithm = 3;
                       System.out.println(" - KNN of spatial entities as items");
                       //wrongArguments = true;
		   }
		   else{
		       System.out.println("Train algorithm takes values 1,2 or 3: [-train algorithm] \n"
                               + "1    SVM training on spatial entities as items\n"
			       + "2    Clustering of spatial entities and k-NN algorithm on clusters of entities\n"
                               + "3    KNN of spatial entities as items\n");
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
		       System.out.println("Train algorithm takes values 1,2 or 3: [-test algorithm] \n"
                                + "1    SVM training on spatial entities as items\n"
                                + "2    Clustering of spatial entities and k-NN algorithm on clusters of entities\n"
                                + "3    KNN of spatial entities as items\n");
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
                            //kClusters = instancesSize/averageInstancesPerCluster;//number of clusters, 
                                            //based on average instances per cluster provided by the user
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
	   System.err.println("Usage:\n java -jar OSMRec-1.0.jar -train trainAlgorithm "
                    + "-i inputFile [-c confParameter] [-k averageSize] [-m model]\n or\n"
                    + "java -jar OSMRec-1.0.jar -test trainAlgorithm -i inputFile [-o outputFile]\n");
	   System.exit(0);
       }
    }

    private static void clearFiles() {        
        File folderToScan = new File(PATH + "/classes/output/");
        File[] listOfFiles = folderToScan.listFiles();
        String fileToDelete;  
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                fileToDelete = listOfFile.getName();
                if (fileToDelete.startsWith("vmatrix") || fileToDelete.startsWith("vector")) {
                    new File(PATH + "/classes/output/" + fileToDelete).delete();
                }
            }
        }       
    }    

    private static void parseFiles() {
        
        File file = new File(PATH + "/classes/mappings/Map"); 
        MappingsParser mappingsParser = new MappingsParser();
        try {   
            mappingsParser.parseFile(file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
        }
        mappings = mappingsParser.getMappings();
        mappingsWithIDs = mappingsParser.getMappingsWithIDs();           
      
        OntologyParser ontologyParser = new OntologyParser(PATH);
        ontologyParser.parseOntology();
        indirectClasses = ontologyParser.getIndirectClasses();
        indirectClassesWithIDs = ontologyParser.getIndirectClassesIDs();
        listHierarchy = ontologyParser.getListHierarchy(); 

        File namesFile = new File(PATH + "/classes/mappings/names");        
        OccurrencesParser nameOccurrencesParser = new OccurrencesParser();
        nameOccurrencesParser.parseNamesFile(namesFile);
        namesList = nameOccurrencesParser.getNamesList();
        
        String osmInputPath;
        if (osmFile.startsWith("/") || osmFile.startsWith("file:///")){  //system has already exited in case of a null osmFile
            osmInputPath = osmFile;
        }
        else{
            osmInputPath = PATH + "/"+ osmFile; 
        }
        
        OSMParser osmParser = null; 
        try {
            osmParser = new OSMParser(osmInputPath); // OSM FILE
        } catch (FactoryException ex) {
            Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
        }

        osmParser.parseDocument(); 
        nodeList = osmParser.getNodeList();
        wayList = osmParser.getWayList();
        instancesSize = wayList.size();
        if(averageInstancesPerCluster!=null){
            kClusters = instancesSize/averageInstancesPerCluster;
        }
        relationList = osmParser.getRelationList();
        
        if(instancesSize ==0){
            System.out.println("Something went wrong.. Please check the path of input osm file");
            System.exit(0);
        }
        else{
            System.out.println("The input file has " + instancesSize + " nodes.");
        }       
          
    }

    private static void constructVectors() {
        //if no c spedified && and train ==1 construct different vector files
        if(!(trainMode && confParameter == null && averageInstancesPerCluster == null && (trainAlgorithm == 1 || trainAlgorithm == 2))){

            InstanceVectors instanceVectors = new InstanceVectors(wayList, relationList, mappings, mappingsWithIDs, 
                indirectClasses, indirectClassesWithIDs, listHierarchy, nodeList, namesList, PATH + "/classes/output/vectors");
        
            instanceVectors.constructWayVectors();
        }
    }

    private static void chooseOptimalConfParameter(){
        int a;
        int b;
        
        if (validation){
            System.out.println("\n\n\nEvaluating VALIDATION set!\n" );
            a = 2;
            b = 3;
        }
        else{
            System.out.println("\n\n\nEvaluating TEST set!\n" );
            a = 3;
            b = 4;
        }
        //custom values for choosing optimal c parameter
        Double[] confParams = new Double[] {1.0, 5.0, 10.0, 20.0, 30.0, 50.0, 100.0, 150.0, 
            200.0, 300.0, 400.0, 500.0, 1000.0, 1500.0, 5000.0, 10000.0, 40000.0, 60000.0, 100000.0, 120000.0, 150000.0,170000.0, 200000.0};
        //Double[] confParams = new Double[] {60000.0, 170000.0};
        String bestModel = model;
        String makePath;
        int i = 0;
        float bestScore = 100;        
        double bestConfParam = 1.0;   //the first of the chosen values 
        
        int testSize = instancesSize/5;

        List<OSMWay> trainList= new ArrayList<>();
        for(int g = 0; g<5*testSize; g++){ 
            if(g == 2*testSize){g=4*testSize;}
            trainList.add(wayList.get(g));
        }
        List<OSMWay> testList= new ArrayList<>();
        for(int g = a*testSize; g<b*testSize; g++){
            testList.add(wayList.get(g));
        }

        InstanceVectors instanceVectorsTrain = new InstanceVectors(trainList, relationList, mappings, mappingsWithIDs, 
            indirectClasses, indirectClassesWithIDs, listHierarchy, nodeList, namesList, PATH + "/classes/output/vectorsTrain");

        instanceVectorsTrain.constructWayVectors();

        InstanceVectors instanceVectorsTest = new InstanceVectors(testList, relationList, mappings, mappingsWithIDs, 
            indirectClasses, indirectClassesWithIDs, listHierarchy, nodeList, namesList, PATH + "/classes/output/vectorsTest", true);

        instanceVectorsTest.constructWayVectors();
        
        if(isLinux){
            makePath = PATH.replace("/target", "");
        }
        else{
            makePath = PATH.replace("\\target","");    
        }
        
        for (Double confParam : confParams) {
            
            System.out.println("\n-c = " + confParam);
            TrainSVM trainSVM = new TrainSVM(makePath, PATH + "/classes/output/vectorsTrain", confParam, model+i, isLinux);
            trainSVM.executeTrain();

            TestSVM testSVM = new TestSVM(makePath, PATH + "/classes/output/vectorsTest", model+i, TEST_SVM_OUTPUT, isLinux);
            testSVM.executeTest();             
            
            float score = testSVM.getScore();
            if(score < bestScore){
                bestScore = score;
                bestModel = model+i;
                bestConfParam = confParam;
            }
            
            PredictionsScorer ps1 = new PredictionsScorer();    
            ps1.computeScore(new File(PATH + "/classes/output/" + TEST_SVM_OUTPUT), new File(PATH + "/classes/output/vectorsTest"), 1);
            
            if(!validation){
                PredictionsScorer ps5 = new PredictionsScorer();    
                ps5.computeScore(new File(PATH + "/classes/output/" + TEST_SVM_OUTPUT), new File(PATH + "/classes/output/vectorsTest"), 5);

                PredictionsScorer ps10 = new PredictionsScorer();    
                ps10.computeScore(new File(PATH + "/classes/output/" + TEST_SVM_OUTPUT), new File(PATH + "/classes/output/vectorsTest"), 10);

            }
            i++;
        }             
        
        //TrainSVM trainSVM = new TrainSVM(makePath, PATH + "/classes/output/vectorsTrain", bestConfParam, bestModel, isLinux);
        //trainSVM.executeTrain();  //final training with optimal c parameter
        
        System.out.println("Best model produced is the file \"" + bestModel +"\", with conf parameter \"-c " 
                                                                                        + bestConfParam + "\"");
        System.out.println("Define this parameter to use the best model for SVM test: \"-m " + bestModel + "\"");       
       
        if(validation == true){
            validation = false;
            chooseOptimalConfParameter(); //call again to evaluate test set for all score categories
        }
    }
    
    private static void trainSVM() {     
        //training the model using SVM multiclass
        String makePath;
        if(isLinux){
            makePath = PATH.replace("/target", "");
        }
        else{
            makePath = PATH.replace("\\target","");    
        }
        TrainSVM trainSVM = new TrainSVM(makePath, PATH + "/classes/output/vectors", confParameter, model, isLinux);
        trainSVM.executeTrain();
    }
    
    private static void testSVM(){
        //testing the input osm file, using the model produced from the training process.
        String makePath;
        if(isLinux){
        makePath = PATH.replace("/target", "");
        }
        else{
        makePath = PATH.replace("\\target","");    
        }

        TestSVM testSVM = new TestSVM(makePath, PATH + "/classes/output/vectors", model, TEST_SVM_OUTPUT, isLinux);
        testSVM.executeTest();

        //this file contains the vectors produced from the test set
        File vectorsOutputFile = new File(PATH + "/classes/output/vectors");

        //this file contains the predictions from svm classify
        File svmPredictionsOutputFile = new File(PATH + "/classes/output/" + TEST_SVM_OUTPUT); 

        SVMRecommender recommender = new SVMRecommender();
        recommender.recommend(recommendationsFileSVM, svmPredictionsOutputFile, vectorsOutputFile, 
                                                                            mappingsWithIDs, wayList, PATH);
    }
    
    private static void chooseOptimalNumberOfClusters(){
        int a;
        int b;
        
        if (validation){
            System.out.println("\n\n\nEvaluating VALIDATION set!\n" );
            a = 2;
            b = 3;           
        }
        else{
            System.out.println("\n\n\nEvaluating TEST set!\n" );
            a = 3;
            b = 4;
        }
        float bestScore = 100; //worst possible score, because score represents classification error
        //int optimalClusters = 70;
        int bestK = 75;
        Integer[] averageInstances = new Integer[] {75,70,65,60,55,50,45,40,35,30,25,20,15};
        //Integer[] averageInstances = new Integer[] {45,40,35,30,25,20,15};
        int trainSize = 3*instancesSize/5; //trainSet is 3/5 of the instances
        int testSize = instancesSize/5;   //testSet is the 1/5 of the instances, one for test and one for validate set.
        
        List<OSMWay> trainList= new ArrayList<>();
        for(int g = 0; g<5*testSize; g++){  
            if(g==2*testSize){g=4*testSize;}
            trainList.add(wayList.get(g));
        }
        List<OSMWay> testList= new ArrayList<>();
        for(int g = a*testSize; g<b*testSize; g++){
            testList.add(wayList.get(g)); //construct without class and relation feats
        }
        
        InstanceVectors instanceVectorsTrain = new InstanceVectors(trainList, relationList, mappings, mappingsWithIDs, 
            indirectClasses, indirectClassesWithIDs, listHierarchy, nodeList, namesList, PATH + "/classes/output/vectorsTrain");

        instanceVectorsTrain.constructWayVectors();

        InstanceVectors instanceVectorsTest = new InstanceVectors(testList, relationList, mappings, mappingsWithIDs, 
            indirectClasses, indirectClassesWithIDs, listHierarchy, nodeList, namesList, PATH + "/classes/output/vectorsTest", true);

        instanceVectorsTest.constructWayVectors();
        
        for(Integer k : averageInstances){       
            
            String vectorMatrixOutputFile = PATH + "/classes/output/vmatrix";
            BalancedVectorsMatrix balancedVectorsMatrix = new BalancedVectorsMatrix(trainList, 
                                                                                vectorMatrixOutputFile, COLUMN_SIZE);
            balancedVectorsMatrix.generateBalancedVectorsMatrix();  
            String makePath;
            if(isLinux){
                makePath = PATH.replace("/target", "");
            }
            else{
                makePath = PATH.replace("\\target","");    
            }             

            VClustering vCluster = new VClustering();
            vCluster.executeClusteringProcess(makePath, (trainSize/k), isLinux);

            //train average cluster vectors and save them to a file. 
            //This file will be used to classify new osm instances in a cluster
            String clusterSolution = PATH + "/classes/output/vmatrix.mat.clustering." + (trainSize/k);

            ClusterVectors clusterVectors = new ClusterVectors(trainList, clusterSolution);
            clusterVectors.produceClusterVectors();

            //avoiding serialization here by getting average vectors directly from the produced clusterVectors
            //ArrayList<Cluster> trainedAverageVectors = clusterVectors.getAverageClusterVectors(); 
            ArrayList<Cluster> trainedAverageVectors = clusterVectors.getIndexAverageClusterVectors();
            ClusteringScorer cs1 = new ClusteringScorer(testList, trainedAverageVectors, mappingsWithIDs, 1);    
            cs1.score();
            
            if(!validation){
                ClusteringScorer cs5 = new ClusteringScorer(testList, trainedAverageVectors, mappingsWithIDs, 5);    
                cs5.score();

                ClusteringScorer cs10 = new ClusteringScorer(testList, trainedAverageVectors, mappingsWithIDs, 10);    
                cs10.score();
            }
            
            float score = cs1.getScore();               
            if(score < bestScore){ //score represents the classification error
                bestScore = score;
                //optimalClusters = (trainSize/k);
                bestK = k;
            }

            //serialize average vectors with the best k parameter
            //commented out for evaluation
//            try (FileOutputStream fileOut = new FileOutputStream(PATH + "/classes/mappings/averageClusterVectors.ser"); 
//                ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
//                out.writeObject(trainedAverageVectors);
//
//            }
//            catch(IOException e){
//                System.out.println("serialize error:\n " + e);
//            }
        }
        System.out.println("Best value for average instances per cluster is " + bestK);
        System.out.println("Try to test defining -k " + bestK);
        
        if(validation == true){
            validation = false;
            chooseOptimalNumberOfClusters(); //call again to evaluate test set for all score categories
        }
    }    

    private static void trainClustering() {
        //produces a matrix for the vcluster process       
        String vectorMatrixOutputFile = PATH + "/classes/output/vmatrix";
        BalancedVectorsMatrix balancedVectorsMatrix = new BalancedVectorsMatrix(wayList, 
                                                                        vectorMatrixOutputFile, COLUMN_SIZE);
        balancedVectorsMatrix.generateBalancedVectorsMatrix();  

        String makePath;
        if(isLinux){
        makePath = PATH.replace("/target", "");
        }
        else{
        makePath = PATH.replace("\\target","");    
        }

        VClustering vCluster = new VClustering();
        vCluster.executeClusteringProcess(makePath, kClusters, isLinux);

        //train average cluster vectors and save them to a file. 
        //This file will be used to classify new osm instances in a cluster
        String clusterSolution = PATH + "/classes/output/vmatrix.mat.clustering." + kClusters;

        ClusterVectors clusterVectors = new ClusterVectors(wayList, clusterSolution);
        clusterVectors.produceClusterVectors();

        //serialize average cluster vectors to file.
        try (FileOutputStream fileOut = new FileOutputStream(PATH + "/classes/mappings/averageClusterVectors.ser");                         
            ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            ArrayList<Cluster> averageVectors = clusterVectors.getIndexAverageClusterVectors();   
            out.writeObject(averageVectors);
        }
        catch(IOException e){
            System.out.println("serialize " + e);
        }
    }
    
    private static void testClustering() {       
        ArrayList<Cluster> trainedAverageVectors = null;

        //obtain serialized average cluster vectors from file.
        try{   
            try (FileInputStream fileIn = new FileInputStream(PATH + "/classes/mappings/averageClusterVectors.ser"); 
                ObjectInputStream in = new ObjectInputStream(fileIn)) {
                trainedAverageVectors = (ArrayList<Cluster>) in.readObject();
            }
        }
        catch(IOException | ClassNotFoundException e){//
            System.err.println("Something went wrong.. Try to train a model first and then "
                    + "test it with the same average instances per cluster!\n\n" +e);
        }
        ClusteringRecommender clusterRecommender = new ClusteringRecommender(wayList, trainedAverageVectors, 
                                                        mappingsWithIDs, PATH, recommendationsFileClustering);
        clusterRecommender.recommendClasses();
    }

    private static void trainKNN() {
        
        TrainInstanceVectors trainInstanceVectors = new TrainInstanceVectors(wayList, relationList, mappings, mappingsWithIDs, 
                                indirectClasses, indirectClassesWithIDs, listHierarchy, nodeList, namesList, PATH);

        trainInstanceVectors.trainVectors();
        //serialize average cluster vectors to file.
        try (FileOutputStream fileOut = new FileOutputStream(PATH + "/classes/mappings/instanceVectors.ser"); 
            ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(wayList);
        }
        catch(IOException e){
            System.out.println("serialize " + e);
        }
    }

    private static void testKNN() {
        ArrayList<OSMWay> trainedList = null;
        //obtain serialized average cluster vectors from file.
        try{   
            try (FileInputStream fileIn = new FileInputStream(PATH + "/classes/mappings/instanceVectors.ser"); 
                ObjectInputStream in = new ObjectInputStream(fileIn)) {
                trainedList = (ArrayList<OSMWay>) in.readObject();
            }
        }
        catch(IOException | ClassNotFoundException e){//
            System.out.println("deserialize failure\n" + e);
            System.err.println("Something went wrong.. Try to train a model first and then "
                    + "test it with the test set instances!");
        }
        KNN knn = new KNN(wayList, mappingsWithIDs, trainedList, 1);
        knn.recommendClasses(); 
    }
    
    //for dev
    private static void evaluateKNN(){
        int a;
        int b;
        
        if (validation){
            System.out.println("\n\n\nEvaluating VALIDATION set!\n" );
            a = 2;
            b = 3;
        }
        else{
            System.out.println("\n\n\nEvaluating TEST set!\n" );
            a = 3;
            b = 4;
        }
        //int trainSize = 3*instancesSize/5; //trainSet is 3/5 of the instances
        int testSize = instancesSize/5;   //testSet is the 1/5 of the instances, one for test and one for validate set.
        
        List<OSMWay> trainList= new ArrayList<>();
        for(int g = 0; g<5*testSize; g++){  // 0~~1~~2~~3~~4~~5
            if(g == 2*testSize){g=4*testSize;}
            trainList.add(wayList.get(g));
        }
        List<OSMWay> testList= new ArrayList<>();
        for(int g = a*testSize; g<b*testSize; g++){
            testList.add(wayList.get(g));
        }
        
        //train
        TrainInstanceVectors trainInstanceVectors = new TrainInstanceVectors(trainList, relationList, mappings, mappingsWithIDs, 
                                indirectClasses, indirectClassesWithIDs, listHierarchy, nodeList, namesList, PATH);

        trainInstanceVectors.trainVectors();
        //avoiding serialization

        InstanceVectors instanceVectorsTest = new InstanceVectors(testList, relationList, mappings, mappingsWithIDs, 
            indirectClasses, indirectClassesWithIDs, listHierarchy, nodeList, namesList, PATH + "/classes/output/vectorsTest", true);

        instanceVectorsTest.constructWayVectors();
        
        KNN knn1 = new KNN(testList, mappingsWithIDs, trainList, 1);
        knn1.recommendClasses();
        
        if(!validation){
            KNN knn5 = new KNN(testList, mappingsWithIDs, trainList, 5);
            knn5.recommendClasses();
            KNN knn10 = new KNN(testList, mappingsWithIDs, trainList, 10);
            knn10.recommendClasses();
        }
        
        if(validation == true){
            validation = false;
            evaluateKNN(); //call again to evaluate test set for all score categories
        }
        
    }
}
