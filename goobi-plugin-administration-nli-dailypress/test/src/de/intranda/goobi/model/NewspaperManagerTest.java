package de.intranda.goobi.model;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ExpressionEngine;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class NewspaperManagerTest {
	
	private static final String CONFIG_PATH = "test/resources/plugin_NliDailyPressPlugin.xml";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws ConfigurationException, IOException {
		XMLConfiguration config = new XMLConfiguration(new File(CONFIG_PATH));
		config.setExpressionEngine(new XPathExpressionEngine());
		NewspaperManager newspaperManager = new NewspaperManager(config);
		Assert.assertEquals("Simmern", newspaperManager.getValue("5678", "PUBLISHING PLACE"));
	}

}
