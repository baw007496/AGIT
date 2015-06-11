package com.centurylink.icl.admin.bundles;

import org.apache.camel.Exchange;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.ParameterBuilder;
import com.centurylink.icl.builder.iclaction.RecordSetBuilder;
import com.centurylink.icl.builder.iclaction.RowBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.common.util.ICLActionRequestDocumentReader;
import com.centurylink.icl.exceptions.ICLException;
import com.centurylink.icl.exceptions.OSSDataNotFoundException;
import com.centurylink.icl.iclaction.ICLActionRequest;
import com.centurylink.icl.iclaction.ICLActionRequestDocument;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;

public class ExportList {

	private BundleTracker bundleTracker;
	private BundleContext bundleContext;
	
	private final ICLActionResponseDocumentBuilder documentBuilder = new ICLActionResponseDocumentBuilder();
	private final ICLActionResponseBuilder responseBuilder = new ICLActionResponseBuilder();
	private final StatusBuilder statusBuilder = new StatusBuilder();
	private final ParameterBuilder parameterBuilder = new ParameterBuilder();
	private final RecordSetBuilder recordSetBuilder = new RecordSetBuilder();
	private final RowBuilder rowBuilder = new RowBuilder();
	private final RecordSetBuilder embededRecordSetBuilder = new RecordSetBuilder();
	private final RowBuilder embededRowBuilder = new RowBuilder();
	
    private final static String ALL = "ALL";
	
	//private final int stateMask = Bundle.ACTIVE + Bundle.INSTALLED + Bundle.RESOLVED;
	private final int stateMask = 254;
	
	public ExportList(BundleContext bundleContext)
	{
		this.bundleContext = bundleContext;
		bundleTracker = new BundleTracker(bundleContext, stateMask, null);
		bundleTracker.open();
	}

	public ICLActionResponseDocument getExportList(Exchange exchange) throws Exception
	{
        ServiceReference ref = bundleContext.getServiceReference(PackageAdmin.class.getName());
        if (ref == null) {
            throw new ICLException("PackageAdmin service is unavailable.");
        }
        try {
            PackageAdmin admin = (PackageAdmin) bundleContext.getService(ref);
            if (admin == null) {
            	throw new ICLException("PackageAdmin service is unavailable.");
            }

            return processExportList(exchange, admin);
        } catch (Exception e) {
        	throw e;
        } finally {
        	bundleContext.ungetService(ref);
        }
	}
	
	public ICLActionResponseDocument processExportList(Exchange exchange, PackageAdmin admin) throws Exception
	{
		Object requestDocument = exchange.getIn().getBody();
		ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();

		Bundle[] bundles = bundleTracker.getBundles();

		responseBuilder.buildICLActionResponse();
		
		recordSetBuilder.buildRecordSet();
		
		String bundleId = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "Bundle");
		if (null == bundleId || bundleId.trim().equals(""))
			bundleId = ALL;

		String packageFilter = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "PackageFilter");
		if (null == packageFilter || packageFilter.trim().equals(""))
			packageFilter = ALL;
		
		String scope = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "Scope");
		if (null == scope)
			scope = "BASIC";

		boolean dataFound = false;
		
		if (null != bundles && bundles.length > 0)
		{
			for (Bundle bundle:bundles)
			{
				if (bundleId.equalsIgnoreCase(ALL) || bundleId.equals(Long.toString(bundle.getBundleId())))
				{
					ExportedPackage[] exports = admin.getExportedPackages((Bundle) bundle);
					if (exports != null && exports.length > 0)
					{
						for (ExportedPackage export:exports)
						{
							if (packageFilter.equalsIgnoreCase("ALL") || export.getName().startsWith(packageFilter))
							{
								dataFound = true;
								
								if (scope.equalsIgnoreCase("DETAILED"))
									buildExportsDetailedResponse(bundle, export);
								else
									buildExportsBasicResponse(bundle, export);
							}
						}
					}
				}
			}
		} else {
			throw new OSSDataNotFoundException();
		}
		
		if (!dataFound)
			throw new OSSDataNotFoundException();
		
		if (scope.equalsIgnoreCase("DETAILED"))
			responseBuilder.addRecordSet(recordSetBuilder.getRecordSet());
		
		statusBuilder.buildStatus();
		responseBuilder.setStatus(statusBuilder.getStatus());
		
		documentBuilder.buildICLActionResponseDocument(responseBuilder.getICLActionResponse());
		
		return documentBuilder.getICLActionResponseDocument();
	}
	
	private void buildExportsBasicResponse(Bundle bundle, ExportedPackage export)
	{
		parameterBuilder.buildParameter(Long.toString(bundle.getBundleId()), export.getName(), export.getVersion().toString());
		responseBuilder.addParameter(parameterBuilder.getParameter());
	}

	private void buildExportsDetailedResponse(Bundle bundle, ExportedPackage export)
	{
		rowBuilder.buildRow();

		String bundleName = bundle.getSymbolicName();
		
		if (bundle.getHeaders() != null && bundle.getHeaders().get(Constants.BUNDLE_NAME) != null)
			bundleName = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME).toString();

		addParmToRow("BundleID", Long.toString(bundle.getBundleId()));
		addParmToRow("BundleName", bundleName);
		addParmToRow("Package", export.getName());
		addParmToRow("Version", export.getVersion().toString());
		
		Bundle[] imports = export.getImportingBundles();
		
		if (imports != null && imports.length > 0)
		{
            embededRecordSetBuilder.buildRecordSet("ImportedBy");
            embededRowBuilder.buildRow();

            for (Bundle iBundle:imports)
            {
	            String importedBundleName = iBundle.getSymbolicName();
	    		
	    		if (iBundle.getHeaders() != null && iBundle.getHeaders().get(Constants.BUNDLE_NAME) != null)
	    			importedBundleName = (String) iBundle.getHeaders().get(Constants.BUNDLE_NAME).toString();
	    		
	    		//importedBundleName += " (" + Long.toString(iBundle.getBundleId()) + ")";
	    		
	    		addParmToRow(Long.toString(iBundle.getBundleId()), importedBundleName, null, embededRowBuilder);
            }
            embededRecordSetBuilder.addRow(embededRowBuilder.getRow());
            rowBuilder.addRecordSet(embededRecordSetBuilder.getRecordSet());
		}

		recordSetBuilder.addRow(rowBuilder.getRow());
	}
	
	private void addParmToRow(String objectId, String key, String value)
	{
		addParmToRow(objectId, key, value, this.rowBuilder);
	}
	
	private void addParmToRow(String key, String value)
	{
		addParmToRow(null, key, value, this.rowBuilder);
	}
	
	private void addParmToRow(String key, String value, RowBuilder rowBuilder)
	{
		addParmToRow(null, key, value, rowBuilder);
	}

	private void addParmToRow(String objectId, String key, String value, RowBuilder rowBuilder)
	{
		parameterBuilder.buildParameter(objectId, key, value);
		rowBuilder.addParameter(parameterBuilder.getParameter());
	}
	
}
