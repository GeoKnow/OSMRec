package gr.athenainnovation.imis.generator;

//import static java.lang.Math.sqrt;
import com.hp.hpl.jena.ontology.OntClass;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import gr.athenainnovation.imis.OSMContainer.OSMNode;
import gr.athenainnovation.imis.OSMContainer.OSMRelation;
import gr.athenainnovation.imis.OSMContainer.OSMWay;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Creates vectors serialized in a text file and in memory for every OSM instance
 * Constructed vectors are separate lines in the vectors file representing each node of the OSM file 
 * They consist of IDs with values that denote the presence of a feature
 * Each vector has five portions: the direct class, the indirect class, the geometry features, 
 * the relation features and the textual features.
 * 
 * @author imis-nkarag
 */

public class InstanceVectors {
    
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(InstanceVectors.class);
    private static final int NUMBER_OF_AREA_FEATURES = 25;
    private static final int NUMBER_OF_POINTS = 13;
    private static final int NUMBER_OF_MEAN = 23; //for boolean intervals
    private static final int NUMBER_OF_VARIANCE = 37; //for boolean intervals
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final Map<String, String> mappings;
    private final Map<String, Integer> mappingsWithIDs;
    private final List<OSMWay> wayList;
    private final List<OSMRelation> relationList;
    private int directClassID = 0;
    private String directClassVectorPortion = "";
    private String indirectClassVectorPortion = "";
    private String geometriesPortion = "";
    private String textualFeaturesPortion = "";
    private String relationPortion = "";
    private final Map<String, List<String>> indirectClasses;
    private final Map<String, Integer> indirectClassesIDs;
    private static final int ADD_TO_GEOMETRY_ID = 1422;
                                                    //size of direct class IDs is 1372
                                                    //indirect class with IDs is 49
                                                    //geometries should start with ID above 1421 in the vector
                                                    //indirect classes should start with ID above 1372
    
    private int id = ADD_TO_GEOMETRY_ID;
    private final List<String> namesList;
    private static String path;    
    private boolean isTestSet;
    private int counter = 0;
  
    public InstanceVectors(List<OSMWay> wayList, List<OSMRelation> relationList, Map<String,String> mappings, 
            Map<String,Integer> mappingsWithIDs, Map<String, List<String>> indirectClasses, 
            Map<String, Integer> indirectClassesIDs, List<OntClass> listHierarchy, List<OSMNode> nodeList, 
            List<String> namesList, String path){
       
        this.wayList = wayList; 
        this.relationList = relationList;
        this.mappings = mappings;
        this.mappingsWithIDs = mappingsWithIDs;
        this.indirectClasses = indirectClasses;
        this.indirectClassesIDs = indirectClassesIDs;
        this.namesList = namesList;
        InstanceVectors.path = path;
        
    }
    
    public InstanceVectors(List<OSMWay> wayList, List<OSMRelation> relationList, Map<String,String> mappings, 
            Map<String,Integer> mappingsWithIDs, Map<String, List<String>> indirectClasses, 
            Map<String, Integer> indirectClassesIDs, List<OntClass> listHierarchy, List<OSMNode> nodeList, 
            List<String> namesList, String path, boolean isTestSet){
       
        this(wayList,relationList, mappings, mappingsWithIDs, indirectClasses, indirectClassesIDs,
                listHierarchy, nodeList, namesList, path); 
        this.isTestSet = isTestSet;
    }

    public void constructWayVectors() {
        
        new File(path).delete();
        System.out.print("constructing vectors");
        int prog = 0;
        int wayNodeListSize = wayList.size();       
        
        for (OSMWay wayNode : wayList) {      
        //for each wayNode parsed from osm xml:   
            
            //createClassFeatures(wayNode);               //create portion of direct and indirect class relationships
            calculateClasses(wayNode);
            createGeometryFeatures(wayNode);            //create geometry portion
//            if(!isTestSet){
//                createRelationFeatures(wayNode);
//            }
//            else{
//                id = id+6;
//            }
            createTextualFeatures(wayNode.getTagKeyValue(), wayNode); //create textual features here        
            
            if(!wayNode.getClassIDs().isEmpty()){
                if(!isTestSet){
                    String trainVector = directClassID + " "  
                    + geometriesPortion + textualFeaturesPortion + " # " + wayNode.getClassIDs();
                    writeToFile(trainVector);
                    //System.out.println("train vector " + trainVector);
                }
                else{

                //vector without class features and without unclassified instances:

                    String vectorWithoutClassFeatures = directClassID + " " + geometriesPortion + textualFeaturesPortion 
                                                                    + " # " + wayNode.getClassIDs();
                    writeToFile(vectorWithoutClassFeatures);
                    //System.out.println("test vector " + vectorWithoutClassFeatures);
                }
            }
            resetVector(); //clean fields for the next vector construction
            
            //kind of a progress bar
            if(prog == wayNodeListSize/2 || prog == wayNodeListSize/5 || 
                                                            prog == wayNodeListSize/10 || prog == wayNodeListSize/50){    
                System.out.print(".");
            }
            prog++;
        }  
        System.out.append(" done.\n");    
        LOG.info("Vectors constructed successfully!"); 
    }

