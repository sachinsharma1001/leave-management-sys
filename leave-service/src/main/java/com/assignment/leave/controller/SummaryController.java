package com.assignment.leave.controller;

import com.assignment.leave.dto.SummaryRequest;
import com.assignment.leave.dto.SummaryResponse;
import com.assignment.leave.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
public class SummaryController {
    private final SummaryService summaryService;

    @PostMapping
    public SummaryResponse summarize(@RequestBody SummaryRequest request) {
        return summaryService.summarize(request);
    }
}
