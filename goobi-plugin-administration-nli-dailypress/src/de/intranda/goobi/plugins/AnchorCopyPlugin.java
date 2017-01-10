package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.MetadataManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import lombok.Data;
import lombok.extern.log4j.Log4j;

@PluginImplementation
@Log4j
public @Data class AnchorCopyPlugin implements IAdministrationPlugin, IPlugin {

    private static final String PLUGIN_NAME = "NliDailyPress";
    private static final String METADATA_TYPE = "InternalNote";
    private static final String METADATA_VALUE = "AnchorMaster";

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
        return "/uii/administration_CopyAnchor.xhtml";
    }
    
    public String getDescription() {
        return PLUGIN_NAME;
    }

    public void copyAnchorFile() {
        if (log.isDebugEnabled()) {
            log.debug("replace anchor files for " + identifier);
        }
        //        alle Vorgänge mit dem Identifier suchen
        List<Integer> processList = MetadataManager.getProcessesWithMetadata("CatalogIDDigital", identifier);

        if (processList.size() == 0) {
            Helper.setFehlerMeldung("noProcessesFound");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(processList.size() + " processes found");
        }

        //        Master Vorgang mit dem Identifier suchen
        Path masterFile = null;
        for (Integer id : processList) {
            List<StringPair> spl = MetadataManager.getMetadata(id);
            for (StringPair sp : spl) {
                if (sp.getOne().equals(METADATA_TYPE) && sp.getTwo().equals(METADATA_VALUE)) {
                    // copy external master to current process
                    masterFile = Paths.get("/opt/digiverso/goobi/metadata/" + id + "/meta_anchor.xml");
                    break;
                }
            }
        }
        if (masterFile == null) {
            Helper.setFehlerMeldung("noMasterProcessFound");
            return;
        }
        //        3.) master anchor in alle anderen Bände kopieren
        for (Integer id : processList) {
            Path destination = Paths.get("/opt/digiverso/goobi/metadata/" + id + "/meta_anchor.xml");
            if (!destination.equals(masterFile)) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Replacing " + destination.toString());
                    }
                    Files.copy(masterFile, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error(e);
                    Helper.setFehlerMeldung(e);
                }
            }
        }
        Helper.setMeldung("replacedProcesses");

    }
}
