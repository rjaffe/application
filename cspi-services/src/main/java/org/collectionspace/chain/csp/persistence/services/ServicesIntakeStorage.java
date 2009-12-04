package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;

import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.csp.api.persistence.Storage;
import org.dom4j.DocumentException;

public class ServicesIntakeStorage extends GenericRecordStorage implements Storage {
	
	public ServicesIntakeStorage(ServicesConnection conn) throws InvalidJXJException, DocumentException, IOException {
		super(conn,"intake.jxj","intake","intakes","intakes_common","intakes-common-list/intake-list-item");
	}
}
