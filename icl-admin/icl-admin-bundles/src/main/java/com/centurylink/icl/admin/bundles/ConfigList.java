package com.centurylink.icl.admin.bundles;

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.camel.Exchange;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.ParameterBuilder;
import com.centurylink.icl.builder.iclaction.RecordSetBuilder;
import com.centurylink.icl.builder.iclaction.RowBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.common.Constants;
import com.centurylink.icl.common.util.ICLActionRequestDocumentReader;
import com.centurylink.icl.common.util.StringHelper;
import com.centurylink.icl.exceptions.ICLException;
import com.centurylink.icl.exceptions.ICLRequestValidationException;
import com.centurylink.icl.iclaction.ICLActionRequest;
import com.centurylink.icl.iclaction.ICLActionRequestDocument;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;

public class ConfigList {
	
	private BundleContext bundleContext;
	private String query = null;
	
	private final ICLActionResponseDocumentBuilder documentBuilder = new ICLActionResponseDocumentBuilder();
	private final ICLActionResponseBuilder responseBuilder = new ICLActionResponseBuilder();
	private final StatusBuilder statusBuilder = new StatusBuilder();
	private final ParameterBuilder parameterBuilder = new ParameterBuilder();
	private final RecordSetBuilder recordSetBuilder = new RecordSetBuilder();
	private final RowBuilder rowBuilder = new RowBuilder();
	private final RecordSetBuilder embededRecordSetBuilder = new RecordSetBuilder();
	private final RowBuilder embededRowBuilder = new RowBuilder();
	
	private final String DEFAULT_PID = "com.centurylink.icl.";
	private final String ALL = "ALL";

	private static final String INVALID_SCOPE_MESSAGE = "Invalid Scope. Valid Values are \"" + Constants.DETAILED + "\" and \"" + Constants.BASIC + "\"";
	
	public ConfigList(BundleContext bundleContext)
	{
		this.bundleContext = bundleContext;
	}
	
	public ICLActionResponseDocument getConfigList(Exchange exchange) throws Exception
	{
        ServiceReference ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if (ref == null) {
            throw new ICLException("ConfigurationAdmin service is unavailable.");
        }
        try {
            ConfigurationAdmin admin = (ConfigurationAdmin) bundleContext.getService(ref);
            if (admin == null) {
                throw new ICLException("ConfigAdmin service is unavailable.");
            } else {
            	Configuration[] configs = admin.listConfigurations(query);
            	return processConfigList(exchange, configs);
            }
        } finally {
        	bundleContext.ungetService(ref);
        }
	}
	
	private ICLActionResponseDocument processConfigList(Exchange exchange, Configuration[] configs)
	{
		Object requestDocument = exchange.getIn().getBody();
		ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();
		
		String scope = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "Scope");
		if (null == scope)
			scope = "BASIC";

		if (!scope.equalsIgnoreCase(Constants.BASIC) && !scope.equalsIgnoreCase(Constants.DETAILED))
			throw new ICLRequestValidationException(INVALID_SCOPE_MESSAGE);

		String pidFilter = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "PidFilter");
		if (null == pidFilter || pidFilter.trim().equals(""))
			pidFilter = DEFAULT_PID;

		responseBuilder.buildICLActionResponse();
		
		if (scope.equalsIgnoreCase("BASIC"))
		{
	        for (Configuration config : configs) {
	        	parameterBuilder.buildParameter(null, null, config.getPid());
	        	responseBuilder.addParameter(parameterBuilder.getParameter());
	        }
		} else {
			recordSetBuilder.buildRecordSet();
			for (Configuration config : configs) {
				if (pidFilter.equalsIgnoreCase(ALL) || config.getPid().toUpperCase().startsWith(pidFilter.toUpperCase()))
				{
					rowBuilder.buildRow();
					parameterBuilder.buildParameter(null, "Pid", config.getPid());
					rowBuilder.addParameter(parameterBuilder.getParameter());
					
	                if (!StringHelper.isEmpty(config.getFactoryPid())) {
	    				parameterBuilder.buildParameter(null, "FactoryPid", config.getFactoryPid());
	    				rowBuilder.addParameter(parameterBuilder.getParameter());
	                }
	                if (!StringHelper.isEmpty(config.getBundleLocation()))
	                {
	    				parameterBuilder.buildParameter(null, "BundleLocation", config.getBundleLocation());
	    				rowBuilder.addParameter(parameterBuilder.getParameter());
	                }
	                
	                if (config.getProperties() != null) {
	                	embededRecordSetBuilder.buildRecordSet("Properties");
	    				embededRowBuilder.buildRow();
	                    Dictionary props = config.getProperties();
	                    for (Enumeration e = props.keys(); e.hasMoreElements();) {
	                    	Object key = e.nextElement();
	        				parameterBuilder.buildParameter(null, key.toString(), props.get(key).toString());
	        				embededRowBuilder.addParameter(parameterBuilder.getParameter());
	                    }
	                    embededRecordSetBuilder.addRow(embededRowBuilder.getRow());
	                    rowBuilder.addRecordSet(embededRecordSetBuilder.getRecordSet());
	                }
	                recordSetBuilder.addRow(rowBuilder.getRow());
				}
			}
            responseBuilder.addRecordSet(recordSetBuilder.getRecordSet());
		}
        
		statusBuilder.buildStatus();
		responseBuilder.setStatus(statusBuilder.getStatus());
		
		documentBuilder.buildICLActionResponseDocument(responseBuilder.getICLActionResponse());
		
		return documentBuilder.getICLActionResponseDocument();
	}
}
