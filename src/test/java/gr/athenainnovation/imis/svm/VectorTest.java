package gr.athenainnovation.imis.svm;

import gr.athenainnovation.imis.OSMContainer.OSMWay;
import gr.athenainnovation.imis.parsers.OSMParser;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author imis-nkarag
 */
public class VectorTest extends TestCase {
    
    public VectorTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of constructWayVectors method, of class Vector.
     */
    
    public void testConstructWayVectors() {
        System.out.println("testing \"constructWayVectors\"...");
        OSMParser osmParser = new OSMParser("/home/imis-nkarag/Desktop/attiki.osm");
        List<OSMWay> wayList =osmParser.getWayList();
        //List<OSMWay> wayList = osmParser;
        //Vector instance = new;
        
        //constructWayVectors(wayList);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }
    
}
