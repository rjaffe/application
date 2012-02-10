/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.helper.test;


import java.io.IOException;

import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TestConfigFinder {

	//used to test multi tenancy
	public static InputSource getConfigStream(String filename) throws CSPDependencyException {
		try {
			ConfigFinder cfg=new ConfigFinder(null);
			//InputSource out=cfg.resolveEntity("-//CSPACE//ROOT",filename);
			InputSource out=cfg.resolveEntity("-//CSPACE//TESTROOT",filename);
			if(out!=null)
				return out;		
			throw new CSPDependencyException("No config file found by any method");
		} catch(SAXException x) {
			throw new CSPDependencyException("Parsing failed",x);
		} catch (IOException x) {
			throw new CSPDependencyException("Parsing failed",x);
		}
	}

	public static InputSource getConfigStream() throws CSPDependencyException {
		return getConfigStream("default.xml");
	}
}
