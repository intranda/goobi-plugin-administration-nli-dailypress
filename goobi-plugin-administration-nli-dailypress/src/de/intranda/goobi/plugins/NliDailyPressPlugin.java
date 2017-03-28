package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.primefaces.event.FileUploadEvent;

import de.intranda.goobi.input.ExcelDataReader;
import de.intranda.goobi.input.PdfExtractor;
import de.intranda.goobi.model.ExcelDataManager;
import de.intranda.goobi.model.FileUpload;
import de.intranda.goobi.model.IssueUploadManager;
import de.intranda.goobi.model.Newspaper;
import de.intranda.goobi.model.NewspaperIssue;
import de.intranda.goobi.model.NewspaperIssue.IssueType;
import de.intranda.goobi.model.NewspaperManager;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.BeanHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.PropertyManager;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.UGHException;
import ugh.fileformats.mets.MetsMods;

@PluginImplementation
@Log4j
public @Data class NliDailyPressPlugin implements IAdministrationPlugin, IPlugin {

	private static final String PLUGIN_NAME = "NliDailyPress";

	public static final NumberFormat filenameFormat = new DecimalFormat("0000");

	private String singleCmsID;
	private Integer singleIssueNumber;
	private Date singleIssueDate;
	private String singleIssueComment;
	private String singleIssueType;
	private Newspaper selectedNewspaper = null;
	private NewspaperManager newspaperManager = null;
	private List<FileUpload> uploadedFiles = new ArrayList<>();
	private File importFolder = null;
	private FileUpload issueBatchFile = null;
	private List<NewspaperIssue> issueBatch = new ArrayList<>();

	private XMLConfiguration config;

	@Override
	public PluginType getType() {
		return PluginType.Administration;
	}

	@Override
	public String getTitle() {
		return PLUGIN_NAME;
	}

	@Override
	public String getGui() {
		return "/uii/administration_NliDailyPress.xhtml";
	}

	public String getDescription() {
		return PLUGIN_NAME;
	}

	public String cancelSingleImport() {
		Helper.setMeldung("plugin_NliDailyPress_cancelMessageSingleImport");
		resetSingleIssue();
		return "";
	}

	public Collection<String> getIssueTypes() {
		IssueType[] types = IssueType.values();
		List<String> typeNames = new ArrayList<>(types.length);
		for (IssueType issueType : types) {
			typeNames.add(issueType.name);
		}
		return typeNames;
	}

	public String cancelMultipleImport() {
		Helper.setMeldung("plugin_NliDailyPress_cancelMessageMultipleImport");
		resetIssueBatch();
		return "";
	}

	public String executeSingleImport() {
		if (selectedNewspaper != null) {
			NewspaperIssue singleIssue = new NewspaperIssue(selectedNewspaper);
			singleIssue.setIssueDate(singleIssueDate);
			singleIssue.setIssueNumber(singleIssueNumber);
			singleIssue.setIssueComment(singleIssueComment);
			singleIssue.setIssueType(IssueType.get(getSingleIssueType()));
			singleIssue.setFiles(getUploadedFiles());
			if (createProcess(singleIssue, createProcessName(singleIssue), getWorkflowName()) != null) {
				Helper.setMeldung("plugin_NliDailyPress_successMessageSingleImport");
				resetSingleIssue();
			} else {
				Helper.setFehlerMeldung("plugin_NliDailyPress_errorMessageSingleImport");
			}
		} else {
			Helper.setFehlerMeldung("plugin_NliDailyPress_errorMissingNewspaperSelection");
		}
		return "";
	}

	public String executeMultipleImport() {
		List<Process> processes = new ArrayList<>();
		for (NewspaperIssue newspaperIssue : issueBatch) {
			Process p = createProcess(newspaperIssue, createProcessName(newspaperIssue), getWorkflowName());
			if (p != null) {
				processes.add(p);
			} else {
				Helper.setFehlerMeldung("plugin_NliDailyPress_errorMessageMultipleImport");
				for (Process process : processes) {
					ProcessManager.deleteProcess(process);
				}
				return "";
			}
		}

		Helper.setMeldung("plugin_NliDailyPress_successMessageMultipleImport");
		resetIssueBatch();
		return "";
	}

	protected String getWorkflowName() {
		return getConfig().getString("workflow/name");
	}

	protected String getNewspaperDocType() {
		return getConfig().getString("ruleset/newspaperType", "Newspaper");
	}

	protected String getIssueDocType() {
		return getConfig().getString("ruleset/issueType", "NewspaperIssue");
	}

	private Prefs getPrefs() {
		Process template = ProcessManager.getProcessByTitle(getWorkflowName());
		return template.getRegelsatz().getPreferences();
	}

	private String createProcessName(NewspaperIssue issue) {
		System.out.println("Issue number  = " + issue.getIssueNumber());
		String name = issue.getNewspaper().getCmsID() + "_"
				+ (issue.hasIssueNumber() ? NewspaperIssue.issueNumberFormat.format(issue.getIssueNumber())
						: "")
				+ "_" + NewspaperIssue.dateYearFormat.format(issue.getIssueDate()) + "_ "
				+ NewspaperIssue.dateMonthFormat.format(issue.getIssueDate()) + "_ "
				+ NewspaperIssue.dateDayFormat.format(issue.getIssueDate());
		return name.replaceAll("\\W", "").replace("__", "_");
	}

	public void setSingleCmsID(String cmsId) {
		this.singleCmsID = cmsId;
		System.out.println("Selected " + cmsId);
		log.debug("selected cmsID " + cmsId);
	}

	public void searchNewspaper() {
		this.selectedNewspaper = searchNewspaper(this.singleCmsID);
	}

	private Newspaper searchNewspaper(String cmsId) {
		if (cmsId != null) {
			Map<String, String> newspaperData;
			try {
				newspaperData = getNewspaperManager().getRow(cmsId);
				if (newspaperData == null) {
					throw new NullPointerException();
				}
				return new Newspaper(cmsId, mapNewspaperColumnsToFields(newspaperData));
			} catch (NullPointerException e) {
				log.debug("no newspaper found for " + this.singleCmsID);
				return null;
			}

		} else {
			log.error("Not cmsID selected");
			return null;
		}
	}

	private Map<String, String> mapNewspaperColumnsToFields(Map<String, String> newspaperData) {
		Map<String, String> fieldMap = new HashMap<>();
		for (String column : newspaperData.keySet()) {
			String field = getConfig().getString("newspaperDataMappings/mapping[column='" + column + "']/field");
			fieldMap.put(field, newspaperData.get(column));
		}
		return fieldMap;
	}

	private Map<String, String> mapIssueColumnsToFields(Map<String, String> newspaperData) {
		Map<String, String> fieldMap = new HashMap<>();
		for (String column : newspaperData.keySet()) {
			String field = getConfig().getString("issueUploadMappings/mapping[column='" + column + "']/field");
			fieldMap.put(field, newspaperData.get(column));
		}
		return fieldMap;
	}

	private NewspaperManager getNewspaperManager() {
		if (this.newspaperManager == null) {
			try {
				this.newspaperManager = new NewspaperManager(getConfig());
			} catch (ConfigurationException | IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return this.newspaperManager;

	}

	private XMLConfiguration getConfig() {
		if (this.config == null) {
			this.config = ConfigPlugins.getPluginConfig(this);
			this.config.setExpressionEngine(new XPathExpressionEngine());
		}
		return this.config;
	}

	public boolean isNewspaperSelected() {
		return this.selectedNewspaper != null;
	}

	private void resetSingleIssue() {
		this.singleCmsID = null;
		this.selectedNewspaper = null;
		this.singleIssueComment = null;
		this.singleIssueDate = null;
		this.singleIssueNumber = null;
		this.deleteAllUploadedFiles();
		this.importFolder = null;
	}

	public List<FileUpload> getUploadedFiles() {
		try {
			Collections.sort(this.uploadedFiles);
		} catch (ConcurrentModificationException e) {
			// too bad
		}
		return this.uploadedFiles;
	}

	private void resetIssueBatch() {
		this.issueBatch = new ArrayList<>();
		if (this.issueBatchFile != null) {
			this.issueBatchFile.delete();
		}
		this.issueBatchFile = null;
	}

	public void handleFileUpload(FileUploadEvent event) {
		FileUpload upload;
		try {
			upload = copyFile(event.getFile().getFileName(), event.getFile().getInputstream());

		} catch (IOException e) {
			log.error(e);
			upload = new FileUpload();
			upload.setPath(new File(event.getFile().getFileName()));
		}

		this.uploadedFiles.add(upload);
	}

	public void handleBatchFileUpload(FileUploadEvent event) {

		resetIssueBatch();
		FileUpload upload;
		try {
			upload = copyFile(event.getFile().getFileName(), event.getFile().getInputstream());

		} catch (IOException e) {
			log.error(e);
			upload = new FileUpload();
			upload.setPath(new File(event.getFile().getFileName()));
		}

		this.issueBatchFile = upload;

		try {
			this.issueBatch = createIssues(this.issueBatchFile.getPath());
		} catch (ConfigurationException | IOException e) {
			log.error(e);
			Helper.setFehlerMeldung("plugin_NliDailyPress_errorMessageReadBatchFile " + e.toString());
		}

	}

	public FileUpload copyFile(String fileName, InputStream in) {

		File file = new File(getImportFolder(true), fileName);
		FileUpload upload = new FileUpload();
		upload.setPath(file);
		OutputStream out = null;
		try {

			// write the inputStream to a FileOutputStream
			out = new FileOutputStream(file);

			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = in.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
		} catch (IOException e) {
			log.error(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e);
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					log.error(e);
				}
			}

		}
		return upload;
	}

	private File getImportFolder(boolean create) {
		if (create && this.importFolder == null) {
			String tempFolder = ConfigurationHelper.getInstance().getTemporaryFolder();
			this.importFolder = new File(tempFolder, "dailyPress_upload_" + System.currentTimeMillis());
		}
		if (create && !this.importFolder.exists()) {
			this.importFolder.mkdirs();
		}
		return this.importFolder;
	}

	public String getAllowedTypes() {
		return getConfig().getString("allowedFileTypes", "/.*?\\.(gif|jpe?g|png|tiff?|jp2|pdf)$/");
	}

	public void deleteAllUploadedFiles() {
		for (FileUpload fileUpload : uploadedFiles) {
			fileUpload.delete();
		}
		if (importFolder != null && importFolder.isDirectory()) {
			importFolder.delete();
		}
		this.uploadedFiles = new ArrayList<>();
	}

	private Process createProcess(NewspaperIssue issue, String processTitle, String workflowName) {

		if (ProcessManager.countProcesses("Titel='" + processTitle + "'") > 0) {
			Helper.setFehlerMeldung("ProcessCreationErrorTitleAllreadyInUse");
			return null;
		}

		BeanHelper bhelp = new BeanHelper();
		Process template = ProcessManager.getProcessByTitle(workflowName);

		Process newProcess = new Process();
		newProcess.setTitel(processTitle);
		newProcess.setIstTemplate(false);
		newProcess.setInAuswahllisteAnzeigen(false);
		newProcess.setProjekt(template.getProjekt());
		newProcess.setRegelsatz(template.getRegelsatz());
		newProcess.setDocket(template.getDocket());

		bhelp.SchritteKopieren(template, newProcess);
		bhelp.ScanvorlagenKopieren(template, newProcess);
		bhelp.WerkstueckeKopieren(template, newProcess);
		bhelp.EigenschaftenKopieren(template, newProcess);

		try {
			ProcessManager.saveProcess(newProcess);
		} catch (DAOException e) {
			log.error(e);
			Helper.setFehlerMeldung(e.toString());
			return null;
		}

		createProcessProperty("Template", template.getTitel(), newProcess);
		createProcessProperty("TemplateID", template.getId() + "", newProcess);
		createProcessProperty("CMS ID", issue.getNewspaper().getCmsID(), newProcess);
		createProcessProperty("Newspaper", issue.getNewspaper().getTitle(), newProcess);
		createProcessProperty("Publisher", issue.getNewspaper().getValue("Publisher"), newProcess);
		createProcessProperty("Issue number",
				issue.getIssueNumber() != null ? NewspaperIssue.issueNumberFormat.format(issue.getIssueNumber()) : "-",
				newProcess);
		createProcessProperty("Issue date", NewspaperIssue.dateFormat.format(issue.getIssueDate()), newProcess);
		createProcessProperty("Issue comment", issue.getIssueComment(), newProcess);
		createProcessProperty("Issue type", issue.getIssueType().name.replaceAll("\\s+", "_"), newProcess);

		try {
			Fileformat ff = createFileformat(issue, processTitle, getPrefs());
			newProcess.writeMetadataFile(ff);
		} catch (Exception e) {
			ProcessManager.deleteProcess(newProcess);
			log.error(e.toString(), e);
			Helper.setFehlerMeldung("plugin_NliDailyPress_errorMessageCreateMetsFile " + e.toString());
			return null;
		}

		try {
			int numFiles = copyMediaFiles(issue.getFiles(), newProcess);
			createProcessProperty("Pages", Integer.toString(numFiles), newProcess);
		} catch (IOException | InterruptedException | SwapException | DAOException e) {
			ProcessManager.deleteProcess(newProcess);
			log.error(e);
			Helper.setFehlerMeldung(e.toString());
			return null;
		}

		Helper.setMeldung("plugin_NliDailyPress_successMessageCreatedProcess " + processTitle);
		return newProcess;

	}

	/**
	 * @param files
	 * @param newProcess
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SwapException
	 * @throws DAOException
	 * @return the number of pdf files or image files, whichever is larger
	 */
	private int copyMediaFiles(List<FileUpload> files, Process newProcess)
			throws IOException, InterruptedException, SwapException, DAOException {
		File masterImagesDir = new File(newProcess.getImagesOrigDirectory(true));
		File tifImagesDir = new File(newProcess.getImagesTifDirectory(true));
		File pdfDir = new File(newProcess.getPdfDirectory());
		File ocrTextDir = new File(newProcess.getTxtDirectory());
		for (FileUpload fileUpload : files) {
			if (fileUpload.isImage()) {
				if (!masterImagesDir.isDirectory()) {
					masterImagesDir.mkdirs();
				}
				FileUtils.copyFile(fileUpload.getPath(), new File(masterImagesDir, fileUpload.getFileName()));
			} else if (fileUpload.isPDF()) {
				if (!pdfDir.exists()) {
					pdfDir.mkdirs();
				}
				if (!ocrTextDir.exists()) {
					ocrTextDir.mkdirs();
				}
				new PdfExtractor().extractPdfs(fileUpload.getPath(), pdfDir, ocrTextDir);
			}
		}

		// copy pdf to images folder is no images exist (needed for pdf creation
		if ((!masterImagesDir.exists() || masterImagesDir.listFiles(getMediaFilter()).length == 0)
				&& (pdfDir.exists() && pdfDir.listFiles(getMediaFilter()).length > 0)) {
			Files.createSymbolicLink(tifImagesDir.toPath(), pdfDir.toPath());
		} else {
			if (!masterImagesDir.exists()) {
				masterImagesDir.mkdirs();
			}
			Files.createSymbolicLink(tifImagesDir.toPath(), masterImagesDir.toPath());
		}

		int numFiles = renameFiles(masterImagesDir, newProcess.getTitel());
		numFiles = Math.max(numFiles, renameFiles(pdfDir, newProcess.getTitel()));
		renameFiles(ocrTextDir, newProcess.getTitel());
		return numFiles;
	}

	/**
	 * @param directory
	 * @return the number of renamed files
	 */
	private int renameFiles(File directory, String baseName) {
		if (directory.listFiles() != null) {
			List<File> files = Arrays.asList(directory.listFiles());
			Collections.sort(files);
			Collections.reverse(files);
			int number = files.size();
			for (File file : files) {
				String filename = baseName + "_" + filenameFormat.format(number) + FilenameUtils.EXTENSION_SEPARATOR_STR
						+ FilenameUtils.getExtension(file.getName());
				file.renameTo(new File(file.getParent(), filename));
				// file.renameTo(new File(
				// file.getParent(),
				// filenameFormat.format(number)
				// + FilenameUtils.EXTENSION_SEPARATOR_STR
				// + FilenameUtils.getExtension(file.getName())));
				number--;
			}
			return files.size();
		}
		return 0;
	}

	/**
	 * @param template
	 * @param newProcess
	 */
	private void createProcessProperty(String label, String value, Process newProcess) {
		Processproperty pp = new Processproperty();
		pp.setTitel(label);
		pp.setWert(value);
		pp.setProzess(newProcess);
		PropertyManager.saveProcessProperty(pp);
	}

	private Fileformat createFileformat(NewspaperIssue singleIssue, String id, Prefs prefs) throws UGHException {
		DocStructType parentType = prefs.getDocStrctTypeByName(getNewspaperDocType());
		DocStructType childType = prefs.getDocStrctTypeByName(getIssueDocType());

		if (parentType == null) {
			throw new UGHException("No valid doc type for newspaper configured");
		}
		if (childType == null) {
			throw new UGHException("No valid doc type for newspaper-issue configured (" + getIssueDocType() + ")");
		}

		Fileformat ff = new MetsMods(prefs);
		ff.setDigitalDocument(new DigitalDocument());

		DocStruct parent = ff.getDigitalDocument().createDocStruct(parentType);
		DocStruct child = ff.getDigitalDocument().createDocStruct(childType);

		setMetadata(singleIssue.getNewspaper().getMetadataMap(), singleIssue.getNewspaper().getCmsID(), parent, prefs);
		setMetadata(singleIssue.getMetadataMap(), id, child, prefs);

		// MetadataType identifierType =
		// prefs.getMetadataTypeByName("CatalogIDDigital");
		// Metadata idParent = new Metadata(identifierType);
		// idParent.setValue(singleIssue.getNewspaper().getCmsID());
		// parent.addMetadata(idParent);
		// Metadata idChild = new Metadata(identifierType);
		// idChild.setValue(id);
		// child.addMetadata(idChild);

		DocStruct book = ff.getDigitalDocument().createDocStruct(prefs.getDocStrctTypeByName("BoundBook"));
		ff.getDigitalDocument().setPhysicalDocStruct(book);

		parent.addChild(child);
		ff.getDigitalDocument().setLogicalDocStruct(parent);

		return ff;
	}

	private void setMetadata(Map<String, String> columnMap, String identifier, DocStruct ds, Prefs prefs)
			throws UGHException {
		for (String column : columnMap.keySet()) {
			String metadataName = getMetadataNameForField(column);
			if (metadataName != null) {
				MetadataType mdType = prefs.getMetadataTypeByName(metadataName);
				if (mdType != null) {
					Metadata md = new Metadata(mdType);
					md.setValue(columnMap.get(column));
					ds.addMetadata(md);
				}
			}
		}
		if (StringUtils.isNotBlank(identifier)) {
			MetadataType identifierType = prefs.getMetadataTypeByName("CatalogIDDigital");
			Metadata mdId = new Metadata(identifierType);
			mdId.setValue(identifier);
			ds.addMetadata(mdId);
		}
	}

	protected String getMetadataNameForField(String column) {
		return getConfig().getString("metadataMappings/mapping[field='" + column + "']/metadata");
	}

	protected List<NewspaperIssue> createIssues(File excelFile) throws ConfigurationException, IOException {
		NewspaperManager manager = new IssueUploadManager(excelFile, getConfig());
		String cmsColumn = getConfig().getString("issueUploadMappings/cmsId", "cms");
		String folderColumn = getConfig().getString("issueUploadMappings/uploadFolder", "code");

		List<NewspaperIssue> issues = new ArrayList<>();
		Iterator<String> rows = manager.getIdentifiers().iterator();
		// skip first row
		if (rows.hasNext()) {
			rows.next();
		}
		while (rows.hasNext()) {
			String rowNumber = rows.next();
			Map<String, String> rowData = manager.getRow(rowNumber);
			try {
				NewspaperIssue issue = createIssue(cmsColumn, rowData);
				String folderName = rowData.get(folderColumn);
				addFiles(issue, folderName);
				issues.add(issue);
			} catch (NullPointerException e) {
				log.warn("No newspaper with cmsId '" + rowData.get(cmsColumn) + "' registered");
			}

		}
		return issues;
	}

	/**
	 * @param issue
	 * @param folderName
	 */
	private void addFiles(NewspaperIssue issue, String folderName) {
		File mediaFolder = new File(getConfig().getString("uploadFilesBasePath", "/opt/digiverso/goobi/import/"),
				folderName);
		if (mediaFolder.isDirectory() && mediaFolder.list().length > 0) {
			issue.setFiles(getFileUploads(mediaFolder));
		} else {
			log.warn("No files found in " + mediaFolder);
		}
	}

	private List<FileUpload> getFileUploads(File mediaFolder) {
		List<FileUpload> files = new ArrayList<FileUpload>();
		if (mediaFolder != null) {
			for (File file : mediaFolder.listFiles(getMediaFilter())) {
				FileUpload upload = new FileUpload();
				upload.setPath(file);
				files.add(upload);
			}
		}
		return files;
	}

	private FilenameFilter getMediaFilter() {
		return new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				System.out.println(getAllowedTypes());
				return name.matches(getAllowedTypes());
			}
		};
	}

	/**
	 * @param cmsColumn
	 * @param folderColumn
	 * @param fields
	 * @param issues
	 * @param rowNumber
	 * @param rowData
	 */
	private NewspaperIssue createIssue(String cmsColumn, Map<String, String> rowData) {
		String cms = rowData.get(cmsColumn);
		Map<String, String> fieldMap = mapIssueColumnsToFields(rowData);
		NewspaperIssue issue = new NewspaperIssue(searchNewspaper(cms));
		for (String field : fieldMap.keySet()) {
			String value = fieldMap.get(field);
			if (value != null && field != null) {
				try {
					switch (field) {
					case "issueDate":
						issue.setIssueDate(ExcelDataReader.inputDateFormat.parse(value));
						break;
					case "issueNumber":
						issue.setIssueNumber(Integer.parseInt(value));
						break;
					case "issueType":
						issue.setIssueType(IssueType.get(Integer.parseInt(value)));
						break;
					case "issueComment":
						issue.setIssueComment(value);
						break;
					default:
						log.warn("Unknown field " + field);
					}

				} catch (ParseException | IllegalArgumentException e) {
					log.warn("Unable to parse value '" + value + "' in column" + field);
				}
			}
		}
		return issue;

	}

}
