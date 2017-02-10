package de.intranda.goobi.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelDataReader {
	
	public static final DateFormat inputDateFormat = new SimpleDateFormat("dd.MM.yyyy");

	
	/**
	 * @param importFile
	 * @return a map of row numbers to a map of column numbers to cell content. 
	 * To Access row x and column y one needs to call map.get(x).get(y)
	 * Both x and y are 1 based
	 * @throws IOException
	 */
	public Map<Integer, Map<Integer, String>> read(File importFile) throws IOException {
		InputStream myxls = new FileInputStream(importFile);
		Sheet sheet = null;
        if (importFile.getName().toString().endsWith(".xlsx")) {
            XSSFWorkbook wb = new XSSFWorkbook(myxls);
            sheet = wb.getSheetAt(0); // first sheet
        } else {
            HSSFWorkbook wb = new HSSFWorkbook(myxls);
            sheet = wb.getSheetAt(0); // first sheet
        }
        if (sheet != null) {

        	int numRows = sheet.getLastRowNum();
        	Map<Integer, Map<Integer, String>> tableMap = new HashMap<>();
            for (int j = 0; j <= numRows; j++) {
                // loop over all cells
                Row row = sheet.getRow(j);
                Integer key = j+1;
                if (row != null) {
                    Map<Integer, String> rowMap = getRowAsIntMap(row);
                    tableMap.put(key, rowMap);
                }
            }
            return tableMap;
        } else {
        	throw new IOException("Unable to extract sheet from " + importFile);
        }
		
	}
	
	/**
	 * @param importFile
	 * @param identifierColumn may be null. If not, and if its content is a number, 
	 * its content is used as row key instead of the row number
	 * @return a map of row numbers to a map of column numbers to cell content. 
	 * To Access row x and column y one needs to call map.get(x).get(y)
	 * Both x and y are 1 based (unless identifierColumn is used
	 * @throws IOException
	 */
	public Map<String, Map<String, String>> read(File importFile, Integer identifierColumn, Integer identifierRow) throws IOException {
		InputStream myxls = new FileInputStream(importFile);
		Sheet sheet = null;
        if (importFile.getName().toString().endsWith(".xlsx")) {
            XSSFWorkbook wb = new XSSFWorkbook(myxls);
            sheet = wb.getSheetAt(0); // first sheet
        } else {
            HSSFWorkbook wb = new HSSFWorkbook(myxls);
            sheet = wb.getSheetAt(0); // first sheet
        }
        if (sheet != null) {

        	int numRows = sheet.getLastRowNum();
        	Map<String, Map<String, String>> tableMap = new HashMap<>();
            for (int j = 0; j <= numRows; j++) {
                // loop over all cells
                Row row = sheet.getRow(j);
                String key = Integer.toString(j+1);
                if(identifierColumn != null) {
                	Cell identifierCell = row.getCell(identifierColumn-1);
                	if(identifierCell != null && identifierCell.getCellType() == Cell.CELL_TYPE_NUMERIC) { 
                		if(HSSFDateUtil.isCellDateFormatted(identifierCell)) {
	                		Date date = identifierCell.getDateCellValue();
	                		key = inputDateFormat.format(date);
                		} else {                			
                			key = Integer.toString((int)identifierCell.getNumericCellValue());
                		}
                	} else if(identifierCell != null && identifierCell.getCellType() == Cell.CELL_TYPE_STRING) {
                		key = identifierCell.getStringCellValue();
                	}
                }
                if (row != null) {
                    Map<Integer, String> rowMap = getRowAsIntMap(row);
                    Map<String, String> rowIdentifierMap = convertToIdentiferMap(rowMap, identifierRow != null ? sheet.getRow(identifierRow-1): null);
                    tableMap.put(key, rowIdentifierMap);
                }
            }
            return tableMap;
        } else {
        	throw new IOException("Unable to extract sheet from " + importFile);
        }
		
	}

	
	private Map<String, String> convertToIdentiferMap(Map<Integer, String> rowMap, Row identifierRow) {
		Map<String, String> idMap = new HashMap<>();
		for (Integer intKey : rowMap.keySet()) {
			String key = Integer.toString(intKey);
            if(identifierRow != null && identifierRow.getCell(intKey-1) != null) {
            	Cell identifierCell = identifierRow.getCell(intKey-1);
            	if(identifierCell != null && identifierCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {                		
            		if(HSSFDateUtil.isCellDateFormatted(identifierCell)) {
                		Date date = identifierCell.getDateCellValue();
                		key = inputDateFormat.format(date);
            		} else {                			
            			key = Integer.toString((int)identifierCell.getNumericCellValue());
            		}
            	} else if(identifierCell != null && identifierCell.getCellType() == Cell.CELL_TYPE_STRING) {
            		key = identifierCell.getStringCellValue();
            	}
            }
            idMap.put(key, rowMap.get(intKey));
		}
		return idMap;
	}

	private Map<Integer, String> getRowAsIntMap(Row row) {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (Iterator<Cell> iterator = row.cellIterator(); iterator.hasNext();) {
            Cell cell = iterator.next();
            int columnIndex = cell.getColumnIndex();
            String cellContent = null;
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_NUMERIC:
                	if(HSSFDateUtil.isCellDateFormatted(cell)) {
                		Date date = cell.getDateCellValue();
                		cellContent = inputDateFormat.format(date);
                	} else {                		
                		Double d = cell.getNumericCellValue();
                		cellContent = getAsIntOrDouble(d);
                	}
                    break;
                case Cell.CELL_TYPE_STRING:
                    cellContent = cell.getStringCellValue();
                    break;
                case Cell.CELL_TYPE_BLANK:
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected cell value at cell " + cell);
            }
            if (cellContent != null) {
                map.put(columnIndex+1, cellContent);
            }
        }
        return map;
    }
	
	public static String getAsIntOrDouble(Double d) {
        String cellContent;
        int i = d.intValue();
        if (d == i) {
            cellContent = Integer.toString(d.intValue());
        } else {
            cellContent = Double.toString(d);
        }
        return cellContent;
    }

}
