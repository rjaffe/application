/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.inner;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;

// XXX call order DependencyNotSatisfiedException
public class CoreConfig implements CSP, Configurable, ConfigRoot {
	private Map<String,Object> roots=new HashMap<String,Object>();
	public static String SECTIONED="org.collectionspace.app.config.spec";
	
	public void go(CSPContext ctx) { 
		ctx.addConfigRules(this);
		ctx.setConfigRoot(this);
		ctx.addConfigRules(this);
	}
	
	public String getName() { return "config.core"; }

	public void configure(Rules rules) {

		rules.addRule("ROOT",new String[]{"collection-space"},"org.collectionspace.app.cfg.main",null,new Target() {
			public Object populate(Object parent, ReadOnlySection milestone) {
				return this;
			}			
		});
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"cspace-config"},SECTIONED,null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				return CoreConfig.this;
			}
		});
		/* ROOT/collection-space -> MAIN */
		
	}
	
	public void setRoot(String key,Object value) { roots.put(key,value); }
	public Object getRoot(String key) { return roots.get(key); }

	public void config_finish() {}
}
