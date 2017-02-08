package de.intranda.goobi.input;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ExcelDataReaderTest {

	private static final String sampleTablePath = "/home/florian/git/goobi-plugin-administration-nli-dailypress/goobi-plugin-administration-nli-dailypress/test/resources/DailyPress_SamplePapers.xlsx";
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRead() throws IOException {
		File file = new File(sampleTablePath);
		Map<String, Map<String, String>> table = new ExcelDataReader().read(file, 1, 1);
		Assert.assertEquals("Wrong number of rows",  4, table.size());
		Assert.assertEquals("Wrong number of columns",  4, table.get("CMS ID").size());
		Assert.assertTrue("Wrong key set: " + table.keySet(), table.keySet().contains("1234"));
		Assert.assertTrue("Wrong key set: " + table.keySet(), table.keySet().contains("5678"));
		Assert.assertTrue("Wrong key set: " + table.keySet(), table.keySet().contains("9101"));
		Assert.assertTrue("Wrong row data in column TITLE: " + table.get("1234").values(), "Göttinger Tageblatt".equals(table.get("1234").get("TITLE")));
		Assert.assertTrue("Wrong row data in column PUBLISHING HOUSE: " + table.get("9101").values(), "Gemeinde Rosdorf".equals(table.get("9101").get("PUBLISHING HOUSE")));
	}
}