    private void createClassFeatures(OSMWay wayNode) {
        
        //iteration for each way node in the wayList
        Set<Integer> sortedIndirectIDs = new TreeSet<>();
        Set<Integer> sortedDirectIDs = new TreeSet<>();        
        for (Entry<String, String> wayTagKeyValue : wayNode.getTagKeyValue().entrySet()){
            //iteration for each tag (key-value) in the current way node
            
            //concat key and value to use it later for checking
            String key = wayTagKeyValue.getKey() + " " + wayTagKeyValue.getValue();
            
            for (Entry<String,String> tagMappedToClass : mappings.entrySet()){
                //entry of mappings is e.g "highway residential <-> ResidentialHighway"
                //iteration to discover the wayNode class. This class's ID will be the start of the vector
                
                if (key.equals(tagMappedToClass.getKey())){
                    String className = tagMappedToClass.getValue();
                    directClassID = mappingsWithIDs.get(className);
                    
                    sortedDirectIDs.add(directClassID); 
                    //the direct class id is the last direct class that the instance is found to belong 

                    List<String> superClassesList = indirectClasses.get(className);

                    if (superClassesList != null){//check if the class has no superclasses                     
                        
                        for (String superClass: superClassesList) {

                            Integer indirectID = indirectClassesIDs.get(superClass);                                                                                                         //to save time here
                            if(indirectID != null){// there is a chance here that the ID is null,
                                //cause the list of super Classes  might contain extra classes  with no ID
                                //in the indirectClassesIDs map which is constructed from listHierarchyRootClasses method
                                //at the OntologyParser.
                                //so this condition check will remain for now
                                
                                if(!(sortedIndirectIDs.contains(indirectID))){
                                    sortedIndirectIDs.add(indirectID);
                                    //wayNode.getIndexVector().put(indirectID, 1.0);
                                }
                                //the construction of the indirectClassVectorPortion has been moved below, sorted
                                //indirectClassVectorPortion = indirectClassVectorPortion + indirectID + ":1 ";
                            }
                        }
                    }
                }
            }            
        }   
        wayNode.setClassIDs(sortedDirectIDs);
        for (Integer dirID : sortedDirectIDs){
            wayNode.getIndexVector().put(dirID, 1.0);
            directClassVectorPortion = directClassVectorPortion + dirID + ":1 ";
        }
        for (Integer indID : sortedIndirectIDs){
            wayNode.getIndexVector().put(indID, 1.0);
            indirectClassVectorPortion = indirectClassVectorPortion + indID + ":1 ";
        } 
    }   
    
