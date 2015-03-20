package gr.athenainnovation.imis.osmrecliblinear.parsers;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parses the ontology from the owl.xml file
 * The owl file contains information about the ontology and hierarchy of the classes
 * Provides methods for retrieving information about the ontology.
 * 
 * 
 * @author imis-nkarag
 */

public class Ontology {    
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(Ontology.class);

    private static String inputFileName; 
    private OntModel ontologyModel;
    private List<OntClass> listHierarchy;
    private static final int additiveID = 1373;     
    private Map<String, List<String>> indirectClasses;    
    private final Map<String, Integer> indirectClassesIDs;
    
    public Ontology(String path){
 
        inputFileName = path;
        indirectClassesIDs = new HashMap<>();
        indirectClasses = new HashMap<>();
        listHierarchy = new ArrayList();
        
    }
    
    public void parseOntology() {
        try {

        //create the ontology model using the base
        ontologyModel = ModelFactory.createOntologyModel();                
     
        InputStream inputFile = FileManager.get().open(inputFileName);
        if (inputFile == null) {
            throw new IllegalArgumentException("File: " + inputFileName + " not found");
        }
        
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.ERROR); 
        ontologyModel.read(inputFile, null);    //Hide RDFDefaultErrorHandler from terminal to keep clear output.
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
   
        listHierarchy = ontologyModel.listHierarchyRootClasses().toList();
        setListHierarchy(listHierarchy);  
        
        ExtendedIterator classes = ontologyModel.listClasses();
        while (classes.hasNext()) {
            String className;
            OntClass obj = (OntClass) classes.next();           
            
            //compare localname with class name from map and call getSuperclass     
            if (obj.hasSubClass()) {

                for (Iterator i = obj.listSubClasses(true); i.hasNext();) {
                    OntClass currentClass = (OntClass) i.next();
                    
                    List<OntClass> superClasses = currentClass.listSuperClasses().toList();                    
                    List<String> superClassesStrings = new ArrayList();
                    
                    for (OntClass superClass : superClasses){                       
                        className = superClass.toString().replace("http://linkedgeodata.org/ontology/", "");
                        superClassesStrings.add(className);
                    }
                    indirectClasses.put(currentClass.getLocalName(), superClassesStrings); 
                }
            }
        }
        createIndirectClassesWithIDs();
        setIndirectClasses(indirectClasses);
        setOntologyModel(ontologyModel);
        } 
        catch (IllegalArgumentException  e) {
             System.out.println(e.getMessage());
        } 
    LOG.info("Ontology from XML loaded!");  
    }
    
    private void createIndirectClassesWithIDs() {
        for (int i=0; i<(listHierarchy.size()); i++){
        
            String key = listHierarchy.get(i).toString().replace("http://linkedgeodata.org/ontology/", "");      
            //we add 1 to the ID because we want IDs beginning from 1. listHierarchy has index beginning from 0
            indirectClassesIDs.put(key, i + additiveID); //the IDs start from 1373 to avoid duplicate IDs at the vectorConstructor
        }        
    }
    
    private void setOntologyModel(OntModel ontologyModel){
        this.ontologyModel = ontologyModel;  
    }
    
    private void setIndirectClasses(Map<String, List<String>> indirectClasses){
        this.indirectClasses = indirectClasses;
        
    }
    
    private void setListHierarchy(List<OntClass> listHierarchy){
        this.listHierarchy = listHierarchy;
    }
            
    public OntModel getOntologyModel(){        
        return this.ontologyModel;
    }


    public Map<String, List<String>> getIndirectClasses(){
        return this.indirectClasses;
    }
    
    public List<OntClass> getListHierarchy(){
        return this.listHierarchy;
    }
    
    public Map<String, Integer> getIndirectClassesIDs(){
        return this.indirectClassesIDs;
    }
       
    //developing/debugging. 
    /*private static void writeToFile(String text) { 
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File("src/main/resources/output/classesTest.txt"), true))) {
            bw.write(text);
            bw.newLine();
        }                
        catch (IOException e) {
            System.err.println("write to file failed");
        }
    }
    
    private void sortFile() throws FileNotFoundException, IOException{        
    List<String> rows = new ArrayList<>();
    FileWriter writer;
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/output/classesTest.txt"))) {
            String s;
            while((s = reader.readLine())!=null) {
                rows.add(s);
            }       
            Collections.sort(rows);
            writer = new FileWriter("src/main/resources/output/sortedClassesTest.txt");
            for(String cur: rows){
                writer.write(cur+"\n");
            }   
        }
    writer.close();                      
    }*/       
}
