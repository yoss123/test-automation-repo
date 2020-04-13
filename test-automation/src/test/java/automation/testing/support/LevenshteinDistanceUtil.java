package automation.testing.support;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class LevenshteinDistanceUtil {
	
	public static int getLevenshteinDistanceInPercentage(Object actualValue, Object referenceValue) {
		
		String actualValueAsStr = null, referenceValueAsStr = null;
		
		if(actualValue != null) {
			actualValueAsStr = actualValue.toString().toLowerCase();
		} else {
			actualValueAsStr = "";
		}
		if(referenceValue != null) {
			referenceValueAsStr = referenceValue.toString().toLowerCase();
		} else {
			referenceValueAsStr = "";
		}
		
		int distance = LevenshteinDistance.getDefaultInstance().apply(actualValueAsStr, referenceValueAsStr);
		
		if(((actualValueAsStr.length() == 0) && (referenceValueAsStr.length() != 0)) ||
				((actualValueAsStr.length() != 0) && (referenceValueAsStr.length() == 0))) {
			return 0;
		}
		
		if(distance <= actualValueAsStr.length()) {
			return (int) (100 - (((double) distance) / actualValueAsStr.length() * 100));
		} else {
			return (int) (100 - (((double) distance) / referenceValueAsStr.length() * 100));
		}
	}
	
	public static int distanceRatio(int distance, String referenceVerbiage, String actualVerbiage)
	{
		if(distance <= actualVerbiage.length()) {
			return (int) (100 - (((double) distance) / actualVerbiage.length() * 100));
		} else {
			return (int) (100 - (((double) distance) / referenceVerbiage.length() * 100));
		}
	}
	
	/*
	 * Avi's code from CrosswalkService.java
	 * remove any non alpha words and gcw template variables (i.e. <<amount>>)
	 */
	
	public static String toAlphaSpace(String verbiage)
	{
		return toAlphaSpace(verbiage,true);
	}
	
	/*
	 * Avi's code from CrosswalkService.java
	 * remove any non alpha words 
	 * preserveVariableTemplate param - to remove also template variables (i.e. <<amount>>)
	 */
	public static String toAlphaSpace(String verbiage,boolean preserveVariableTemplate)
	{
		String regex = (preserveVariableTemplate ? "<<.*>>|":"") + "[\\W]|_";
		
		String[] words = verbiage.split(" ");
		List<String> alpha_words = new ArrayList<String>();//new ArrayList()<>(words.length);

		for (String word : words) 
		{
			String word_alnum = word.replaceAll(regex, "");

			if (StringUtils.isAlphaSpace(word_alnum) && StringUtils.isNotBlank(word_alnum))
				alpha_words.add(StringUtils.trimToEmpty(word_alnum));
		}

		return alpha_words.stream().collect(Collectors.joining(" "));
	}
	
	
	/*
	 * Avi's code from CrosswalkService.java
	 * Measure distance between two strings 
	 */
	public static int distance(String left, String right)
	{
		if(left == null)
			return StringUtils.length(right);
		if(right == null)
			return StringUtils.length(left);
		
		if(StringUtils.equalsIgnoreCase(left, right))
			return 0;
		
		LevenshteinDistance distance = LevenshteinDistance.getDefaultInstance();
		
		return distance.apply(StringUtils.upperCase(left), StringUtils.upperCase(right));
	}
}
