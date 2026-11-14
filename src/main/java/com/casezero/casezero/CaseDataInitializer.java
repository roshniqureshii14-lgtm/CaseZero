
package com.casezero.casezero;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CaseDataInitializer implements CommandLineRunner {

    private final CaseRecordRepository repository;

    public CaseDataInitializer(CaseRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {

        if (repository.count() == 0) {

            repository.saveAll(List.of(

                new CaseRecord(
                    "Demonstration Missing Person Record",
                    "Missing Persons",
                    "Lahore, Punjab",
                    "Fictional demonstration record showing how publicly reported missing-person information will be organized.",
                    "Open",
                    "CASEZERO Demo",
                    null
                ),

                new CaseRecord(
                    "Demonstration Unidentified Person Record",
                    "Unidentified Persons",
                    "Rawalpindi, Punjab",
                    "Fictional demonstration record showing how unidentified-person information and source references will appear.",
                    "Under Review",
                    "CASEZERO Demo",
                    null
                ),

                new CaseRecord(
                    "Demonstration Unsolved Crime Record",
                    "Unsolved Crimes",
                    "Karachi, Sindh",
                    "Fictional demonstration record showing how unresolved crime information will be organized.",
                    "Open",
                    "CASEZERO Demo",
                    null
                )
            ));
        }
    }
}
