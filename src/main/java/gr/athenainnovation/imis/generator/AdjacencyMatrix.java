
package gr.athenainnovation.imis.generator;

import com.vividsolutions.jts.geom.Geometry;
import gr.athenainnovation.imis.OSMContainer.OSMWay;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Generates an adjacency matrix to be used for input in the scluster process
 * However BalancedVectorMatrix with vcluster process is preferred for better performance and results.
 * 
 * @author imis-nkarag
 */

public class AdjacencyMatrix {
    private final List<OSMWay> wayList;
    private final String path;
    private final String readyMatrixFile;
    
    public AdjacencyMatrix(List<OSMWay> wayList, int numNodes, String path) {
        this.wayList = wayList;
        this.path = path + "/classes/output/matrix";  
        readyMatrixFile = path + "/classes/output/matrix.graph";
    }
    
    public void constructMatrix() throws FileNotFoundException, IOException{
        new File(path).delete();
        new File(readyMatrixFile).delete();
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(path,true)); 
        PrintStream printStream = new PrintStream(output)){
        System.out.println("constructing matrix...");
        int numberOfNonZeros=0; 
        int i=1;    
        
        for(OSMWay irow : wayList){
        if(wayList.size()/1000 == i || wayList.size()/100 == i || wayList.size()/10 == i || wayList.size()/5 == i){
            System.out.print(".");
        }    
        int j = 1;    
        String line = "";       

        StringBuilder sb = new StringBuilder("");
            for(OSMWay jcolumn : wayList){
                if(i==j){
                    sb.append(line).append(" ").append(j).append(" 1.000000");
                    numberOfNonZeros++;
                }    
                else{

                    ArrayList<Integer> vectorA = irow.getVector();
                    ArrayList<Integer> vectorB = jcolumn.getVector();
                    double cosineSimilarity = cosineSimilarity(vectorA, vectorB); //compute cosine similarity
                    
                    //compute geometry feature similarity
                    Geometry geomA = irow.getGeometry();
                    Geometry geomB = jcolumn.getGeometry();
                    double pointsSimilarity = pointsSimilarity(geomA.getNumPoints(), geomB.getNumPoints());
                    double areaSimilarity = areaSimilarity(geomA.getArea(), geomB.getArea());
                    double geometryTypeSimilarity = geometryTypeSimilarity(geomA.getGeometryType(), geomB.getGeometryType());
                    
                    double similarity = (geometryTypeSimilarity + pointsSimilarity + areaSimilarity + cosineSimilarity)/4;
                    //take average, format it to String with 6 decimal places                    
                    
                    DecimalFormat similarityDecimalFormat = new DecimalFormat("0.000000");
                    String sim = similarityDecimalFormat.format(similarity);
                    
                    if(!sim.equals("0.000000")){
                        sb.append(line).append(" ").append(j).append(" ").append(sim);
                        numberOfNonZeros++;
                    }
                }                    
            j++;    
            }

            String vect = sb.toString();
            printStream.println(vect);

        i++;    
        }
        printStream.close();
        String defineSparseMatrix = wayList.size() + " " + numberOfNonZeros; //for sparse matrix

        File inp = new File(path);
        Scanner in = new Scanner(inp);
        try (BufferedWriter bwriter = new BufferedWriter(new FileWriter(new File(readyMatrixFile), true))) {   
            bwriter.write(defineSparseMatrix);
            bwriter.newLine();
            String line;
            int o = 0;
            while ((in.hasNext())) {  
               line = in.nextLine();
               
               bwriter.write(line);
               bwriter.newLine();
               o++; 
            }
            new File(path).delete();
        }
    System.out.println("size: " + wayList.size() + " non zeros: " + numberOfNonZeros);
    }  
       
  }
    private static double cosineSimilarity(ArrayList<Integer> vectorA, ArrayList<Integer> vectorB){
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i)*vectorB.get(i);
            normA += vectorA.get(i)*vectorA.get(i);
            normB += vectorB.get(i)*vectorB.get(i);
        }  
        double cosSim =  (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
        return cosSim;
    }
    
    private static double pointsSimilarity(int pointsA, int pointsB){
        int geometricSimilarity;
            if(pointsA > pointsB){
                geometricSimilarity = 1-((pointsA - pointsB)/pointsA);
                return geometricSimilarity;
            }
            else if(pointsA < pointsB){
                geometricSimilarity = 1-((pointsB - pointsA)/pointsB);
                return geometricSimilarity;
            }
            else{
                return 1.0;
            }              
    } 
    
    private static double areaSimilarity(double areaA, double areaB){
        
        double areaSimilarity;
        if(areaA > areaB){
            areaSimilarity = 1-((areaA - areaB)/areaA);
            return areaSimilarity;
        }
        else if(areaA < areaB){
            areaSimilarity = 1-((areaB - areaA)/areaB);
            return areaSimilarity;
        }
        else{
           return 1.0; 
        }

    }
    
    private static double geometryTypeSimilarity(String typeA, String typeB){
        if(typeA.equals(typeB)){
            return 1.0;
        }
        else{
            return 0;
        }
    }   
}
