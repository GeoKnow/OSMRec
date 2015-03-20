package gr.athenainnovation.imis.generator;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

/**
 *  Generates input file for CLUTO's vcluster process. 
 * 
 * @author imis-nkarag
 */
public class BalancedVectorsMatrix {
    
//    private static final double CLASS_FEATURE_WEIGHT = 9; 
//    private static final double GEOMETRY_TYPE_FEATURE_WEIGHT = 15.75;   
//    private static final double RECTANGLE_FEATURE_WEIGHT = 31.5;    
//    private static final double NUMBER_OF_POINTS_FEATURE_WEIGHT = 31.5;
//    private static final double AREA_FEATURE_WEIGHT = 31.5;   
//    private static final double CIRCLE_FEATURE_WEIGHT = 31.5;
//    private static final double TEXTUAL_FEATURE_WEIGHT = 7;
//    private static final double NORMALIZED_GEOMETRY_WEIGHT = 31.5;   
//    private static final double NORMALIZED_MEAN_AND_VARIANCE_WEIGHT = 31.5;
//    private static final double NORMALIZED_MEAN_WEIGHT = 31.5;
    
    private static final double CLASS_FEATURE_WEIGHT = 2.4494;          //sqrt(6) = 2.4494
    private static final double GEOMETRY_TYPE_FEATURE_WEIGHT = 4.8989;   //sqrt(6)*sqrt(4) = 4.8989
    private static final double RECTANGLE_FEATURE_WEIGHT = 4.8989;    
    private static final double NUMBER_OF_POINTS_FEATURE_WEIGHT = 4.8989;
    private static final double AREA_FEATURE_WEIGHT = 4.8989;   
    private static final double CIRCLE_FEATURE_WEIGHT = 4.8989;
    private static final double NORMALIZED_TEXTUAL_FEATURE_WEIGHT = 2;  //sqrt(4) = 2
    private static final double NORMALIZED_GEOMETRY_WEIGHT = 4.8989;   
    private static final double NORMALIZED_MEAN_AND_VARIANCE_WEIGHT = 4.8989;
    private static final double NORMALIZED_RELATION_WEIGHT = 4.8989;
    
    private final List<OSMWay> wayList;
    private final String vectorMatrixOutputFile;
    private final String readyVectorsMatrix;
    private final int columnSize;
    private int id;
    
    public BalancedVectorsMatrix(List<OSMWay> wayList, String vectorMatrixOutputFile, int columnSize){
        this.vectorMatrixOutputFile = vectorMatrixOutputFile;
        this.wayList = wayList;
        readyVectorsMatrix = vectorMatrixOutputFile + ".mat";
        this.columnSize = columnSize;
    }
    
