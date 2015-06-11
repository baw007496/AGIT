package com.centurylink.icl.admin.logconfig;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.apache.camel.Exchange;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.centurylink.icl.common.util.ICLActionRequestDocumentReader;
import com.centurylink.icl.iclaction.ICLActionRequest;
import com.centurylink.icl.iclaction.ICLActionRequestDocument;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;

public class GetLogLevel extends AbstractLogLevel{
	
	private String level;
	private String logger;
	private static final String ALL_LOGGER = "ALL";
	private String levelToBeDisplayed = "";
	
	
	public GetLogLevel(BundleContext bundleContext) {
		super(bundleContext);
	}

    private  Object doExecute() throws Exception {
        ConfigurationAdmin cfgAdmin = getConfigAdmin();
        Configuration cfg = cfgAdmin.getConfiguration(CONFIGURATION_PID, null);
        Dictionary props = cfg.getProperties();

        if (ROOT_LOGGER.equalsIgnoreCase(this.logger)) {
            this.logger = null;
        }
        if (ALL_LOGGER.equalsIgnoreCase(logger)) {
            String root = getLevel((String) props.get(ROOT_LOGGER_PREFIX));
            Map<String, String> loggers = new TreeMap<String, String>();
            for (Enumeration e = props.keys(); e.hasMoreElements();) {
                String prop = (String) e.nextElement();
                if (prop.startsWith(LOGGER_PREFIX)) {
                    String val = getLevel((String) props.get(prop));
                    loggers.put(prop.substring(LOGGER_PREFIX.length()), val);
                }
            }
            System.out.println("ROOT: " + root);
            for (String logger : loggers.keySet()) {
                  	levelToBeDisplayed = logger + ": " + loggers.get(logger);
            }
        } else {
            String logger = this.logger;
            String val;
            for (;;) {
                String prop;
                if (logger == null) {
                    prop = ROOT_LOGGER_PREFIX;
                } else {
                    prop = LOGGER_PREFIX + logger;
                }
                val = (String) props.get(prop);
                val = getLevel(val);
                if (val != null || logger == null) {
                    break;
                }
                int idx = logger.lastIndexOf('.');
                if (idx < 0) {
                    logger = null;
                } else {
                    logger = logger.substring(0, idx);
                }
            }
            String st = "Level: " + val;
            if (logger != this.logger) {
                st += " (inherited from " + (logger != null ? logger : "ROOT") + ")";
            }
            levelToBeDisplayed = st;
        }
        return null;
    }

    private String getLevel(String prop) {
        if (prop == null) {
            return null;
        } else {
            String val = prop.trim();
            int idx = val.indexOf(",");
            if (idx == 0) {
                val = null;
            } else if (idx > 0) {
                val = val.substring(0, idx);
            }
            return val;
        }
    }

   
    public ICLActionResponseDocument getLogLevel(Exchange exchange) throws Exception
    {
    	try
    	{
    		Object requestDocument = exchange.getIn().getBody();
    		ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();
    		
    		this.logger = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "PackageName");
    		
    		//no need to worry when this.logger is StringHelper.empty()...it will inherit from root  		
    		doExecute();
    		return buildResponseWithMessage(levelToBeDisplayed, false);
    	}
    	catch(Exception e)
    	{
    		return buildResponseWithMessage("Unable to get the log level for specified package", true);
    	}
    	finally
    	{
    		ungetServices();
    	}
    }

}
