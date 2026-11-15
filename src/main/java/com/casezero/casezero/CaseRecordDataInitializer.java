package com.casezero.casezero;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Adds a small set of documented Pakistan case records when the application
 * starts for the first time. Existing records are not overwritten.
 *
 * These entries are based on the cited public reporting in
 * docs/REAL_CASE_DATA_SOURCES.md. A news report is not proof of guilt.
 */
@Component
public class CaseRecordDataInitializer implements CommandLineRunner {

    private final CaseRecordRepository caseRecordRepository;

    public CaseRecordDataInitializer(CaseRecordRepository caseRecordRepository) {
        this.caseRecordRepository = caseRecordRepository;
    }

    @Override
    public void run(String... args) {
        List<CaseRecord> starterCases = List.of(
            new CaseRecord(
                "Journalist Shan Dahar murder case",
                "Unsolved Crimes",
                "Badah, Larkana, Sindh",
                "Journalist Zakir Hussain Dahar, known as Shan Dahar, was shot in January 2014 while reporting in Badah, Larkana district, and later died in hospital. A report discussed by Dawn on 22 January 2026 described serious failures in the investigation, including lost or tampered evidence, and said the killers remained at large at the time of publication. This CASEZERO record reflects that public reporting only. It does not identify suspects or present allegations as proven facts. Last source check: 14 November 2026.",
                "Open",
                "Dawn",
                "https://www.dawn.com/news/1968258"
            ),
            new CaseRecord(
                "Mir Raza Ali death investigation",
                "Unsolved Crimes",
                "Gulistan-e-Jauhar, Karachi, Sindh",
                "Karachi business owner Mir Raza Ali was reported missing on 28 July 2026; his body was found the following day in Gulistan-e-Jauhar. Public reporting described differing questions around the initial investigation and later medical findings. The judicial commission's proceedings concluded in October 2026, but reporting said important questions remained unanswered and called for further investigation. CASEZERO does not determine the cause of death or assign responsibility. Last source check: 14 November 2026.",
                "Under Review",
                "The Express Tribune",
                "https://tribune.com.pk/story/2633540/mir-raza-commission-closes-but-mystery-remains"
            ),
            new CaseRecord(
                "Fayyaz Ahmed Janjhi missing-person report",
                "Missing Persons",
                "Ayesha Manzil, Karachi, Sindh",
                "Dawn reported in July 2026 that Fayyaz Ahmed Janjhi, a NADRA assistant director, had not reached home after leaving his Karachi workplace on the evening of 20 July. His family approached the Sindh police chief for help, and the Human Rights Commission of Pakistan publicly expressed concern. This record reflects the last public report located by CASEZERO, not confirmation of his present whereabouts. No later public outcome was located in the sources checked on 14 November 2026; the status must be rechecked before being described as current.",
                "Under Review",
                "Dawn",
                "https://www.dawn.com/news/2017263"
            ),
            new CaseRecord(
                "Three unidentified persons found in Quetta's Eastern Bypass",
                "Unidentified Persons",
                "Eastern Bypass, Quetta, Balochistan",
                "Dawn reported on 15 February 2026 that police took custody of three unidentified people found at a waste depot near Quetta's Eastern Bypass and moved the bodies to Civil Hospital Quetta for identification. The report said police investigations were continuing at the time. This grouped record represents the three people in that single news report; it does not claim their identities or imply that they remain unidentified today. No later public identification outcome was located in the sources checked on 14 November 2026.",
                "Under Review",
                "Dawn",
                "https://www.dawn.com/news/1973321"
            )
        );

        for (CaseRecord candidate : starterCases) {
            if (!caseRecordRepository.existsByTitleIgnoreCase(candidate.getTitle())) {
                caseRecordRepository.save(candidate);
            }
        }
    }
}
