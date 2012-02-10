/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.persistence.services.GenericStorage;
import org.collectionspace.chain.csp.persistence.services.XmlJsonConversion;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Relationship;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.DocumentException;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfiguredVocabStorage extends GenericStorage {
	private static final Logger log=LoggerFactory.getLogger(ConfiguredVocabStorage.class);
	private ServicesConnection conn;
	private Record r;

	public ConfiguredVocabStorage(Record r,ServicesConnection conn) throws  DocumentException, IOException {
		super(r,conn);
		initializeGlean(r);
		this.conn=conn;
		this.r=r;
	}
	
	private String getDisplayNameKey() throws UnderlyingStorageException {
		Field dnf=(Field)r.getDisplayNameField();
		if(dnf==null)
			throw new UnderlyingStorageException("no display-name='yes' field");
		return dnf.getID();
	}
	private Document createEntry(String section,String namespace,String root_tag,JSONObject data,String vocab,String refname, Record r, Boolean isAuth) throws UnderlyingStorageException, ConnectionException, ExistException, JSONException {
		Document out=XmlJsonConversion.convertToXml(r,data,section,"POST",isAuth);
		if(section.equals("common")){//XXX not great... but not sure how else to differentiate
			if(out!=null){
				Element root=out.getRootElement();
				Element vocabtag=root.addElement("inAuthority");
				if(vocab!=null){
					vocabtag.addText(vocab);
				}
				if(refname!=null){
				//	CSPACE-4460
				//	Element refnametag=root.addElement("refName");
				//	refnametag.addText(refname);
				}
				if(r.isType("compute-displayname")) {
					Element dnc=root.addElement("displayNameComputed");
					dnc.addText("false");
				}
				//log.info("create Configured Vocab Entry"+out.asXML());
			}
		}
		return out;
	}
	
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Map<String,Document> body=new HashMap<String,Document>();
			String vocab = null;

			String pathurl ="/"+r.getServicesURL()+"/";
			if(filePath.equals("")){ //this creating an authority instance not an item 

				for(String section : r.getServicesInstancesPaths()) {
					String path=r.getServicesInstancesPath(section);
					String[] record_path=path.split(":",2);
					String[] tag_path=record_path[1].split(",",2);
					Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,null,r,true);
					if(temp!=null){
						body.put(record_path[0],temp);
						//log.info(temp.asXML());
					}

				}
			}
			else{
				vocab = RefName.shortIdToPath(filePath);
				pathurl ="/"+r.getServicesURL()+"/"+vocab+"/items";
				for(String section : r.getServicesRecordPaths()) {
					String path=r.getServicesRecordPath(section);
					String[] record_path=path.split(":",2);
					String[] tag_path=record_path[1].split(",",2);
					Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,null,r, false);
					if(temp!=null){
						body.put(record_path[0],temp);
						//log.info(temp.asXML());
					}

				}
			}
	

			if(r.hasHierarchyUsed("screen")){
				ArrayList<Element> alleles = new ArrayList<Element>();
				for(Relationship rel : r.getSpec().getAllRelations()){
					Relationship newrel=rel;
					Boolean inverse = false;
					if(rel.hasInverse()){
						newrel = r.getSpec().getRelation(rel.getInverse());
						inverse = true;
					}
					//does this relationship apply to this record
					if(rel.hasSourceType(r.getID())){
						
						//does this record have the data in the json
						if(jsonObject.has(rel.getID())){
							if(rel.getObject().equals("1")){
								if(jsonObject.has(rel.getID()) && !jsonObject.get(rel.getID()).equals("")){
									Element bit = createRelationship(newrel,jsonObject.get(rel.getID()),null,r.getServicesURL(),null, inverse);
									if(bit != null){
										alleles.add(bit);
									}
								}
							}
							else if(rel.getObject().equals("n")){
								JSONArray temp = jsonObject.getJSONArray(rel.getID());
								for(int i=0; i<temp.length();i++){
									String argh = rel.getChildName();
									JSONObject brgh = temp.getJSONObject(i);
									if(brgh.has(argh) && !brgh.getString(argh).equals("")){
										String uri = brgh.getString(argh);
										Element bit = createRelationship(newrel,uri,null,r.getServicesURL(),null, inverse);
										if(bit != null){
											alleles.add(bit);
										}
									}
								}
							}
						}
					}
				
				}
				//add relationships section
				if(!alleles.isEmpty()){
					Element[] array = alleles.toArray(new Element[0]);
					Document out=XmlJsonConversion.getXMLRelationship(array);

					body.put("relations-common-list",out);
				}
				else{
					Document out=XmlJsonConversion.getXMLRelationship(null);
					body.put("relations-common-list",out);
				}
			}

			ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,pathurl,body,creds,cache);		
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary",out.getStatus(),pathurl);


			String csid=out.getURLTail();
			//CACHE????? should we cache things?
			
			// create related sub records?
			for(FieldSet fs : r.getAllSubRecords("POST")){
				Record sr = fs.usesRecordId();
				//sr.getID()
				if(sr.isType("authority")){
					String savePath = out.getURL() + "/" + sr.getServicesURL();
					if(fs instanceof Field){//get the fields form inline XXX untested - might not work...
						JSONObject subdata = new JSONObject();
						//loop thr jsonObject and find the fields I need
						for(FieldSet subfs: sr.getAllFieldTopLevel("POST")){
							String key = subfs.getID();
							if(jsonObject.has(key)){
								subdata.put(key, jsonObject.get(key));
							}
						}
						subautocreateJSON(root,creds,cache,sr,subdata,savePath);
					}
					else if(fs instanceof Group){//JSONObject
						if(jsonObject.has(fs.getID())){
							Object subdata = jsonObject.get(fs.getID());
							if(subdata instanceof JSONObject){
								JSONObject subrecord = (JSONObject)subdata;
								subautocreateJSON(root,creds,cache,sr,subrecord,savePath);
							}
						}
					}
					else{//JSONArray
						if(jsonObject.has(fs.getID())){
							Object subdata = jsonObject.get(fs.getID());
							if(subdata instanceof JSONArray){
								JSONArray subarray = (JSONArray)subdata;

								for(int i=0;i<subarray.length();i++) {
									JSONObject subrecord = subarray.getJSONObject(i);
									subautocreateJSON(root,creds,cache,sr,subrecord,savePath);
								}
								
							}
						}
					}
				}
			}
			return csid;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON"+e.getLocalizedMessage(),e);
		}
	}
	

	
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Integer num = 0;
			String[] parts=filePath.split("/");
			//deal with different url structures
			String vocab,csid;
			if("_direct".equals(parts[0])) { 
				vocab=parts[2];
				csid=parts[3];
				num=4;
			} else {
				vocab = RefName.shortIdToPath(parts[0]);
				csid=parts[1];	
				num = 2;
			}		
			
			if(parts.length>num) {
				String extra = "";
				Integer extradata = num + 1;
				if(parts.length>extradata){
					extra = parts[extradata];
				}
				String servicepath = generateURL(vocab,csid,"",this.r);
				
				return viewRetrieveJSON(root,creds,cache,null,parts[num], extra, restrictions, servicepath);
			} else
				return simpleRetrieveJSON(root,creds,cache,vocab,csid);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}  catch(JSONException x) {
			throw new UnderlyingStorageException("Error building JSON"+x.getLocalizedMessage(),x);
		}  catch (UnsupportedEncodingException x) {
			throw new UnderlyingStorageException("Error UnsupportedEncodingException JSON",x);
		}
		
	}
	private String generateURL(String vocab,String path,String extrapath,Record myr) throws ExistException, ConnectionException, UnderlyingStorageException {
		String url = myr.getServicesURL()+"/"+vocab+"/items/"+path+extrapath;
		return url;
	}
	
	public JSONObject simpleRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab, String csid) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException{
		JSONObject out=new JSONObject();
		out=get(storage, creds,cache,vocab,csid,conn.getIMSBase());
		//cache.setCached(getClass(),new String[]{"csidfor",vocab,csid},out.get("csid"));//cos csid might be a refname at this point..
		//cache.setCached(getClass(),new String[]{"namefor",vocab,csid},out.get(getDisplayNameKey()));
		//cache.setCached(getClass(),new String[]{"reffor",vocab,csid},out.get("refid"));
		//cache.setCached(getClass(),new String[]{"shortId",vocab,csid},out.get("shortIdentifier"));
		return out;
	}
	
	private JSONObject get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab,String csid,String ims_url) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		String url = generateURL(vocab,csid,"",this.r);
		return get(storage,creds,cache,url,ims_url);
	}
	private JSONObject get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String url,String ims_url) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		//int status=0;
		String csid = "";
		JSONObject out = new JSONObject();
			// XXX pagination support

			String softurl = url;
			if(r.hasSoftDeleteMethod()){
				softurl = softpath(url);
			}
			if(r.hasHierarchyUsed("screen")){
				softurl = hierarchicalpath(softurl);
			}
			
			
			ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.GET,softurl,null,creds,cache);
			if(doc.getStatus()==404)
				throw new ExistException("Does not exist "+softurl);
			if(doc.getStatus()==403){
				//permission error - keep calm and carry on with what we can glean
				out.put("displayName",getDisplayNameKey());
				out.put("csid",csid);
				out.put("recordtype",r.getWebURL());
				return out;
			}
			if(doc.getStatus()>299)
				throw new UnderlyingStorageException("Could not retrieve vocabulary status="+doc.getStatus());
			String name = null;
			String refid = null;
			String termStatus = null;
			String parentcsid = null;
			String shortIdentifier = "";
			
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);
				Document result=doc.getDocument(record_path[0]);


				if("common".equals(section)) { // XXX hardwired :(
					name=result.selectSingleNode(tag_path[1]+"/displayName").getText();
					if(result.selectSingleNode(tag_path[1]+"/shortIdentifier")!=null){
						shortIdentifier = result.selectSingleNode(tag_path[1]+"/shortIdentifier").getText();
					}
					refid=result.selectSingleNode(tag_path[1]+"/refName").getText();
					if(result.selectSingleNode(tag_path[1]+"/termStatus") != null){
					termStatus=result.selectSingleNode(tag_path[1]+"/termStatus").getText();
					}
					else{
						termStatus = "";
					}
					csid=result.selectSingleNode(tag_path[1]+"/csid").getText();
					parentcsid = result.selectSingleNode(tag_path[1]+"/inAuthority").getText();
					XmlJsonConversion.convertToJson(out,r,result,"GET",section,csid,ims_url);	
				}
				else{
					XmlJsonConversion.convertToJson(out,r,result,"GET",section,csid,ims_url);
				}
			}
			

	        //RefName.AuthorityItem itemParsed = RefName.AuthorityItem.parse(refid);
	        //String thisShortid = itemParsed.getShortIdentifier();
	        String thiscsid = csid;
	        //String thisparent = itemParsed.getParentShortIdentifier();

			
			if(r.hasHierarchyUsed("screen")){
				//lets do relationship stuff...
				Document list = doc.getDocument("relations-common-list");
				//loop over all the relationship types

				//List<String> listitems=new ArrayList<String>();
//persons-common-list/person_list_item
				List<Node> nodes=list.selectNodes("/relations-common-list/*");

				for(Node node : nodes) {
					if(node.matches("/relations-common-list/relation-list-item")){
						//String test = node.asXML();
						String relationshipType = node.selectSingleNode("relationshipType").getText();
						//String subjCSID = node.selectSingleNode("subjectCsid").getText();
						String objCSID = node.selectSingleNode("objectCsid").getText();
						String suri="";
						String scsid="";
						String sser="";
						if(node.selectSingleNode("subject/uri")!=null){
							//String surl = node.selectSingleNode("subject/uri").getText();
							scsid=node.selectSingleNode("subject/csid").getText();
							//String sname=node.selectSingleNode("subject/name").getText();
							sser=node.selectSingleNode("subject/documentType").getText();
							suri = node.selectSingleNode("subject/refName").getText();
					//		suri=urn_processor.constructURN("id",urnbits[2],"id",urnbits[4],sname);
					//		RefName.Authority authority2 = RefName.Authority.parse(RefName.AUTHORITY_EXAMPLE2);
					//        RefName.AuthorityItem item3 = RefName.buildAuthorityItem(authority2,
					//                                                                RefName.EX_itemShortIdentifier,
					//                                                                RefName.EX_itemDisplayName);
						}
						String ouri="";
						String oser="";
						String ocsid="";
						if(node.selectSingleNode("object/uri")!=null){
							//String ourl=node.selectSingleNode("object/uri").getText();
							ocsid=node.selectSingleNode("object/csid").getText();
							//String oname=node.selectSingleNode("object/name").getText();
							oser=node.selectSingleNode("object/documentType").getText();
							//String[] urnbits = ourl.split("/");
							ouri = node.selectSingleNode("object/refName").getText();
					//		ouri=urn_processor.constructURN("id",urnbits[2],"id",urnbits[4],oname);
						}

						String relateduri = ouri;
						String relatedcsid = ocsid;
						String relatedser = oser;
						
						if(r.getSpec().hasRelationshipByPredicate(relationshipType)){
							Relationship rel = r.getSpec().getRelationshipByPredicate(relationshipType);
							Relationship newrel = rel;
							//is this subject or object
							if(thiscsid.equals(objCSID)){
								//should we invert
								if(r.getSpec().hasRelationshipInverse(rel.getID())){
									newrel = r.getSpec().getInverseRelationship(rel.getID());
									relateduri = suri;
									relatedcsid = scsid;
									relatedser = sser;
								}
							}
							

							if(newrel.getObject().equals("n")){ //array
								JSONObject subdata = new JSONObject();
								subdata.put(newrel.getChildName(),relateduri);
								if(out.has(newrel.getID())){
									out.getJSONArray(newrel.getID()).put(subdata);
								}
								else{
									JSONArray bob = new JSONArray();
									bob.put(subdata);
									out.put(newrel.getID(), bob);
								}
							}
							else{//string
								out.put(newrel.getID(), relateduri);
								if(newrel.showSiblings()){
									out.put(newrel.getSiblingChild(), relatedser+"/"+relatedcsid);
									//out.put(newrel.getSiblingChild(), relateduri);
								}
							}
						}
					}
				}
			}
			

			// get related sub records?
			for(FieldSet fs : r.getAllSubRecords("GET")){
				Record sr = fs.usesRecordId();
				//sr.getID()
				if(sr.isType("authority")){
					String getPath = url + "/" + sr.getServicesURL();
					JSONArray subout = get(storage, creds,cache,url,getPath,sr);
					if(fs instanceof Field){
						JSONObject fielddata = subout.getJSONObject(0);

						Iterator<String> rit=fielddata.keys();
						while(rit.hasNext()) {
							String key=rit.next();
							out.put(key, fielddata.get(key));
						}
					}
					else if(fs instanceof Group){
						if(subout.length()>0){
							out.put(fs.getID(), subout.getJSONObject(0));
						}
					}
					else{
						out.put(fs.getID(), subout);
					}
				}
			}
			
			//csid = urn_processor.deconstructURN(refid,false)[4];
			
			out.put(getDisplayNameKey(),name);
			out.put("csid",csid);
			out.put("refid",refid);
			out.put("shortIdentifier", shortIdentifier);
			out.put("termStatus", termStatus);
			out.put("authorityid", parentcsid);
			out.put("recordtype",r.getWebURL());
			return out;
	}
	

	/**
	 * Returns JSON containing pagenumber, pagesize, itemsinpage, totalitems and the list of items itself 
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			JSONObject out = new JSONObject();
			List<String> list=new ArrayList<String>();	
			String vocab = RefName.shortIdToPath(rootPath);
			String url="/"+r.getServicesURL()+"/"+vocab+"/items";
				

			String prefix=null;

			if(restrictions!=null){
				if(restrictions.has(getDisplayNameKey())){
					prefix=restrictions.getString(getDisplayNameKey()); 
				}
			}

			String path = getRestrictedPath(url, restrictions, r.getServicesSearchKeyword(), "", true, getDisplayNameKey() );
			

			if(r.hasSoftDeleteMethod()){
				path = softpath(path);
			}
			if(r.hasHierarchyUsed("screen")){
				path = hierarchicalpath(path);
			}
			
			ReturnedDocument data = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
			Document doc=data.getDocument();

			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve vocabularies",data.getStatus(),path);
			String[] tag_parts=r.getServicesListPath().split(",",2);
			
			JSONObject pagination = new JSONObject();
			List<Node> nodes = doc.selectNodes("/"+tag_parts[1].split("/")[0]+"/*");
			for(Node node : nodes){
				if(node.matches("/"+tag_parts[1])){
					String name=node.selectSingleNode("displayName").getText();
					String csid=node.selectSingleNode("csid").getText();
					/*
					String refName=null;
					if(node.selectSingleNode("refName")!=null){
						refName=node.selectSingleNode("refName").getText();
					}
					String shortIdentifier=null;
					if(node.selectSingleNode("shortIdentifier")!=null){
						shortIdentifier=node.selectSingleNode("shortIdentifier").getText();
					}
					*/
					if(prefix==null || name.toLowerCase().contains(prefix.toLowerCase())){
						list.add(csid);
					}
					//cache.setCached(getClass(),new String[]{"namefor",vocab,csid},name);
					//cache.setCached(getClass(),new String[]{"reffor",vocab,csid},refName);
					//cache.setCached(getClass(),new String[]{"shortId",vocab,csid},shortIdentifier);

					List<Node> fields=node.selectNodes("*");
					for(Node field : fields) {
						String json_name=view_map.get(field.getName());
						if(json_name!=null) {
							String value=field.getText();
							// XXX hack to cope with multi values		
							if(value==null || "".equals(value)) {
								List<Node> inners=field.selectNodes("*");
								for(Node n : inners) {
									value+=n.getText();
								}
							}
							setGleanedValue(cache,url+"/"+csid,json_name,value);
						}
					}
					/* this hopefully will reduce fan out - needs more testing */
					if(doc.selectSingleNode(r.getServicesFieldsPath())!=null){
						String myfields = doc.selectSingleNode(r.getServicesFieldsPath()).getText();
						String[] allfields = myfields.split("\\|");
						for(String s : allfields){
							String gleaned = getGleanedValue(cache,vocab+"/"+csid,s);
							if(gleaned==null){
								setGleanedValue(cache,url+"/"+csid,s,"");
							}
						}
					}
					
				}else{
					pagination.put(node.getName(), node.getText());
				}
			}
			
			out.put("pagination", pagination);
			out.put("listItems", list.toArray(new String[0]));
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("UTF-8 not supported!?"+e.getLocalizedMessage());
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Error parsing JSON"+e.getLocalizedMessage());
		}
	}
	
	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {			
			String vocab = RefName.shortIdToPath(filePath.split("/")[0]);
			String url = generateURL(vocab,filePath.split("/")[1],"",this.r);

			if(r.hasSoftDeleteMethod()){
				int status = 0;
				Document doc = null;
				doc=XmlJsonConversion.getXMLSoftDelete();
				String workflow = "/workflow";
				ReturnedDocument docm = conn.getXMLDocument(RequestMethod.PUT, url+workflow, doc, creds, cache);
				status = docm.getStatus();
				if(status>299 || status<200)
					throw new UnderlyingStorageException("Bad response ",status,url);
			}
			else{
				int status=conn.getNone(RequestMethod.DELETE,url,null,creds,cache);
				if(status>299)
					throw new UnderlyingStorageException("Could not retrieve vocabulary",status,url);
			}
			//cache.removeCached(getClass(),new String[]{"namefor",vocab,filePath.split("/")[1]});
			//cache.removeCached(getClass(),new String[]{"reffor",vocab,filePath.split("/")[1]});
			//cache.removeCached(getClass(),new String[]{"shortId",vocab,filePath.split("/")[1]});
			//cache.removeCached(getClass(),new String[]{"csidfor",vocab,filePath.split("/")[1]});
			//delete name and id versions from teh cache?
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}	
	}
	
	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject,Record thisr, String serviceurl)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		String vocab = RefName.shortIdToPath(filePath.split("/")[0]);
		String csid = filePath.split("/")[1];
		String savePath;
		try {
			savePath = generateURL(vocab,csid,"",thisr);
			updateJSON(root,creds,cache,jsonObject, thisr, savePath);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception "+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
	}
	
	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, JSONObject jsonObject,Record thisr, String savePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String csid = savePath.split("/")[3];
			
			Map<String,Document> body=new HashMap<String,Document>();
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);

				Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,null,null,thisr, false);
				if(temp!=null){
					body.put(record_path[0],temp);
					//log.info(temp.asXML());
				}
				
			}

			if(thisr.hasHierarchyUsed("screen")){
				ArrayList<Element> alleles = new ArrayList<Element>();
				for(Relationship rel : r.getSpec().getAllRelations()){
					Relationship newrel=rel;
					Boolean inverse = false;
					if(rel.hasInverse()){
						newrel = thisr.getSpec().getRelation(rel.getInverse());
						inverse = true;
					}
					//does this relationship apply to this record
					if(rel.hasSourceType(r.getID())){
						
						//does this record have the data in the json
						if(jsonObject.has(rel.getID())){
							if(rel.getObject().equals("1")){
								if(jsonObject.has(rel.getID()) && !jsonObject.get(rel.getID()).equals("")){
									Element bit = createRelationship(newrel,jsonObject.get(rel.getID()),csid,r.getServicesURL(),savePath, inverse);
									if(bit != null){
										alleles.add(bit);
									}
								}
							}
							else if(rel.getObject().equals("n")){
								//if()
								JSONArray temp = jsonObject.getJSONArray(rel.getID());
								for(int i=0; i<temp.length();i++){
									String argh = rel.getChildName();
									JSONObject brgh = temp.getJSONObject(i);
									if(brgh.has(argh) && !brgh.getString(argh).equals("")){
										String uri = brgh.getString(argh);
										Element bit = createRelationship(newrel,uri,csid,r.getServicesURL(),savePath, inverse);
										if(bit != null){
											alleles.add(bit);
										}
									}
								}
								
							}
						}
					}
				}
				//add relationships section
				if(!alleles.isEmpty()){
					Element[] array = alleles.toArray(new Element[0]);
					Document out=XmlJsonConversion.getXMLRelationship(array);
					body.put("relations-common-list",out);
					//log.info(out.asXML());
				}
				else{

					Document out=XmlJsonConversion.getXMLRelationship(null);
					body.put("relations-common-list",out);
					//log.info(out.asXML());
				}
				//probably should put empty array in if no data
			}
			
			
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.PUT,savePath,body,creds,cache);
			if(out.getStatus()>299){
				throw new UnderlyingStorageException("Could not update vocabulary",out.getStatus(),savePath);
			}
			//cache.setCached(getClass(),new String[]{"namefor",vocab,filePath.split("/")[1]},name);
			//cache.setCached(getClass(),new String[]{"reffor",vocab,filePath.split("/")[1]},refname);
			
			
			//subrecord update
			for(FieldSet fs : thisr.getAllSubRecords("PUT")){
				Record sr = fs.usesRecordId();
				
				//get list of existing subrecords
				JSONObject existingcsid = new JSONObject();
				JSONObject updatecsid = new JSONObject();
				JSONArray createcsid = new JSONArray();
				String getPath = savePath + "/" + sr.getServicesURL();
				String node = "/"+sr.getServicesListPath().split("/")[0]+"/*";
				JSONObject data = getListView(creds,cache,getPath,node,"/"+sr.getServicesListPath(),"csid",false, sr);
				

				String[] filepaths = (String[]) data.get("listItems");
				for(String uri : filepaths) {
					String path = uri;
					if(path!=null && path.startsWith("/"))
						path=path.substring(1);
					existingcsid.put(path,"original");
				}
				
				//how does that compare to what we need
				if(sr.isType("authority")){
					if(fs instanceof Field){
						JSONObject subdata = new JSONObject();
						//loop thr jsonObject and find the fields I need
						for(FieldSet subfs: sr.getAllFieldTopLevel("PUT")){
							String key = subfs.getID();
							if(jsonObject.has(key)){
								subdata.put(key, jsonObject.get(key));
							}
						}

						if(filepaths.length ==0){
							//create
							createcsid.put(subdata);
						}
						else{
							//update - there should only be one
							String firstcsid = filepaths[0];
							updatecsid.put(firstcsid, subdata);
							existingcsid.remove(firstcsid);
						}
					}
					else if(fs instanceof Group){//JSONObject
						//do we have a csid
						//subrecorddata.put(value);
						if(jsonObject.has(fs.getID())){
							Object subdata = jsonObject.get(fs.getID());
							if(subdata instanceof JSONObject){
								if(((JSONObject) subdata).has("_subrecordcsid")){
									String thiscsid = ((JSONObject) subdata).getString("_subrecordcsid");
									//update
									if(existingcsid.has(thiscsid)){
										updatecsid.put(thiscsid, (JSONObject) subdata);
										existingcsid.remove(thiscsid);
									}
									else{
										//something has gone wrong... best just create it from scratch
										createcsid.put(subdata);
									}
								}
								else{
									//create
									createcsid.put(subdata);
								}
							}
						}
					}
					else{//JSONArray Repeat
						//need to find if we have csid's for each one
						if(jsonObject.has(fs.getID())){
							Object subdata = jsonObject.get(fs.getID());
							if(subdata instanceof JSONArray){
								JSONArray subarray = (JSONArray)subdata;

								for(int i=0;i<subarray.length();i++) {
									JSONObject subrecord = subarray.getJSONObject(i);
									if(subrecord.has("_subrecordcsid")){
										String thiscsid = subrecord.getString("_subrecordcsid");
										//update
										if(existingcsid.has(thiscsid)){
											updatecsid.put(thiscsid, (JSONObject) subdata);
											existingcsid.remove(thiscsid);
										}
										else{
											//something has gone wrong... best just create it from scratch
											createcsid.put(subdata);
										}
									}
									else{
										//create
										createcsid.put(subdata);
									}
								}
							}
						}
					}
					

					String savePathSr = savePath + "/" + sr.getServicesURL()+"/";
					
					//do delete JSONObject existingcsid = new JSONObject();
					Iterator<String> rit=existingcsid.keys();
					while(rit.hasNext()) {
						String key=rit.next();
						deleteJSON(root,creds,cache,key,savePathSr,sr);
					}
					
					//do update JSONObject updatecsid = new JSONObject();
					Iterator<String> keys = updatecsid.keys();
					while(keys.hasNext()) {
						String key=keys.next();
						JSONObject value = updatecsid.getJSONObject(key);
						String thissave = savePathSr + key;

						updateJSON(root,creds,cache,value, sr, thissave);
						//updateJSON( root, creds, cache, key,  value, sr, savePathSr);
					}
					
					
					//do create JSONArray createcsid = new JSONArray();
					for(int i=0;i<createcsid.length();i++){
						JSONObject value = createcsid.getJSONObject(i);
						subautocreateJSON(root,creds,cache,sr,value,savePathSr);
					}
				}
			}
			
			
			//XXX dont currently update the shortID???
			//cache.setCached(getClass(),new String[]{"shortId",vocab,filePath.split("/")[1]},shortId);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception "+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON "+e.getLocalizedMessage(),e);
		}
	}
	
	private JSONArray get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab,String csid,String filePath, Record thisr) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		String url = generateURL(vocab,csid,"",this.r);
		return get(storage,creds,cache,url,filePath,thisr);
	}
	private JSONArray get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String url,String filePath, Record thisr) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		JSONArray itemarray = new JSONArray();
