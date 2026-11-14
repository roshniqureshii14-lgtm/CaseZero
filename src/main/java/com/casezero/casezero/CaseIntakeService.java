package com.casezero.casezero;

import java.util.List;
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
    private final CaseResearchReportRepository reportRepository;

    public CaseIntakeService(NewsResearchService newsResearchService,
                             CaseResearchReportRepository reportRepository) {
        this.newsResearchService = newsResearchService;
        this.reportRepository = reportRepository;
    }

    @Transactional
    public synchronized int collectPakistanReports() {
        List<ResearchArticle> articles =
                newsResearchService.searchPakistanNews(PAKISTAN_CASE_SEARCH);
        int saved = 0;
        for (ResearchArticle article : articles) {
            if (article.url() != null && article.url().startsWith("https://")
                    && !reportRepository.existsBySourceUrl(article.url())) {
                reportRepository.save(new CaseResearchReport(article));
                saved++;
            }
        }
        return saved;
    }
}