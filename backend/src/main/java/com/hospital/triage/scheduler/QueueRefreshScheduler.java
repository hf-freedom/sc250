package com.hospital.triage.scheduler;

import com.hospital.triage.service.TriageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueRefreshScheduler {
    @Autowired
    private TriageService triageService;

    @Scheduled(fixedRateString = "${triage.schedule.refresh-interval:30000}")
    public void refreshQueues() {
        triageService.refreshQueues();
    }
}
