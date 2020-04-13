package automation.testing.support;

import org.w3c.dom.Attr;
import org.xmlunit.util.Predicate;

public class AttributesFilterForXmlComparison implements Predicate<Attr> {

	String[] attributesToFilter;
	
	public AttributesFilterForXmlComparison(String[] attributesToFilter) {
		this.attributesToFilter = attributesToFilter;
	}
	
	public boolean test(Attr a) {
		String attrName = a.getName();
		for(int arrayCounter=0 ; arrayCounter < attributesToFilter.length ; arrayCounter++) {
			if(attrName.equals(attributesToFilter[arrayCounter])) return false;
		}
		return true;
	}

}
