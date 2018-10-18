
package gr.athenainnovation.imis.osmreccli.features;

import com.cybozu.labs.langdetect.LangDetectException;
import de.bwaldvogel.liblinear.FeatureNode;
import gr.athenainnovation.imis.osmreccli.container.OSMWay;
import gr.athenainnovation.imis.osmreccli.extractor.LanguageDetector;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;


/**
 * Constructs the textual features from the given textual list.
 * 
 * @author imis-nkarag
 */

public class TextualFeatures {
    
    private final int id;
    private final List<String> textualList;
    private static String language;
    private final LanguageDetector languageDetector;
    private final QueryParser greekParser;
    private final QueryParser englishParser;
    
    public TextualFeatures(int id, List<String> textualList, LanguageDetector languageDetector){
        this.id = id;
        this.textualList = textualList;
        this.languageDetector = languageDetector;
        GreekAnalyzer greekAnalyzer = new GreekAnalyzer();
        greekParser = new QueryParser("", greekAnalyzer);
        EnglishAnalyzer englishAnalyzer = new EnglishAnalyzer();
        englishParser = new QueryParser("", englishAnalyzer);        
    }
    
    public void createTextualFeatures(OSMWay wayNode) {        
        //namesList.indexOf(name) this index can be zero.
        //In that case it conflicts the previous geometry id, so we increment id. 
        //System.out.println("begin textual id from InstanceVectors = " + id);
        
        //idWords: populated with the ID that will be ginen as a feature, mapped with the word found.
        //Chose to store the name for future use.
        Map<Integer,String> idWords= new TreeMap<>(); 
        Map<String, String> tags = wayNode.getTagKeyValue();
        if (tags.keySet().contains("name")){           
            String nameTag = tags.get("name"); //get the value of the name tag of the current node
            String[] nameTagSplitList = nameTag.split("\\s");    //split the value to compare individually 
                                                                 //with the namesList 
            String lang = "";
            try {
                lang = detectLanguage(nameTag);
            } catch (LangDetectException ex) {
                Logger.getLogger(TextualFeatures.class.getName()).log(Level.SEVERE, null, ex);
            }

            for(String split : nameTagSplitList){
                try {
                    //System.out.println("split before: " + split);
                    //detectLanguage(split);

                    //TOGGLE
                    split = split.replaceAll("[-+.^:,?;'{}\"!()\\[\\]]", "");
                    if(lang.equals("el")){

                        //if(false){
                        split = stemGreek(split);
                        //System.out.println("en: split after stem: " + split);
                    }
                    else{
                        split = stemEnglish(split);
                        //System.out.println("el: split after stem: " + split);
                    }

                    if(textualList.contains(split)){
                        int currentID = textualList.indexOf(split) + id;
                        idWords.put(currentID, split);
                        //System.out.println(currentID + " " + split);
                    }
                } catch (ParseException ex) {
                    Logger.getLogger(TextualFeatures.class.getName()).log(Level.SEVERE, null, ex);
                }
            }     
            
            for(Integer wordID : idWords.keySet()){
                wayNode.getFeatureNodeList().add(new FeatureNode(wordID, 1.0));
                //System.out.println(wordID);
            }  
            //System.out.println("until textual " + wayNode.getFeatureNodeList());           
        }        
        //System.out.println("textual id (number of features):  " + (namesList.size()+id));
        //System.out.println("textual id (number of features):  " + numberOfFeatures);
    }
    
    public int getLastID(){
        return textualList.size()+id;
    }
    
    private String detectLanguage(String nameTag) throws LangDetectException{
        
        if(!nameTag.isEmpty()){
            language = languageDetector.detect(nameTag);
            return language;
        }
        else{
            return "no_lang";
        }
    }     
    
    private String stemGreek(String word) throws ParseException {
        String stemmedWord;

        if(!word.isEmpty()){
            stemmedWord = greekParser.parse(word).toString();
        }
        else{
            return word;
        }
        return stemmedWord;
    }
    
    private String stemEnglish(String word) throws ParseException{

        String stemmedWord;
        if(!word.isEmpty()){
            stemmedWord = englishParser.parse(word).toString();
        }
        else{
            return word;
        }
        return stemmedWord;
    }
}
