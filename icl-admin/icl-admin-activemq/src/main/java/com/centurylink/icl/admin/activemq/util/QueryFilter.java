package com.centurylink.icl.admin.activemq.util;

import java.util.List;
import java.util.Map;

public interface QueryFilter {
    
    String QUERY_DELIMETER = ",";

    /**
     * Interface for querying
     * @param queryStr - the query string
     * @return collection of objects that satisfies the query
     * @throws Exception
     */
    List query(String queryStr) throws Exception;

    /**
     * Interface for querying
     * @param queries - list of individual queries
     * @return collection of objects that satisfies the query
     * @throws Exception
     */
    List query(List queries) throws Exception;
}
