package automation.testing.tools;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.testng.annotations.AfterClass;

import automation.testing.tests.TestBase;

public class ToolBase extends TestBase {
	
	public static final String SYSTEM_PROPERTY_DATE_FROM = "DateFrom";
	public static final String SYSTEM_PROPERTY_TIME_FROM = "TimeFrom";
	public static final String SYSTEM_PROPERTY_DATE_TO = "DateTo";
	public static final String SYSTEM_PROPERTY_TIME_TO = "TimeTo";
	
	public static final String LOCAL_TEMP_DIRECTORY = "tempDir";
	
	public static final int NO_LIMIT_FOR_NUMBER_OF_RECORDS = -1;
	
	public static final String REMOTE_DIRECTORY_OF_REFERENCE_FILE_USER_NAME_AUTH ="adminUser";
	public static final String REMOTE_DIRECTORY_OF_REFERENCE_FILE_PASSWORD_AUTH ="password";
	public static final String REMOTE_DIRECTORY_OF_REFERENCE_FILE_DOMAIN ="\\\\200.20.20.20\\h$";
	public static final String REMOTE_DIRECTORY_OF_REFERENCE_FILE = "\\\\200.20.20.20\\h$\\templates\\";
	
//	public static final int MAX_ITEM_WITH_SAME_NAME = 1000;
	
	protected String startDateOfSample = null;
	protected String startTimeOfSample = null;
	protected String toDateOfSample = null;
	protected String toTimeOfSample = null;
	
	// Amazon S3 Prod parameters
	protected String amazonS3ProdClientRegion = null;
	protected String amazonS3ProdAccessKeyId = null;
	protected String amazonS3ProdSecretKeyId = null;
		
	protected String tempDirectoryToDownloadFilesTo = null;
	
	protected String getTempDirectoryPathToDownloadItemsTo() {
		if(this.tempDirectoryToDownloadFilesTo == null) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss");  
		    Date date = new Date();
		    this.tempDirectoryToDownloadFilesTo = LOCAL_TEMP_DIRECTORY + "_" + formatter.format(date);
		    File tempDir = new File(this.tempDirectoryToDownloadFilesTo);
		    boolean isTempDirCreated = tempDir.mkdir();
		    if(isTempDirCreated) {
		    	logger.info("Temp directory named "+this.tempDirectoryToDownloadFilesTo+" has created");
		    } else {
		    	logger.error("Failed to create temp directory named "+this.tempDirectoryToDownloadFilesTo);
		    }
		}
		
