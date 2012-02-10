/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.ui;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public interface UIRequest {	
	public String[] getPrincipalPath() throws UIException;
	
	public TTYOutputter getTTYOutputter() throws UIException;
	public void sendXMLResponse(String out) throws UIException;
	public void sendUnknown(byte[] data, String contenttype) throws UIException;
	public void sendUnknown(String data, String contenttype) throws UIException;
	public void sendJSONResponse(JSONObject out) throws UIException;
	public void sendJSONResponse(JSONArray out) throws UIException;

	JSONObject getJSONBody() throws UIException;
	JSONObject getPostBody() throws UIException;
	String getBody() throws UIException;
	String getContentType() throws UIException;
	public byte[] getbyteBody() throws UIException;
	String getFileName() throws UIException;
	Boolean isJSON() throws UIException;
	
	public void setFailure(boolean isit,Exception why) throws UIException;
	
	public void setOperationPerformed(Operation op) throws UIException;
	
	public void setRedirectPath(String[] in) throws UIException;
	public void setSecondaryRedirectPath(String[] in) throws UIException;
	public void setRedirectArgument(String key,String value) throws UIException;
	public void deleteRedirectArgument(String key) throws UIException;
	public String getRedirectArgument(String key) throws UIException;

	public String getRequestArgument(String key) throws UIException;
	public Set<String> getAllRequestArgument() throws UIException;
	public Operation getRequestedOperation() throws UIException;
	
	public UISession getSession() throws UIException;


}
