package com.casezero.casezero;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledCaseCollection {
    private static final Logger log = LoggerFactory.getLogger(ScheduledCaseCollection.class);
    private final CaseIntakeService caseIntakeService;

    public ScheduledCaseCollection(CaseIntakeService caseIntakeService) {
        this.caseIntakeService = caseIntakeService;
    }

    // First automatic run one minute after startup, then every six hours.
    @Scheduled(initialDelay = 60_000, fixedDelay = 21_600_000)
    public void collectRecentReports() {
        try {
            int saved = caseIntakeService.collectPakistanReports();
            log.info("Scheduled CASEZERO collection completed: {} new report(s) saved.", saved);
        } catch (Exception ex) {
            // A temporary publisher/network/database issue must not stop the application.
            log.error("Scheduled CASEZERO collection failed; the next scheduled run will retry.", ex);
        }
    }
}