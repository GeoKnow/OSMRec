
package gr.athenainnovation.imis.osmrecliblinear.statistics;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects language of osm textual information
 * 
 * @author imis-nkarag
 */

public class LanguageDetector {
    
    private static LanguageDetector languageDetector = null;
    
    private LanguageDetector(){
        //prevent instatiation
    }
    
    public static LanguageDetector getInstance(String languageProfilesPath){
        System.out.println("language profile path: \n" + languageProfilesPath + "/el");
        if(languageDetector == null){
            try {
                languageDetector = new LanguageDetector();
                init(languageProfilesPath);
            } catch (LangDetectException ex) {
                Logger.getLogger(LanguageDetector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return languageDetector;        
    }
    
    private static void init(String profileDirectory) throws LangDetectException {
                
        DetectorFactory.loadProfile(profileDirectory);
    }   
    
    public String detect(String text) {
        try {
            Detector detector = DetectorFactory.create();
            detector.append(text);
            return detector.detect();
        } catch (LangDetectException ex) {
            Logger.getLogger(LanguageDetector.class.getName()).log(Level.SEVERE, null, ex);
            return "en";
        }
    }       
}
