/*package gr.athenainnovation.imis.generator;

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
*/
/**
 * Generates input file for CLUTO's vcluster 
 * BalancedVectorsMatrix is preferred for better results. 
 * 
 * 
 * @author imis-nkarag
 */

/*
public class VectorsMatrix {
    private final List<OSMWay> wayList;
    private final String vectorMatrixOutputFile;
    private final String readyVectorsMatrix;
    
    public VectorsMatrix(List<OSMWay> wayList, String vectorMatrixOutputFile){
        this.vectorMatrixOutputFile = vectorMatrixOutputFile;
        this.wayList = wayList;
        readyVectorsMatrix = vectorMatrixOutputFile + ".mat";
    }
    
    public void generateVectorsMatrix(){
        new File(vectorMatrixOutputFile).delete();
        new File(readyVectorsMatrix).delete();
        int nonZeros = 0;
        int columnSize = wayList.get(1).getVector().size();
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(vectorMatrixOutputFile,true)); 
        PrintStream printStream = new PrintStream(output)){
            
            
            for (OSMWay node : wayList){
                ArrayList<Integer> vector = node.getVector();
                StringBuilder stringBuilder = new StringBuilder("");                
                for(int k=0; k<vector.size(); k++){    
                   if(vector.get(k) != 0){
                   nonZeros++;    

                   stringBuilder.append(k).append(" ").append(vector.get(k)).append(" ");
                   }
 
                }                
                String stringVector = stringBuilder.toString(); 
                printStream.println(stringVector);
            }
            
        printStream.close();
        String defineSparseMatrix = wayList.size() + " " + columnSize + " " + nonZeros;
        
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
        catch(IOException e){
            System.err.println("Something went wrong producing the vectors matrix for the clustering process");
        }
    }    
}
*/