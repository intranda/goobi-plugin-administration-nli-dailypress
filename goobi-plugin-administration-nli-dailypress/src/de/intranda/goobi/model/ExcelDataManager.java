package de.intranda.goobi.model;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import de.intranda.goobi.input.ExcelDataReader;
import lombok.Data;

@Data
public abstract class ExcelDataManager {

	private final Map<String, Map<String, String>> map;

	public ExcelDataManager(Map<String, Map<String, String>> map) {
		this.map = map;
	}
	
	public ExcelDataManager(File excelFile, int identifierColumn, int identifierRow) throws ConfigurationException, IOException {
		this.map = init(excelFile, identifierColumn, identifierRow);
	}
	
	private Map<String, Map<String, String>> init(File excelFile, int identifierColumn, int identifierRow) throws IOException, ConfigurationException {
		if(excelFile != null) {
			return new ExcelDataReader().read(excelFile, identifierColumn, identifierRow);
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
