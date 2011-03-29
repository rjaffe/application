/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.authorities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Option;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * checks content of Vocabulary against a list and adds anything missing
 * eventually the service layer will have a function to do this so we wont have to do all the checking
 * 
 * @author caret
 *
 */
public class AuthoritiesVocabulariesInitialize implements WebMethod  {
	private static final Logger log=LoggerFactory.getLogger(AuthoritiesVocabulariesInitialize.class);
	private boolean append;
	private Instance n;
	private Record r;
	

	public AuthoritiesVocabulariesInitialize(Instance n, Boolean append) {
		this.append = append;
		this.n = n;
		this.r = n.getRecord();
	}
	public AuthoritiesVocabulariesInitialize(Record r, Boolean append) {
		this.append = append;
		this.r = r;
		this.n = null;
	}
	
	private JSONObject getDisplayNameList(Storage storage,String auth_type,String inst_type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		//should be using cached data (hopefully) from previous getPathsJson call
		JSONObject out=storage.retrieveJSON(auth_type+"/"+inst_type+"/"+csid+"/view", new JSONObject());
		return out;
	}
		
	
	
	private JSONObject list_vocab(JSONObject displayNames,Instance n,Storage storage,UIRequest ui,String param, Integer pageSize, Integer pageNum) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject restriction=new JSONObject();
		if(param!=null){
			restriction.put(n.getRecord().getDisplayNameField().getID(),param);
		}
		if(pageNum!=null){
			restriction.put("pageNum",pageNum);
		}
		if(pageSize!=null){
			restriction.put("pageSize",pageSize);
		}
		JSONObject data = storage.getPathsJSON(r.getID()+"/"+n.getTitleRef(),restriction);
		String[] results = (String[]) data.get("listItems");
		/* Get a view of each */
		for(String result : results) {
			//change csid into displayName
			JSONObject datanames = getDisplayNameList(storage,r.getID(),n.getTitleRef(),result);
			
			displayNames.put(datanames.getString("displayName"),result);
		}
		JSONObject alldata = new JSONObject();
		alldata.put("displayName", displayNames);
		alldata.put("pagination",  data.getJSONObject("pagination"));
		return alldata;
	}
	
	private void initializeVocab(Storage storage,UIRequest request,String path) throws UIException {

		if(n==null) {
			// For now simply loop thr all the instances one after the other.
			for(Instance n : r.getAllInstances()) {
				log.info(n.getID());
				resetvocabdata(storage, request, n);
			}
		} else {
			resetvocabdata(storage, request, this.n);
		}
	}
	
	private JSONObject getJSONResource(String in) throws IOException, JSONException {	
		return new JSONObject(getResource(in));
	}

	private String getResource(String in) throws IOException {
		File file=new File(in);
		if(!file.exists())
			return null;
		InputStream stream= new FileInputStream(file);
		String data=IOUtils.toString(stream);
		stream.close();		
		return data;
	}
	
	private void resetvocabdata(Storage storage,UIRequest request, Instance instance) throws UIException {
		//Where do we get the list from?
		//from Spec
		Option[] allOpts = instance.getAllOptions();
		
		//but first check: do we have a path?
		Set<String> args = request.getAllRequestArgument();
		if(args.contains("datapath")){
			//remove all opts from instance as we have a path
			if(allOpts != null && allOpts.length > 0){
				for(Option opt : allOpts){
					String name = opt.getName();
					String shortIdentifier = opt.getID();
					String sample = opt.getSample();
					Boolean dfault = opt.isDefault();
					instance.deleteOption(shortIdentifier, name, sample, dfault);
				}
			}
			
			//add from path
			String value = request.getRequestArgument("datapath");
			log.info("getting data from path: "+value);
			try{
				String names = getResource(value);
				for (String line : names.split("\n")) {
					line = line.trim();
					instance.addOption(null, line, null, false);
				}
			} catch (IOException e) {
				throw new UIException("IOException",e);
			}
			allOpts = instance.getAllOptions();
		}

		//step away if we have nothing
		if(allOpts != null && allOpts.length > 0){

			//get list from Service layer
			JSONObject results = new JSONObject();
			try {
				Integer pageNum = 0;
				Integer pageSize = 100;
				JSONObject fulldata= list_vocab(results,instance,storage,request,null, pageSize,pageNum);

				while(!fulldata.isNull("pagination")){
					Integer total = fulldata.getJSONObject("pagination").getInt("totalItems");
					pageSize = fulldata.getJSONObject("pagination").getInt("pageSize");
					Integer itemsInPage = fulldata.getJSONObject("pagination").getInt("itemsInPage");
					pageNum = fulldata.getJSONObject("pagination").getInt("pageNum");
					results=fulldata.getJSONObject("displayName");
					
					pageNum++;
					//are there more results
					if(total > (pageSize * (pageNum))){
						fulldata= list_vocab(results, instance, storage, request, null, pageSize, pageNum);
					}
					else{
						break;
					}
				}

				//compare
				results= fulldata.getJSONObject("displayName");

				//only add if term is not already present
				for(Option opt : allOpts){
					String name = opt.getName();
					String shortIdentifier = opt.getID();
					if(!results.has(name)){
						log.info("adding term "+name);
						//create it if term is not already present
						JSONObject data=new JSONObject("{'displayName':'"+name+"'}");
						if(opt.getID() == null){
							//XXX here until the service layer does this
							shortIdentifier = name.replaceAll("\\W", "").toLowerCase();
						}
						data.put("shortIdentifier", shortIdentifier);
						storage.autocreateJSON(r.getID()+"/"+instance.getTitleRef(),data);
						results.remove(name);
					}
					else{
						//remove from results so can delete everything else if necessary in next stage
						//tho has issues with duplicates
						results.remove(name);
					}
				}
				if(!this.append){
					//delete everything that is not in options
					Iterator<String> rit=results.keys();
					while(rit.hasNext()) {
						String key=rit.next();
						String csid = results.getString(key);
						storage.deleteJSON(r.getID()+"/"+instance.getTitleRef()+"/"+csid);
						log.info("deleting term "+key);
					}
				}

			} catch (JSONException e) {
				throw new UIException("Cannot generate JSON",e);
			} catch (ExistException e) {
				throw new UIException("Exist exception",e);
			} catch (UnimplementedException e) {
				throw new UIException("Unimplemented exception",e);
			} catch (UnderlyingStorageException x) {
				UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
				request.sendJSONResponse(uiexception.getJSON());
			}
		}
	}
	
	public void configure(WebUI ui, Spec spec) {}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		initializeVocab(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}
}