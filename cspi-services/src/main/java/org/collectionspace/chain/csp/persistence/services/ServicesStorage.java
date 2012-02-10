/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.collectionspace.csp.helper.persistence.SplittingStorage;
import org.json.JSONObject;

/** The direct implementation of storage; only an instance of SplittingStorage which at the moment only splits
 * into ServicesCollectionObjectStorage.
 * 
 */
public class ServicesStorage extends SplittingStorage implements Storage {
	private CSPRequestCache cache;
	private ContextualisedStorage storage;
	private CSPRequestCredentials creds;
	
	public ServicesStorage(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache) {
		this.cache=cache;
		this.storage=storage;
		this.creds=creds;
	}
	
	public String getName() { return "persistence.services"; }

	public String autocreateJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		return storage.autocreateJSON(storage,creds,cache, filePath, jsonObject);
	}

	public void createJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		storage.createJSON(storage,creds,cache, filePath, jsonObject);
	}

	public void deleteJSON(String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		storage.deleteJSON(storage,creds,cache,filePath);
	}

	public JSONObject getPathsJSON(String rootPath,JSONObject restrictions) 
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		return storage.getPathsJSON(storage,creds,cache,rootPath,restrictions);
	}
	
	public String[] getPaths(String rootPath,JSONObject restrictions) 
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		return storage.getPaths(storage,creds,cache,rootPath,restrictions);
	}

	public JSONObject retrieveJSON(String filePath, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		return storage.retrieveJSON(storage,creds,cache,filePath, restrictions);
	}

	public void updateJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		storage.updateJSON(storage,creds,cache, filePath, jsonObject);
	}

}
