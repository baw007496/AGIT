package com.centurylink.icl.admin.bundles;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.centurylink.icl.admin.bundles.util.manifest.Attribute;
import com.centurylink.icl.admin.bundles.util.manifest.Clause;
import com.centurylink.icl.admin.bundles.util.manifest.Parser;
import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.ParameterBuilder;
import com.centurylink.icl.builder.iclaction.RecordSetBuilder;
import com.centurylink.icl.builder.iclaction.RowBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.common.util.ICLActionRequestDocumentReader;
import com.centurylink.icl.exceptions.ICLRequestValidationException;
import com.centurylink.icl.exceptions.OSSDataNotFoundException;
import com.centurylink.icl.iclaction.ICLActionRequest;
import com.centurylink.icl.iclaction.ICLActionRequestDocument;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;

public class HeadersDetail {

	private BundleTracker bundleTracker;
	
	private final ICLActionResponseDocumentBuilder documentBuilder = new ICLActionResponseDocumentBuilder();
	private final ICLActionResponseBuilder responseBuilder = new ICLActionResponseBuilder();
	private final StatusBuilder statusBuilder = new StatusBuilder();
	private final ParameterBuilder parameterBuilder = new ParameterBuilder();
	private final RecordSetBuilder recordSetBuilder = new RecordSetBuilder();
	private final RowBuilder rowBuilder = new RowBuilder();
	private final RecordSetBuilder embededRecordSetBuilder = new RecordSetBuilder();
	private final RowBuilder embededRowBuilder = new RowBuilder();
	
    protected final static String BUNDLE_PREFIX = "Bundle-";
    protected final static String PACKAGE_SUFFFIX = "-Package";
    protected final static String SERVICE_SUFFIX = "-Service";
    protected final static String IMPORT_PACKAGES_ATTRIB = "Import-Package";
    protected final static String REQUIRE_BUNDLE_ATTRIB = "Require-Bundle";
	
	//private final int stateMask = Bundle.ACTIVE + Bundle.INSTALLED + Bundle.RESOLVED;
	private final int stateMask = 254;
	
	public HeadersDetail(BundleContext bundleContext)
	{
		bundleTracker = new BundleTracker(bundleContext, stateMask, null);
		bundleTracker.open();
	}
	
	public ICLActionResponseDocument getHeadersDetail(Exchange exchange) throws Exception
	{
		Object requestDocument = exchange.getIn().getBody();
		ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();

		Bundle[] bundles = bundleTracker.getBundles();

		responseBuilder.buildICLActionResponse();
		
		recordSetBuilder.buildRecordSet();
		
		String bundleId = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "Bundle");
		if (null == bundleId || bundleId.trim().equals(""))
			throw new ICLRequestValidationException();
		
		boolean dataFound = false;
		
		if (null != bundles && bundles.length > 0)
		{
			for (Bundle bundle:bundles)
			{
				if (bundleId.equals(Long.toString(bundle.getBundleId())))
				{
					dataFound = true;
					buildHeadersResponse(bundle);
				}
			}
		} else {
			throw new OSSDataNotFoundException();
		}
		
		if (!dataFound)
			throw new OSSDataNotFoundException();
		
		responseBuilder.addRecordSet(recordSetBuilder.getRecordSet());
		
		statusBuilder.buildStatus();
		responseBuilder.setStatus(statusBuilder.getStatus());
		
		documentBuilder.buildICLActionResponseDocument(responseBuilder.getICLActionResponse());
		
		return documentBuilder.getICLActionResponseDocument();
	}
	
	private void buildHeadersResponse(Bundle bundle)
	{
		rowBuilder.buildRow();
        Map<String, Object> otherAttribs = new HashMap<String, Object>();
        Map<String, Object> bundleAttribs = new HashMap<String, Object>();
        Map<String, Object> serviceAttribs = new HashMap<String, Object>();
        Map<String, Object> packagesAttribs = new HashMap<String, Object>();
        Dictionary dict = bundle.getHeaders();
        Enumeration keys = dict.keys();

        // do an initial loop and separate the attributes in different groups
        while (keys.hasMoreElements()) {
            String k = (String) keys.nextElement();
            Object v = dict.get(k);
            if (k.startsWith(BUNDLE_PREFIX)) {
                // starts with Bundle-xxx
                bundleAttribs.put(k, v);
            } else if (k.endsWith(SERVICE_SUFFIX)) {
                // ends with xxx-Service
                serviceAttribs.put(k, v);
            } else if (k.endsWith(PACKAGE_SUFFFIX)) {
                // ends with xxx-Package
                packagesAttribs.put(k, v);
            } else if (k.endsWith(REQUIRE_BUNDLE_ATTRIB)) {
                // require bundle statement
                packagesAttribs.put(k, v);
            } else {
                // the remaining attribs
                otherAttribs.put(k, v);
            }
        }
        
        for (String key:otherAttribs.keySet())
        {
        	Object entry = otherAttribs.get(key);
        	addParmToRow(key, entry.toString());
        }
        for (String key:bundleAttribs.keySet())
        {
        	Object entry = bundleAttribs.get(key);
        	addParmToRow(key, entry.toString());
        }
        for (String key:serviceAttribs.keySet())
        {
            embededRecordSetBuilder.buildRecordSet(key);
            embededRowBuilder.buildRow();
        	Object entry = serviceAttribs.get(key);
        	Clause[] clauses = Parser.parseHeader(entry.toString());
        	for (Clause clause:clauses)
        	{
        		String attr = "";
        		boolean firstTime = true;
        		for (Attribute attrib:clause.getAttributes())
        		{
        			if (firstTime)
        				firstTime = false;
        			else
        				attr += ",";
        			attr += attrib.getName() + "=" + attrib.getValue();
        		}
        		addParmToRow(clause.getName(), attr, embededRowBuilder);
        	}
            embededRecordSetBuilder.addRow(embededRowBuilder.getRow());
            rowBuilder.addRecordSet(embededRecordSetBuilder.getRecordSet());
        }
        for (String key:packagesAttribs.keySet())
        {
            embededRecordSetBuilder.buildRecordSet(key);
            embededRowBuilder.buildRow();
        	Object entry = packagesAttribs.get(key);
        	Clause[] clauses = Parser.parseHeader(entry.toString());
        	for (Clause clause:clauses)
        	{
        		addParmToRow(clause.getName(), clause.getAttribute("version"), embededRowBuilder);
        	}
            embededRecordSetBuilder.addRow(embededRowBuilder.getRow());
            rowBuilder.addRecordSet(embededRecordSetBuilder.getRecordSet());
        }        
		
		recordSetBuilder.addRow(rowBuilder.getRow());
	}
	
	public String getStateValue(int state)
	{
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		default:
			return "UNKNOWN";
		}
	}
	
	private void addParmToRow(String key, String value)
	{
		addParmToRow(key, value, this.rowBuilder);
	}
	
	private void addParmToRow(String key, String value, RowBuilder rowBuilder)
	{
		parameterBuilder.buildParameter(key, value);
		rowBuilder.addParameter(parameterBuilder.getParameter());
	}
	
}
