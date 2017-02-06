package de.intranda.goobi.plugins;

import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import lombok.Data;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j
public @Data class NliDailyPressPlugin implements IAdministrationPlugin, IPlugin {

    private static final String PLUGIN_NAME = "NliDailyPress";

    private String identifier;

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

}
