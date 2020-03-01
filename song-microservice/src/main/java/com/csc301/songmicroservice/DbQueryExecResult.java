package com.csc301.songmicroservice;

public enum DbQueryExecResult {
  QUERY_OK, // 200
  QUERY_ERROR_NOT_FOUND, // 404
  QUERY_ERROR_GENERIC // 500
}
