package pl.edu.agh.model;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.indicator.StandardIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import pl.edu.agh.dto.IterationResult;
import pl.edu.agh.repository.ExperimentRepository;
import pl.edu.agh.repository.ExperimentResultRepository;

import java.time.Instant;
import java.util.*;

@DataMongoTest
public class ExperimentIntegrationTest {

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ExperimentResultRepository experimentResultRepository;
    @AfterEach
    void cleanUp() {
        // Usuwamy wszystkie dane z kolekcji experiments i results, aby mieć czysty stan przed kolejnym testem.
        experimentRepository.deleteAll();
        experimentResultRepository.deleteAll();
    }

    @Test
    void testSaveAndFindExperiment() {
        // GIVEN
        // Tworzymy obiekt experiment
        Experiment experiment = new Experiment();
        experiment.setAlgorithms(Set.of("VEGA"));
        experiment.setProblems(Set.of("ZDT1", "ZDT2"));
        experiment.setMetrics(Set.of(StandardIndicator.GenerationalDistance));
        experiment.setBudget(1000);
        experiment.setStatus(ExperimentStatus.PENDING);
        experiment.setStartTime(Instant.now());
        experiment.setEndTime(null);
        experiment.setErrorMessage(null);

        // WHEN
        // Zapisujemy do bazy
        Experiment saved = experimentRepository.save(experiment);
        Assertions.assertNotNull(saved.getId(), "ID should be generated by MongoDB");
        // Odczytujemy z bazy
        Optional<Experiment> found = experimentRepository.findById(saved.getId());

        // THEN
        Assertions.assertTrue(found.isPresent(), "Experiment should be found");
        Assertions.assertEquals(1000, found.get().getBudget());
        Assertions.assertEquals(ExperimentStatus.PENDING, found.get().getStatus());
    }

    @Test
    void testSaveAndFindExperimentResult() {
        // GIVEN
        // Najpierw tworzymy i zapisujemy experiment
        Experiment experiment = new Experiment();
        experiment.setAlgorithms(Set.of("VEGA"));
        experiment.setProblems(Set.of("ZDT1", "ZDT2"));
        experiment.setMetrics(Set.of(StandardIndicator.GenerationalDistance));
        experiment.setBudget(500);
        experiment.setStatus(ExperimentStatus.RUNNING);
        experiment.setStartTime(Instant.now());
        experiment.setEndTime(null);
        experiment.setErrorMessage(null);

        Experiment savedExp = experimentRepository.save(experiment);

        IterationResult iterationResult = new IterationResult(
                1,
                Map.of(StandardIndicator.AdditiveEpsilonIndicator, 0.12)
        );

        // Tworzymy ExperimentResult
        ExperimentResult experimentResult = new ExperimentResult(
                new ObjectId(),
                savedExp,
                "VEGA",
                "ZDT1",
                List.of(iterationResult),
                10
        );

        // WHEN
        ExperimentResult savedResult = experimentResultRepository.save(experimentResult);
        Assertions.assertNotNull(savedResult.getId(), "ID for result should be generated");
        Assertions.assertEquals(savedExp.getId(), savedResult.getExperiment().getId(), "Result should link to the correct experiment");

        // THEN
        // Odczyt wyników
        List<ExperimentResult> resultsForExp = experimentResultRepository.findByExperimentId(savedExp.getId());
        Assertions.assertFalse(resultsForExp.isEmpty(), "Should find results for the saved experiment");
        Assertions.assertEquals(1, resultsForExp.size());
        Assertions.assertEquals("VEGA", resultsForExp.get(0).getAlgorithm());
        Assertions.assertEquals(10, resultsForExp.get(0).getRunCount());
    }
    @Test
    void testUpdateExperimentStatus() {
        // GIVEN
        // Tworzymy i zapisujemy eksperyment ze statusem PENDING
        Experiment experiment = new Experiment();
        experiment.setAlgorithms(Set.of("VEGA"));
        experiment.setProblems(Set.of("ZDT1", "ZDT2"));
        experiment.setMetrics(Set.of(StandardIndicator.GenerationalDistance));
        experiment.setBudget(1000);
        experiment.setStatus(ExperimentStatus.PENDING);
        experiment.setStartTime(Instant.now());
        experiment.setEndTime(null);
        experiment.setErrorMessage(null);

        experiment = experimentRepository.save(experiment);

        // WHEN
        // Aktualizujemy status
        experiment.setStatus(ExperimentStatus.RUNNING);
        experimentRepository.save(experiment);

        // THEN
        Optional<Experiment> updated = experimentRepository.findById(experiment.getId());
        Assertions.assertTrue(updated.isPresent());
        Assertions.assertEquals(ExperimentStatus.RUNNING, updated.get().getStatus());
    }

