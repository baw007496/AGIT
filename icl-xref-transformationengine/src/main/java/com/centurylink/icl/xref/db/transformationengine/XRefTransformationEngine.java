 package com.centurylink.icl.xref.db.transformationengine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.ParameterBuilder;
import com.centurylink.icl.builder.iclaction.RecordSetBuilder;
import com.centurylink.icl.builder.iclaction.RowBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.component.ITransformationEngine;
import com.centurylink.icl.exceptions.OSSDataNotFoundException;
import com.centurylink.icl.xref.helper.CktXRefDetails;
import com.iclnbi.iclnbiV200.SearchResourceRequest;
import com.iclnbi.iclnbiV200.SearchResourceRequestDocument;

public class XRefTransformationEngine implements ITransformationEngine {

private static final Log LOG = LogFactory.getLog(XRefTransformationEngine.class);
	
	final private ICLActionResponseDocumentBuilder iclActionResponseDocumentBuilder;
	final private ICLActionResponseBuilder iclActionResponseBuilder;
	final private ParameterBuilder parameterBuilder;
	final private RecordSetBuilder recordSetBuilder;
	final private StatusBuilder statusBuilder;
	final private RowBuilder rowBuilder;
	XrefTransformationEngine2 xref2=new XrefTransformationEngine2();

	public XRefTransformationEngine() {
		
		this.parameterBuilder = new ParameterBuilder();
		this.recordSetBuilder = new RecordSetBuilder();
		this.rowBuilder = new RowBuilder();
		this.iclActionResponseDocumentBuilder = new ICLActionResponseDocumentBuilder();
		this.iclActionResponseBuilder = new ICLActionResponseBuilder();
		this.statusBuilder = new StatusBuilder();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object transformToCim(Object input, HashMap<String, Object> ihashMap) throws Exception, OperationNotSupportedException 
	{
		
		if(ihashMap!=null && ihashMap.get("FromSidType")!=null && ((String)ihashMap.get("FromSidType")).equals("CktXRefDetails")&& input instanceof List<?>)	
		{
			
			return xref2.transformXrefInfoToCim((List<CktXRefDetails>)input);
		}
		
		else if (input instanceof Integer)
		{
			Integer result=(Integer)input;
			
			if(result!=null && result.intValue()>0)
			{
				iclActionResponseBuilder.buildICLActionResponse();
				statusBuilder.buildStatus();
				iclActionResponseBuilder.setStatus(statusBuilder.getStatus());
				iclActionResponseDocumentBuilder.buildICLActionResponseDocument(iclActionResponseBuilder.getICLActionResponse());
			       LOG.debug(iclActionResponseDocumentBuilder.getICLActionResponseDocument());
				return iclActionResponseDocumentBuilder.getICLActionResponseDocument();
			}
			
			else
			{
				throw new OperationNotSupportedException();
			}
		}
		else if (input instanceof List<?>)
		{
			
			List<Map<String, String>> columnNamesValues = (List<Map<String,String>>)input;
			
			if(columnNamesValues!=null && columnNamesValues.size()>0)
			{
				
			recordSetBuilder.buildRecordSet();
			for (Map<String, String> map : columnNamesValues) {
				
				rowBuilder.buildRow();
				Set<String> columnNames = map.keySet();
			
				
				for (String columnName : columnNames) {
					if(null!=map.get(columnName))
					{
					parameterBuilder.buildParameter(columnName, map.get(columnName));
					rowBuilder.addParameter(parameterBuilder.getParameter());
					}

				}
				
				recordSetBuilder.addRow(rowBuilder.getRow());
			}
			iclActionResponseBuilder.buildICLActionResponse();
			statusBuilder.buildStatus();
			iclActionResponseBuilder.setStatus(statusBuilder.getStatus());
		    iclActionResponseBuilder.addRecordSet(recordSetBuilder.getRecordSet());
	        iclActionResponseDocumentBuilder.buildICLActionResponseDocument(iclActionResponseBuilder.getICLActionResponse());
	       //LOG.info(iclActionResponseDocumentBuilder.getICLActionResponseDocument());
			return iclActionResponseDocumentBuilder.getICLActionResponseDocument();
			}
			else
			{
				throw new OSSDataNotFoundException();
			}
		}
		else
		{
			throw new OperationNotSupportedException();
		}
		
	}
	public Object transformFromCim(Object cimDocument, HashMap<String, Object> ihashMap) throws Exception, OperationNotSupportedException
	{

		if(ihashMap!=null && ihashMap.get("FromSidType")!=null && ((String)ihashMap.get("FromSidType")).equals("CktXRefDetails")&& cimDocument instanceof SearchResourceRequestDocument)
		{
			SearchResourceRequest searchResourceRequest=((SearchResourceRequestDocument)cimDocument).getSearchResourceRequest();
			return xref2.transformXRefFromCim(searchResourceRequest.getSearchResourceDetails());
		}
		else 
		{
			throw new OperationNotSupportedException();
		}
	}
}