    private void createGeometryFeatures(OSMWay wayNode){
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////  geometry Features ///////////////////            
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // geometry type feature //
        String  geometryType= wayNode.getGeometry().getGeometryType();
        switch (geometryType) {
            case "LineString":
                addGeometryFeature(id);
                wayNode.getIndexVector().put(id, 1.0);
                id += 4;
                break;
            case "Polygon":
                addGeometryFeature(id+1); //the IDs are unique for each geometry type
                wayNode.getIndexVector().put(id+1, 1.0);
                id += 4;
                break;
            case "LinearRing":
                addGeometryFeature(id+2);
                wayNode.getIndexVector().put(id+2, 1.0);
                id += 4;
                break;
            case "Point":
                addGeometryFeature(id+3);
                wayNode.getIndexVector().put(id+3, 1.0);
                id += 4;                                        
                break;
        }
        //LOG.info("after type " + id + " and further increase " + (id+1));
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // rectangle geometry shape feature //
        //id 1426
        if (wayNode.getGeometry().isRectangle()){                 
            addGeometryFeature(id);
            wayNode.getIndexVector().put(id, 1.0);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // number of points of geometry feature //
        id++; //1427      
        int numberOfPoints = wayNode.getGeometry().getNumPoints();
        numberOfPointsFeature(numberOfPoints, wayNode);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // area of geometry feature //
        //id 1440
        double area = wayNode.getGeometry().getArea();

        if(geometryType.equals("Polygon")){ 

            areaFeature(area,wayNode);
            //the id increases by 25 in the areaFeature method
        }
        else{
            id += 25;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
        // resembles to a circle feature //  
        //id 1465
        //id++;
        if(geometryResemblesCircle(wayNode)){ //this method checks if the shape of the geometry resembles to a circle
            addGeometryFeature(id);
            wayNode.getIndexVector().put(id, 1.0);
        }
        
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // mean edge feature // 
        
        id++;
        /*
        //System.out.println("mean start" + id);
        Coordinate[] nodeGeometries = wayNode.getGeometry().getCoordinates();
        List<Double> edgeLengths = new ArrayList();
        
        if(!wayNode.getGeometry().getGeometryType().toUpperCase().equals("POINT")){
            for (int i = 0; i < nodeGeometries.length-1; i++) {
                Coordinate[] nodePair = new Coordinate[2];
                nodePair[0] = nodeGeometries[i];
                nodePair[1] = nodeGeometries[i+1];
                LineString tempGeom = geometryFactory.createLineString(nodePair);
                edgeLengths.add(tempGeom.getLength()); 
            }
        }
        else{          
            edgeLengths.add(0.0);
        }
        double edgeSum = 0;
        for(Double edge : edgeLengths){
            edgeSum = edgeSum + edge;
        }
        double mean = edgeSum/edgeLengths.size();
        //double normalizedMean = sqrt(mean);
        //geometriesPortion = geometriesPortion + id + ":" + normalizedMean + " "; 
        //wayNode.getIndexVector().put(id, normalizedMean);

//intervals with boolean values for mean feature    
        
        if(mean<2){
            wayNode.getIndexVector().put(id, 1.0);
            addGeometryFeature(id);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<4){
            wayNode.getIndexVector().put(id+1, 1.0);
            addGeometryFeature(id+1);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<6){
            wayNode.getIndexVector().put(id+2, 1.0);
            addGeometryFeature(id+2);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<8){
            wayNode.getIndexVector().put(id+3, 1.0);
            addGeometryFeature(id+3);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<10){
            wayNode.getIndexVector().put(id+4, 1.0);
            addGeometryFeature(id+4);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<12){
            wayNode.getIndexVector().put(id+5, 1.0);
            addGeometryFeature(id+5);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<14){
            wayNode.getIndexVector().put(id+6, 1.0);
            addGeometryFeature(id+6);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<16){
            wayNode.getIndexVector().put(id+7, 1.0);
            addGeometryFeature(id+7);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<18){        
            wayNode.getIndexVector().put(id+8, 1.0);
            addGeometryFeature(id+8);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<20){        
            wayNode.getIndexVector().put(id+9, 1.0);
            addGeometryFeature(id+9);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<25){        
            wayNode.getIndexVector().put(id+10, 1.0);
            addGeometryFeature(id+10);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<30){        
            wayNode.getIndexVector().put(id+11, 1.0);
            addGeometryFeature(id+11);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<35){        
            wayNode.getIndexVector().put(id+12, 1.0);
            addGeometryFeature(id+12);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<40){        
            wayNode.getIndexVector().put(id+13, 1.0);
            addGeometryFeature(id+13);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<45){        
            wayNode.getIndexVector().put(id+14, 1.0);
            addGeometryFeature(id+14);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<50){
            wayNode.getIndexVector().put(id+15, 1.0);
            addGeometryFeature(id+15);
            id = id + NUMBER_OF_MEAN;
        }        
        else if(mean<60){
            wayNode.getIndexVector().put(id+16, 1.0);
            addGeometryFeature(id+16);
            id = id + NUMBER_OF_MEAN;
        }        
        else if(mean<70){
            wayNode.getIndexVector().put(id+17, 1.0);
            addGeometryFeature(id+17);
            id = id + NUMBER_OF_MEAN;
        }        
        else if(mean<80){
            wayNode.getIndexVector().put(id+18, 1.0);
            addGeometryFeature(id+18);
            id = id + NUMBER_OF_MEAN;
        }        
        else if(mean<90){
            wayNode.getIndexVector().put(id+19, 1.0);
            addGeometryFeature(id+19);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<100){
            wayNode.getIndexVector().put(id+20, 1.0);
            addGeometryFeature(id+20);
            id = id + NUMBER_OF_MEAN;
        }
        else if(mean<200){
            wayNode.getIndexVector().put(id+21, 1.0);
            addGeometryFeature(id+21);
            id = id + NUMBER_OF_MEAN;
        }
        else {
            wayNode.getIndexVector().put(id+22, 1.0);
            addGeometryFeature(id+22);
            id = id + NUMBER_OF_MEAN;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // variance feature// 
        //id++; //this should be removed if using boolean features with intervals
        //System.out.println("must be 1467" + id);
        double sum = 0;
        for(Double edge : edgeLengths){
            sum = sum + (edge-mean)*(edge-mean);
        }
        
        //double variance = sum/edgeLengths.size();  
        double normalizedVariance = (sum/edgeLengths.size())/(mean*mean); //normalized with square of mean value
        //geometriesPortion = geometriesPortion + id + ":" + normalizedVariance + " ";
        //wayNode.getIndexVector().put(id, normalizedVariance);
 //intervals with boolean values for variance feature  
        
        if(normalizedVariance == 0){
            wayNode.getIndexVector().put(id, 1.0);
            addGeometryFeature(id);
            id = id + NUMBER_OF_VARIANCE;
        }                           
        else if(normalizedVariance < 0.005){
            wayNode.getIndexVector().put(id+1, 1.0);
            addGeometryFeature(id+1);
            id = id + NUMBER_OF_VARIANCE;
        }             
        else if(normalizedVariance < 0.01){
            wayNode.getIndexVector().put(id+2, 1.0);
            addGeometryFeature(id+2);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.02){
            wayNode.getIndexVector().put(id+3, 1.0);
            addGeometryFeature(id+3);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.03){
            wayNode.getIndexVector().put(id+4, 1.0);
            addGeometryFeature(id+4);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.04){
            wayNode.getIndexVector().put(id+5, 1.0);
            addGeometryFeature(id+5);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.05){
            wayNode.getIndexVector().put(id+6, 1.0);
            addGeometryFeature(id+6);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.06){
            wayNode.getIndexVector().put(id+7, 1.0);
            addGeometryFeature(id+7);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.07){
            wayNode.getIndexVector().put(id+8, 1.0);
            addGeometryFeature(id+8);
            id = id + NUMBER_OF_VARIANCE;
        }       
        else if(normalizedVariance < 0.08){
            wayNode.getIndexVector().put(id+9, 1.0);
            addGeometryFeature(id+9);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.09){
            wayNode.getIndexVector().put(id+10, 1.0);
            addGeometryFeature(id+10);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.1){
            wayNode.getIndexVector().put(id+11, 1.0);
            addGeometryFeature(id+11);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.12){
            wayNode.getIndexVector().put(id+12, 1.0);
            addGeometryFeature(id+12);
            id = id + NUMBER_OF_VARIANCE;
        }      
        else if(normalizedVariance < 0.14){
            wayNode.getIndexVector().put(id+13, 1.0);
            addGeometryFeature(id+13);
            id = id + NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.16){
            wayNode.getIndexVector().put(id+14, 1.0);
            addGeometryFeature(id+14);
            id = id + NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.18){
            wayNode.getIndexVector().put(id+15, 1.0);
            addGeometryFeature(id+15);
            id = id + NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.20){
            wayNode.getIndexVector().put(id+16, 1.0);
            addGeometryFeature(id+16);
            id = id + NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.22){
            wayNode.getIndexVector().put(id+17, 1.0);
            addGeometryFeature(id+17);
            id = id + NUMBER_OF_VARIANCE;
        } 
        else if(normalizedVariance < 0.24){
            wayNode.getIndexVector().put(id+18, 1.0);
            addGeometryFeature(id+18);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.26){
            wayNode.getIndexVector().put(id+19, 1.0);
            addGeometryFeature(id+19);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.28){
            wayNode.getIndexVector().put(id+20, 1.0);
            addGeometryFeature(id+20);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.30){
            wayNode.getIndexVector().put(id+21, 1.0);
            addGeometryFeature(id+21);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.32){
            wayNode.getIndexVector().put(id+22, 1.0);
            addGeometryFeature(id+22);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.34){
            wayNode.getIndexVector().put(id+23, 1.0);
            addGeometryFeature(id+23);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.36){
            wayNode.getIndexVector().put(id+24, 1.0);
            addGeometryFeature(id+24);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.38){
            wayNode.getIndexVector().put(id+25, 1.0);
            addGeometryFeature(id+25);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.40){
            wayNode.getIndexVector().put(id+26, 1.0);
            addGeometryFeature(id+26);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.42){
            wayNode.getIndexVector().put(id+27, 1.0);
            addGeometryFeature(id+27);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.44){
            wayNode.getIndexVector().put(id+28, 1.0);
            addGeometryFeature(id+28);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.46){
            wayNode.getIndexVector().put(id+29, 1.0);
            addGeometryFeature(id+29);
            id = id + NUMBER_OF_VARIANCE;
        }        
        else if(normalizedVariance < 0.48){
            wayNode.getIndexVector().put(id+30, 1.0);
            addGeometryFeature(id+30);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.5){
            wayNode.getIndexVector().put(id+31, 1.0);
            addGeometryFeature(id+31);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.6){
            wayNode.getIndexVector().put(id+32, 1.0);
            addGeometryFeature(id+32);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.7){
            wayNode.getIndexVector().put(id+33, 1.0);
            addGeometryFeature(id+33);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.8){
            wayNode.getIndexVector().put(id+34, 1.0);
            addGeometryFeature(id+34);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 0.9){
            wayNode.getIndexVector().put(id+35, 1.0);
            addGeometryFeature(id+35);
            id = id + NUMBER_OF_VARIANCE;
        }
        else if(normalizedVariance < 1){
            wayNode.getIndexVector().put(id+36, 1.0);
            addGeometryFeature(id+36);
            id = id + NUMBER_OF_VARIANCE;
        }
        else {
            wayNode.getIndexVector().put(id+37, 1.0);
            addGeometryFeature(id+37);
            id = id + NUMBER_OF_VARIANCE;
        } 
        */        
        //System.out.println("mean end from instanceVectors " + id);
    }
    
    private void createTextualFeatures(Map<String, String> tags, OSMWay wayNode){
        //namesList.indexOf(name) this index can be zero.
        //In that case it conflicts the previous geometry id, so we increment id. 
        //System.out.println("begin textual id from InstanceVectors = " + id);
        TreeSet<Integer> textIDs = new TreeSet<>();
        
        if (tags.keySet().contains("name")){           
            String nameTag = tags.get("name"); //get the value of the name tag of the current node
            String[] nameTagSplitList = nameTag.split("\\s");    //split the value to compare individually 
                                                                 //with the namesList 
                                                                    
            for (String name : namesList){
            //for each name in namesList, we check if a value of a name tag is present in the namesList
                
                for(String split : nameTagSplitList){
                    if (split.equals(name)){
                    //tag name value found in this node, construct the id and concat it with ":1 "     
                        int currentID = namesList.indexOf(name) + id;
                        textIDs.add(currentID); //make the proper id by adding the namesList index
                        //wayNode.getVector().set(currentID, 1);//vector for clustering
                        wayNode.getIndexVector().put(currentID, 1.0);
                    }
                }
            }
            for (Integer textID : textIDs){
                addTextualFeature(textID);                
            }     
        }
    }

    private void createRelationFeatures(OSMWay wayNode) {      
        id++; //this should be removed when using boolean intervals for mean and variance
        //System.out.println("must be 1468" + id);
        //System.out.println("InstanceVectors rel start" + id);
        boolean hasRelation = false;
        for(OSMRelation relation : relationList){
            if(hasRelation){break;}
            if(relation.getMemberReferences().contains(wayNode.getID())){
                hasRelation = true;
                Map<String, String> tags = relation.getTagKeyValue();
                
                if(tags.containsKey("route")){
                    wayNode.getIndexVector().put(id, 1.0);
                    addRelationFeature(id);                    
                }
                else if(tags.containsKey("multipolygon")){
                    wayNode.getIndexVector().put(id+1, 1.0);
                    addRelationFeature(id+1);                                      
                }
                else if(tags.containsKey("boundary")){
                    wayNode.getIndexVector().put(id+2, 1.0);
                    addRelationFeature(id+2);                                       
                }
                else if(tags.containsKey("restriction")){
                    wayNode.getIndexVector().put(id+3, 1.0);
                    addRelationFeature(id+3);                                      
                }
                else{
                    //the instance may be a member of a relation, but the relation has no type or is incomplete.
                    wayNode.getIndexVector().put(id+4, 1.0);
                    addRelationFeature(id+4);                  
                }
                id = id + 5;
            }
        }
        if(!hasRelation){
            id = id + 5;
        }
    }
    
    private void numberOfPointsFeature(int numberOfPoints, OSMWay wayNode) {           
        //int NUMBER_OF_POINTS = 13; //increase the id after the feature is found for the next portion of the vector.

        if(numberOfPoints<10){
            addGeometryFeature(id);
            wayNode.getIndexVector().put(id, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<20){
            addGeometryFeature(id+1);
            wayNode.getIndexVector().put(id+1, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<30){
            addGeometryFeature(id+2);
            wayNode.getIndexVector().put(id+2, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<40){
            addGeometryFeature(id+3);
            wayNode.getIndexVector().put(id+3, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<50){
            addGeometryFeature(id+4);
            wayNode.getIndexVector().put(id+4, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<75){
            addGeometryFeature(id+5);
            wayNode.getIndexVector().put(id+5, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<100){
            addGeometryFeature(id+6);
            wayNode.getIndexVector().put(id+6, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<150){
            addGeometryFeature(id+7);
            wayNode.getIndexVector().put(id+7, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<200){
            addGeometryFeature(id+8);
            wayNode.getIndexVector().put(id+8, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<300){
            addGeometryFeature(id+9);
            wayNode.getIndexVector().put(id+9, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<500){
            addGeometryFeature(id+10);
            wayNode.getIndexVector().put(id+10, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else if(numberOfPoints<1000){ 
            addGeometryFeature(id+11);
            wayNode.getIndexVector().put(id+11, 1.0);
            id += NUMBER_OF_POINTS;
        }
        else{
            addGeometryFeature(id+12);
            wayNode.getIndexVector().put(id+12, 1.0);
            id += NUMBER_OF_POINTS;
        }
    }
    
    private void areaFeature(double area, OSMWay wayNode) {        
        
        if(area<50){
            addGeometryFeature(id);
            wayNode.getIndexVector().put(id, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<100){
            addGeometryFeature(id + 1);
            wayNode.getIndexVector().put(id+1, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<150){
            addGeometryFeature(id + 2);
            wayNode.getIndexVector().put(id+2, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<200){
            addGeometryFeature(id + 3);
            wayNode.getIndexVector().put(id+3, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<250){
            addGeometryFeature(id + 4);
            wayNode.getIndexVector().put(id+4, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<300){
            addGeometryFeature(id + 5);
            wayNode.getIndexVector().put(id+5, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<350){
            addGeometryFeature(id + 6);
            wayNode.getIndexVector().put(id+6, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<400){
            addGeometryFeature(id + 7);
            wayNode.getIndexVector().put(id+7, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<450){
            addGeometryFeature(id + 8);
            wayNode.getIndexVector().put(id+8, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<500){
            addGeometryFeature(id + 9);
            wayNode.getIndexVector().put(id+9, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<750){
            addGeometryFeature(id + 10);
            wayNode.getIndexVector().put(id+10, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<1000){
            addGeometryFeature(id + 11);
            wayNode.getIndexVector().put(id+11, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<1250){
            addGeometryFeature(id + 12);
            wayNode.getIndexVector().put(id+12, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<1500){
            addGeometryFeature(id + 13);
            wayNode.getIndexVector().put(id+13, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<1750){
            addGeometryFeature(id + 14);
            wayNode.getIndexVector().put(id+14, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<2000){
            addGeometryFeature(id + 15);
            wayNode.getIndexVector().put(id+15, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<2250){
            addGeometryFeature(id + 16);
            wayNode.getIndexVector().put(id+16, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<2500){
            addGeometryFeature(id + 17);
            wayNode.getIndexVector().put(id+17, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<2750){
            addGeometryFeature(id + 18);
            wayNode.getIndexVector().put(id+18, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<3000){
            addGeometryFeature(id + 19);
            wayNode.getIndexVector().put(id+19, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<3500){
            addGeometryFeature(id + 20);
            wayNode.getIndexVector().put(id+20, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<4000){
            addGeometryFeature(id + 21);
            wayNode.getIndexVector().put(id+21, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<5000){
            addGeometryFeature(id + 22);
            wayNode.getIndexVector().put(id+22, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else if(area<10000){
            addGeometryFeature(id + 23);
            wayNode.getIndexVector().put(id+23, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
        else{
            addGeometryFeature(id + 24);
            wayNode.getIndexVector().put(id+24, 1.0);
            id += NUMBER_OF_AREA_FEATURES;
        }
    } 
    
    private boolean geometryResemblesCircle(OSMWay way){
        Geometry wayGeometry = way.getGeometry();
        boolean isCircle = false;
        if(wayGeometry.getGeometryType().equals("Polygon") && wayGeometry.getNumPoints()>=16){ 
             
            List<Geometry> points = way.getNodeGeometries();           
            Geometry firstPoint = points.get(0);            
            double radius = firstPoint.distance(wayGeometry.getCentroid());
            
            // buffer around the distance of the first point to centroid
            double radiusBufferSmaller = radius*0.6; 
            //the rest of the point-to-centroid distances will be compared with these 
            double radiusBufferGreater = radius*1.4; 
            isCircle = true;
            
            for (Geometry point : points){                
                double tempRadius = point.distance(wayGeometry.getCentroid());
                boolean tempIsCircle = (radiusBufferSmaller <= tempRadius) && (tempRadius <= radiusBufferGreater);
                isCircle = isCircle && tempIsCircle; //if any of the points give a false, the method will return false
                //if (!isCircle){break;}
            }     
            
            double ratio = wayGeometry.getLength() / wayGeometry.getArea();            
            boolean tempIsCircle = ratio < 0.06; //arbitary value based on statistic measure of osm instances. 
                                                 //The smaller this value, the closer this polygon resembles to a circle            
            isCircle = isCircle && tempIsCircle;
        }
        return isCircle;
    }  
    
    private void addGeometryFeature(int id){
        geometriesPortion = geometriesPortion + id +":1 ";       
    }
    
    private void addTextualFeature(int id){
        textualFeaturesPortion = textualFeaturesPortion + id + ":1 ";
    }  
    
    private void addRelationFeature(int id){
        relationPortion = relationPortion + id + ":1 ";
    }
    
    private void resetVector(){
        directClassID = 0;
        directClassVectorPortion = "";
        indirectClassVectorPortion = "";
        geometriesPortion = "";
        textualFeaturesPortion = "";
        relationPortion = "";
        id = ADD_TO_GEOMETRY_ID; 
    } 
    
    private static void writeToFile(String text) {               
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path), true))) {
            bw.write(text);
            bw.newLine();
        }                
        catch (IOException e) {
            System.err.println("write to file failed: " + e);
        }
    }
    
    private void calculateClasses(OSMWay wayNode) {
        
        //iteration for each way node in the wayList
        Set<Integer> sortedIndirectIDs = new TreeSet<>();
        Set<Integer> sortedDirectIDs = new TreeSet<>();        
        for (Entry<String, String> wayTagKeyValue : wayNode.getTagKeyValue().entrySet()){
            //iteration for each tag (key-value) in the current way node
            
            //concat key and value to use it later for checking
            String key = wayTagKeyValue.getKey() + " " + wayTagKeyValue.getValue();
            
            for (Entry<String,String> tagMappedToClass : mappings.entrySet()){
                //entry of mappings is e.g "highway residential <-> ResidentialHighway"
                //iteration to discover the wayNode class. This class's ID will be the start of the vector
                
                if (key.equals(tagMappedToClass.getKey())){
                    String className = tagMappedToClass.getValue();
                    directClassID = mappingsWithIDs.get(className);
                    
                    sortedDirectIDs.add(directClassID); 
                    //the direct class id is the last direct class that the instance is found to belong 

                    List<String> superClassesList = indirectClasses.get(className);

                    if (superClassesList != null){//check if the class has no superclasses                     
                        
                        for (String superClass: superClassesList) {

                            Integer indirectID = indirectClassesIDs.get(superClass);                                                                                                         //to save time here
                            if(indirectID != null){// there is a chance here that the ID is null,
                                //cause the list of super Classes  might contain extra classes  with no ID
                                //in the indirectClassesIDs map which is constructed from listHierarchyRootClasses method
                                //at the OntologyParser.
                                //so this condition check will remain for now
                                
                                if(!(sortedIndirectIDs.contains(indirectID))){
                                    sortedIndirectIDs.add(indirectID);
                                }
                            }
                        }
                    }
                }
            }            
        }   
        wayNode.setClassIDs(sortedDirectIDs);
    }
}