		return this.tempDirectoryToDownloadFilesTo;
	}
	
	protected void deleteTempDirectory() {
		File tempDir = new File(getTempDirectoryPathToDownloadItemsTo());
		try {
			if(tempDir.exists()) {
				FileUtils.cleanDirectory(tempDir);
				FileUtils.deleteDirectory(tempDir);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public void tearDown() {
		
		logger.info("Finished executing test suite");
		
		webDriver.quit();
		
		deleteTempDirectory();
	}
	
	public static String getStringValueOfToolParameter(String parameterPrefix, String parameterValueFromTestNgXml, String systemPropertyName) {
		String systemPropertyValueAsString = System.getProperty(systemPropertyName); // When running the test from Jenkins then the report file name and path defined there because this flexibility is needed. Otherwise, take it from the test suite xml file
		if(systemPropertyValueAsString != null) {
			systemPropertyValueAsString = parameterPrefix+systemPropertyValueAsString;
			logger.info("Set "+systemPropertyName+" as "+systemPropertyValueAsString+" from System.property");
			return systemPropertyValueAsString;
		} else {
			parameterValueFromTestNgXml = parameterPrefix + parameterValueFromTestNgXml;
			logger.info("Set "+systemPropertyName+" as "+parameterValueFromTestNgXml);
			return parameterValueFromTestNgXml;
		}
	}
	
	public static boolean getBooleanValueOfToolParameter(boolean parameterValueFromTestNgXml, String systemPropertyName) {
		String systemPropertyValueAsString = System.getProperty(systemPropertyName); // When running the test from Jenkins then the report file name and path defined there because this flexibility is needed. Otherwise, take it from the test suite xml file
		if(systemPropertyValueAsString != null) {
			logger.info("Set "+systemPropertyName+" as "+systemPropertyValueAsString+" from System.property");
			return Boolean.valueOf(systemPropertyValueAsString);
		} else {
			logger.info("Set "+systemPropertyName+" as "+parameterValueFromTestNgXml);
			return parameterValueFromTestNgXml;
		}
	}
	
	protected String[] getArrayOfStringsValueOfToolParameter(String parameterValueFromTestNgXml,
			String systemPropertyName, String systemPropertyDelimiter) {
		String paramValue = null;
		String systemPropertyValueAsString = System.getProperty(systemPropertyName); // When running the test from Jenkins then the report file name and path defined there because this flexibility is needed. Otherwise, take it from the test suite xml file
		if(systemPropertyValueAsString != null) {
			paramValue = systemPropertyValueAsString;
			logger.info("Set "+systemPropertyName+" as "+systemPropertyValueAsString+" from System.property");
		} else {
			paramValue = parameterValueFromTestNgXml;
			logger.info("Set "+systemPropertyName+" as "+parameterValueFromTestNgXml);
		}
		
		return paramValue.split(",");
	}
	
	protected int getIntValueOfToolParameter(String parameterValueFromTestNgXml, String systemPropertyName) {
		String systemPropertyValueAsString = System.getProperty(systemPropertyName); // When running the test from Jenkins then the report file name and path defined there because this flexibility is needed. Otherwise, take it from the test suite xml file
		if(systemPropertyValueAsString != null) {
			logger.info("Set "+systemPropertyName+" as "+systemPropertyValueAsString+" from System.property");
			return Integer.parseInt(systemPropertyValueAsString);
		} else {
			logger.info("Set "+systemPropertyName+" as "+parameterValueFromTestNgXml);
			return Integer.parseInt(parameterValueFromTestNgXml);
		}
	}
	
	protected int getIntFromPercentageValueOfToolParameter(String parameterValueFromTestNgXml, String systemPropertyName, int total) {
		String value = null;
		String systemPropertyValueAsString = System.getProperty(systemPropertyName); // When running the test from Jenkins then the report file name and path defined there because this flexibility is needed. Otherwise, take it from the test suite xml file
		
		if(systemPropertyValueAsString != null) {
			value = systemPropertyValueAsString;
			logger.info("Set "+systemPropertyName+" as "+value+" from System.property");
		} else {
			logger.info("Set "+systemPropertyName+" as "+value);
		}
		
		value.replace("%", "");
		return Integer.parseInt(value) / 100 *total;
	}
	
	protected void setRangeOfDatesAndTimes(String dateFrom, String timeFrom, String dateTo, String timeTo) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		SimpleDateFormat timeFormater = new SimpleDateFormat("HH:mm");
		this.startDateOfSample = getDateOrTimeParameterValue(SYSTEM_PROPERTY_DATE_FROM, dateFrom, dateFormat);
		this.startTimeOfSample = getDateOrTimeParameterValue(SYSTEM_PROPERTY_TIME_FROM, timeFrom, timeFormater);
		this.toDateOfSample = getDateOrTimeParameterValue(SYSTEM_PROPERTY_DATE_TO, dateTo, dateFormat);
		this.toTimeOfSample = getDateOrTimeParameterValue(SYSTEM_PROPERTY_TIME_TO, timeTo, timeFormater);
	}
	
	protected String getDateOrTimeParameterValue(String systemPropertyName, String parameterValueInXml, SimpleDateFormat dateFormat) {
		String parameterValueFromSystemProperty = System.getProperty(systemPropertyName);
		if((parameterValueFromSystemProperty != null) && (parameterValueFromSystemProperty.trim().length() > 0)) {
			try {
				dateFormat.parse(parameterValueFromSystemProperty);
				return parameterValueFromSystemProperty;
			} catch(ParseException e) {
				logger.info("Date or time parameter entered '"+parameterValueFromSystemProperty+"' is not in the right format"+dateFormat.toPattern());
				throw new RuntimeException("Date or time parameter entered '"+parameterValueFromSystemProperty+"' is not in the right format"+dateFormat.toPattern());
			}
		} else {
			return parameterValueInXml;
		}
	}
	
	
	protected static File getExcelTemplateFile(String sampleWorksheetReferenceFileName) throws IOException {
		File localFileCopiedFromRemote = new File(LOCAL_TEMP_DIRECTORY + "/" + sampleWorksheetReferenceFileName);
		if(localFileCopiedFromRemote.exists())
	    {
			localFileCopiedFromRemote.delete();
	    }
//		localFileCopiedFromRemote.createNewFile(); 
	    FileObject destn=VFS.getManager().resolveFile(localFileCopiedFromRemote.getAbsolutePath());
		
		UserAuthenticator auth=new StaticUserAuthenticator(REMOTE_DIRECTORY_OF_REFERENCE_FILE_DOMAIN, REMOTE_DIRECTORY_OF_REFERENCE_FILE_USER_NAME_AUTH, REMOTE_DIRECTORY_OF_REFERENCE_FILE_PASSWORD_AUTH);
	    FileSystemOptions opts=new FileSystemOptions();
	    DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
	    FileObject remoteFile=VFS.getManager().resolveFile(REMOTE_DIRECTORY_OF_REFERENCE_FILE+sampleWorksheetReferenceFileName,opts);
	    
	    destn.copyFrom(remoteFile, Selectors.SELECT_SELF);
	    destn.close();
		return localFileCopiedFromRemote;
	}
	
	protected static File setExcelTemplateFile(String sampleWorksheetReferenceFilePath) throws IOException {
		File localFileToCopyToRemote = new File(sampleWorksheetReferenceFilePath);
		if(!localFileToCopyToRemote.exists()) {
			throw new RuntimeException("Failed to copy template file to remote server because the template file doesn't exist locally in " + sampleWorksheetReferenceFilePath);
	    }
		
		UserAuthenticator auth=new StaticUserAuthenticator(REMOTE_DIRECTORY_OF_REFERENCE_FILE_DOMAIN, REMOTE_DIRECTORY_OF_REFERENCE_FILE_USER_NAME_AUTH, REMOTE_DIRECTORY_OF_REFERENCE_FILE_PASSWORD_AUTH);
	    FileSystemOptions opts=new FileSystemOptions();
	    DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
	    FileObject destn=VFS.getManager().resolveFile(REMOTE_DIRECTORY_OF_REFERENCE_FILE+localFileToCopyToRemote.getName(),opts);
				
	    FileObject localFile=VFS.getManager().resolveFile(localFileToCopyToRemote.getAbsolutePath());
	    
	    destn.copyFrom(localFile, Selectors.SELECT_SELF);
	    destn.close();
		return localFileToCopyToRemote;
	}

}
