package com.centurylink.icl.admin.bundles;

import org.apache.camel.Exchange;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.common.util.ICLActionRequestDocumentReader;
import com.centurylink.icl.exceptions.ICLRequestValidationException;
import com.centurylink.icl.exceptions.OSSDataNotFoundException;
import com.centurylink.icl.iclaction.ICLActionRequest;
import com.centurylink.icl.iclaction.ICLActionRequestDocument;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;

public class BundleStateChange {

	private BundleTracker bundleTracker;
	
	private final ICLActionResponseDocumentBuilder documentBuilder = new ICLActionResponseDocumentBuilder();
	private final ICLActionResponseBuilder responseBuilder = new ICLActionResponseBuilder();
	private final StatusBuilder statusBuilder = new StatusBuilder();	
	
	//private final int stateMask = Bundle.ACTIVE + Bundle.INSTALLED + Bundle.RESOLVED;
	private final int stateMask = 254;
	
	public BundleStateChange(BundleContext bundleContext)
	{
		bundleTracker = new BundleTracker(bundleContext, stateMask, null);
		bundleTracker.open();
	}
	
	public ICLActionResponseDocument changeBundleState(Exchange exchange) throws Exception
	{
		Object requestDocument = exchange.getIn().getBody();
		ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();

		Bundle[] bundles = bundleTracker.getBundles();

		responseBuilder.buildICLActionResponse();
				
		String bundleId = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "Bundle");
		if (null == bundleId || bundleId.trim().equals(""))
			throw new ICLRequestValidationException("Bundle ID is required as input");
		
		String state = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "State");
		if (null == state || state.trim().equals(""))
			throw new ICLRequestValidationException("State (START/STOP/RESTART) is required as input");
		
		state = state.toUpperCase();
		
		if (!state.equals("START") && !state.equals("STOP") && !state.equals("RESTART"))
			throw new ICLRequestValidationException("Invalid State passed, possible values are START, STOP & RESTART");
		
		boolean dataFound = false;
		
		if (null != bundles && bundles.length > 0)
		{
			for (Bundle bundle:bundles)
			{
				if (bundleId.equals(Long.toString(bundle.getBundleId())))
				{
					dataFound = true;
					if (state.equals("STOP") || state.equals("RESTART"))
						bundle.stop();
					if (state.equals("START") || state.equals("RESTART"))
						bundle.start();
				}
			}
		} else {
			throw new OSSDataNotFoundException();
		}
		
		if (!dataFound)
			throw new OSSDataNotFoundException();
		
		
		statusBuilder.buildStatus();
		responseBuilder.setStatus(statusBuilder.getStatus());
		
		documentBuilder.buildICLActionResponseDocument(responseBuilder.getICLActionResponse());
		
		return documentBuilder.getICLActionResponseDocument();
	}
		
}
