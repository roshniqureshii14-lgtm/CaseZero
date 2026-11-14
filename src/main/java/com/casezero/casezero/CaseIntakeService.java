package com.casezero.casezero;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseIntakeService {
    private static final String PAKISTAN_CASE_SEARCH =
            "\"missing person\" OR \"missing persons\" OR \"missing girl\" OR \"missing boy\" OR "
            + "\"unidentified body\" OR \"unidentified persons\" OR \"unclaimed body\" OR "
            + "\"unsolved murder\" OR \"unsolved crime\" OR \"cold case\" OR "
            + "\"kidnapped\" OR \"abducted\" OR \"لاپتہ\" OR \"گمشدہ\" OR "
            + "\"نامعلوم لاش\" OR \"اغوا\" OR \"قتل\"";

    private final NewsResearchService newsResearchService;
    private final NewsDataApiService newsDataApiService;
    private final CaseResearchReportRepository reportRepository;

    public CaseIntakeService(NewsResearchService newsResearchService,
                             NewsDataApiService newsDataApiService,
                             CaseResearchReportRepository reportRepository) {
        this.newsResearchService = newsResearchService;
        this.newsDataApiService = newsDataApiService;
        this.reportRepository = reportRepository;
    }

    @Transactional
    public synchronized int collectPakistanReports() {
        Map<String, ResearchArticle> combined = new LinkedHashMap<>();
        for (ResearchArticle article : newsResearchService.searchPakistanNews(PAKISTAN_CASE_SEARCH)) {
            combined.putIfAbsent(article.url(), article);
        }
        for (ResearchArticle article : newsDataApiService.collectPakistanReports()) {
            combined.putIfAbsent(article.url(), article);
        }

        int saved = 0;
        for (ResearchArticle article : new ArrayList<>(combined.values())) {
            if (article.url() != null && article.url().startsWith("https://")
                    && !reportRepository.existsBySourceUrl(article.url())
                    && !reportRepository.existsByTitleIgnoreCase(article.title())) {
                reportRepository.save(new CaseResearchReport(article));
                saved++;
            }
        }
        return saved;
    }
}