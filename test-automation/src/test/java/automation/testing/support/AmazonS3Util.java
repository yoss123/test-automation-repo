package automation.testing.support;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class AmazonS3Util {
	
	public static final String LOCAL_TEMP_DIRECTORY = "temp";
	
	public static boolean isObjectExist(AmazonS3 s3Client, String bucketName, String objectKey) {
		
		try {
			ObjectMetadata objectMetadata = s3Client.getObjectMetadata(bucketName, objectKey);
			LogManager.getLogger().info("Object "+objectKey+" exists in Amazon S3 under bucket "+bucketName);
		} catch (AmazonS3Exception e) {
//			LogManager.getLogger().info("Exception was thrown when running a request for Object Metadata from s3");
			if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
		        return false; 
		    } else {
		        throw e;
		    }
		}
		return true;
	}
	
	public static boolean isBucketExist(String s3AccessKeyId, String s3SecretKeyId, 
			String s3ClientRegion, String s3BucketName) {
		AmazonS3 s3Client = null;
		
		try {
	        s3Client = getS3Client(s3AccessKeyId, s3SecretKeyId, s3ClientRegion);
	        
	        Object fullObject = s3Client.getObject(new GetObjectRequest(s3BucketName, "0.json.gzip"));
	        return fullObject != null;
		} catch(Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException("Couldn't check if bucket named "+s3BucketName+" exist");
			return false;
		} finally {
			s3Client.shutdown();
		}
	}
	
	public static void uploadFilesToS3(File directoryToUploadFilesFrom, String s3AccessKeyId, String s3SecretKeyId, 
			String s3ClientRegion, String s3BucketName, Logger logger) {
		
		AmazonS3 s3Client = null;
		try {
			s3Client = getS3Client(s3AccessKeyId, s3SecretKeyId, s3ClientRegion);
			
	        File[] listOfFilesToUpload = directoryToUploadFilesFrom.listFiles();
	        logger.info("Uploading "+listOfFilesToUpload.length+" files to S3");
	        for(int counter=0 ; counter < listOfFilesToUpload.length ; counter++) {
	        	uploadFileToS3(s3Client, listOfFilesToUpload[counter], s3BucketName, logger);
	        }	        
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			s3Client.shutdown();
		}
	}
	
	public static void uploadFilesToS3(File directoryToUploadFilesFrom, AmazonS3 s3Client, String s3BucketName, Logger logger) {		
		try {
			File[] listOfFilesToUpload = directoryToUploadFilesFrom.listFiles();
	        logger.info("Uploading "+listOfFilesToUpload.length+" files to S3");
	        for(int counter=0 ; counter < listOfFilesToUpload.length ; counter++) {
	        	uploadFileToS3(s3Client, listOfFilesToUpload[counter], s3BucketName, logger);
	        }	        
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void uploadFileToS3(AmazonS3 s3Client, File fileToUpload, String s3BucketName, Logger logger) {
		PutObjectRequest request = new PutObjectRequest(s3BucketName, fileToUpload.getName(), fileToUpload);
        s3Client.putObject(request);
        logger.info("File named "+fileToUpload.getName()+" was uploaded successfully to S3 bucket named "+s3BucketName);
	}
	
	public static AmazonS3 getS3Client(String s3AccessKeyId, String s3SecretKeyId, String s3ClientRegion) {
		
		BasicAWSCredentials awsCreds = null;
		AmazonS3 s3Client = null;
		String amazonAwsCertChecking = System.getProperty("com.amazonaws.sdk.disableCertChecking");
		if(amazonAwsCertChecking == null) {
			System.setProperty("com.amazonaws.sdk.disableCertChecking", String.valueOf(true));
		}
      	awsCreds = new BasicAWSCredentials(s3AccessKeyId, s3SecretKeyId);
        	
      	s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(s3ClientRegion)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .build();
        
        return s3Client;
	}
	
	public static List<S3ObjectSummary> getObjectsKeyList(AmazonS3 s3Client, String s3BucketName, Logger logger) {
		
		List<S3ObjectSummary> listOfObjectSummary = new ArrayList<S3ObjectSummary>();
		ListObjectsV2Request listObjectsV2Request = null;
		ListObjectsV2Result listObjectsV2Result = null;
		
        try {       	
            if(s3BucketName.contains("/")) {
            	listObjectsV2Request = new ListObjectsV2Request()
	            		.withBucketName(s3BucketName.substring(0, s3BucketName.indexOf("/")))
	            		.withPrefix(s3BucketName.substring(s3BucketName.indexOf("/")+1))
	                    .withEncodingType("url");
            } else {
            	listObjectsV2Request = new ListObjectsV2Request()
	            		.withBucketName(s3BucketName)
	                    .withEncodingType("url");
            }
            
            do {
	            listObjectsV2Result = s3Client.listObjectsV2(listObjectsV2Request);
	            listOfObjectSummary.addAll(listObjectsV2Result.getObjectSummaries());
	            
	            String token = listObjectsV2Result.getNextContinuationToken();
	            listObjectsV2Request.setContinuationToken(token);
	        } while(listObjectsV2Result.isTruncated());
            
            return listOfObjectSummary;
        } catch(Exception e) {
        	throw new RuntimeException("Failed to connect and download items from S3 in bucket " 
        					+ s3BucketName + ". Error message "+e.getMessage());
        }
	}
	
	public static File downloadFile(AmazonS3 s3Client, String bucketName, String objectKey, String actualFileName) {
		
        S3Object fullObject = null;
        
        try {        	
        	fullObject = s3Client.getObject(new GetObjectRequest(bucketName, objectKey));
            InputStream S3ObjectInputStream = fullObject.getObjectContent();
            File targetFile = new File(actualFileName);
    	    FileUtils.copyInputStreamToFile(S3ObjectInputStream, targetFile);
    	    return targetFile;
        } catch(Exception e) {
        	LogManager.getLogger().error("Error while trying to download objectKey '"+objectKey+"' from bucketName '"+bucketName+"' ; isFullObjectNull="+(fullObject==null));
//        	e.printStackTrace();
        	return null;
        } finally {
        	// To ensure that the network connection doesn't remain open, close any open input streams.
			try {
	            if(fullObject != null) {
	                fullObject.close();
	            }
			} catch(Exception e) {
				e.printStackTrace();
	        	return null;
			}
        }
    }
	
	public static File downloadFile(String clientRegion, String bucketName, String objectKey, 
			String accessKeyId, String secretKeyId, String actualFileName) {
		
        S3Object fullObject = null;
        
        try {        	
        	AmazonS3 s3Client = getS3Client(accessKeyId, secretKeyId, clientRegion);
            fullObject = s3Client.getObject(new GetObjectRequest(bucketName, objectKey));
            InputStream S3ObjectInputStream = fullObject.getObjectContent();
            File targetFile = new File(actualFileName);
    	    FileUtils.copyInputStreamToFile(S3ObjectInputStream, targetFile);
    	    return targetFile;
        } catch(Exception e) {
        	LogManager.getLogger().error("Error while trying to download objectKey '"+objectKey+"' from bucketName '"+bucketName+"' ; isFullObjectNull="+(fullObject==null));
//        	e.printStackTrace();
        	return null;
        } finally {
        	// To ensure that the network connection doesn't remain open, close any open input streams.
			try {
	            if(fullObject != null) {
	                fullObject.close();
	            }
			} catch(Exception e) {
				e.printStackTrace();
	        	return null;
			}
        }
    }

}
