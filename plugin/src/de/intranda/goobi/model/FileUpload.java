package de.intranda.goobi.model;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import lombok.Data;

@Data
public class FileUpload implements Comparable<FileUpload> {

	private File path;

	public boolean isValid() {
		return path.isFile() && path.length() > 0;
	}

	public void delete() {
		if (path.isFile()) {
			path.delete();
		}
		if(path.getParentFile().list().length == 0) {
			path.getParentFile().delete();
		}
	}

	public String getFileName() {
		return path.getName();
	}

	@Override
	public int compareTo(FileUpload o) {
		return path.compareTo(o.path);
	}

	public boolean isPDF() {
		return FilenameUtils.getExtension(getFileName()).equalsIgnoreCase("PDF");
	}

	public boolean isImage() {
		return FilenameUtils.getExtension(getFileName()).equalsIgnoreCase("TIF") ||
				FilenameUtils.getExtension(getFileName()).equalsIgnoreCase("TIFF") ||
				FilenameUtils.getExtension(getFileName()).equalsIgnoreCase("JPG") ||
				FilenameUtils.getExtension(getFileName()).equalsIgnoreCase("JPEG") ||
				FilenameUtils.getExtension(getFileName()).equalsIgnoreCase("PNG");
	}
	
	public boolean isExcelFile() {
		return FilenameUtils.getExtension(getFileName()).equalsIgnoreCase("XLSX") ||
				FilenameUtils.getExtension(getFileName()).equalsIgnoreCase("XLS");
	}
}
