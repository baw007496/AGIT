package com.centurylink.icl.admin.logconfig;

import java.util.Dictionary;

import org.apache.camel.Exchange;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;

import com.centurylink.icl.common.util.ICLActionRequestDocumentReader;
import com.centurylink.icl.common.util.StringHelper;
import com.centurylink.icl.iclaction.ICLActionRequest;
import com.centurylink.icl.iclaction.ICLActionRequestDocument;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;

/**
 * Set the log level for a given logger
 */

public class SetLogLevel extends AbstractLogLevel{
	
	
    private String level;
    private String logger;
    private static final String LOGSET = "log:set";
  
    
    public SetLogLevel(BundleContext bundleContext)
    {
    	super(bundleContext);
    }
   

    private void doExecute() throws Exception {
        if (ROOT_LOGGER.equalsIgnoreCase(this.logger)) {
            this.logger = null;
        }
        
        // make sure both uppercase and lowercase levels are supported
        level = level.toUpperCase();
        
       
        
        if (Level.isDefault(level) && logger == null) {
           throw new Exception("Can not unset the ROOT logger");
        }

        Configuration cfg = getConfiguration();
        Dictionary props = cfg.getProperties();

        String logger = this.logger;
        String val;
        String prop;
        if (logger == null) {
            prop = ROOT_LOGGER_PREFIX;
        } else {
            prop = LOGGER_PREFIX + logger;
        }
        val = (String) props.get(prop);
        if (Level.isDefault(level)) {
            if (val != null) {
                val = val.trim();
                int idx = val.indexOf(",");
                if (idx < 0) {
                    val = null;
                } else {
                    val = val.substring(idx);
                }
            }
        } else {
            if (val == null) {
                val = level;
            } else {
                val = val.trim();
                int idx = val.indexOf(",");
                if (idx < 0) {
                    val = level;
                } else {
                    val = level + val.substring(idx);
                }
            }
        }
        if (val == null) {
            props.remove(prop);
        } else {
            props.put(prop, val);
        }
        cfg.update(props);

    }
    
    //LD
    public ICLActionResponseDocument setLogLevel(Exchange exchange) throws Exception
	{
    	try
    	{
    		Object requestDocument = exchange.getIn().getBody();
    		ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();
    		
    		
    		String packageOnWhichLogHasToSet = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "PackageName");
    		if(StringHelper.isEmpty(packageOnWhichLogHasToSet))
    		{
    			return buildResponseWithMessage("Invalid package name", true);
    		}
    		
    		String logLevel = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), "LogLevel");
    		if(StringHelper.isEmpty(logLevel))
    			//logLevel = "INFO";  
    			logLevel = "DEFAULT";    ///TODO :- Laukik :- Need to revisit this. Good idea to assign blank in case of nothing is passed as log level ??
    		
    		
    		String command = LOGSET + SPACE + logLevel +  SPACE + packageOnWhichLogHasToSet;
    		
    		String[] commandParts = command.split(SPACE);
    		
    		if(commandParts.length != 3)
    		{
    			return buildResponseWithMessage("["+ command + "] is not valid command",true);
    		}
    		
    		//finally set the log level 
    		this.level = logLevel;
    		this.logger = packageOnWhichLogHasToSet;
    		
    		try {
    			
    			Level.valueOf(level);
    			
    		} catch (IllegalArgumentException e) {
    			
    			return buildResponseWithMessage("["+ logLevel +"] is invalid value of LogLevel",true);
    		}
    		
    		doExecute();
    		
    		return buildResponseWithMessage("Log Level " + logLevel + " set for package "+ packageOnWhichLogHasToSet +" successfully", false);
    		
    	}
    	catch(Exception e)
    	{
    		return buildResponseWithMessage(e.getMessage(),true);
    	}
    	finally
    	{
    		ungetServices();
    	}

	}
    
   
}
