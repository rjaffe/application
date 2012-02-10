/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestVocabThroughWebapp  {
	private static final Logger log = LoggerFactory
			.getLogger(TestVocabThroughWebapp.class);
	private static TestBase tester = new TestBase();
	static ServletTester jetty;
	static {
		try{
			jetty=tester.setupJetty();
			}
		catch(Exception ex){
			
		}
	}
	
	@AfterClass public static  void testStop() throws Exception {
		tester.stopJetty(jetty);
	}

	@Before
	public void testInitialise() throws Exception {
		String vocabtype = "currency";
		HttpTester out;
		// String vocabtype="loanoutstatus";
		// Create a single vocab
		// out = tester.GETData("/vocabularies/"+vocabtype+"/initialize",jetty);

		// create all vocabularies in <record id="vocab"
		out = tester.GETData("/authorities/vocab/initialize", jetty);

		// update and remove fields not in list
		// out = tester.GETData("/vocabularies/"+vocabtype+"/refresh",jetty);

		// update and remove fields not in each list within an authority
		// out = tester.GETData("/authorities/vocab/refresh",jetty);
		
		// get data to init from a file
		// /chain/vocabularies/"+vocabtype+"/initialize?datapath=/Users/csm22/Documents/collectionspace/svcapp/cspi-webui/src/main/resources/org/collectionspace/chain/csp/webui/misc/names.txt
			log.info(out.getContent());

	}

	@Test
	public void testCRUDitem() throws Exception {
		String displayname = "XXXStuff1";
		String displaynameUpdate = "XXXLessStuff";
		String vocabtype = "languages";
		String testfield = "displayName";

		// Create
		JSONObject data = new JSONObject("{'fields':{'" + testfield + "':'"
				+ displayname + "'}}");
		HttpTester out = tester.POSTData("/vocabularies/" + vocabtype + "/", data,
				jetty);
		String url = out.getHeader("Location");
		// Read
		out = tester.GETData("/vocabularies" + url, jetty);
		data = new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"), url.split("/")[2]);
		assertEquals(displayname, data.getString(testfield));
		// Update
		data = new JSONObject("{'fields':{'" + testfield + "':'"
				+ displaynameUpdate + "'}}");
		out = tester.PUTData("/vocabularies" + url, data, jetty);
		// Read
		out = tester.GETData("/vocabularies" + url, jetty);
		data = new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"), url.split("/")[2]);
		assertEquals(displaynameUpdate, data.getString(testfield));
		// Delete
		tester.DELETEData("/vocabularies/" + url, jetty);

	}

	@Test
	public void testList() throws Exception {
		String displayname = "XXXStuff2";
		String vocabtype = "languages";
		String testfield = "displayName";

		// Create
		JSONObject data = new JSONObject("{'fields':{'" + testfield + "':'"
				+ displayname + "'}}");
		HttpTester out = tester.POSTData("/vocabularies/" + vocabtype + "/", data,
				jetty);
		String url = out.getHeader("Location");
		// Get List
		int resultsize = 1;
		int pagenum = 0;
		String checkpagination = "";
		boolean found = false;
		while (resultsize > 0) {
			out = tester.GETData("/vocabularies/" + vocabtype
					+ "?pageSize=200&pageNum=" + pagenum, jetty);
			pagenum++;
			JSONArray results = new JSONObject(out.getContent())
					.getJSONArray("items");

			if (results.length() == 0
					|| checkpagination.equals(results.getJSONObject(0)
							.getString("csid"))) {
				resultsize = 0;
				break;
				// testing whether we have actually returned the same page or
				// the next page - all csid returned should be unique
			}
			checkpagination = results.getJSONObject(0).getString("csid");

			for (int i = 0; i < results.length(); i++) {
				JSONObject entry = results.getJSONObject(i);
				if (entry.getString(testfield).toLowerCase().contains(
						displayname.toLowerCase())) {
					found = true;
					resultsize = 0;
				}
			}
		}
		assertTrue(found);
		// Delete
		tester.DELETEData("/vocabularies/" + url, jetty);
	}

	@Test
	public void testSearch() throws Exception {
		String displayname = "XXXStuff3";
		String vocabtype = "languages";
		String testfield = "displayName";

		// Create the entry we are going to check for
		JSONObject data = new JSONObject("{'fields':{'" + testfield + "':'"
				+ displayname + "'}}");
		HttpTester out = tester.POSTData("/vocabularies/" + vocabtype + "/", data,
				jetty);
		String url = out.getHeader("Location");

		out = tester.GETData("/vocabularies/" + vocabtype + "/search?query="
				+ displayname, jetty);
		JSONArray results = new JSONObject(out.getContent())
				.getJSONArray("results");

		// Delete the entry from the database
		tester.DELETEData("/vocabularies/" + url, jetty);
		
		for (int i = 0; i < results.length(); i++) {
			JSONObject entry = results.getJSONObject(i);
			assertTrue(entry.getString(testfield).toLowerCase().contains(
					displayname.toLowerCase()));
			assertEquals(entry.getString("number"), entry.getString(testfield));
			assertTrue(entry.has("refid"));
		}


	}

	// Tests that a redirect goes to the expected place
	@Test
	public void testAutocompleteRedirect() throws Exception {
		HttpTester out = tester.GETData(
				"/cataloging/source-vocab/inscriptionContentLanguage", jetty);

		JSONArray data = new JSONArray(out.getContent());
		boolean test = false;
		for (int i = 0; i < data.length(); i++) {
			String url = data.getJSONObject(i).getString("url");
			if (url.equals("/vocabularies/languages")) {
				test = true;
			}
		}
		assertTrue("correct vocab not found", test);

	}
	// inscriptionContentLanguage

}
