package gr.athenainnovation.imis.osmreccli;

import com.cybozu.labs.langdetect.LangDetectException;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import de.bwaldvogel.liblinear.*;
import gr.athenainnovation.imis.osmreccli.parsers.ArgumentsParser;
import gr.athenainnovation.imis.osmreccli.features.GeometryFeatures;
import gr.athenainnovation.imis.osmreccli.container.OSMRelation;
import gr.athenainnovation.imis.osmreccli.container.OSMWay;
import gr.athenainnovation.imis.osmreccli.features.ClassFeatures;
import gr.athenainnovation.imis.osmreccli.features.OSMClassification;
import gr.athenainnovation.imis.osmreccli.features.RelationFeatures;
import gr.athenainnovation.imis.osmreccli.features.TextualFeatures;
import gr.athenainnovation.imis.osmreccli.parsers.OSMParser;
import gr.athenainnovation.imis.osmreccli.parsers.Ontology;
import gr.athenainnovation.imis.osmreccli.parsers.Statistics;
import gr.athenainnovation.imis.osmreccli.parsers.Mapper;
import gr.athenainnovation.imis.osmreccli.extractor.Analyzer;
import gr.athenainnovation.imis.osmreccli.extractor.LanguageDetector;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengis.referencing.FactoryException;
import org.apache.lucene.queryparser.classic.ParseException;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 
 * @author imis-nkarag
 */

public class OSMRecCLI {
    
    private static final String SEPARATOR = System.lineSeparator();
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isLinux = false;
    private static final String PATH = Paths.get("").toAbsolutePath().toString();
    private static String osmFilename = null;
    private static int numberOfTrainingInstances;
    private static boolean trainMode;
    private static List<OSMWay> wayList;
    private static Map<String,String> mappings;
    private static List<OSMRelation> relationList;
    private static HashMap<String, Integer> mapperWithIDs;
    private static Map<String, List<String>> indirectClasses;
    private static Map<String, Integer> indirectClassesWithIDs;
    //private static List<OntClass> listHierarchy;
    private static List<String> namesList;
    //private static int instancesSize;
    private static String osmFilePath;
    private static final boolean netbeans = false;
    private static double score1 = 0;
    private static double score5 = 0;
    private static double score10 = 0;
    private static double foldScore1 = 0;
    private static double foldScore5 = 0;
    private static double foldScore10 = 0;
    private static double bestScore = 0;
    //private static int totalVectors;
    private static String modelFilePath;
    private static String outputFilePath;    
    private static LanguageDetector languageDetector;
    private static final boolean USE_CLASS_FEATURES = false ;
    private static final boolean USE_RELATION_FEATURES = false;
    private static final boolean USE_TEXTUAL_FEATURES = true;
    private static int numberOfFeatures;

    public static void main(String[] args) throws FileNotFoundException, IOException, FactoryException, ParseException, LangDetectException {

        defineOS();
    
        ArgumentsParser initialiseArguments = new ArgumentsParser();
        ArgumentsParser parsedArguments = initialiseArguments.parseArguments(args);
        
        osmFilename = parsedArguments.getOsmFile();
        System.out.println("filename: " + osmFilename);
    
        //String osmFilePath;
        if (osmFilename.startsWith("/") || osmFilename.startsWith("file:///")){  //system has already exited in case of a null osmFile
            osmFilePath = osmFilename;
            //System.out.println("path1: " + osmFilename);
        }
        else{
            osmFilePath = PATH + "/"+ osmFilename; 
            //System.out.println("path2: " + osmFilename);
        }
        
        modelFilePath = parsedArguments.getModel();
        trainMode = parsedArguments.isTrainMode();
        outputFilePath = parsedArguments.getRecommendationsFileSVM();
        
        if(outputFilePath == null){
            if(netbeans){
                outputFilePath = PATH + "/src/main/resources/output.txt";
                System.out.println("using SVM model: " + PATH + "/src/main/resources/output.txt");
            }
            else{
                outputFilePath = PATH + "/classes/output.txt";
                System.out.println("using SVM model: " + PATH + "/classes/output.txt");
            }
        }

        if(netbeans){

                languageDetector = LanguageDetector.getInstance(PATH +"/src/main/resources/profiles");

        } else{
                languageDetector = LanguageDetector.getInstance(PATH +"/classes/profiles");

        }
        languageDetector = LanguageDetector.getInstance(PATH +"/src/main/resources/profiles");
        extractTextualList();
        parseFiles();
        
        if(numberOfTrainingInstances ==0){
            System.out.println("Something went wrong.. Please check the path of input osm file");
            System.exit(0);
        }
        else{
            System.out.println("The input file has " + numberOfTrainingInstances + " nodes.");
        }
        
        if(trainMode){
            //run training
            trainSVMModel(parsedArguments);
        }
        else{
            //run test
            testSVM();
        }
    } 
    
