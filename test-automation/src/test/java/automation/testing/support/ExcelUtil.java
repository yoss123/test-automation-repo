package automation.testing.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtil {
	
	public static final int DEFAULT_ROW_HEADER_INDEX = 0;
	
	public static final int NO_COLUMN_HEADER_WITH_SUCH_TITLE = -1;
	
	public static void addKeyValuePairPerRowToSheet(XSSFSheet sheet, Map<String, Object> map2add2sheet) {
		for (String currentKey : map2add2sheet.keySet()) {
			XSSFRow newAddedRow = sheet.createRow(sheet.getLastRowNum()+1);
			newAddedRow.createCell(0).setCellValue(currentKey);
			if(map2add2sheet.get(currentKey) instanceof String) {
				newAddedRow.createCell(1).setCellValue((String) map2add2sheet.get(currentKey));
			} else if((map2add2sheet.get(currentKey) instanceof Integer)) {
				newAddedRow.createCell(1).setCellValue((Integer) map2add2sheet.get(currentKey));
			} else if((map2add2sheet.get(currentKey) instanceof Double)) {
				newAddedRow.createCell(1).setCellValue((Double) map2add2sheet.get(currentKey));
			} 
		}
	}
	
	public static List<Map<String, Object>> mapSheetToList(XSSFSheet sheet) {		
		List<Map<String, Object>> mapedSheet = new ArrayList<Map<String,Object>>();
		Map<String, Integer> sheetHeaders = getMapOfHeaders(sheet);		
		for(int counter = 1 ; counter <= sheet.getLastRowNum() ; counter++) {
			mapedSheet.add(getMappedRow(sheet.getRow(counter), sheetHeaders));
		}		
		return mapedSheet;
	}
		
	private static HashMap<String, Object> getMappedRow(XSSFRow xssfRow, Map<String, Integer> sheetHeaders) {
		HashMap<String, Object> map = new HashMap<String, Object>();
//		String currentHeaderTitle = null;
		XSSFCell currentCell = null;
		for (Map.Entry<String, Integer> entry : sheetHeaders.entrySet()) {
//			currentHeaderTitle = entry.getKey();
			currentCell = xssfRow.getCell(entry.getValue());
			if(currentCell != null) {
				if(currentCell.getCellType() == CellType.STRING) {
					map.put(entry.getKey(), xssfRow.getCell(entry.getValue()).getStringCellValue());
				} else if(currentCell.getCellType() == CellType.NUMERIC) {
					map.put(entry.getKey(), xssfRow.getCell(entry.getValue()).getNumericCellValue());
				} else if(currentCell.getCellType() == CellType.FORMULA) {
					map.put(entry.getKey(), xssfRow.getCell(entry.getValue()).getCellFormula());
				}
			}
		}
		return map;
	}
	
	public static String getCellValue(XSSFSheet sheet, int rowNumber, String headerName) {
		int columnHeaderCount = getColumnHeaderCountByTitle(sheet, headerName);
		return sheet.getRow(rowNumber).getCell(columnHeaderCount).getStringCellValue();
	}
	
	private static Map<String, Integer> getMapOfHeaders(XSSFSheet sheet) {
		Map<String, Integer> sheetHeaders = new HashMap<String, Integer>();
		int numberOfColumnsInRecord = sheet.getRow(0).getLastCellNum();
		XSSFCell currentTitleCell = null;
		for(int counter=0 ; counter<=numberOfColumnsInRecord ; counter++) {
			currentTitleCell = sheet.getRow(0).getCell(counter);
			if((currentTitleCell != null) && (currentTitleCell.getCellType() == CellType.STRING)) {
				sheetHeaders.put(currentTitleCell.getStringCellValue(), counter);
			}
		}
		return sheetHeaders;
	}
	
	public static void addColumnHeaderTitle(XSSFSheet sheet, String columnHeaderTitle) throws IOException {		
		sheet.getRow(0).createCell(sheet.getRow(0).getLastCellNum()).setCellValue(columnHeaderTitle);
	}
	
	public static void addValueToWorkbook(XSSFSheet sheet, String columnHeaderTitle,
			String rowHeaderTitle, Object value) throws IOException {
		addValueToWorkbook(sheet, columnHeaderTitle, rowHeaderTitle, DEFAULT_ROW_HEADER_INDEX, value);
	}

	public static void addValueToWorkbook(XSSFSheet sheet, String columnHeaderTitle,
			String rowHeaderTitle, int rowHeaderIndex, Object value) throws IOException {
		
		if(value ==null) return; // Because if trying to set null value then getting an Exception
		
		int columnHeaderCount = getColumnHeaderCountByTitle(sheet, columnHeaderTitle);
		int rowHeaderCount = getRowHeaderCountByTitle(sheet, rowHeaderTitle, rowHeaderIndex);
		
		XSSFCell cellToSetValue = sheet.getRow(rowHeaderCount).getCell(columnHeaderCount);
		if(cellToSetValue == null) {
			cellToSetValue = sheet.getRow(rowHeaderCount).createCell(columnHeaderCount);
		}
		
		if(value instanceof String) {
			cellToSetValue.setCellValue((String) value);
		} else if(value instanceof Integer) {
			cellToSetValue.setCellValue((Integer) value);
		} else if(value instanceof Double) {
			cellToSetValue.setCellValue((Double) value);
		} else {
			throw new RuntimeException("Failed to set value "+value+" in excel file with sheet name "+sheet.getSheetName()
								+" under column with title "+columnHeaderTitle
								+"(#"+columnHeaderCount+") and row with title "
								+rowHeaderTitle+"(#"+rowHeaderCount+")");
		}
	}
	
	public static void addValueToWorkbook(String excelFilePath, String sheetName, String columnHeaderTitle,
			String rowHeaderTitle, int rowHeaderIndex, Object value) throws IOException {
		
		if(value ==null) return; // Because if trying to set null value then getting an Exception
		
		FileInputStream file = new FileInputStream(excelFilePath);
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		
		try {
			addValueToWorkbook(workbook.getSheet(sheetName), columnHeaderTitle, rowHeaderTitle, rowHeaderIndex, value);
		} finally {
			file.close();		
			FileOutputStream outFile =new FileOutputStream(new File(excelFilePath));
	        workbook.write(outFile);
	        outFile.close();
	        workbook.close();
		}
	}
	
	public static void addValueToWorkbook(String excelFilePath, String sheetName, String columnHeaderTitle, 
			String rowHeaderTitle, Object value) throws IOException {		
		addValueToWorkbook(excelFilePath, sheetName, columnHeaderTitle, rowHeaderTitle, DEFAULT_ROW_HEADER_INDEX, value);
	}
	
	public static int getRowHeaderCountByTitle(XSSFSheet sheet, String rowHeaderTitle, int rowHeaderIndex) {
		
		int lastRowCounter = sheet.getLastRowNum();
		XSSFCell currentTitleCell = null;
		String cellValue = "";
		for(int counter=0 ; counter<=lastRowCounter ; counter++) {
			currentTitleCell = sheet.getRow(counter).getCell(rowHeaderIndex);
			if(currentTitleCell != null) {
				if(currentTitleCell.getCellType() == CellType.STRING) {
					cellValue = currentTitleCell.getStringCellValue();
				} else if(currentTitleCell.getCellType() == CellType.NUMERIC) {
					cellValue = Double.toString(currentTitleCell.getNumericCellValue());
				}
				if(cellValue.equalsIgnoreCase(rowHeaderTitle)) {
					return counter;
				}
			}
		}
		throw new RuntimeException("Failed to find row with title (column 0) "+rowHeaderTitle+" in sheet name "+sheet.getSheetName());
	}
	
	public static int getRowHeaderCountByTitle(XSSFWorkbook workbook, String sheetName, String rowHeaderTitle, int rowHeaderIndex) {
		return getRowHeaderCountByTitle(workbook.getSheet(sheetName), rowHeaderTitle, rowHeaderIndex);
	}

	public static int getColumnHeaderCountByTitle(XSSFSheet sheet, String columnHeaderTitle) {
		
		int numberOfColumnsInRecord = sheet.getRow(0).getLastCellNum();
		XSSFCell currentTitleCell = null;
		String cellValue = "";
		int counter=0 ;
		for(counter=0 ; counter<=numberOfColumnsInRecord ; counter++) {
			currentTitleCell = sheet.getRow(0).getCell(counter);
			if(currentTitleCell == null) {
				continue;
			} else if(currentTitleCell.getCellType() == CellType.STRING) {
				cellValue = currentTitleCell.getStringCellValue();
			} else if(currentTitleCell.getCellType() == CellType.NUMERIC) {
				cellValue = ((Double) currentTitleCell.getNumericCellValue()).intValue() + "";
			} else if(currentTitleCell.getCellType() == CellType.BLANK) {
				continue;
			}
			if(cellValue.equalsIgnoreCase(columnHeaderTitle)) {
				return counter;
			}
		}
		LogManager.getLogger().warn("Failed to find column with title "+columnHeaderTitle+" in sheet name "+sheet.getSheetName());
		return NO_COLUMN_HEADER_WITH_SUCH_TITLE;
	}
	
	public static int getColumnHeaderCountByTitle(XSSFWorkbook workbook, String sheetName, String columnHeaderTitle) {		
		return getColumnHeaderCountByTitle(workbook.getSheet(sheetName), columnHeaderTitle);
	}
	
	public static void addRecord(XSSFSheet sheet, Map<String, Object> recordToAdd) {
		int numberOfColumnsInRecord = sheet.getRow(0).getLastCellNum();
		XSSFRow newAddedRow = sheet.createRow(sheet.getLastRowNum()+1);
		String currentFieldName = null;
		Object valueToAdd = null;
		for(int columnCounter=0 ; columnCounter<numberOfColumnsInRecord ; columnCounter++) {
			currentFieldName = sheet.getRow(0).getCell(columnCounter).getStringCellValue();
			valueToAdd = recordToAdd.get(currentFieldName);
			if(valueToAdd != null) {
				if(valueToAdd instanceof Integer) {
					newAddedRow.createCell(columnCounter).setCellValue((Integer) valueToAdd);
				} else {
					newAddedRow.createCell(columnCounter).setCellValue(valueToAdd.toString());
				}
			}
		}
	}
	
	public static void addRecord(XSSFWorkbook workbook, String sheetName, Map<String, Object> recordToAdd) {
		addRecord(workbook.getSheet(sheetName), recordToAdd);
	}
	
	public static void addRecordToWorkbook(String excelFilePath, String sheetName, Map<String, Object> recordToAdd) throws IOException {
		FileInputStream file = new FileInputStream(excelFilePath);
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		addRecord(workbook, sheetName, recordToAdd);
		file.close();
		
		FileOutputStream outFile =new FileOutputStream(new File(excelFilePath));
        workbook.write(outFile);
        outFile.close();
	}
	
	public static void cleanWorkbookOldData(String excelFilePath) throws IOException {
		FileInputStream file = new FileInputStream(excelFilePath);
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		
		for(int sheetsCounter=0 ; sheetsCounter<workbook.getNumberOfSheets() ; sheetsCounter++) {
			while(workbook.getSheetAt(sheetsCounter).getLastRowNum() > 0) {
				workbook.getSheetAt(sheetsCounter).removeRow(workbook.getSheetAt(sheetsCounter).getRow(workbook.getSheetAt(sheetsCounter).getLastRowNum()));
			}
		}
		
		file.close();		
		FileOutputStream outFile =new FileOutputStream(new File(excelFilePath));
        workbook.write(outFile);
        outFile.close();
        workbook.close();
	}
	
	public static void addPairOfData(String excelFilePath, String sheetName, String valueDesc, int value) throws IOException {
		FileInputStream file = new FileInputStream(excelFilePath);
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		
		XSSFRow newAddedRow = workbook.getSheet(sheetName).createRow(workbook.getSheet(sheetName).getLastRowNum()+1);
		newAddedRow.createCell(0).setCellValue(valueDesc);
		newAddedRow.createCell(1).setCellValue(value);
		
		file.close();		
		FileOutputStream outFile =new FileOutputStream(new File(excelFilePath));
        workbook.write(outFile);
        outFile.close();
        workbook.close();
	}
	
	// create a new copy of the reference file according to the template 'excelFileTemplate'
	public static void createNewExcelReferenceFile(File templateFile, String fileNameToCreate) throws IOException {		
		File newReferenceFile = new File(fileNameToCreate);
		FileUtils.copyFile(templateFile, newReferenceFile);
	}
	
	public static void evaluateAllFormulaCellsInReport(String excelFilePath) throws IOException {
		FileInputStream file = new FileInputStream(excelFilePath);
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);		
		file.close();
		
		FileOutputStream outFile =new FileOutputStream(new File(excelFilePath));
        workbook.write(outFile);
        outFile.close();
        workbook.close();
	}

	public static void addAllRecordFields(XSSFSheet sheet, Map<String, Object> recordToAdd, List<String> listOfFieldsToFilter) {
		XSSFRow newAddedRow = sheet.createRow(sheet.getLastRowNum()+1);
		Object valueToAdd = null;
		int headerCount;
		
		for (Map.Entry<String, Object> entry : recordToAdd.entrySet()) {
			if(listOfFieldsToFilter.contains(entry.getKey())) continue;
			
			headerCount = getColumnHeaderCountByTitle(sheet, entry.getKey());
			if(headerCount == NO_COLUMN_HEADER_WITH_SUCH_TITLE) {
				headerCount = sheet.getRow(0).getLastCellNum();
				sheet.getRow(0).createCell(headerCount).setCellValue(entry.getKey());
			}
			valueToAdd = entry.getValue();
			if(valueToAdd != null) {
				if(valueToAdd instanceof Integer) {
					newAddedRow.createCell(headerCount).setCellValue((Integer) valueToAdd);
				} else {
					newAddedRow.createCell(headerCount).setCellValue(valueToAdd.toString());
				}
			}
	    }
	}
	
	public static void addListOfRecordsToSheet(List<Map<String, Object>> listToDumpInExcel,
			XSSFSheet itemsTasksDumpSheet, List<String> listOfFieldsToFilterInReport) {
		for(Map<String, Object> currentMappedTask : listToDumpInExcel) {
			ExcelUtil.addAllRecordFields(itemsTasksDumpSheet, currentMappedTask, listOfFieldsToFilterInReport);
		}
	}

}
