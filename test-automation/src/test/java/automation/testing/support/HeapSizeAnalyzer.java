package automation.testing.support;

import org.apache.logging.log4j.Logger;

public class HeapSizeAnalyzer {
	
	public static long KB = 1024;
	public static long MB = 1024 * 1024;
	public static long GB = 1024 * 1024 * 1024;
	
	
	public static void logHeapSize(Logger logger, String messageBeforeLogingData) {
		
		if(messageBeforeLogingData != null) {
			logger.info("Logging Heap Data "+messageBeforeLogingData);
		}
		
		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
		logger.info("Maximum TOTAL heap size: "+formatSize(Runtime.getRuntime().maxMemory()));
		
		// Get current size of heap in bytes
		logger.info("Current USED heap size: "+formatSize(Runtime.getRuntime().totalMemory()));
		
		// Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
		logger.info("Free AVAILABLE heap size: "+formatSize(Runtime.getRuntime().freeMemory()));
	}

	private static String formatSize(long memorySize) {
		
		if(memorySize > GB) {
			return String.format("%.9f", (float) memorySize / GB) + " GB";
		} else if(memorySize > MB) {
			return String.format("%.6f", (float) memorySize / MB) + " MB";
		} else if(memorySize > KB) {
			return String.format("%.3f", (float) memorySize / KB) + " KB";
		} else {
			return memorySize + " B";
		}
	}

}
