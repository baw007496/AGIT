package com.centurylink.icl.admin.bundles;

import java.util.List;

import org.apache.camel.Exchange;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.ParameterBuilder;
import com.centurylink.icl.builder.iclaction.RecordSetBuilder;
import com.centurylink.icl.builder.iclaction.RowBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.common.util.ICLActionRequestDocumentReader;
import com.centurylink.icl.common.util.StringHelper;
import com.centurylink.icl.exceptions.OSSDataNotFoundException;
import com.centurylink.icl.iclaction.ICLActionRequest;
import com.centurylink.icl.iclaction.ICLActionRequestDocument;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;

import com.centurylink.icl.admin.bundles.listener.BundleStateListener;

public class BundleList {

	private BundleTracker bundleTracker;
	
	private final ICLActionResponseDocumentBuilder documentBuilder = new ICLActionResponseDocumentBuilder();
	private final ICLActionResponseBuilder responseBuilder = new ICLActionResponseBuilder();
	private final StatusBuilder statusBuilder = new StatusBuilder();
	private final ParameterBuilder parameterBuilder = new ParameterBuilder();
	private final RecordSetBuilder recordSetBuilder = new RecordSetBuilder();
	private final RowBuilder rowBuilder = new RowBuilder();
	
	
	//private final int stateMask = Bundle.ACTIVE + Bundle.INSTALLED + Bundle.RESOLVED;
	private final int stateMask = 254;

	private List<BundleStateListener.Factory> bundleStateListenerFactories;

    public void setBundleStateListenerFactories(List<BundleStateListener.Factory> bundleStateListenerFactories) {
        this.bundleStateListenerFactories = bundleStateListenerFactories;
    }
	
	public BundleList(BundleContext bundleContext)
	{
		bundleTracker = new BundleTracker(bundleContext, stateMask, null);
		bundleTracker.open();
	}
	
	public ICLActionResponseDocument getBundleList(Exchange exchange) throws Exception
	{
		Object requestDocument = exchange.getIn().getBody();
		ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();

		Bundle[] bundles = bundleTracker.getBundles();

		responseBuilder.buildICLActionResponse();
		
		recordSetBuilder.buildRecordSet();
		
		String filter = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "SymbolicNameFilter");
		if (null == filter)
			filter = "com.centurylink";
		
		String scope = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "Scope");
		if (null == scope)
			scope = "BASIC";

		boolean dataFound = false;
		
		if (null != bundles && bundles.length > 0)
		{
			for (Bundle bundle:bundles)
			{
				if (filter.equalsIgnoreCase("ALL") || bundle.getSymbolicName().toUpperCase().startsWith(filter.toUpperCase()))
				{
					dataFound = true;
					if (scope.equals("DETAILED"))
						buildDetailedResponse(bundle);
					else
						buildBasicResponse(bundle);
				}
			}
		} else {
			throw new OSSDataNotFoundException();
		}
		
		if (!dataFound)
			throw new OSSDataNotFoundException();
		
		if (scope.equals("DETAILED"))
			responseBuilder.addRecordSet(recordSetBuilder.getRecordSet());
		
		statusBuilder.buildStatus();
		responseBuilder.setStatus(statusBuilder.getStatus());
		
		documentBuilder.buildICLActionResponseDocument(responseBuilder.getICLActionResponse());
		
		return documentBuilder.getICLActionResponseDocument();
	}
	
	private void buildBasicResponse(Bundle bundle)
	{
		String bundleName = bundle.getSymbolicName();
		
		if (bundle.getHeaders() != null && bundle.getHeaders().get(Constants.BUNDLE_NAME) != null)
			bundleName = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME).toString();
		
		String bundleVersion = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
		
		if (bundleVersion != null)
			bundleName += " (" + bundleVersion + ")";
		
		parameterBuilder.buildParameter(Long.toString(bundle.getBundleId()), bundleName, getStateValue(bundle.getState()));
		responseBuilder.addParameter(parameterBuilder.getParameter());		
	}
	
	private void buildDetailedResponse(Bundle bundle)
	{
		rowBuilder.buildRow();
		
		String bundleName = null;
		String bundleVersion = null;
		String updateLocation = null;
		
		if (bundle.getHeaders() != null)
		{
			if (bundle.getHeaders().get(Constants.BUNDLE_NAME) != null)
				bundleName = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME).toString();
			if (bundle.getHeaders().get(Constants.BUNDLE_NAME) != null)
				bundleVersion = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
			if (bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION) != null)
				updateLocation = (String) bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
		}
		
		parameterBuilder.buildParameter("ID", Long.toString(bundle.getBundleId()));
		rowBuilder.addParameter(parameterBuilder.getParameter());
		parameterBuilder.buildParameter("SymbolicName", bundle.getSymbolicName());
		rowBuilder.addParameter(parameterBuilder.getParameter());
		if (null != bundleName)
		{
			parameterBuilder.buildParameter("Name", bundleName);
			rowBuilder.addParameter(parameterBuilder.getParameter());
		}
		if (null != bundleVersion)
		{
			parameterBuilder.buildParameter("Version", bundleVersion);
			rowBuilder.addParameter(parameterBuilder.getParameter());
		}
		parameterBuilder.buildParameter("State", getStateValue(bundle.getState()));
		rowBuilder.addParameter(parameterBuilder.getParameter());
		
		parameterBuilder.buildParameter("Location", bundle.getLocation());
		rowBuilder.addParameter(parameterBuilder.getParameter());
		
		if (null != updateLocation)
		{
			parameterBuilder.buildParameter("UpdateLocation", updateLocation);
			rowBuilder.addParameter(parameterBuilder.getParameter());
		}

		for (BundleStateListener.Factory factory : bundleStateListenerFactories) {
            BundleStateListener listener = factory.getListener();
            if (listener != null) {
            	if (!StringHelper.isEmpty(listener.getState(bundle)))
            	{
	    			parameterBuilder.buildParameter(listener.getName(), listener.getState(bundle));
	    			rowBuilder.addParameter(parameterBuilder.getParameter());
            	}
            }
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
	
}
