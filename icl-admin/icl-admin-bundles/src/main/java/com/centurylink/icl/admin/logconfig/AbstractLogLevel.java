package com.centurylink.icl.admin.logconfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;


public abstract class AbstractLogLevel {

		///TODO - laukik - Need to check if log level is not getting unset
	
	 private List<ServiceReference> usedReferences;
	 private BundleContext bundleContext;
	 private final ICLActionResponseDocumentBuilder documentBuilder = new ICLActionResponseDocumentBuilder();
	 private final ICLActionResponseBuilder responseBuilder = new ICLActionResponseBuilder();
	 private final StatusBuilder statusBuilder = new StatusBuilder();
	 protected static final String CONFIGURATION_PID  = "org.ops4j.pax.logging";
	 protected static final String ROOT_LOGGER_PREFIX = "log4j.rootLogger";
	 protected static final String LOGGER_PREFIX      = "log4j.logger.";
	 protected static final String ROOT_LOGGER        = "ROOT";
	 protected static final String SPACE = " ";
	
	public AbstractLogLevel(BundleContext bundleContext)
	{
		this.bundleContext  = bundleContext;
	}
	protected enum Level {

        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        DEFAULT;
        
        /**
         * Convert the list of values into a String array
         * 
         * @return all the values as a String array
         */
        public static String[] strings() {
            String[] values = new String[values().length];
            for (int i = 0 ; i < values.length ; i++) {
                values[i] = values()[i].name();
            }
            return values;
        }
        /**
         * Check if the string value represents the default level
         * 
         * @param level the level value
         * @return <code>true</code> if the value represents the {@link #DEFAULT} level
         */
        public static boolean isDefault(String level) {
            return valueOf(level).equals(DEFAULT);
        }
	}
	
	
	protected void ungetServices() {
	        if (usedReferences != null) {
	            for (ServiceReference ref : usedReferences) {
	                this.bundleContext.ungetService(ref);
	            }
	        }
	  }
	
	 protected Configuration getConfiguration() throws IOException {
	        Configuration cfg = getConfigAdmin().getConfiguration(CONFIGURATION_PID, null);
	        return cfg;
	 }
	 
	 protected ConfigurationAdmin getConfigAdmin() {
	        ServiceReference ref = this.bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
	        return getService(ConfigurationAdmin.class, ref);
	 }
	 
	 private <T> T getService(Class<T> clazz, ServiceReference reference) {
	        T t = (T) this.bundleContext.getService(reference);
	        if (t != null) {
	            if (usedReferences == null) {
	                usedReferences = new ArrayList<ServiceReference>();
	            }
	            usedReferences.add(reference);
	        }
	        return t;
	 }
	 
	 
	 protected ICLActionResponseDocument buildResponseWithMessage(String text,boolean isError)
	    {
	    	
	    	if(isError)
	    	   	statusBuilder.buildStatus("1947", text, "FAIL");
	    	 else
	    		statusBuilder.buildStatus("0", text, "SUCCESS");
	    	
	    	responseBuilder.buildICLActionResponse();
	    	responseBuilder.setStatus(statusBuilder.getStatus());
	    	
	    	documentBuilder.buildICLActionResponseDocument(responseBuilder.getICLActionResponse());
	    	
	    	return documentBuilder.getICLActionResponseDocument();
	    }
}
