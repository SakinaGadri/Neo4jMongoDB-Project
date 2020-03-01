package com.csc301.songmicroservice;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import okhttp3.RequestBody;
import org.springframework.http.HttpStatus;

public class Utils {

  public static RequestBody emptyRequestBody = RequestBody.create(null, "");

  // Used to determine path that was called from within each REST route, you don't need to modify this
  public static String getUrl(HttpServletRequest req) {
    String requestUrl = req.getRequestURL().toString();
    String queryString = req.getQueryString();

    if (queryString != null) {
      requestUrl += "?" + queryString;
    }
    return requestUrl;
  }

  // Sets the response status and data for a response from the server. You will not always be able to use this function
  public static Map<String, Object> setResponseStatus(Map<String, Object> response,
      DbQueryExecResult dbQueryExecResult, Object data) {
    System.out.println("dbqueryresult " + dbQueryExecResult);
    switch (dbQueryExecResult) {
      case QUERY_OK:
        System.out.println("returning 200");
        response.put("status", HttpStatus.OK);
        if (data != null) {
          response.put("data", data);
        }
        break;
      case QUERY_ERROR_NOT_FOUND:
        System.out.println("returning 404");
        response.put("status", HttpStatus.NOT_FOUND);
        break;
      case QUERY_ERROR_GENERIC:
        System.out.println("returning 500");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
        break;
    }

    return response;
  }
}