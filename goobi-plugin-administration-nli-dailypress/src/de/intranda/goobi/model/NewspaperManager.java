package de.intranda.goobi.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import de.intranda.goobi.input.ExcelDataReader;

public class NewspaperManager {
	
	private final Map<String, Map<String, String>> map;
	
	public NewspaperManager(File excelFile, Integer identifierColumn, Integer identifierRow) throws IOException {
		this.map = new ExcelDataReader().read(excelFile, identifierColumn, identifierRow);
	}
	
	public NewspaperManager(XMLConfiguration config) throws ConfigurationException, IOException{
		String excelPath = config.getString("newspaperTablePath");
		Integer identifierColumn = config.getInteger("metadataMappings/@identifierColumn", null);
		Integer identifierRow = config.getInteger("metadataMappings/@identifierRow", null);
		if(excelPath != null) {
			this.map = new ExcelDataReader().read(new File(excelPath), identifierColumn, identifierRow);
		} else {
			throw new ConfigurationException("Missing configuration option 'newspaperTablePath'");
		}
	}

	public Set<String> getIdentifiers() {
		return this.map.keySet();
	}
	
	public Map<String,String> getRow(String identifier) {
		return this.map.get(identifier);
	}
	
	public String getValue(String row, String column) throws IllegalArgumentException{
		if(!this.map.keySet().contains(row)) {
			throw new IllegalArgumentException("Row '" + row + "' does not exist");
		}
		if(!this.map.get(row).keySet().contains(column)) {
			throw new IllegalArgumentException("Column '" + column + "' does not exist");
		}
		return this.map.get(row).get(column);
	}
}
