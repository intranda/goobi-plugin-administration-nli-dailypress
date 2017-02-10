package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.primefaces.event.FileUploadEvent;

import de.intranda.goobi.model.FileUpload;
import de.intranda.goobi.model.Newspaper;
import de.intranda.goobi.model.NewspaperIssue;
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
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

	private String singleCmsID;
	private String singleIssueNumber;
	private Date singleIssueDate;
	private String singleIssueComment;
	private Newspaper selectedNewspaper = null;
	private NewspaperManager newspaperManager = null;
	private List<FileUpload> uploadedFiles = new ArrayList<>();
	private File importFolder = null;

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

	public String cancelMultipleImport() {
		Helper.setMeldung("plugin_NliDailyPress_cancelMessageMultipleImport");
		return "";
	}

	public String executeSingleImport() {
		NewspaperIssue singleIssue = new NewspaperIssue(selectedNewspaper);
		singleIssue.setIssueDate(dateFormat.format(singleIssueDate));
		singleIssue.setIssueNumber(singleIssueNumber);
		singleIssue.setIssueComment(singleIssueComment);
		if(createProcess(singleIssue, getUploadedFiles(), createProcessName(singleIssue), getWorkflowName())) {
			Helper.setMeldung("plugin_NliDailyPress_successMessageSingleImport");			
			resetSingleIssue();
		}
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
		String name = issue.getNewspaper().getCmsID() + "_" + issue.getIssueNumber() + "_" + issue.getIssueDate();
		return name.replaceAll("\\W", "");
	}

	public String executeMultipleImport() {
		Helper.setMeldung("plugin_NliDailyPress_successMessageMultipleImport");
		return "";
	}

	public void setSingleCmsID(String cmsId) {
		this.singleCmsID = cmsId;
		System.out.println("Selected " + cmsId);
		log.debug("selected cmsID " + cmsId);
	}

	public void searchNewspaper() {
		if (this.singleCmsID != null) {
			Map<String, String> newspaperData;
			try {
				newspaperData = getNewspaperManager().getRow(this.singleCmsID);
				for (String key : newspaperData.keySet()) {
					System.out.println(key + ": " + newspaperData.get(key));
				}
				this.selectedNewspaper = new Newspaper(this.singleCmsID, newspaperData);
			} catch (NullPointerException e) {
				System.out.println("no newspaper found for " + this.singleCmsID);
				this.selectedNewspaper = null;
			}

		} else {
			System.out.println("Not cmsID selected");
			this.selectedNewspaper = null;
		}
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
		} catch(ConcurrentModificationException e) {
			//too bad
		}
		return this.uploadedFiles;
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

	public FileUpload copyFile(String fileName, InputStream in) {
		OutputStream out = null;

		File file = new File(getImportFolder(true), fileName);
		FileUpload upload = new FileUpload();
		upload.setPath(file);
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
		return getConfig().getString("allowedFileTypes", "/(\\.|\\/)(gif|jpe?g|png|tiff?|jp2|pdf)$/");
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

	private boolean createProcess(NewspaperIssue issue, List<FileUpload> files, String processTitle,
			String workflowName) {

		if (ProcessManager.countProcesses("Titel='" + processTitle + "'") > 0) {
			Helper.setFehlerMeldung("processAlreadyInUse");
			return false;
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
			return false;
		}

		createProcessProperty("Template", template.getTitel(), newProcess);
		createProcessProperty("TemplateID", template.getId() + "", newProcess);
		createProcessProperty("CMS ID", issue.getNewspaper().getCmsID(), newProcess);
		createProcessProperty("Newspaper", issue.getNewspaper().getTitle(), newProcess);
		createProcessProperty("Issue number", issue.getIssueNumber(), newProcess);
		createProcessProperty("Issue date", issue.getIssueDate(), newProcess);
		createProcessProperty("Issue commenet", issue.getIssueComment(), newProcess);

		try {
			Fileformat ff = createFileformat(issue ,processTitle, getPrefs());
			newProcess.writeMetadataFile(ff);
		} catch (Exception e) {
			ProcessManager.deleteProcess(newProcess);
			log.error(e.toString(), e);
			Helper.setFehlerMeldung("Failed to create mets file: " + e.toString());
			return false;
		}
		
		try {
			File masterDir = new File(newProcess.getImagesOrigDirectory(true));
			if(!masterDir.isDirectory()) {
				masterDir.mkdirs();
			}
			for (FileUpload fileUpload : files) {
				FileUtils.copyFile(fileUpload.getPath(), new File(masterDir, fileUpload.getFileName()));
			}
		} catch (IOException | InterruptedException | SwapException | DAOException e) {
			ProcessManager.deleteProcess(newProcess);
			log.error(e);
			Helper.setFehlerMeldung(e.toString());
			return false;
		}

		Helper.setMeldung("Created process " + processTitle);
		return true;

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

		if(parentType == null) {
			throw new UGHException("No valid doc type for newspaper configured");
		}
		if(childType == null) {
			throw new UGHException("No valid doc type for newspaper-issue configured (" + getIssueDocType() + ")");
		}
		
		Fileformat ff = new MetsMods(prefs);
		ff.setDigitalDocument(new DigitalDocument());

		DocStruct parent = ff.getDigitalDocument().createDocStruct(parentType);
		DocStruct child = ff.getDigitalDocument().createDocStruct(childType);

		setMetadata(singleIssue.getNewspaper().getMetadataMap(), singleIssue.getNewspaper().getCmsID(), parent, prefs);
		setMetadata(singleIssue.getMetadataMap(), id, child, prefs);
		
//		MetadataType identifierType = prefs.getMetadataTypeByName("CatalogIDDigital");
//		Metadata idParent = new Metadata(identifierType);
//		idParent.setValue(singleIssue.getNewspaper().getCmsID());
//		parent.addMetadata(idParent);
//		Metadata idChild = new Metadata(identifierType);
//		idChild.setValue(id);
//		child.addMetadata(idChild);
		
		DocStruct book = ff.getDigitalDocument().createDocStruct(prefs.getDocStrctTypeByName("BoundBook"));
		ff.getDigitalDocument().setPhysicalDocStruct(book);
			
		parent.addChild(child);
		ff.getDigitalDocument().setLogicalDocStruct(parent);

		return ff;
	}

	private void setMetadata(Map<String, String> columnMap, String identifier, DocStruct ds, Prefs prefs) throws UGHException {
		for (String column : columnMap.keySet()) {
			String metadataName = getMetadataNameForColumn(column);
			if (metadataName != null) {
				MetadataType mdType = prefs.getMetadataTypeByName(metadataName);
				if (mdType != null) {
					Metadata md = new Metadata(mdType);
					md.setValue(columnMap.get(column));
					ds.addMetadata(md);
				}
			}
		}
		if(StringUtils.isNotBlank(identifier)) {	
			MetadataType identifierType = prefs.getMetadataTypeByName("CatalogIDDigital");
			Metadata mdId = new Metadata(identifierType);
			mdId.setValue(identifier);
			ds.addMetadata(mdId);
		}
	}

	protected String getMetadataNameForColumn(String column) {
		return getConfig().getString("metadataMappings/mapping[column='" + column + "']/metadata");
	}

}