    public void generateBalancedVectorsMatrix(){
        
        new File(vectorMatrixOutputFile).delete();
        new File(readyVectorsMatrix).delete();
        int nonZeros = 0;

        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(vectorMatrixOutputFile,true)); 
        PrintStream printStream = new PrintStream(output)){ 
    //normalization based on max area and points. May find very big areas and force the small area features to zero.            
    //        ArrayList maxValues = getMaximumValues(wayList);
    //        double maxArea = (Double) maxValues.get(0);
    //        double maxPoints = (Integer) maxValues.get(1);
        
        double maxArea = 5000;    
        int maxPoints = 100;
        for (OSMWay node : wayList){

            TreeMap<Integer, Double> indexVector = node.getIndexVector();
            StringBuilder stringBuilder = new StringBuilder("");
           
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////               
        ///////////////////////////////////////  BALANCING FEATURE TYPES /////////////////////////////////////////////// 
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////                
                
        /////////// class features portion /////////////////           
            for(id = 0; id < 1422; id++){    
                if(indexVector.containsKey(id)){                      
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)*CLASS_FEATURE_WEIGHT).append(" ");
                    nonZeros++;
                } 
            } 
                
        //////////// geometry type features portion /////////      
            while(id<1426){
                if(indexVector.containsKey(id)){
                    nonZeros++;    
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)*GEOMETRY_TYPE_FEATURE_WEIGHT).append(" ");
                }
                id++;
            }
        //////// rectangle feature ///////// 
            if(indexVector.containsKey(id)){
                nonZeros++;    
                stringBuilder.append(id).append(" ").append(indexVector.get(id)*RECTANGLE_FEATURE_WEIGHT).append(" ");
            }
            id++; //id becomes 1427
        //////// number of points features portion /////////  
            while(id<1440){
                if(indexVector.containsKey(id)){
                    nonZeros++;    
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)*NUMBER_OF_POINTS_FEATURE_WEIGHT).append(" ");
                } 
                id++;
            }
            //id 1440
        //////// area features portion /////////  
            while(id<1465){
                if(indexVector.containsKey(id)){
                    nonZeros++;    
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)*AREA_FEATURE_WEIGHT).append(" ");
                }
                id++;
            }
           //id 1465     
        //////// circle features portion /////////   
        //    while(id<1466){
                if(indexVector.containsKey(id)){
                    nonZeros++;    
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)*CIRCLE_FEATURE_WEIGHT).append(" ");
                }
                id++;
         //   }
                
            //commented out, mean and var double values
            /*    
            //id 1466
            //added variance and mean features
            if(indexVector.containsKey(id)){
                nonZeros++;    
                stringBuilder.append(id).append(" ").append(indexVector.get(id)*NORMALIZED_VARIANCE_WEIGHT).append(" ");
            }
            id++;
            // mean feature // 
            //id 1467
            if(indexVector.containsKey(id)){
                nonZeros++;    
                stringBuilder.append(id).append(" ").append(indexVector.get(id)*NORMALIZED_MEAN_WEIGHT).append(" ");
            }
            id++;
            
                
                
                
            //relations
            while(id<1472){
                if(indexVector.containsKey(id)){
                    nonZeros++;
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)).append(" ");
                } 
                id++;
            }
            
        //////// textual features portion ///////// 2280 features
            //id 1472           
            while(id<3752){ //3748 became 3750 after variance and mean features
                if(indexVector.containsKey(id)){
                    nonZeros++;    
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)*TEXTUAL_FEATURE_WEIGHT).append(" ");
                }
                id++;
            }
            */ //end - mean and var double values
            
                
            //mean and var boolean intervals   
            while(id<1526){//1466 -> 1466 + 23 + 36 +1
                if(indexVector.containsKey(id)){
                    nonZeros++;    
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)*NORMALIZED_MEAN_AND_VARIANCE_WEIGHT).append(" ");
                }
                id++;
            }   
            //System.out.println("balanced mean start " + 1466 + " mean end" + 1525);
            //end of mean and var boolean intervals    
            

            //relations
            int relEnd = id + 5;
            while(id<relEnd){
                if(indexVector.containsKey(id)){
                    nonZeros++;
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)*NORMALIZED_RELATION_WEIGHT).append(" ");
                } 
                id++;
            }
            
            
            //textual features
            int texEnd = relEnd + 2280;
            while(id<texEnd){
                if(indexVector.containsKey(id)){
                    nonZeros++;    
                    stringBuilder.append(id).append(" ").append(indexVector.get(id)*NORMALIZED_TEXTUAL_FEATURE_WEIGHT).append(" ");
                }
                id++;
            }   
            //System.out.println("balanced tex start(relEnd) " + relEnd + " tex end" + texEnd);          
            
            
            
            
            //if area is bigger than max permited value, make the feature 1. 
            if(node.getGeometry().getArea() < maxArea){
                double normalizedAreaFeature = (node.getGeometry().getArea()/maxArea)*NORMALIZED_GEOMETRY_WEIGHT;                
                stringBuilder.append(columnSize+1).append(" ").append(normalizedAreaFeature).append(" "); 
                nonZeros++;
            }
            else{
                stringBuilder.append(columnSize+1).append(" ").append(NORMALIZED_GEOMETRY_WEIGHT).append(" ");
                nonZeros++;
            }

            //same for max Points
            if(node.getNumberOfNodes() < maxPoints){
                double normalizedPointsFeature =  ((double)node.getGeometry().getNumPoints()/maxPoints)*NORMALIZED_GEOMETRY_WEIGHT;
                stringBuilder.append(columnSize+2).append(" ").append(normalizedPointsFeature).append(" ");
                nonZeros++;
            }
            else{
                double normalizedPointsFeature = NORMALIZED_GEOMETRY_WEIGHT;
                stringBuilder.append(columnSize+2).append(" ").append(normalizedPointsFeature).append(" ");
                nonZeros++;
            }               
                
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////// END OF BALANCING FEATURE TYPES //////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////                
                
            String stringVector = stringBuilder.toString(); 
            printStream.println(stringVector);
        } //end of wayList iteration
            
        printStream.close();       
        String defineSparseMatrix = wayList.size() + " " + (columnSize+2) + " " + nonZeros;   //we add 2 in columnSize,
                                                        //because we added 2 more features in the vector for this matrix
        
        File inp = new File(vectorMatrixOutputFile);
        Scanner in = new Scanner(inp);
            try (BufferedWriter bwriter = new BufferedWriter(new FileWriter(new File(readyVectorsMatrix), true))) {   
                bwriter.write(defineSparseMatrix);
                bwriter.newLine();
                String line;

                while ((in.hasNext())) {  
                   line = in.nextLine();
                   bwriter.write(line);
                   bwriter.newLine();
                }
                new File(vectorMatrixOutputFile).delete();
            }
        }
        catch(IOException |ArrayIndexOutOfBoundsException e){
            System.err.println("Something went wrong producing the vectors matrix for the clustering process");
        }      
    }  
    
    //not used currently
    private ArrayList<Object> getMaximumValues(List<OSMWay> wayList){  
        ArrayList values = new ArrayList();  
        double maxArea = 0.0;
        double nodeArea;       
        int maxPoints = 0;
        int nodePoints;
        for (OSMWay node : wayList){
            nodeArea = node.getGeometry().getArea();
            if(nodeArea > maxArea){
               maxArea = nodeArea;               
            }
            nodePoints = node.getNumberOfNodes();
            if(nodePoints > maxPoints){
                maxPoints = nodePoints;
            }
        }        
        values.add(maxArea);
        values.add(maxPoints);
    return values;    
    }   
}
