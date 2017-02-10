package de.intranda.goobi.model;

import java.io.File;

import lombok.Data;

@Data
public class FileUpload implements Comparable<FileUpload> {
	
	private File path;

	public boolean isValid() {
		return path.isFile() && path.length() > 0;
	}
	
	

	public void delete() {
		if(path.isFile()) {
			path.delete();
		}
	}
	
	public String getFileName() {
		return path.getName();
	}



	@Override
	public int compareTo(FileUpload o) {
		return path.compareTo(o.path);
	}
		
}
