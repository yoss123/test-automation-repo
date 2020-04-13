package automation.testing.support;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DifferenceEvaluator;

public class IgnorElementsAndAttributesInDifferenceEvaluator implements DifferenceEvaluator {
	
	public static final int ACCEPTABLE_POSITION_OFFSET = 3;
	
	private String[] attributeNamesToIgnor;
	private String[] elementNamesToIgnor;
	
	public IgnorElementsAndAttributesInDifferenceEvaluator(String[] attributeNamesToIgnor, String[] elementNamesToIgnor) {
		this.attributeNamesToIgnor = attributeNamesToIgnor;
		this.elementNamesToIgnor = elementNamesToIgnor;
	}
	
	public IgnorElementsAndAttributesInDifferenceEvaluator() {}

	public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
		
		if (outcome == ComparisonResult.EQUAL) return outcome; // only evaluate differences.
        
		Node comparisonControlNode = comparison.getControlDetails().getTarget();
		
		String comparisonControlAttrValue = null;
		Node comparisonTestAttrNode = null;
		String comparisonTestAttValue = null;
        if (comparisonControlNode instanceof Attr) {
            Attr comparisonAttr = (Attr) comparisonControlNode;
            
            if(comparisonAttr.getName().equals("Id") && comparisonAttr.getOwnerElement().getTagName().equals("ProprietaryInfo")) {
            	return ComparisonResult.SIMILAR;
            }
            
            // Ignore attributes sent as parameter
            if(attributeNamesToIgnor != null) {
	            for(int arrayCounter=0 ; arrayCounter<attributeNamesToIgnor.length ; arrayCounter++) {
		            if (comparisonAttr.getName().equals(attributeNamesToIgnor[arrayCounter])) {
		            	return ComparisonResult.SIMILAR;
		            }
	            }
            }
            
            // Ignore elements sent as parameter
            if(elementNamesToIgnor != null) {
//            	System.out.println("comparisonAttr.getOwnerElement().getTagName()="+comparisonAttr.getOwnerElement().getTagName());
//            	comparisonAttr.getOwnerElement().getTagName().equals("Matching")
	            for(int arrayCounter=0 ; arrayCounter<elementNamesToIgnor.length ; arrayCounter++) {
		            if (comparisonAttr.getOwnerElement().getTagName().equals(elementNamesToIgnor[arrayCounter])) {
		                return ComparisonResult.SIMILAR;
		            }
	            }
            }
            
            // Ignore position attributes with ACCEPTABLE_POSITION_OFFSET
            if (comparisonAttr.getName().equals("X") || comparisonAttr.getName().equals("Y")) {
            	comparisonControlAttrValue = comparisonAttr.getValue();
            	comparisonTestAttrNode = comparison.getTestDetails().getTarget();
            	comparisonTestAttValue = comparisonTestAttrNode.getNodeValue();
            	
            	int comparisonControlIntValue = Integer.parseInt(comparisonControlAttrValue);
            	int comparisonTestIntValue = Integer.parseInt(comparisonTestAttValue);
            	int absoluteValue = Math.abs(comparisonControlIntValue-comparisonTestIntValue);
            	
            	if(absoluteValue < IgnorElementsAndAttributesInDifferenceEvaluator.ACCEPTABLE_POSITION_OFFSET) {
            		return ComparisonResult.SIMILAR; // will evaluate this difference as similar
            	}
            }            
            
        }  else if (comparisonControlNode instanceof Element) {
        	Element comparisonControlElement = (Element) comparisonControlNode;
        	String elementTagName = comparisonControlElement.getTagName();
        	if(elementNamesToIgnor != null) {
            	for(int arrayCounter=0 ; arrayCounter<elementNamesToIgnor.length ; arrayCounter++) {
		            if (comparisonControlElement.getTagName().equals(elementNamesToIgnor[arrayCounter])) {
		                return ComparisonResult.SIMILAR;
		            }
	            }
            }
		}
        
        return outcome;
	}

}
