/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.userroles;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UserRolesSearchList implements WebMethod{
	private boolean search;
	private String base;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public UserRolesSearchList(Record r,boolean search) {
		this.base=r.getID();
		this.search=search;
	}
		
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"", new JSONObject());
		out.put("csid",csid);
		out.put("recordtype",type_to_url.get(type));
		return out;		
	}
	
	private JSONObject generateEntry(Storage storage,String base,String member) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		return generateMiniRecord(storage,base,member);
	}
	
	private JSONObject pathsToJSON(Storage storage,String base,String[] paths,String key) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		for(String p : paths)
			members.put(generateEntry(storage,base,p));
		out.put(key,members);
		return out;
	}
	
	private void search_or_list(Storage storage,UIRequest ui,String param, String pageSize, String pageNum) throws UIException {
		try {
			JSONObject restriction=new JSONObject();
			String key="items";
			if(param!=null) {
				restriction.put("screenName",param);
				key="results";
			}
			if(pageSize!=null) {
				restriction.put("pageSize",pageSize);
			}
			if(pageNum!=null) {
				restriction.put("pageNum",pageNum);
			}
			JSONObject data = storage.getPathsJSON(base,restriction);
			String[] paths = (String[]) data.get("listItems");
			for(int i=0;i<paths.length;i++) {
				if(paths[i].startsWith(base+"/"))
					paths[i]=paths[i].substring((base+"/").length());
			}
			JSONObject resultsObject=new JSONObject();
			resultsObject = pathsToJSON(storage,base,paths,key);
			ui.sendJSONResponse(resultsObject);
		} catch (JSONException e) {
			throw new UIException("JSONException during autocompletion",e);
		} catch (ExistException e) {
			throw new UIException("ExistException during autocompletion",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during autocompletion",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			ui.sendJSONResponse(uiexception.getJSON());
		}			
	}
	
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		if(search)
			search_or_list(q.getStorage(),q.getUIRequest(),q.getUIRequest().getRequestArgument("query"),q.getUIRequest().getRequestArgument("pageSize"),q.getUIRequest().getRequestArgument("pageNum"));
		else
			search_or_list(q.getStorage(),q.getUIRequest(),null,q.getUIRequest().getRequestArgument("pageSize"),q.getUIRequest().getRequestArgument("pageNum"));
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getServicesURL(),r.getWebURL());
		}
	}
}
