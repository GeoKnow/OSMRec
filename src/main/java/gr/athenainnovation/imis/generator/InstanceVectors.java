package gr.athenainnovation.imis.generator;

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
 * Creates vectors in a text file
 * Constructed vectors are separate lines in the vectors file representing each node of the OSM file 
 * They consist of IDs with values that denote the presence of a feature
 * Each vector has four portions: the direct class, the indirect class, the geometry features and the textual features.
 * 
 * @author imis-nkarag
 */

public class InstanceVectors {
    
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(InstanceVectors.class);
    private final Map<String, String> mappings;
    private final Map<String, Integer> mappingsWithIDs;
    //private final List<OSMNode> nodeList;
    private final List<OSMWay> wayList;
    private final List<OSMRelation> relationList;
    private int directClassID;
    private String directClassVectorPortion;
    private String indirectClassVectorPortion;
    private String geometriesPortion;
    private String textualFeaturesPortion;
    private String relationFeaturesPortion;
    private final Map<String, List<String>> indirectClasses;
    private final Map<String, Integer> indirectClassesIDs;
    private int id;
    private static final int geometryAdditiveID = 1422;
                                                    //size of direct class IDs is 1372
                                                    //indirect class with IDs is 49
                                                    //geometries should start with ID above 1421 in the vector
                                                    //indirect classes should start with ID above 1372
    
    private final List<String> namesList;
    private static String path;    
  
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
        InstanceVectors.path = path + "/classes/output/vectors";
        directClassVectorPortion = "";
        indirectClassVectorPortion = "";
        geometriesPortion = "";
        textualFeaturesPortion = "";
        relationFeaturesPortion = "";
        id = geometryAdditiveID;
       
    }

    public void constructWayVectors() {
        new File(path).delete();
        System.out.print("constructing vectors");
        int prog = 0;
        int wayNodeListSize = wayList.size();       
        
        for (OSMWay wayNode : wayList) {      
        //for each wayNode parsed from osm xml:                
            createClassFeatures(wayNode);               //create portion of direct and indirect class relationships
            createGeometryFeatures(wayNode);            //create geometry portion
            createNameOccurencesFeatures(wayNode.getTagKeyValue(), wayNode); //create textual features here        
            createRelationFeatures(wayNode);              
     
            //String vector = directClassID + " " + directClassVectorPortion + indirectClassVectorPortion 
            //+ geometriesPortion + textualFeaturesPortion; //+ " # " +wayNode.getClassIDs();
            
            //vector without class features and without unclassified instances:
            if(directClassID != 0){
                String vectorWithoutClassFeatures = directClassID + " " + geometriesPortion + textualFeaturesPortion 
                                                                + " # " + wayNode.getClassIDs() + " " + directClassID;
                writeToFile(vectorWithoutClassFeatures);
            }
            
            resetWayVector(); //clean fields for the next vector construction
            
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
                    
                    //directClassVectorPortion = directClassVectorPortion + directClassID + ":1 ";
                    
                    //the direct class id is the last direct class that the instance is found to belong
                    wayNode.setClassID(directClassID); 
                    
                    wayNode.getIndexVector().put(directClassID, 0.0);
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
                                    wayNode.getIndexVector().put(indirectID, 0.0);
                                }
                                //the construction of the indirectClassVectorPortion has been moved below, sorted
                                //indirectClassVectorPortion = indirectClassVectorPortion + indirectID + ":1 ";
                            }
                        }
 
                    }
                }
