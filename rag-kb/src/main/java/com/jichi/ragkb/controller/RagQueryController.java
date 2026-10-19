package com.jichi.ragkb.controller;

import com.jichi.ragkb.dto.ApiResponse;
import com.jichi.ragkb.dto.RagQueryRequest;
import com.jichi.ragkb.dto.RagResponse;
import com.jichi.ragkb.service.FullRagPipeline;
import com.jichi.ragkb.service.RagQueryService;
import com.jichi.ragkb.service.RagQueryServiceV2;
import com.jichi.ragkb.service.RagQueryServiceV3;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagQueryController {

    private final RagQueryService ragQueryService;

    private final RagQueryServiceV2 ragQueryServiceV2;

    private final RagQueryServiceV3 ragQueryServiceV3;

    private final FullRagPipeline fullRagPipeline;

//    @PostMapping("/query")
//    public ApiResponse<String> query(@RequestBody RagQueryRequest req) {
//        return ApiResponse.ok(ragQueryService.query(req.getQuestion(), req.getKbIds()));
//    }

//    @PostMapping("/query")
//    public ApiResponse<String> query(@RequestBody RagQueryRequest req) {
//        return ApiResponse.ok(ragQueryServiceV2.query(req.getQuestion(), req.getKbIds()));
//    }

//    @PostMapping("/query")
//    public ApiResponse<String> query(@RequestBody RagQueryRequest req) {
//        return ApiResponse.ok(ragQueryServiceV3.query(req.getQuestion(), req.getKbIds()));
//    }

    @PostMapping("/query")
    public ApiResponse<RagResponse> query(@RequestBody RagQueryRequest req) {
        return ApiResponse.ok(fullRagPipeline.query(req.getQuestion(), req.getKbIds()));
    }

}