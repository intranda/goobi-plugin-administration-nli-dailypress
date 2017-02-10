package de.intranda.goobi.model;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class NewspaperIssue {

	private Newspaper newspaper;
	private String issueNumber;
	private String issueDate;
	private String issueComment;
	private List<File> files;
	
	public NewspaperIssue(Newspaper newspaper) {
		this.newspaper = newspaper;
	}

	public Map<String, String> getMetadataMap() {
		HashMap<String, String> map = new HashMap<>();
		map.put("issueNumber", issueNumber);
		map.put("issueDate", issueDate);
		map.put("issueComment", issueComment);
		return map;
	}
	
}
