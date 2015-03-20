
package gr.athenainnovation.imis.osmrecliblinear.container;

import de.bwaldvogel.liblinear.FeatureNode;

/**
 *
 * @author imis-nkarag
 */
public class OSMFeature extends FeatureNode {

    public OSMFeature(int index, double value) {
        super(index, value);
    }

    @Override
    public int getIndex() {
        return index; 
    }

    @Override
    public double getValue() {
        return value; 
    }    
}
