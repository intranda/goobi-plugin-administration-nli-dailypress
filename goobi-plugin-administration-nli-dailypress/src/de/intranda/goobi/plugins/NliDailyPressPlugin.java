package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.intranda.goobi.model.Newspaper;
import de.intranda.goobi.model.NewspaperIssue;
import de.intranda.goobi.model.NewspaperManager;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.Fileformat;

@PluginImplementation
@Log4j
public @Data class NliDailyPressPlugin implements IAdministrationPlugin, IPlugin {

    private static final String PLUGIN_NAME = "NliDailyPress";

    private String singleCmsID;
    private String singleIssueNumber;
    private String singleIssueDate;
    private String singleIssueComment;
    private Newspaper selectedNewspaper = null;
    private NewspaperManager newspaperManager = null;
    
    private  XMLConfiguration config;

    
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
    
    public String cancelSingleImport(){
    	Helper.setMeldung("plugin_NliDailyPress_cancelMessageSingleImport");
    	resetSingleIssue();
    	return "";
    }
    

    public String cancelMultipleImport(){
    	Helper.setMeldung("plugin_NliDailyPress_cancelMessageMultipleImport");
    	return "";
    }
    
    public String executeSingleImport(){
    	Helper.setMeldung("plugin_NliDailyPress_successMessageSingleImport");
    	NewspaperIssue singleIssue = new NewspaperIssue(selectedNewspaper);
    	singleIssue.setIssueDate(singleIssueDate);
    	singleIssue.setIssueNumber(singleIssueNumber);
    	singleIssue.setIssueComment(singleIssueComment);
    	Fileformat ff = createFileformat(singleIssue);
    	createProcess(ff);
    	resetSingleIssue();
    	return "";
    } 
    


	public String executeMultipleImport(){
    	Helper.setMeldung("plugin_NliDailyPress_successMessageMultipleImport");
    	return "";
    }    
    
    public void setSingleCmsID(String cmsId) {
    	this.singleCmsID = cmsId;
    	System.out.println("Selected " + cmsId);
    	log.debug("selected cmsID " + cmsId);
    }
    
    public void searchNewspaper(){
    	if(this.singleCmsID != null) {
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
		if(this.newspaperManager == null) {			
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
		if(this.config == null) {
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
    }
    
    private void createProcess(Fileformat ff) {
		// TODO Auto-generated method stub
		
	}

	private Fileformat createFileformat(NewspaperIssue singleIssue) {
		// TODO Auto-generated method stub
		return null;
	}

}
