package com.anvesha.core.config;

import com.anvesha.core.entity.Institution;
import com.anvesha.core.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final InstitutionRepository institutionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (institutionRepository.count() == 0) {
            log.info("No institutions found in database. Seeding default institutions...");

            Institution iitb = Institution.builder()
                    .name("IIT Bombay")
                    .type("IIT")
                    .ttoContactEmail("tto@iitb.ac.in")
                    .build();

            Institution ncl = Institution.builder()
                    .name("CSIR-NCL")
                    .type("CSIR_LAB")
                    .ttoContactEmail("tto@ncl.res.in")
                    .build();

            Institution iisc = Institution.builder()
                    .name("IISc Bangalore")
                    .type("OTHER")
                    .ttoContactEmail("tto@iisc.ac.in")
                    .build();

            institutionRepository.save(iitb);
            institutionRepository.save(ncl);
            institutionRepository.save(iisc);

            log.info("Default institutions seeded successfully! IIT Bombay has ID: 1");
        } else {
            log.info("Institutions already present in database. Skipping seed.");
        }
    }
}