    private static void crossValidateFold(int a, int b, int c, int d, boolean skip, double param, boolean validateSet, int e, int f) 
            throws IOException, LangDetectException, ParseException{  

        int testSize = wayList.size()/5;

        List<OSMWay> trainList= new ArrayList<>();
        for(int g = a*testSize; g<b*testSize; g++){  // 0~~1~~2~~3~~4~~5
            if(skip){
                if(g == (c)*testSize){g=(c+2)*testSize;}
            
            }
            trainList.add(wayList.get(g)); 
        }                           
            
        int wayListSizeWithoutUnclassified = trainList.size(); 
        //int u = 0;
        System.out.println("trainList size: " + wayListSizeWithoutUnclassified);       
        
        //set classes for each osm instance
        int sizeToBeAddedToArray = 0; //this will be used to proper init the features array, adding the multiple vectors size 
        //int lalala = 0;
        for(OSMWay way : trainList){  
            //ClassFeatures class_vector = new ClassFeatures();
            //class_vector.createClassFeatures(way, mappings, mapperWithIDs, indirectClasses, indirectClassesWithIDs);

            OSMClassification classifyInstances = new OSMClassification();
            classifyInstances.calculateClasses(way, mappings, mapperWithIDs, indirectClasses, indirectClassesWithIDs);

            if(way.getClassIDs().isEmpty()){
                //System.out.println("found unclassified" + way.getClassIDs() + "class: " +way.getClassID());
                wayListSizeWithoutUnclassified = wayListSizeWithoutUnclassified-1;
                //u++;
            }
            else {
                sizeToBeAddedToArray = sizeToBeAddedToArray + way.getClassIDs().size()-1;
            }               
        }
  
        double C = param;
        double eps = 0.01; 
        double[] GROUPS_ARRAY2 = new double[wayListSizeWithoutUnclassified+sizeToBeAddedToArray];//new double[117558];//

        FeatureNode[][] trainingSetWithUnknown2 = new FeatureNode[wayListSizeWithoutUnclassified+sizeToBeAddedToArray][110]; //working[3812];//610
        int k =0;       

        /*extract info about the produced number of vectors
        System.out.println("training vectors number:\n" + (wayListSizeWithoutUnclassified+sizeToBeAddedToArray));
        System.out.println("total: " + wayList.size());
        System.out.println("without unclassified: " + wayListSizeWithoutUnclassified);
        System.out.println("vectors to be added from multiple classes:" + sizeToBeAddedToArray);
        */
        for(OSMWay way : trainList){
            //adding multiple vectors                    
            int id;
            
            if(USE_CLASS_FEATURES){                
                ClassFeatures class_vector = new ClassFeatures();
                class_vector.createClassFeatures(way, mappings, mapperWithIDs, indirectClasses, indirectClassesWithIDs);
                id = 1422;
            }
            else{
                id = 1;
            }
            //System.out.println("(train) starting id="+id);
            //pass id also: 1422 if using classes, 1 if not
            GeometryFeatures geometryFeatures = new GeometryFeatures(id);
            geometryFeatures.createGeometryFeatures(way);
            id = geometryFeatures.getLastID();
            //id after geometry, cases: all geometry features with mean-variance boolean intervals:
            //id = 1526
            //System.out.println("id 1526 -> " + geometryFeatures.getLastID());
            //System.out.println("(train) after geometry id="+id);
            if(USE_RELATION_FEATURES){
                RelationFeatures relationFeatures = new RelationFeatures(id);
                relationFeatures.createRelationFeatures(way, relationList);    
                id = relationFeatures.getLastID();
            }
            else {
                id = geometryFeatures.getLastID();
                //System.out.println("geom feat " + id);
            }
            //System.out.println("(train) after relations id="+id);
            //System.out.println("id 1532 -> " + relationFeatures.getLastID());
            
            TextualFeatures textualFeatures;
            if(USE_TEXTUAL_FEATURES){
                textualFeatures = new TextualFeatures(id, namesList, languageDetector);
                textualFeatures.createTextualFeatures(way);
                //System.out.println("last textual id: " + textualFeatures.getLastID());
                //System.out.println("full:  " + way.getFeatureNodeList());
            }
            
            //System.out.println("(train) after textual id=" + textualFeatures.getLastID());           
            List<FeatureNode> featureNodeList = way.getFeatureNodeList();
            //System.out.println(featureNodeList);
            FeatureNode[] featureNodeArray = new FeatureNode[featureNodeList.size()];           
            
            if(!way.getClassIDs().isEmpty()){
                int i =0;
                //int lastIndex1 = 0;
                for(FeatureNode featureNode : featureNodeList){
                    
                    featureNodeArray[i] = featureNode;
                    //System.out.println("i: " + i + " array:" + featureNodeArray[i]);
                    i++;
                    //lastIndex1 = featureNode.getIndex();
                }
                
                for(int classID : way.getClassIDs()){
                    trainingSetWithUnknown2[k] = featureNodeArray;
                    GROUPS_ARRAY2[k] = classID;
                    k++;                   
                }                                
            }
            //System.out.println("feature vector : " + Arrays.toString(trainingSetWithUnknown2[k]));
            //System.out.println("array length: " + featureNodeArray.length);
            //System.out.println("trainingSetWithUnknown2: " + Arrays.toString(trainingSetWithUnknown2[k]));
            //System.out.println(k+ " vector size: " + listForArray.size());
            //System.out.println("feat length" + train1.length);
            //groupArrayList.add(way.getClassID());
            //Set<Integer> laa = way.getClassIDs();           
            //System.out.println("class: " + GROUPS_ARRAY2[k]);            
        }
        
        //Linear.enableDebugOutput();
        Problem problem = new Problem();  
        problem.l = wayListSizeWithoutUnclassified+sizeToBeAddedToArray;//wayListSizeWithoutUnclassified;//wayList.size();
        problem.n = numberOfFeatures;//3797; // number of features //the largest index of all features //3811;//3812 //1812 with classes //1110 //110
        problem.x = trainingSetWithUnknown2; // feature nodes
        problem.y = GROUPS_ARRAY2; // target values
        //SolverType solver2 = SolverType.MCSVM_CS; //Cramer and Singer for multiclass classification - equivalent of SVMlight
        SolverType solver2 = SolverType.getById(2); //2 -- L2-regularized L2-loss support vector classification (primal)
        //System.out.println("must add to arrays" + lalala);
        //System.out.println("array size is " + (wayListSizeWithoutUnclassified+sizeToBeAddedToArray));
        //System.out.println("array size original " + (wayListSizeWithoutUnclassified));       
        
        Parameter parameter = new Parameter(solver2, C, eps);
        //System.out.println("param set ok");
        //System.out.println("number of features: " + vc.getNumOfFeatures());       
        
        long start = System.nanoTime();
        System.out.println("training...");
        PrintStream original = System.out;
        System.setOut(new PrintStream(new OutputStream() {

           @Override
           public void write(int arg0) throws IOException {

           }
        }));
        
        Model model = Linear.train(problem, parameter);
        long end = System.nanoTime();
        Long elapsedTime = end-start;
        System.setOut(original);
        System.out.println("training process completed in: " + NANOSECONDS.toSeconds(elapsedTime) + " seconds.");

        File modelFile;
        if(netbeans){
            modelFile = new File(PATH + "/src/main/resources/liblinear_model");
        }
        else{
            modelFile = new File(PATH + "/classes/liblinear_model");
        }
        model.save(modelFile);
        
        //end of training   
        
        List<OSMWay> testList;
        if(validateSet){
            //validate set 
            testList= new ArrayList<>();
            for(int g = c*testSize; g<d*testSize; g++){
                testList.add(wayList.get(g));                     
            }
        }
        else {
            //test set: running only for the best c parameter
            testList= new ArrayList<>();
            for(int g = (e)*testSize; g<(f)*testSize; g++){
                testList.add(wayList.get(g));                      
            }            
        }
        System.out.println("testList size: " + testList.size());
        int succededInstances=0;
        int succededInstances5=0;
        int succededInstances10=0;
        model = Model.load(modelFile);
        int modelLabelSize = model.getLabels().length;
        int[] labels = model.getLabels();
        Map<Integer, Integer> mapLabelsToIDs = new HashMap<>();
        for(int h =0; h < model.getLabels().length; h++){
            mapLabelsToIDs.put(labels[h], h);
            //System.out.println(h + "   <->    " + labels[h]);
        }

        int wayListSizeWithoutUnclassified2 = testList.size();
        for(OSMWay way : testList){

            OSMClassification classifyInstances = new OSMClassification();
            classifyInstances.calculateClasses(way, mappings, mapperWithIDs, indirectClasses, indirectClassesWithIDs);
            if(way.getClassIDs().isEmpty()){
                //System.out.println("found unclassified" + way.getClassIDs() + "class: " +way.getClassID());
                wayListSizeWithoutUnclassified2 -= 1;
                //wayListSizeWithoutUnclassified2 = wayListSizeWithoutUnclassified2-1;
                //u++;
            }
        }          
        
        FeatureNode[] testInstance2;
        for(OSMWay way : testList){           
            int id;
            
            if(USE_CLASS_FEATURES){                
                ClassFeatures class_vector = new ClassFeatures();
                class_vector.createClassFeatures(way, mappings, mapperWithIDs, indirectClasses, indirectClassesWithIDs);
                id = 1422;
            }
            else{
                id = 1;
            }
            
            //pass id also: 1422 if using classes, 1 if not
            GeometryFeatures geometryFeatures = new GeometryFeatures(id);
            geometryFeatures.createGeometryFeatures(way);
            id = geometryFeatures.getLastID();
            //id after geometry, cases: all geometry features with mean-variance boolean intervals:
            //id = 1526
            //System.out.println("id 1526 -> " + geometryFeatures.getLastID());
            if(USE_RELATION_FEATURES){
                RelationFeatures relationFeatures = new RelationFeatures(id);
                relationFeatures.createRelationFeatures(way, relationList);       
                id = relationFeatures.getLastID();
            }
            else {
                id = geometryFeatures.getLastID();
                //System.out.println("geom feat " + id);
            }
            //id 1531
            //System.out.println("id 1532 -> " + relationFeatures.getLastID());
            if(USE_TEXTUAL_FEATURES){
                TextualFeatures textualFeatures = new TextualFeatures(id, namesList, languageDetector);
                textualFeatures.createTextualFeatures(way);
                //System.out.println("last textual id: " + textualFeatures.getLastID());
                //System.out.println("full:  " + way.getFeatureNodeList());
            }
            List<FeatureNode> featureNodeList = way.getFeatureNodeList();

            FeatureNode[] featureNodeArray = new FeatureNode[featureNodeList.size()];

            int i = 0;
            for(FeatureNode featureNode : featureNodeList){
                //if(la.getIndex() > lastIndex1){
                    featureNodeArray[i] = featureNode;
                    //System.out.println("i: " + i + " array:" + featureNodeArray[i]);
                    i++;
                //}
            }
            
            //System.out.println("array length: " + featureNodeArray.length);
            //System.out.println("trainingSetWithUnknown2: " + Arrays.toString(trainingSetWithUnknown2[k]));
            //System.out.println(k+ " vector size: " + listForArray.size());
            //System.out.println("feat length" + train1.length);
            //groupArrayList.add(way.getClassID());
            //Set<Integer> laa = way.getClassIDs();            
            //System.out.println("class: " + GROUPS_ARRAY2[k]);
            testInstance2 = featureNodeArray;
            //double prediction = Linear.predict(model, testInstance2);
            //System.out.println("test prediction: " + prediction);
            double[] scores = new double[modelLabelSize];
            Linear.predictValues(model, testInstance2, scores);
            //Double max = Collections.max(Arrays.asList(ArrayUtils.toObject(scores)));
            //System.out.println("predict values: " + Arrays.toString(scores));
            
            //find index of max values in scores array: predicted classes are the elements of these indexes from array model.getlabels
            //iter scores and find 10 max values with their indexes first. then ask those indexes from model.getlabels
            Map<Double, Integer> scoresValues = new HashMap<>();
            for(int h = 0; h < scores.length; h++){
                scoresValues.put(scores[h], h);               
                //System.out.println(h + "   <->    " + scores[h]);
            }            

            Arrays.sort(scores);
            //System.out.println("max value: " + scores[scores.length-1] + " second max: " + scores[scores.length-2]);
            //System.out.println("ask this index from labels: " + scoresValues.get(scores[scores.length-1]));
            //System.out.println("got from labels: " +  labels[scoresValues.get(scores[scores.length-1])]);
            //System.out.println("next prediction: " +  labels[scoresValues.get(scores[scores.length-2])]);
            //System.out.println("way labels: " + way.getClassIDs());
            //System.out.println("test prediction: " + prediction);
            if(way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-1])])){
                succededInstances++;              
            }
            if(                    
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-1])]) || 
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-2])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-3])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-4])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-5])])
              ){
                succededInstances5++;
            }
            if(                    
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-1])]) || 
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-2])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-3])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-4])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-5])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-6])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-7])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-8])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-9])]) ||
                way.getClassIDs().contains(labels[scoresValues.get(scores[scores.length-10])])
              ){
                succededInstances10++;
            }
            //System.out.println("labels: " + Arrays.toString(model.getLab els()));
            //System.out.println("label[0]: " + model.getLabels()[0]);           
        }
        if(validateSet){
            System.out.println("Validation Set:");
        }
        else{
            System.out.println("Test Set:");
        }
        System.out.println("Succeeded " + succededInstances + " of " + wayListSizeWithoutUnclassified2 + " total (1 class prediction)");
        double precision1 = succededInstances/(double)wayListSizeWithoutUnclassified2;
        score1 = precision1;
        System.out.println(precision1);
        
        System.out.println("Succeeded " + succededInstances5 + " of " + wayListSizeWithoutUnclassified2+ " total (5 class prediction)");
        double precision5 = succededInstances5/(double)wayListSizeWithoutUnclassified2;
        score5 = precision5;
        System.out.println(precision5);
        
        System.out.println("Succeeded " + succededInstances10 + " of " + wayListSizeWithoutUnclassified2+ " total (10 class prediction)");
        double precision10 = succededInstances10/(double)wayListSizeWithoutUnclassified2;
        score10 = precision10;
        System.out.println(precision10);              
    }    
    
    private static void trainSVMModel(ArgumentsParser parsedArguments) throws IOException, LangDetectException, ParseException{
                
        double bestC;
        if(parsedArguments.getConfParameter() != null){
            bestC = parsedArguments.getConfParameter();
            //System.out.println("train with c param= " + bestC);
        }
        else{ 
            bestC = Math.pow(2, -10);
        
            //trainModel();
            //Double[] confParams = new Double[] {1.0, 2.0, 4.0, 8.0, 16.0, 32.0};
            Double[] confParams = new Double[] {Math.pow(2, -10), Math.pow(2, -5),Math.pow(2, -3),Math.pow(2, -1),
                Math.pow(2, 0),Math.pow(2, 1),Math.pow(2, 2),Math.pow(2, 3),Math.pow(2, 4),Math.pow(2, 5),Math.pow(2, 6),
                Math.pow(2, 8),Math.pow(2, 10),Math.pow(2, 12),Math.pow(2, 14),Math.pow(2, 15),Math.pow(2, 16),Math.pow(2, 17)};

            //validate set
            System.out.println("\n\nValidation Set:\n");
            for(Double param : confParams){
                //totalVectors = 0;
                foldScore1 = 0;
                foldScore5 = 0;
                foldScore10 = 0;
                System.out.println("\nrunning for C = " + param);
                clearDataset();
                System.out.println("fold1");
                crossValidateFold(0, 3, 3, 4, false, param, true, 0, 0);
                foldScore1 = foldScore1 + score1;
                foldScore5 = foldScore5 + score5;
                foldScore10 = foldScore10 + score10;
                clearDataset();
                System.out.println("fold2");
                crossValidateFold(1, 4, 4, 5, false, param, true, 0, 0);
                foldScore1 = foldScore1 + score1;
                foldScore5 = foldScore5 + score5;
                foldScore10 = foldScore10 + score10;
                clearDataset();
                System.out.println("fold3");
                crossValidateFold(2, 5, 0, 1, false, param, true, 0, 0);
                foldScore1 = foldScore1 + score1;
                foldScore5 = foldScore5 + score5;
                foldScore10 = foldScore10 + score10;
                clearDataset();
                System.out.println("fold4");
                crossValidateFold(0, 5, 1, 2, true, param, true, 0, 0);  
                foldScore1 = foldScore1 + score1;
                foldScore5 = foldScore5 + score5;
                foldScore10 = foldScore10 + score10;
                clearDataset();
                System.out.println("fold5");
                crossValidateFold(0, 5, 2, 3, true, param, true, 0, 0);
                foldScore1 = foldScore1 + score1;
                foldScore5 = foldScore5 + score5;
                foldScore10 = foldScore10 + score10;
                System.out.println("\n\nC=" + param + ", average score 1-5-10: " + foldScore1/5 +" "+ foldScore5/5 + " "+ foldScore10/5);
                if(bestScore < foldScore1 ){
                    bestScore = foldScore1;
                    bestC = param;
                }
            }
            System.out.println("best c param= " + bestC + ", score: " + bestScore/5 );
        }

        //run/score test set with the best c parameter
        //totalVectors = 0;
        foldScore1 = 0;
        foldScore5 = 0;
        foldScore10 = 0;
        System.out.println("\n\nTest Set\nrunning for best C = " + bestC);
        clearDataset();
        System.out.println("fold1");
        crossValidateFold(0, 3, 3, 4, false, bestC, false, 4, 5);
        foldScore1 = foldScore1 + score1;
        foldScore5 = foldScore5 + score5;
        foldScore10 = foldScore10 + score10;
        clearDataset();
        System.out.println("fold2");
        crossValidateFold(1, 4, 4, 5, false, bestC, false, 0, 1);
        foldScore1 = foldScore1 + score1;
        foldScore5 = foldScore5 + score5;
        foldScore10 = foldScore10 + score10;
        clearDataset();
        System.out.println("fold3");
        crossValidateFold(2, 5, 0, 1, false, bestC, false, 1, 2);
        foldScore1 = foldScore1 + score1;
        foldScore5 = foldScore5 + score5;
        foldScore10 = foldScore10 + score10;
        clearDataset();
        System.out.println("fold4");
        crossValidateFold(0, 5, 1, 2, true, bestC, false, 2, 3);  
        foldScore1 = foldScore1 + score1;
        foldScore5 = foldScore5 + score5;
        foldScore10 = foldScore10 + score10;
        clearDataset();
        System.out.println("fold5");
        crossValidateFold(0, 5, 2, 3, true, bestC, false, 3, 4);
        foldScore1 = foldScore1 + score1;
        foldScore5 = foldScore5 + score5;
        foldScore10 = foldScore10 + score10;
        System.out.println("\n\nTest set with best C=" + bestC + ", average score 1-5-10: " + foldScore1/5 +" "+ foldScore5/5 + " "+ foldScore10/5);
    }
    
    private static void testSVM() {
        File modelFile;
        
        if(modelFilePath != null){
            modelFile = new File(modelFilePath);
            System.out.println("using SVM model: " + modelFilePath);
        }
        else {
            if(netbeans){
                modelFile = new File(PATH + "/src/main/resources/liblinear_model");
                System.out.println("using SVM model: " + PATH + "/src/main/resources/liblinear_model");
            }
            else{
                modelFile = new File(PATH + "/classes/liblinear_model");
                System.out.println("using SVM model: " + PATH + "/classes/liblinear_model");
            }
        }
        
        List<OSMWay> testList = wayList;
        System.out.println("testList size: " + testList.size());
        //int succededInstances=0;
        //int succededInstances5=0;
        //int succededInstances10=0;
        Model model = null;
        try {
            model = Model.load(modelFile);
        } catch (IOException ex) {
            
            Logger.getLogger(OSMRecCLI.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Problem loading the model file!");
            System.exit(0);
        }
        
        int modelLabelSize = model.getLabels().length;
        int[] labels = model.getLabels();
        Map<Integer, Integer> mapLabelsToIDs = new HashMap<>();
        for(int h =0; h < model.getLabels().length; h++){
            mapLabelsToIDs.put(labels[h], h);

            //System.out.println(h + "   <->    " + labels[h]);
        }
        //System.out.println("file loaded");
        FeatureNode[] testInstance2;
        
        
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(outputFilePath));
        } catch (IOException ex) {
            Logger.getLogger(OSMRecCLI.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for(OSMWay way : testList){
            
            int id = 1;           
//            if(USE_CLASS_FEATURES){                
//                ClassFeatures class_vector = new ClassFeatures();
//                class_vector.createClassFeatures(way, mappings, mapperWithIDs, indirectClasses, indirectClassesWithIDs);
//                id = 1422;
//            }
//            else{
//                id = 1;
//            }
            
            //pass id also: 1422 if using classes, 1 if not
            GeometryFeatures geometryFeatures = new GeometryFeatures(id);
            geometryFeatures.createGeometryFeatures(way);
            id = geometryFeatures.getLastID();
            //id after geometry, cases: all geometry features with mean-variance boolean intervals:
            //id = 1526
            //System.out.println("id 1526 -> " + geometryFeatures.getLastID());
//            if(USE_RELATION_FEATURES){
//                RelationFeatures relationFeatures = new RelationFeatures(id);
//                relationFeatures.createRelationFeatures(way, relationList);       
//                id = relationFeatures.getLastID();
//            }
//            else {
//                id = geometryFeatures.getLastID();
//                //System.out.println("geom feat " + id);
//            }
            //id 1531
            //System.out.println("id 1532 -> " + relationFeatures.getLastID());
            //if(USE_TEXTUAL_FEATURES){
                TextualFeatures textualFeatures = new TextualFeatures(id, namesList, languageDetector);
                textualFeatures.createTextualFeatures(way);
                //System.out.println("last textual id: " + textualFeatures.getLastID());
                //System.out.println("full:  " + way.getFeatureNodeList());
            //}

            List<FeatureNode> featureNodeList = way.getFeatureNodeList();

            FeatureNode[] featureNodeArray = new FeatureNode[featureNodeList.size()];

            int i = 0;
            for(FeatureNode featureNode : featureNodeList){
                //if(la.getIndex() > lastIndex1){
                    featureNodeArray[i] = featureNode;
                    //System.out.println("i: " + i + " array:" + featureNodeArray[i]);
                    i++;
                //}
            }
            
            //System.out.println("array length: " + featureNodeArray.length);
            //System.out.println("trainingSetWithUnknown2: " + Arrays.toString(trainingSetWithUnknown2[k]));
            //System.out.println(k+ " vector size: " + listForArray.size());
            //System.out.println("feat length" + train1.length);
            //groupArrayList.add(way.getClassID());
            //Set<Integer> laa = way.getClassIDs();
            
            //System.out.println("class: " + GROUPS_ARRAY2[k]);
            testInstance2 = featureNodeArray;
            //double prediction = Linear.predict(model, testInstance2);
            //System.out.println("test prediction: " + prediction);
            double[] scores = new double[modelLabelSize];
            Linear.predictValues(model, testInstance2, scores);
            //Double max = Collections.max(Arrays.asList(ArrayUtils.toObject(scores)));
            //System.out.println("predict values: " + Arrays.toString(scores));
            
            //find index of max values in scores array: predicted classes are the elements of these indexes from array model.getlabels
            //iter scores and find 10 max values with their indexes first. then ask those indexes from model.getlabels
            Map<Double, Integer> scoresValues = new HashMap<>();
            for(int h = 0; h < scores.length; h++){
                scoresValues.put(scores[h], h);
                
                //System.out.println(h + "   <->    " + scores[h]);
            }            

            Arrays.sort(scores);
            
            int predicted1 = labels[scoresValues.get(scores[scores.length-1])];  
            int predicted2 = labels[scoresValues.get(scores[scores.length-2])];
            int predicted3 = labels[scoresValues.get(scores[scores.length-3])];
            int predicted4 = labels[scoresValues.get(scores[scores.length-4])];  
            int predicted5 = labels[scoresValues.get(scores[scores.length-5])];
            int predicted6 = labels[scoresValues.get(scores[scores.length-6])];
            int predicted7 = labels[scoresValues.get(scores[scores.length-7])];  
            int predicted8 = labels[scoresValues.get(scores[scores.length-8])];
            int predicted9 = labels[scoresValues.get(scores[scores.length-9])];
            int predicted10 = labels[scoresValues.get(scores[scores.length-10])];             
            
            String[] predictedTags = new String[10];
            
            for( Map.Entry<String, Integer> entry : mapperWithIDs.entrySet()){

                if(entry.getValue().equals(predicted1)){
                    //System.out.println("1st predicted class: " +entry.getKey());                     
                    predictedTags[0] = entry.getKey();
                }
                else if(entry.getValue().equals(predicted2)){
                    predictedTags[1] = entry.getKey();
                    //System.out.println("2nd predicted class: " +entry.getKey());
                }
                else if(entry.getValue().equals(predicted3)){
                    predictedTags[2] = entry.getKey();
                    //System.out.println("3rd predicted class: " +entry.getKey()); 
                }
                else if(entry.getValue().equals(predicted4)){
                    predictedTags[3] = entry.getKey();
                    //System.out.println("2nd predicted class: " +entry.getKey());
                }
                else if(entry.getValue().equals(predicted5)){
                    predictedTags[4] = entry.getKey();
                    //System.out.println("3rd predicted class: " +entry.getKey()); 
                }                    
                else if(entry.getValue().equals(predicted6)){
                    predictedTags[5] = entry.getKey();
                }
                else if(entry.getValue().equals(predicted7)){
                    predictedTags[6] = entry.getKey();
                    //System.out.println("3rd predicted class: " +entry.getKey()); 
                }
                else if(entry.getValue().equals(predicted8)){
                    predictedTags[7] = entry.getKey();
                    //System.out.println("2nd predicted class: " +entry.getKey());
                }
                else if(entry.getValue().equals(predicted9)){
                    predictedTags[8] = entry.getKey();
                    //System.out.println("3rd predicted class: " +entry.getKey()); 
                }     
                else if(entry.getValue().equals(predicted10)){
                    predictedTags[9] = entry.getKey();
                    //System.out.println("10nth predicted class: " +entry.getKey()); 
                }                     
            }            
            
            for(Map.Entry<String, String> tag : mappings.entrySet()){

                for(int k=0; k<10; k++){                    
                    if(tag.getValue().equals(predictedTags[k])){    

                        predictedTags[k] = tag.getKey();
                    }
                }
            }
            //System.out.println("predicted classes: " + Arrays.toString(predictedTags)); 
            try {
                bufferedWriter.write(SEPARATOR +"OSM ID: " + way.getID() + ", predicted classes: " + SEPARATOR);
                bufferedWriter.newLine();
                for (String predictedTag : predictedTags) {
                    if(predictedTag == null){
                        //
                    }
                    else{
                        bufferedWriter.write(predictedTag);
                        bufferedWriter.newLine();   
                    }
                }                
            }        
            catch(IOException ex){
                System.out.println("Something went wrong computing the recommendations.. "
                                             + "check the input file parameters" + ex);
            }        
        }
        try {
            if(bufferedWriter != null){
                bufferedWriter.flush();
                bufferedWriter.close();
            }

        }    
        catch(IOException ex){
            System.out.println("Something went wrong computing the recommendations.. "
                                         + "check the input file parameters" + ex);
        } 
    }
    
    private static void parseFiles() throws UnsupportedEncodingException, FileNotFoundException{
        File file;
        if(netbeans){
            file = new File(PATH + "/src/main/resources/mappings/Map"); 
        }
        else{
            file = new File(PATH + "/classes/mappings/Map");
        }
        Mapper mapper = new Mapper();
        try {   
            mapper.parseFile(file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Mapper.class.getName()).log(Level.SEVERE, null, ex);
        }
        mappings = mapper.getMappings();
        mapperWithIDs = mapper.getMappingsWithIDs();           
        Ontology ontology;
        if(netbeans){
            ontology = new Ontology(PATH +"/src/main/resources/mappings/owl.xml");       
        }
        else{
            ontology = new Ontology(PATH+"/classes/mappings/owl.xml");
        }
        ontology.parseOntology();
        indirectClasses = ontology.getIndirectClasses();
        indirectClassesWithIDs = ontology.getIndirectClassesIDs();
        //listHierarchy = ontology.getListHierarchy(); 
        
        String names;
        if(netbeans){
            names = PATH +"/src/main/resources/textualList.txt";       
        }
        else{
            names = PATH+"/classes/textualList.txt";
        }
        readTextualFromDefaultList(names);
        
        if (osmFilename.startsWith("/") || osmFilename.startsWith("file:///")){  //system has already exited in case of a null osmFile
            osmFilePath = osmFilename;
        }
        else{
            osmFilePath = PATH + "/"+ osmFilename; 
        }
        
        OSMParser osmParser = null;
        try {
            System.out.println("parsing file..");
            osmParser = new OSMParser(osmFilePath);
            System.out.println("osm file parsed!");
        } catch (FactoryException ex) {
            Logger.getLogger(OSMRecCLI.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(0);
        }

        osmParser.parseDocument();
        
        relationList = osmParser.getRelationList();
        wayList = osmParser.getWayList();
        numberOfTrainingInstances = osmParser.getWayList().size();
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
    
    private static void clearDataset(){
        for(OSMWay way : wayList){
            way.getFeatureNodeList().clear();
        }
    }
    
    private static void readTextualFromDefaultList(String file) throws UnsupportedEncodingException, FileNotFoundException{
       
        File namesFile = new File(file);            
        Statistics statistics = new Statistics();
        statistics.parseNamesFile(namesFile);
        namesList = statistics.getNamesList();
        //System.out.println("list used: " + namesList); 
    }
    
    private static void writeTextualListToFile(String filePath, List<Map.Entry<String, Integer>> textualList) {
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, true), "UTF-8"))) {            
            for(Map.Entry<String, Integer> entry : textualList){
                writer.write(entry.getKey());
                writer.newLine();
                System.out.println(entry.getKey());
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(OSMRecCLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(OSMRecCLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OSMRecCLI.class.getName()).log(Level.SEVERE, null, ex);
        }       
    }

    private static void extractTextualList(){
        System.out.println("Running analysis..");
        
        String textualListFilePath;
        String stopWordsPath;
        if(netbeans){
            stopWordsPath = PATH + "/src/main/resources/stopWords.txt";
            textualListFilePath = PATH + "/src/main/resources/textualList.txt";
            //System.out.println("using textual list path: " + PATH + "/src/main/resources/textualList.txt");
        }
        else{
            textualListFilePath = PATH + "/classes/textualList.txt";
            stopWordsPath = PATH + "/classes/stopWords.txt";
            //System.out.println("using textual list path: " + PATH + "/classes/textualList.txt");
        } 
        
        //provide top-K
        //Keep the top-K most frequent terms
        //Keep terms with frequency higher than N
        //Use the remaining terms as training features
        
        Analyzer anal = new Analyzer(osmFilePath, languageDetector, stopWordsPath);
        anal.runAnalysis();
        //System.out.println(anal.getFrequencies());
        //System.out.println(anal.getTopKMostFrequent(15));
        //System.out.println(anal.getWithFrequency(100));
       

        File textualFile = new File(textualListFilePath); 
        if(textualFile.exists()){
            textualFile.delete();
        }        
        try {
            textualFile.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(OSMRecCLI.class.getName()).log(Level.SEVERE, null, ex);
        }   

        List<Map.Entry<String, Integer>> textualList = anal.getFrequencies();
        //System.out.println("textual list to file:\n " + textualList);

        writeTextualListToFile(textualListFilePath, textualList);
        System.out.println("textual list saved at location:\n" + textualListFilePath);
        //write list to file and let parser do the loading from the names file
        
        //method read default list
        //method extract textual list - > the list will be already in memory, so the names parser doesn t have to be called
        if(USE_CLASS_FEATURES){
            numberOfFeatures = 1422 + 105 + textualList.size();
        }
        else{          
            numberOfFeatures = 105 + textualList.size();
        }
    }        
}
