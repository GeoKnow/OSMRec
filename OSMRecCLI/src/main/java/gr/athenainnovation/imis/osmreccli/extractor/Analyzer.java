
package gr.athenainnovation.imis.osmreccli.extractor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 * Analyzes textual information. Languager detection, stop words removal, stemming based on language.
 * Provides methods for retrieving the textual list by frequency and top-K terms.
 * 
 * @author imis-nkarag
 */

public class Analyzer {
    
    private final String osmFilePath;
    private static final HashSet<String> stopWordsList = new HashSet<>(); //add greek list to same file
    private String language;
    private ArrayList<Entry<String, Integer>> frequencies;
    private final LanguageDetector languageDetector;
    private final String stopWordsPath;
    
    public Analyzer(String osmFilePath, LanguageDetector languageDetector, String stopWordsPath){
        this.osmFilePath = osmFilePath;
        this.languageDetector = languageDetector;
        this.stopWordsPath = stopWordsPath;
    }
    
    public void runAnalysis() {
        //textual list
        FrequenceExtractor frequenceExtractor = new FrequenceExtractor(osmFilePath);
        frequenceExtractor.parseDocument();
        Set<Map.Entry<String, Integer>> frequencyEntries = frequenceExtractor.getFrequency().entrySet();
        
        //parse stop words
        loadStopWords();
        
        //detect language        
//        String  profilesDirectory = "/home/imis-nkarag/software/OSMRec_LIBLINEAR/OSMRecLIBLINEAR/src/main/resources/profiles";
//        LanguageDetector languageDetector = new LanguageDetector();
//        languageDetector.init(profilesDirectory);
        
        //send some samples
        ArrayList<Map.Entry<String, Integer>> normalizedList = new ArrayList<>();
        ArrayList<String> sampleList = new ArrayList<>();
        int iters = 0;  
        for(Map.Entry<String, Integer> frequencyEntry : frequencyEntries){
            if(iters <10){
                sampleList.add(frequencyEntry.getKey());
                iters++;
            }
            //remove parenthesis etc here
            if(!stopWordsList.contains(frequencyEntry.getKey())){//stopwords
                String normalizedName = frequencyEntry.getKey().toLowerCase();
                normalizedName = normalizedName.replaceAll("[-+.^:,?;'{}\"!()\\[\\]]", "");

                AbstractMap.SimpleEntry<String, Integer> normalizedEntry = new AbstractMap.SimpleEntry<>(normalizedName,frequencyEntry.getValue());                       
                normalizedList.add(normalizedEntry);
            }
        }

        int en=0;
        int el=0;
        for(String word : sampleList){
            //System.out.println("to be detected: " + word);
            if(!word.isEmpty()){
                String lang = languageDetector.detect(word);
                if(lang.equals("en")){
                    en++;
                }
                else if(lang.equals("el")){
                    el++;
                }
                else {
                    //other lang, no support yet
                    //System.out.println("found other language, no support yet :(");
                }
                //System.out.println("glwssa: " + lang);
            }
        }
        if(el>en){
            language = "el"; 
            normalizedList = stemGreek(normalizedList);
        }
        else{
            language = "en";
            normalizedList = stemEnglish(normalizedList);
        }
        
        Collections.sort( normalizedList, new Comparator<Map.Entry<String, Integer>>()
        {
            @Override
            public int compare( Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        } );
        
        setFrequencies(normalizedList);        
        //end of textual list
    }
    
    private ArrayList<Map.Entry<String, Integer>> stemGreek(ArrayList<Map.Entry<String, Integer>> normalizedList) {
        org.apache.lucene.analysis.Analyzer greekAnalyzer = new GreekAnalyzer();

        QueryParser greekParser = new QueryParser("", greekAnalyzer);
        ArrayList<Map.Entry<String, Integer>> stemmedList = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : normalizedList){
            if(!entry.getKey().isEmpty()){
                try {
                    //System.out.println("result: " + greekParser.parse(entry.getKey())); 
                    String stemmedWord = greekParser.parse(entry.getKey()).toString();
                    SimpleEntry<String, Integer> stemmed = new SimpleEntry<>(stemmedWord, entry.getValue());
                    stemmedList.add(stemmed);
                    
                } catch (ParseException ex) {
                    Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }  
        return stemmedList;
    }
    
    private ArrayList<Map.Entry<String, Integer>> stemEnglish(ArrayList<Map.Entry<String, Integer>> normalizedList){
        org.apache.lucene.analysis.Analyzer englishAnalyzer = new EnglishAnalyzer();
        QueryParser englishParser = new QueryParser("", englishAnalyzer);
        ArrayList<Map.Entry<String, Integer>> stemmedList = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : normalizedList){
            if(!entry.getKey().isEmpty()){
                try {
                    //System.out.println("result: " + englishParser.parse(entry.getKey())); 
                    String stemmedWord = englishParser.parse(entry.getKey()).toString();
                    SimpleEntry<String, Integer> stemmed = new SimpleEntry<>(stemmedWord, entry.getValue());
                    stemmedList.add(stemmed);
                } catch (ParseException ex) {
                    Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }  
        return stemmedList;
    }
    
    private void loadStopWords(){
        //parse stopwordsList
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(stopWordsPath);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fstream))) {
            String strLine;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                //System.out.println (strLine);
                stopWordsList.add(strLine);

            }

            //System.out.println(stopWordsList.size());

        } catch (IOException ex) {
        Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
        //return stopWordsList;
    }
    
    private void setFrequencies(ArrayList<Map.Entry<String, Integer>> frequencies){
        this.frequencies = frequencies;
        
    }
    
    public List<Map.Entry<String, Integer>> getFrequencies(){       
        return frequencies;
    }
    
    public List<Map.Entry<String, Integer>> getTopKMostFrequent(int topK){
        return frequencies.subList(0, topK);
    }
    
    public List<Map.Entry<String, Integer>> getWithFrequency(int minFrequency){
        ArrayList<Map.Entry<String, Integer>> withFrequency = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : frequencies){
            if(entry.getValue()> minFrequency){
                withFrequency.add(entry);
            }
            else{
                return withFrequency;
            }
        }
        return withFrequency;
    }
    
    public List<String> getCompleteTextualList(){
        List<String> completeTextualList = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : frequencies){
            completeTextualList.add(entry.getKey());
        }
        return completeTextualList;
    }
    
//    public void detectLanguage(){
//        
//    }
}
//exclude empty words from normalized list. 
//kapoia meta to normalize exoun ginei kena. deal with them