//                    else
//                    { //the concat of a ":0 " in the vector is not needed using SVM multiclass, 
//                        //so this stays out for this implementation
//                        directClassVectorPortion = directClassVectorPortion + mappingsWithIDs.get(k.getValue()) + ":0 ";
//                    }
            }            
        }
        wayNode.setClassIDs(sortedDirectIDs);
        for (Integer dirID : sortedDirectIDs){
            directClassVectorPortion = directClassVectorPortion + dirID + ":1 ";
        }
        for (Integer indID : sortedIndirectIDs){
            indirectClassVectorPortion = indirectClassVectorPortion + indID + ":1 ";
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

    private void resetWayVector(){
        directClassVectorPortion = "";
        indirectClassVectorPortion = "";
        geometriesPortion = "";
        textualFeaturesPortion = "";
        relationFeaturesPortion = "";
        id = geometryAdditiveID; 
    } 
    
    private void createNameOccurencesFeatures(Map<String, String> tags, OSMWay wayNode){
        //namesList.indexOf(name) this index can be zero.
        //In that case it conflicts the previous geometry id, so we increment id.
        id++;  
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
    
    private static void writeToFile(String text) {               
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path), true))) {
            bw.write(text);
            bw.newLine();
        }                
        catch (IOException e) {
            System.err.println("write to file failed: " + e);
        }
    }
    
    private void numberOfPointsFeature(int numberOfPoints, OSMWay wayNode) {           
        int increment = 13; //increase the id after the feature is found for the next portion of the vector.

        if(numberOfPoints<10){
            addGeometryFeature(id);
            wayNode.getIndexVector().put(id, 1.0);
            id += increment;
        }
        else if(numberOfPoints<20){
            addGeometryFeature(id+1);
            wayNode.getIndexVector().put(id+1, 1.0);
            id += increment;
        }
        else if(numberOfPoints<30){
            addGeometryFeature(id+2);
            wayNode.getIndexVector().put(id+2, 1.0);
            id += increment;
        }
        else if(numberOfPoints<40){
            addGeometryFeature(id+3);
            wayNode.getIndexVector().put(id+3, 1.0);
            id += increment;
        }
        else if(numberOfPoints<50){
            addGeometryFeature(id+4);
            wayNode.getIndexVector().put(id+4, 1.0);
            id += increment;
        }
        else if(numberOfPoints<75){
            addGeometryFeature(id+5);
            wayNode.getIndexVector().put(id+5, 1.0);
            id += increment;
        }
        else if(numberOfPoints<100){
            addGeometryFeature(id+6);
            wayNode.getIndexVector().put(id+6, 1.0);
            id += increment;
        }
        else if(numberOfPoints<150){
            addGeometryFeature(id+7);
            wayNode.getIndexVector().put(id+7, 1.0);
            id += increment;
        }
        else if(numberOfPoints<200){
            addGeometryFeature(id+8);
            wayNode.getIndexVector().put(id+8, 1.0);
            id += increment;
        }
        else if(numberOfPoints<300){
            addGeometryFeature(id+9);
            wayNode.getIndexVector().put(id+9, 1.0);
            id += increment;
        }
        else if(numberOfPoints<500){
            addGeometryFeature(id+10);
            wayNode.getIndexVector().put(id+10, 1.0);
            id += increment;
        }
        else if(numberOfPoints<1000){ 
            addGeometryFeature(id+11);
            wayNode.getIndexVector().put(id+11, 1.0);
            id += increment;
        }
        else{
            addGeometryFeature(id+12);
            wayNode.getIndexVector().put(id+12, 1.0);
            id += increment;
        }
    }

    private void areaFeature(double area, OSMWay wayNode) {
        int increment = 25;        
        
        if(area<50){
            addGeometryFeature(id);
            wayNode.getIndexVector().put(id, 1.0);
            id += increment;
        }
        else if(area<100){
            addGeometryFeature(id + 1);
            wayNode.getIndexVector().put(id+1, 1.0);
            id += increment;
        }
        else if(area<150){
            addGeometryFeature(id + 2);
            wayNode.getIndexVector().put(id+2, 1.0);
            id += increment;
        }
        else if(area<200){
            addGeometryFeature(id + 3);
            wayNode.getIndexVector().put(id+3, 1.0);
            id += increment;
        }
        else if(area<250){
            addGeometryFeature(id + 4);
            wayNode.getIndexVector().put(id+4, 1.0);
            id += increment;
        }
        else if(area<300){
            addGeometryFeature(id + 5);
            wayNode.getIndexVector().put(id+5, 1.0);
            id += increment;
        }
        else if(area<350){
            addGeometryFeature(id + 6);
            wayNode.getIndexVector().put(id+6, 1.0);
            id += increment;
        }
        else if(area<400){
            addGeometryFeature(id + 7);
            wayNode.getIndexVector().put(id+7, 1.0);
            id += increment;
        }
        else if(area<450){
            addGeometryFeature(id + 8);
            wayNode.getIndexVector().put(id+8, 1.0);
            id += increment;
        }
        else if(area<500){
            addGeometryFeature(id + 9);
            wayNode.getIndexVector().put(id+9, 1.0);
            id += increment;
        }
        else if(area<750){
            addGeometryFeature(id + 10);
            wayNode.getIndexVector().put(id+10, 1.0);
            id += increment;
        }
        else if(area<1000){
            addGeometryFeature(id + 11);
            wayNode.getIndexVector().put(id+11, 1.0);
            id += increment;
        }
        else if(area<1250){
            addGeometryFeature(id + 12);
            wayNode.getIndexVector().put(id+12, 1.0);
            id += increment;
        }
        else if(area<1500){
            addGeometryFeature(id + 13);
            wayNode.getIndexVector().put(id+13, 1.0);
            id += increment;
        }
        else if(area<1750){
            addGeometryFeature(id + 14);
            wayNode.getIndexVector().put(id+14, 1.0);
            id += increment;
        }
        else if(area<2000){
            addGeometryFeature(id + 15);
            wayNode.getIndexVector().put(id+15, 1.0);
            id += increment;
        }
        else if(area<2250){
            addGeometryFeature(id + 16);
            wayNode.getIndexVector().put(id+16, 1.0);
            id += increment;
        }
        else if(area<2500){
            addGeometryFeature(id + 17);
            wayNode.getIndexVector().put(id+17, 1.0);
            id += increment;
        }
        else if(area<2750){
            addGeometryFeature(id + 18);
            wayNode.getIndexVector().put(id+18, 1.0);
            id += increment;
        }
        else if(area<3000){
            addGeometryFeature(id + 19);
            wayNode.getIndexVector().put(id+19, 1.0);
            id += increment;
        }
        else if(area<3500){
            addGeometryFeature(id + 20);
            wayNode.getIndexVector().put(id+20, 1.0);
            id += increment;
        }
        else if(area<4000){
            addGeometryFeature(id + 21);
            wayNode.getIndexVector().put(id+21, 1.0);
            id += increment;
        }
        else if(area<5000){
            addGeometryFeature(id + 22);
            wayNode.getIndexVector().put(id+22, 1.0);
            id += increment;
        }
        else if(area<10000){
            addGeometryFeature(id + 23);
            wayNode.getIndexVector().put(id+23, 1.0);
            id += increment;
        }
        else{
            addGeometryFeature(id + 24);
            wayNode.getIndexVector().put(id+24, 1.0);
            id += increment;
        }
    }
    
    private void addGeometryFeature(int id){
        geometriesPortion = geometriesPortion + id +":1 ";       
    }
    
    private void addTextualFeature(int id){
        textualFeaturesPortion = textualFeaturesPortion + id + ":1 ";
    }    
    
    private void createGeometryFeatures(OSMWay wayNode){
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////  geometry Portion ///////////////////            
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
                wayNode.getIndexVector().put(id, 1.0);
                id += 4;
                break;
            case "LinearRing":
                addGeometryFeature(id+2);
                wayNode.getIndexVector().put(id, 1.0);
                id += 4;
                break;
            case "Point":
                addGeometryFeature(id+3);
                wayNode.getIndexVector().put(id, 1.0);
                id += 4;                                        
                break;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // rectangle geometry shape feature //
        id++;
        if (wayNode.getGeometry().isRectangle()){                 
            addGeometryFeature(id);
            wayNode.getIndexVector().put(id, 1.0);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // number of points of geometry feature //
        id++;           
        int numberOfPoints = wayNode.getGeometry().getNumPoints();
        numberOfPointsFeature(numberOfPoints, wayNode);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // area of geometry feature //

        id++;
        double area = wayNode.getGeometry().getArea();

        if(geometryType.equals("Polygon")){ 

            areaFeature(area,wayNode);

            //the id increases in the areaFeature method
        }
        else{

            id += 25;

        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
        // resembles to a circle feature //  
        id++;
        if(geometryResemblesCircle(wayNode)){ //this method checks if the shape of the geometry resembles to a circle
            addGeometryFeature(id);
            wayNode.getIndexVector().put(id, 1.0);
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // mean edge feature //
        id++;
        Coordinate[] nodeGeometries = wayNode.getGeometry().getCoordinates();
        List<Double> edgeLengths = new ArrayList();
        
        if(!wayNode.getGeometry().getGeometryType().toUpperCase().equals("POINT")){
            GeometryFactory geometryFactory = new GeometryFactory();
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
        geometriesPortion = geometriesPortion + id +":" + mean + " ";
        wayNode.getIndexVector().put(id, mean);
        
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // variance feature//
        id++;
        double sum = 0;
        for(Double edge : edgeLengths){
            sum = sum + (edge-mean)*(edge-mean);
        }
        double variance = sum/edgeLengths.size();
        wayNode.getIndexVector().put(id, variance);
        geometriesPortion = geometriesPortion + id +":" + variance + " ";
    }

    private void createRelationFeatures(OSMWay wayNode) {      
        id++;
        int relations = 0;
        for(OSMRelation relation : relationList){

            if(relation.getMemberReferences().contains(wayNode.getID())){
                relations++;
                Map<String, String> tags = relation.getTagKeyValue();
                for(Entry<String, String> tag : tags.entrySet()){
                  
                }             
            }
        }
        //add appropriate feature     
        relationFeaturesPortion = relationFeaturesPortion + id + ":" + relations + " ";
    }
}