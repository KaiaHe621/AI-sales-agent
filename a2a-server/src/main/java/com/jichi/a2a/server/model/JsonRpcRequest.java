package com.jichi.a2a.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

// JSON-RPC 请求
public record JsonRpcRequest(
        String jsonrpc,
        String method,
        Map<String, Object> params,
        String id
) {
}