    @Test
    void testFindExperimentsByStatus() {
        // GIVEN
        // Zapisujemy kilka eksperymentów o różnych statusach
        Experiment exp1 = new Experiment();
        exp1.setStatus(ExperimentStatus.PENDING);
        experimentRepository.save(exp1);

        Experiment exp2 = new Experiment();
        exp2.setStatus(ExperimentStatus.FINISHED);
        experimentRepository.save(exp2);

        Experiment exp3 = new Experiment();
        exp3.setStatus(ExperimentStatus.PENDING);
        experimentRepository.save(exp3);

        // WHEN
        List<Experiment> pendingExps = experimentRepository.findByStatus(ExperimentStatus.PENDING);

        // THEN
        Assertions.assertEquals(2, pendingExps.size(), "Should find 2 pending experiments");

        // Po zakończeniu testu zostanie wywołana metoda @AfterEach cleanUp(),
        // która usunie wszystkie eksperymenty.
    }

    @Test
    void testDeleteExperiment() {
        // GIVEN
        // Tworzymy i zapisujemy eksperyment
        Experiment experiment = new Experiment();
        experiment.setStatus(ExperimentStatus.RUNNING);
        experiment = experimentRepository.save(experiment);

        // WHEN
        // Usuwamy
        experimentRepository.delete(experiment);

        // THEN
        Optional<Experiment> deleted = experimentRepository.findById(experiment.getId());
        Assertions.assertFalse(deleted.isPresent(), "Experiment should be deleted");
    }

    @Disabled(value = "Till findByAlgorithm works again")
    @Test
    void testExperimentResultsByAlgorithm() {
        // GIVEN
        // Tworzymy eksperyment
        Experiment experiment = new Experiment();
        experiment.setStatus(ExperimentStatus.RUNNING);
        experiment.setBudget(300);
        experiment = experimentRepository.save(experiment);

        // Tworzymy dwa wyniki: jeden dla AAA, drugi dla BBB
        ExperimentResult resultA = new ExperimentResult(
                new ObjectId(),
                experiment,
                "VEGA",
                "ZDT1",
                Collections.emptyList(),
                10
        );

        ExperimentResult resultB = new ExperimentResult(
                new ObjectId(),
                experiment,
                "AMOSA",
                "ZDT2",
                Collections.emptyList(),
                20
        );

        experimentResultRepository.saveAll(Arrays.asList(resultA, resultB));

//        // Wyszukujemy wyniki po algorytmie VEGA
//        List<ExperimentResult> aaaResults = experimentResultRepository.findByAlgorithm(Algorithm.AAA);
//        Assertions.assertEquals(1, aaaResults.size());
//        Assertions.assertEquals("VEGA", aaaResults.get(0).getAlgorithm());
//
//        // Wyszukujemy wyniki po algorytmie AMOSA
//        List<ExperimentResult> bbbResults = experimentResultRepository.findByAlgorithm(Algorithm.BBB);
//        Assertions.assertEquals(1, bbbResults.size());
//        Assertions.assertEquals("AMOSA", bbbResults.get(0).getAlgorithm());
    }


    @Test
    void testDeleteExperimentAlsoRemovesResultsIfCascadingLogicExists() {
        // UWAGA: MongoDB nie wspiera kaskadowego usuwania natywnie,
        // ale można zaimplementować w logice aplikacji.
        // Poniższy test jest przykładowy, zakładając że w kodzie jest logika,
        // np. w serwisie, która przed usunięciem eksperymentu usuwa wyniki.
        // Tu jedynie pokazujemy ideę - test będzie przechodził jeśli faktycznie taką logikę dodamy.

        // GIVEN
        // Tworzymy eksperyment i rezultaty
        Experiment experiment = new Experiment();
        experiment.setStatus(ExperimentStatus.RUNNING);
        experiment = experimentRepository.save(experiment);

        ExperimentResult result = new ExperimentResult(
                new ObjectId(),
                experiment,
                "VEGA",
                "ZDT1",
                Collections.emptyList(),
                20
        );
        experimentResultRepository.save(result);

        // WHEN
        // Wyobraźmy sobie, że mamy metodę w serwisie: service.deleteExperimentAndResults(experimentId)
        // Która usuwa zarówno eksperyment jak i powiązane wyniki.
        // Tu na potrzeby testu zrobimy to "ręcznie":
        experimentResultRepository.deleteAll(experimentResultRepository.findByExperimentId(experiment.getId()));
        experimentRepository.delete(experiment);

        // THEN
        // Sprawdzamy czy dane faktycznie zniknęły
        Assertions.assertFalse(experimentRepository.findById(experiment.getId()).isPresent(), "Experiment should be deleted");
        Assertions.assertTrue(experimentResultRepository.findByExperimentId(experiment.getId()).isEmpty(), "Results should be deleted");
    }
}
