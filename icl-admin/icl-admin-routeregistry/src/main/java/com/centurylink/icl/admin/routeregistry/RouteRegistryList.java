package com.centurylink.icl.admin.routeregistry;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import com.centurylink.icl.builder.iclaction.ICLActionResponseBuilder;
import com.centurylink.icl.builder.iclaction.ICLActionResponseDocumentBuilder;
import com.centurylink.icl.builder.iclaction.ParameterBuilder;
import com.centurylink.icl.builder.iclaction.RecordSetBuilder;
import com.centurylink.icl.builder.iclaction.RowBuilder;
import com.centurylink.icl.builder.iclaction.StatusBuilder;
import com.centurylink.icl.common.Constants;
import com.centurylink.icl.common.util.ICLActionRequestDocumentReader;
import com.centurylink.icl.exceptions.ICLException;
import com.centurylink.icl.exceptions.ICLRequestValidationException;
import com.centurylink.icl.exceptions.OSSDataNotFoundException;
import com.centurylink.icl.iclaction.ICLActionRequest;
import com.centurylink.icl.iclaction.ICLActionRequestDocument;
import com.centurylink.icl.iclaction.ICLActionResponseDocument;
import com.centurylink.icl.service.ICLRequestFact;
import com.centurylink.icl.service.ICLRouteIdentity;
import com.centurylink.icl.service.ICLRouteRegistry;

public class RouteRegistryList {
	
	private static final Log LOG = LogFactory.getLog(RouteRegistryList.class);
	
	private final ServiceTracker iclRegistryServiceTracker;
	
	private RouteRegistryService routeRegistryService = null;

	private final ICLActionResponseDocumentBuilder documentBuilder = new ICLActionResponseDocumentBuilder();
	private final ICLActionResponseBuilder responseBuilder = new ICLActionResponseBuilder();
	private final StatusBuilder statusBuilder = new StatusBuilder();
	private final ParameterBuilder parameterBuilder = new ParameterBuilder();
	private final RecordSetBuilder recordSetBuilder = new RecordSetBuilder();
	private final RowBuilder rowBuilder = new RowBuilder();
	
//	private static final String SEP = "|";
	private static final String SEP = "%";
	private static final String ALL = "ALL";
	private static final String SCOPE = "Scope";
	private static final String FILTER_SOURCESYSTEM = "SourceSystemFilter";
	private static final String FILTER_ENTITY = "EntityFilter";
	private static final String FILTER_LEVEL = "LevelFilter";
	private static final String FILTER_SCOPE = "ScopeFilter";
	private static final String FILTER_ACTION = "ActionFilter";
	private static final String FILTER_MATCHSTRATEGY = "MatchStrategyFilter";
	
	private static final String INVALID_SCOPE_MESSAGE = "Invalid Scope. Valid Values are \"" + Constants.DETAILED + "\" and \"" + Constants.BASIC + "\"";
	
	public RouteRegistryList(BundleContext bundleContext) throws InvalidSyntaxException
	{
		Filter filter = bundleContext.createFilter("(&(name=iclRouteRegistryService))");
		iclRegistryServiceTracker = new ServiceTracker(bundleContext, filter, null);
		iclRegistryServiceTracker.open();
	}
	
	public void destroy()
	{
		iclRegistryServiceTracker.close();
	}
	
	public void setRouteRegistryService(RouteRegistryService routeRegistryService)
	{
		this.routeRegistryService = routeRegistryService;
	}
		
