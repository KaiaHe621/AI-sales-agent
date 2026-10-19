package com.jichi.springaialibaba.controller.parallel;

import com.jichi.springaialibaba.service.MultiModelVotingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vote")
public class MultiModelVotingController {

    private final MultiModelVotingService votingService;

    public MultiModelVotingController(MultiModelVotingService votingService) {
        this.votingService = votingService;
    }

    @GetMapping
    public MultiModelVotingService.VoteResult vote(@RequestParam String question) throws Exception {
        return votingService.vote(question);
    }
}