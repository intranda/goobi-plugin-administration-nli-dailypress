package de.intranda.goobi.model;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import de.intranda.goobi.input.ExcelDataReader;

public class NewspaperManager extends ExcelDataManager {
	
	public NewspaperManager(XMLConfiguration config) throws ConfigurationException, IOException{
		super(new File(config.getString("newspaperTablePath")), getIdentifierColumn(config), getIdentifierRow(config));
	}
	
	public NewspaperManager(File excelPath, XMLConfiguration config) throws ConfigurationException, IOException{
		super(excelPath, getIdentifierColumn(config), getIdentifierRow(config));
	}
	
	private static Integer getIdentifierColumn(XMLConfiguration config) {
		Integer identifierColumn = config.getInteger("metadataMappings/@identifierColumn", null);
		return identifierColumn;
	}
	
	private static Integer getIdentifierRow(XMLConfiguration config) {
		Integer identifierColumn = config.getInteger("metadataMappings/@identifierRow", null);
		return identifierColumn;
	} 
	
	public NewspaperManager(File excelFile, Integer identifierColumn, Integer identifierRow) throws IOException {
		super(new ExcelDataReader().read(excelFile, identifierColumn, identifierRow));
	}

}