//get list view

		String node = "/"+thisr.getServicesListPath().split("/")[0]+"/*";
		JSONObject data = getListView(creds,cache,filePath,node,"/"+thisr.getServicesListPath(),"csid",false, thisr);
		

		String[] filepaths = (String[]) data.get("listItems");
		for(String uri : filepaths) {
			String path = uri;
			if(path!=null && path.startsWith("/"))
				path=path.substring(1);
			
			String[] parts=path.split("/");
			String recordurl = parts[0];
			String mycsid = parts[parts.length-1];
			
			try {
				JSONObject itemdata= simpleRetrieveJSON( creds, cache, filePath+"/"+mycsid,"",  thisr);
				itemdata.put("_subrecordcsid", mycsid);//add in csid so I can do update with a modicum of confidence
				itemarray.put(itemdata);
			} catch (UnimplementedException e) {
				throw new UnderlyingStorageException(e.getMessage());
			}
		}
		return itemarray;
		
	}
	

	protected JSONObject miniViewAbstract(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,JSONObject out, String servicepath, String filePath) throws UnderlyingStorageException{
		try{
			//actually use cache
			String cachelistitem = "/"+servicepath;
			if(filePath !=null){
				cachelistitem = cachelistitem+"/"+filePath;
			}

			if(!cachelistitem.startsWith("/")){
				cachelistitem = "/"+cachelistitem;
			}
			String g1=getGleanedValue(cache,cachelistitem,"refName");
			String g2=getGleanedValue(cache,cachelistitem,"shortIdentifier");
			String g3=getGleanedValue(cache,cachelistitem,"displayName");
			String g4=getGleanedValue(cache,cachelistitem,"csid");
			String g5=getGleanedValue(cache,cachelistitem,"termStatus");
			if(g1==null|| g2==null||g3==null||g4==null||g5==null){
				JSONObject cached =  get(storage, creds,cache,servicepath,filePath);
				g1 = cached.getString("refid");
				g2 = cached.getString("shortIdentifier");
				g3 = cached.getString("displayName");
				g4 = cached.getString("csid");
				g5 = cached.getString("termStatus");
				
			}
			out.put(getDisplayNameKey(), g3);
			out.put("refid", g1);
			out.put("csid", g4);
			out.put("termStatus", g5);
			//out.put("authorityid", cached.get("authorityid"));
			out.put("shortIdentifier", g2);
			out.put("recordtype",r.getWebURL());
			
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (ExistException e) {
			throw new UnderlyingStorageException("ExistException exception"+e.getLocalizedMessage(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("JSONException exception"+e.getLocalizedMessage(),e);
		}
	}

	protected Element createRelationship(Relationship rel, Object data, String csid, String subjtype, String subjuri, Boolean reverseIt) throws ExistException, UnderlyingStorageException{

		Document doc=DocumentFactory.getInstance().createDocument();
		Element subroot = doc.addElement("relation-list-item");

		Element predicate=subroot.addElement("predicate");
		predicate.addText(rel.getPredicate());
		String subjectName = "subject";
		String objectName = "object";
		if(reverseIt){
			subjectName = "object";
			objectName = "subject";
		}
		Element subject=subroot.addElement(subjectName);
		Element subjcsid = subject.addElement("csid");
		if(csid != null){
			subjcsid.addText(csid);
		}
		else{
			subjcsid.addText("${itemCSID}");
		}
		
		//find out record type from urn 
		String associatedtest = (String)data;
		Element object=subroot.addElement(objectName);

        RefName.AuthorityItem itemParsed = RefName.AuthorityItem.parse(associatedtest);
        String serviceurl = itemParsed.inAuthority.resource;
		Record myr = this.r.getSpec().getRecordByServicesUrl(serviceurl);
        if(myr.isType("authority")){

    		Element objcsid = object.addElement("refName");
    		objcsid.addText(associatedtest);
        }
        else{

			//not implemented yet - but I bet I will have to - assume /chain/intake/csid
    		Element objcsid = object.addElement("csid");
    		objcsid.addText(associatedtest);
        }
		
		//log.info(subroot.asXML());
		return subroot;
	}
}