	public ICLActionResponseDocument getRegisterdRoutesList(Exchange exchange) throws Exception
	{
		Object requestDocument = exchange.getIn().getBody();
		ICLActionRequest request = ((ICLActionRequestDocument) requestDocument).getICLActionRequest();

		responseBuilder.buildICLActionResponse();
		recordSetBuilder.buildRecordSet();
		
		Map<ICLRequestFact, ICLRouteIdentity> registerdRoutes = getRegisterdRoutes();
		
		if (registerdRoutes == null || registerdRoutes.size() < 1)
		{
			// Not sure how it would be possible to get here as the route to get here would have to be registered but just incase... 
			throw new OSSDataNotFoundException();
		}
		
		String sourceSystemFilter = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), FILTER_SOURCESYSTEM);
		if (sourceSystemFilter == null)
			sourceSystemFilter = ALL;
		
		String entityFilter = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), FILTER_ENTITY);
		if (entityFilter == null)
			entityFilter = ALL;
		
		String levelFilter = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), FILTER_LEVEL);
		if (levelFilter == null)
			levelFilter = ALL;
		
		String scopeFilter = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), FILTER_SCOPE);
		if (scopeFilter == null)
			scopeFilter = ALL;

		String actionFilter = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), FILTER_ACTION);
		if (actionFilter == null)
			actionFilter = ALL;
		
		String matchStrategyFilter = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), FILTER_MATCHSTRATEGY);
		if (matchStrategyFilter == null)
			matchStrategyFilter = ALL;
		 
		String scope = ICLActionRequestDocumentReader.getParameterValue(request.getParameterList(), SCOPE);
		if (null == scope)
			scope = Constants.BASIC;
		
		if (!scope.equalsIgnoreCase(Constants.BASIC) && !scope.equalsIgnoreCase(Constants.DETAILED))
			throw new ICLRequestValidationException(INVALID_SCOPE_MESSAGE);
		
		boolean entryFound = false;
		for (ICLRequestFact requestFact:registerdRoutes.keySet())
		{
			ICLRouteIdentity routeIdentity = registerdRoutes.get(requestFact);
			if(!filterRequest(requestFact, routeIdentity, sourceSystemFilter, entityFilter, levelFilter, scopeFilter, actionFilter, matchStrategyFilter))
			{
				entryFound = true;
				if (scope.equalsIgnoreCase(Constants.DETAILED))
					buildDetailedResponse(requestFact, routeIdentity);
				else
					buildBasicResponse(requestFact, routeIdentity);
			}
		}
		
		if (!entryFound)
			throw new OSSDataNotFoundException();
		
		statusBuilder.buildStatus();
		responseBuilder.setStatus(statusBuilder.getStatus());

		if (scope.equalsIgnoreCase(Constants.DETAILED))
			responseBuilder.addRecordSet(recordSetBuilder.getRecordSet());
		
		documentBuilder.buildICLActionResponseDocument(responseBuilder.getICLActionResponse());
		
		return documentBuilder.getICLActionResponseDocument();
	}
	
	private void buildBasicResponse(ICLRequestFact requestFact, ICLRouteIdentity routeIdentity)
	{
		requestFact.hashCode();
		parameterBuilder.buildParameter(Integer.toString(requestFact.hashCode()), buildRequestFactSummary(requestFact), buildRouteIdentitySummary(routeIdentity));
		responseBuilder.addParameter(parameterBuilder.getParameter());		
	}
	
	private void buildDetailedResponse(ICLRequestFact requestFact, ICLRouteIdentity routeIdentity)
	{
		rowBuilder.buildRow();

		addParmToRow("ObjectId", Integer.toString(requestFact.hashCode()));
		addParmToRow("Action", requestFact.getAction());
		addParmToRow("Entity", requestFact.getEntity());
		addParmToRow("Level", requestFact.getLevel());
		addParmToRow("ResourceType", requestFact.getResourceType());
		addParmToRow("Scope", requestFact.getScope());
		addParmToRow("ServiceType", requestFact.getServiceType());
		addParmToRow("ServiceVersion", requestFact.getServiceVersion());
		addParmToRow("SourceSystem", requestFact.getSourceSystem());
		addParmToRow("MatchStrategy", requestFact.getMatchStrategy());
		addParmToRow("ICLServiceName", routeIdentity.getIclServiceName());
		addParmToRow("ICLRouteName", routeIdentity.getIclRouteName());
		addParmToRow("ICLAuthenticationLevel", routeIdentity.getIclAuthenticationLevel());
		
		recordSetBuilder.addRow(rowBuilder.getRow());
	}
	
	private boolean filterRequest(ICLRequestFact requestFact, ICLRouteIdentity routeIdentity, String sourceSystemFilter, String entityFilter, String levelFilter, String scopeFilter, String actionFilter, String matchStrategyFilter)
	{
		boolean response = false;
		
		if(!ALL.equalsIgnoreCase(sourceSystemFilter) && !requestFact.getSourceSystem().equalsIgnoreCase(sourceSystemFilter))
			response = true;
		if(!ALL.equalsIgnoreCase(entityFilter) && !requestFact.getEntity().equalsIgnoreCase(entityFilter))
			response = true;
		if(!ALL.equalsIgnoreCase(levelFilter) && !requestFact.getLevel().equalsIgnoreCase(levelFilter))
			response = true;
		if(!ALL.equalsIgnoreCase(scopeFilter) && !requestFact.getScope().equalsIgnoreCase(scopeFilter))
			response = true;
		if(!ALL.equalsIgnoreCase(actionFilter) && !requestFact.getAction().equalsIgnoreCase(actionFilter))
			response = true;
		if (!ALL.equalsIgnoreCase(matchStrategyFilter) && !requestFact.getMatchStrategy().equalsIgnoreCase(matchStrategyFilter))
			response = true;
		
		return response;
	}
	
	private void addParmToRow(String key, String value)
	{
		parameterBuilder.buildParameter(key, value);
		rowBuilder.addParameter(parameterBuilder.getParameter());
	}
	
	private String buildRequestFactSummary(ICLRequestFact requestFact)
	{
		StringBuilder response = new StringBuilder();
		
		response.append(requestFact.getAction())
		.append(SEP)
		.append(requestFact.getEntity())
		.append(SEP)
		.append(requestFact.getLevel())
		.append(SEP)
		.append(requestFact.getResourceType())
		.append(SEP)
		.append(requestFact.getScope())
		.append(SEP)
		.append(requestFact.getServiceType())
		.append(SEP)
		.append(requestFact.getServiceVersion())
		.append(SEP)
		.append(requestFact.getSourceSystem())
		.append(SEP)
		.append(requestFact.getMatchStrategy());
		
		return response.toString();
	}

	private String buildRouteIdentitySummary(ICLRouteIdentity routeIdentity)
	{
		StringBuilder response = new StringBuilder();
		
		response.append(routeIdentity.getIclServiceName())
		.append(SEP)
		.append(routeIdentity.getIclRouteName())
		.append(SEP)
		.append(routeIdentity.getIclAuthenticationLevel());
		
		return response.toString();
	}
	
	public Map<ICLRequestFact, ICLRouteIdentity> getRegisterdRoutes()
	{
		ICLRouteRegistry registryService = (ICLRouteRegistry)iclRegistryServiceTracker.getService();
		
		if (registryService == null)
		{
			throw new ICLException("Route Registry not Available");
		}
		
		Map<ICLRequestFact, ICLRouteIdentity> routeMap = registryService.getRouteIdentities();
		
		return routeMap;
	}
}
