package de.intranda.goobi.plugins;

import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.helper.Helper;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j
public @Data class NliDailyPressPlugin implements IAdministrationPlugin, IPlugin {

    private static final String PLUGIN_NAME = "NliDailyPress";

    private String singleCmsID;
    private String singleIssueNumber;
    private String singleIssueDate;
    private String singleIssueComment;
    
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
    	return "";
    }
    
    public String cancelMultipleImport(){
    	Helper.setMeldung("plugin_NliDailyPress_cancelMessageMultipleImport");
    	return "";
    }
    
    public String executeSingleImport(){
    	Helper.setMeldung("plugin_NliDailyPress_successMessageSingleImport");
    	return "";
    } 
    
    public String executeMultipleImport(){
    	Helper.setMeldung("plugin_NliDailyPress_successMessageMultipleImport");
    	return "";
    }    
    
    

}
