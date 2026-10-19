package com.jichi.a2a.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

// JSON-RPC 响应
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcResponse(
        String jsonrpc,
        Object result,
        JsonRpcError error,
        String id
) {
    public static JsonRpcResponse success(Object result, String id) {
        return new JsonRpcResponse("2.0", result, null, id);
    }

    public static JsonRpcResponse error(int code, String message, String id) {
        return new JsonRpcResponse("2.0", null, new JsonRpcError(code, message), id);
    }
}