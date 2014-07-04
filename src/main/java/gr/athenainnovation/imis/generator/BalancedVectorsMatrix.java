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
    
    private static final double CLASS_FEATURE_WEIGHT = 9; 
    private static final double GEOMETRY_TYPE_FEATURE_WEIGHT = 15.75;   
    private static final double RECTANGLE_FEATURE_WEIGHT = 31.5;    
    private static final double NUMBER_OF_POINTS_FEATURE_WEIGHT = 31.5;
    private static final double AREA_FEATURE_WEIGHT = 31.5;   
    private static final double CIRCLE_FEATURE_WEIGHT = 31.5;
    private static final double TEXTUAL_FEATURE_WEIGHT = 7;
    private static final double NORMALIZED_GEOMETRY_WEIGHT = 31.5;    
    private final List<OSMWay> wayList;
    private final String vectorMatrixOutputFile;
    private final String readyVectorsMatrix;
    private final int columnSize;
    
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
            for(int k=0; k<1421; k++){    
                if(indexVector.containsKey(k)){                      
                    stringBuilder.append(k).append(" ").append(indexVector.get(k)*CLASS_FEATURE_WEIGHT).append(" ");
                    nonZeros++;
                } 
            } 
                
        //////////// geometry type features portion /////////              
            for(int k=1421; k<1425; k++){    
                if(indexVector.containsKey(k)){
                    nonZeros++;    
                    stringBuilder.append(k).append(" ").append(indexVector.get(k)*GEOMETRY_TYPE_FEATURE_WEIGHT).append(" ");
                }
            }
        //////// rectangle feature /////////             
            for(int k=1425; k<1426; k++){    
                if(indexVector.containsKey(k)){
                    nonZeros++;    
                    stringBuilder.append(k).append(" ").append(indexVector.get(k)*RECTANGLE_FEATURE_WEIGHT).append(" ");
                }
            }
        //////// number of points features portion /////////          
            for(int k=1426; k<1439; k++){    
                if(indexVector.containsKey(k)){
                    nonZeros++;    
                    stringBuilder.append(k).append(" ").append(indexVector.get(k)*NUMBER_OF_POINTS_FEATURE_WEIGHT).append(" ");
                } 
            }
        //////// area features portion /////////          
            for(int k=1439; k<1464; k++){    
                if(indexVector.containsKey(k)){
                    nonZeros++;    
                    stringBuilder.append(k).append(" ").append(indexVector.get(k)*AREA_FEATURE_WEIGHT).append(" ");
                }
            } 
                
        //////// circle features portion /////////          
            for(int k=1464; k<1465; k++){    
                if(indexVector.containsKey(k)){
                    nonZeros++;    
                    stringBuilder.append(k).append(" ").append(indexVector.get(k)*CIRCLE_FEATURE_WEIGHT).append(" ");
                }
            } 
                
        //////// textual features portion /////////          
            for(int k=1465; k<3748; k++){    
                if(indexVector.containsKey(k)){
                    nonZeros++;    
                    stringBuilder.append(k).append(" ").append(indexVector.get(k)*TEXTUAL_FEATURE_WEIGHT).append(" ");
                }
            }
                
            //if area is bigger than max permited value, make the feature 1. 
            if(node.getGeometry().getArea() < maxArea){
                double normalizedAreaFeature = (node.getGeometry().getArea()/maxArea)*NORMALIZED_GEOMETRY_WEIGHT;                
                stringBuilder.append(columnSize+1).append(" ").append(normalizedAreaFeature).append(" "); 
                nonZeros++;
            }
            else{
                double normalizedAreaFeature = NORMALIZED_GEOMETRY_WEIGHT;
                stringBuilder.append(columnSize+1).append(" ").append(normalizedAreaFeature).append(" ");
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
