package de.intranda.goobi.plugins;

import java.io.File;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class NliDailyPressPluginTest {
	
	NliDailyPressPlugin plugin;
	String configPath = "plugin_NliDailyPressPlugin.xml";

	@Before
	public void setUp() throws Exception {
		plugin = new NliDailyPressPlugin();
		XMLConfiguration config = new XMLConfiguration(new File(configPath));
		config.setExpressionEngine(new XPathExpressionEngine());
		plugin.setConfig(config);
		
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void getWorkflowName() {
		Assert.assertEquals("DailyPress", plugin.getWorkflowName());
	}

	@Test
	public void testGetMetadataForColumn() {
		Assert.assertEquals("TitleDocMain", plugin.getMetadataNameForField("Title"));
		Assert.assertEquals("PublisherName", plugin.getMetadataNameForField("Publisher"));
	}

}
