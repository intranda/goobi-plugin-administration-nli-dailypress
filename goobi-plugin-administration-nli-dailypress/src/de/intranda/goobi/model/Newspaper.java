package de.intranda.goobi.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class Newspaper {
	
	private String cmsID;
	private Map<String, String> metadataMap = new HashMap<>();
	
	public Newspaper(String cmsId) {
		this.cmsID = cmsId;
	}

	public Newspaper(String cmsId, Map<String, String> metadata) {
		this.cmsID = cmsId;
		if(metadata != null) {			
			this.metadataMap = metadata;
		}
	}
	
	public String getValue(String metadataName) {
		if(this.metadataMap.containsKey(metadataName)) {
			return this.metadataMap.get(metadataName);
		} else {
			return "";
		}
	}
	
	public String getTitle() {
		return this.metadataMap.get("TITLE");
	}
	
}
