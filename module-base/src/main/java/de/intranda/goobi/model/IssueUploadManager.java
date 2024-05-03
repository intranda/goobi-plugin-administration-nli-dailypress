package de.intranda.goobi.model;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import de.intranda.goobi.input.ExcelDataReader;

public class IssueUploadManager extends NewspaperManager{
	
	public IssueUploadManager(File excelPath, XMLConfiguration config) throws ConfigurationException, IOException{
		super(excelPath, getIdentifierColumn(config), getIdentifierRow(config));
	}
	
	private static Integer getIdentifierColumn(XMLConfiguration config) {
		Integer identifierColumn = config.getInteger("issueUploadMappings/@identifierColumn", null);
		return identifierColumn;
	}
	
	private static Integer getIdentifierRow(XMLConfiguration config) {
		Integer identifierColumn = config.getInteger("issueUploadMappings/@identifierRow", null);
		return identifierColumn;
	}
	
}
