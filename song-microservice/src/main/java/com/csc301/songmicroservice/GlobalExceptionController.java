package com.csc301.songmicroservice;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionController {

  @ExceptionHandler(NoHandlerFoundException.class)
  public @ResponseBody
  Map<String, Object> handleError404(HttpServletRequest request, Exception e) {
    DbQueryStatus dbQueryStatus = new DbQueryStatus("No Handler Found for Path",
        DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("POST %s", Utils.getUrl(request)));
    response.put("message", dbQueryStatus.getMessage());
    return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
  }
}