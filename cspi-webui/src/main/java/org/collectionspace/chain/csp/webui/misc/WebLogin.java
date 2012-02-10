/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.misc;

import java.util.HashSet;
import java.util.Set;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebLogin implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebLogin.class);
	private String login_dest,login_failed_dest,tenantid;
	private Spec spec;
	private WebUI ui;
	
	public WebLogin(WebUI ui,Spec spec) {
		this.spec=spec;
		this.ui=ui;
		this.tenantid = spec.getAdminData().getTenant();
	}
	
	private boolean testSuccess(Storage storage, String tenant) {

		try {
			String base = spec.getRecordByWebUrl("userperm").getID();
			JSONObject activePermissions = storage.retrieveJSON(base + "/0/", new JSONObject());
			
			if(activePermissions.has("isError")){
				if(activePermissions.getBoolean("isError")== false){
					return false;
				}
			}

			//check tenant
			if(activePermissions.has("account")){
				JSONObject acc = activePermissions.getJSONObject("account");
				if(acc.has("tenantId")){
					if(acc.getString("tenantId").equals(tenant)){
						return true;
					}
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
		
	}
	
	private void login(Request in) throws UIException { // Temporary hack for Mars
		UIRequest request=in.getUIRequest();
		String username=request.getRequestArgument("userid");
		String password=request.getRequestArgument("password");
		String tenantId=tenantid;
	
		if(request.getRequestArgument("userid") ==  null){
			JSONObject data = new JSONObject();
			if(request.isJSON()){
				data=request.getJSONBody();
			}
			else{
				data=request.getPostBody();
			}
		// XXX stop defaulting to GET request when UI layer stops doing login via GET
			if(data.has("userid")){
				try {
					username=data.getString("userid");
					password=data.getString("password");
					if(data.has("tenant")){
						tenantId=data.getString("tenant");
					}
				} catch (JSONException e) {
					username=request.getRequestArgument("userid");
					password=request.getRequestArgument("password");
				}
			}
		}
		request.getSession().setValue(UISession.USERID,username);
		request.getSession().setValue(UISession.PASSWORD,password);
		request.getSession().setValue(UISession.TENANT,tenantId);
		in.reset();
		if(testSuccess(in.getStorage(), tenantId)) {
			request.setRedirectPath(login_dest.split("/"));
		} else {
			request.getSession().setValue(UISession.USERID,"");
			request.getSession().setValue(UISession.PASSWORD,"");
			request.getSession().setValue(UISession.TENANT,"");
			request.setRedirectPath(login_failed_dest.split("/"));
			request.setRedirectArgument("result","fail");
		}
	}
		
	public void run(Object in,String[] tail) throws UIException {
		login((Request)in);
	}

	public void configure() throws ConfigException {}

	public void configure(WebUI ui,Spec spec) {
		login_dest=ui.getLoginDest();
		login_failed_dest=ui.getLoginFailedDest();
	}
}
