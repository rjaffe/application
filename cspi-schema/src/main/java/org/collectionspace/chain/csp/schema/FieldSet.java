/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.List;

public interface FieldSet {
	public String getID();
	public String[] getIDPath();

	public SchemaUtils getUtils();
	public FieldParent getParent();
	public void setParent(FieldParent fp);
	public Record getRecord();
	public String getServicesTag();
	public String getServicesUrl();
	public String[] getServicesParent();
	public boolean hasServicesParent();
	public String getSection();
	public Boolean isInServices();
	public boolean hasFieldPerm(String k);
	public String[] getAllFieldOperations();

	public void config_finish(Spec spec);
	
	//subrecords
	public String getSelectorAffix();
	public String getUISpecPrefix();
	public Boolean getUISpecInherit();
	public String getUILabelSelector();
	public String getLabelAffix();
	public String getContainerSelector();
	public String getDecoratorSelector();
	public String getPreContainerSelector();
	public String getTitleSelector();
	public String getPreTitleSelector();
	public String getSelector();
	public String getPreSelector();
	public String getLabel();
	//used in generateGroupField in uispec for elpaths
	public String getPrimaryKey();
	public Boolean usesRecord();
	public String usesRecordValidator();
	public Record usesRecordId();
	
	//
	public Boolean hasAutocompleteInstance();
	public Boolean hasMergeData();
	public List<String> getAllMerge();
	public boolean isExpander();
	public boolean isConditionExpander();
	public boolean isRepeatSubRecord();
	public boolean isReadOnly();
	public void setRepeatSubRecord(Boolean var);
	public String getUIType();
	public String getUIFunc();
	public String getWithCSID();
	public String getSearchType();
}
