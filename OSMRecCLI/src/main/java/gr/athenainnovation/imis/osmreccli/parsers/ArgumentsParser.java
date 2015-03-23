
package gr.athenainnovation.imis.osmreccli.parsers;

/**
 * Class that validates/keeps the CLI argument information.
 * 
 * @author imis-nkarag
 */

public class ArgumentsParser {
    private static int trainAlgorithm = 0;
    private static boolean wrongArguments = false;
    private boolean trainMode = false;
    private static boolean testMode = false;
    private String osmFile = null;
    private Double confParameter = null;
    private static Integer averageInstancesPerCluster = null;
    private String model = "model0";
    private String recommendationsFileSVM = "output.txt";
    private static String recommendationsFileClustering = "output.txt";


    
    public ArgumentsParser(){
        trainAlgorithm = 0;
        wrongArguments = false;
        trainMode = false;
        testMode = false;
        osmFile = null;
        confParameter = null;
        averageInstancesPerCluster = null;
        model = null;
        recommendationsFileSVM = "output.txt";
        recommendationsFileClustering = "output.txt";
        //parseArguments(args);
    }
    
    public ArgumentsParser parseArguments(String[] args){
         
        //Double confParameter = null;
        //osmFile = null;
        String arg;
        String value;
        int i =0;
        while (i < args.length){

           arg = args[i];
           if(arg.startsWith("-")){ 
	       if(arg.equals("-help")){
		   System.out.println("Usage:\n java -jar OSMRec-1.0.jar -train -i inputFile "
                           + "[-c confParameter] [-k averageSize] [-m model]\n or\n"
			   + "java -jar OSMRec-1.0.jar -test -i inputFile [-m model] [-o outputFile]\n");
		   System.out.println(""		      
			    + "-i requires a filename: [-i inputFile]\n"
			    + "-c requires a number parameter: [-c confParameter]\n"
			    + "-m requires a filename for the model: [-m model]\n");	
	       System.exit(0);	   
	       }
	       value = args[i+1]; 
	       if(arg.equals("-train")){
                   trainAlgorithm = 1;
                   trainMode = true;
		   System.out.println("Train mode on!");
//		   if(value.equals("1")){              
//		       
//		       System.out.println(" - SVM training on spatial entities as items");
//		   }
//		   else if(value.equals("2")){
//		       trainAlgorithm = 2;
//                       System.out.println(" - Clustering of spatial entities and k-NN algorithm on clusters of entities");
//		   }
//		   else if(value.equals("3")){
//		       trainAlgorithm = 3;
//                       System.out.println(" - KNN of spatial entities as items");
//                       //wrongArguments = true;
//		   }
//		   else{
//		       System.out.println("Train algorithm takes values 1,2 or 3: [-train algorithm] \n"
//                               + "1    SVM training on spatial entities as items\n"
//			       + "2    Clustering of spatial entities and k-NN algorithm on clusters of entities\n"
//                               + "3    KNN of spatial entities as items\n");
//                       wrongArguments = true;
//		   }		   
	       }
	       else if(arg.equals("-test")){
                   testMode = true;
		   System.out.println("Test mode on!");
//		   if(value.equals("1")){              
//		       trainAlgorithm = 1;
//		   }
//		   else if(value.equals("2")){
//		       trainAlgorithm = 2;
//		   }
//		   else if(value.equals("3")){
//		       trainAlgorithm = 3;
//		   }
//		   else{
//		       System.out.println("Train algorithm takes values 1,2 or 3: [-test algorithm] \n"
//                                + "1    SVM training on spatial entities as items\n"
//                                + "2    Clustering of spatial entities and k-NN algorithm on clusters of entities\n"
//                                + "3    KNN of spatial entities as items\n");
//                       wrongArguments = true;
//		   }		   
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
       if (args.length == 0 || getOsmFile() == null || isWrongArguments()){
	   System.err.println("Usage:\n java -jar OSMRec-1.0.jar -train "
                    + "-i inputFile [-c confParameter] [-m model]\n or\n"
                    + "java -jar OSMRec-1.0.jar -test -i inputFile [-o outputFile]\n");
	   System.exit(0);
       }
       
    return this;   
    }
    
     /**
     * @return the trainAlgorithm
     */
    public static int getTrainAlgorithm() {
        return trainAlgorithm;
    }

    /**
     * @return the wrongArguments
     */
    public static boolean isWrongArguments() {
        return wrongArguments;
    }

    /**
     * @return the trainMode
     */
    public boolean isTrainMode() {
        return trainMode;
    }

    /**
     * @return the testMode
     */
    public static boolean isTestMode() {
        return testMode;
    }

    /**
     * @return the osmFile
     */
    public String getOsmFile() {
        return osmFile;
    }

    /**
     * @return the confParameter
     */
    public Double getConfParameter() {
        return confParameter;
    }

    /**
     * @return the averageInstancesPerCluster
     */
    public static Integer getAverageInstancesPerCluster() {
        return averageInstancesPerCluster;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @return the recommendationsFileSVM
     */
    public String getRecommendationsFileSVM() {
        return recommendationsFileSVM;
    }

    /**
     * @return the recommendationsFileClustering
     */
    public static String getRecommendationsFileClustering() {
        return recommendationsFileClustering;
    }
    
}
