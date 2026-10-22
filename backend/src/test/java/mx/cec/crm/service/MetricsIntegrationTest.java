package mx.cec.crm.service;

import mx.cec.crm.entity.*;
import mx.cec.crm.repository.MetricsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:metrics;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop"
}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MetricsService.class, MetricsRepository.class, MetricsIntegrationTest.Config.class})
class MetricsIntegrationTest {
    @TestConfiguration
    static class Config {
        @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC); }
    }
    @Autowired MetricsService service;
    @Autowired TestEntityManager em;

    private User user(String email) {
        return em.persistAndFlush(User.builder().name("Test").email(email).password("test-only-hash").build());
    }
    private void deal(User owner, Deal.Stage stage, String value) {
        em.persistAndFlush(Deal.builder().user(owner).title("Test deal").stage(stage)
                .value(value == null ? null : new BigDecimal(value)).build());
    }
    private void contact(User owner, String createdAt) {
        Contact contact = em.persistAndFlush(Contact.builder().user(owner).name("Test contact").build());
        em.getEntityManager().createNativeQuery("update contacts set created_at = :date where id = :id")
                .setParameter("date", LocalDateTime.parse(createdAt)).setParameter("id", contact.getId()).executeUpdate();
    }
    private void company(User owner, String createdAt) {
        Company company = em.persistAndFlush(Company.builder().user(owner).name("Test company").build());
        em.getEntityManager().createNativeQuery("update companies set created_at = :date where id = :id")
                .setParameter("date", LocalDateTime.parse(createdAt)).setParameter("id", company.getId()).executeUpdate();
    }

    @Test
    void aggregatesExactAmountsNullValuesAndTaskStatesOnlyForTheOwner() {
        User owner = user("owner@example.com"), other = user("other@example.com");
        deal(owner, Deal.Stage.NUEVO, "0.10"); deal(owner, Deal.Stage.CONTACTADO, "0.20");
        deal(owner, Deal.Stage.PROPUESTA, null); deal(owner, Deal.Stage.CERRADO, "10.05");
        deal(owner, Deal.Stage.PERDIDO, "1000.00"); deal(other, Deal.Stage.CERRADO, "9000.00");
        em.persistAndFlush(Task.builder().user(owner).title("Pending").done(false).build());
        em.persistAndFlush(Task.builder().user(owner).title("Done").done(true).build());
        em.persistAndFlush(Task.builder().user(other).title("Other done").done(true).build());
        var result = service.overview(owner.getId(), 6);
        assertEquals(5, result.summary().deals());
        assertEquals(0, new BigDecimal("0.30").compareTo(result.summary().openValue()));
        assertEquals(0, new BigDecimal("10.05").compareTo(result.summary().closedValue()));
        assertEquals(1, result.summary().pendingTasks()); assertEquals(1, result.summary().completedTasks());
        assertEquals(6, result.dealStages().size());
        var proposal = result.dealStages().stream().filter(stage -> stage.stage() == Deal.Stage.PROPUESTA).findFirst().orElseThrow();
        assertEquals(1, proposal.count()); assertEquals(0, proposal.value().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.dealStages().stream().filter(stage -> stage.stage() == Deal.Stage.NEGOCIACION).findFirst().orElseThrow().count());
    }

    @Test
    void fillsMissingMonthsAcrossYearBoundaryAndKeepsHistoricalTotalsSeparate() {
        User owner = user("owner@example.com"), other = user("other@example.com");
        contact(owner, "2025-08-01T00:00:00"); contact(owner, "2026-01-15T10:00:00");
        contact(owner, "2025-07-31T23:59:59"); contact(owner, "2026-02-01T00:00:00");
        contact(other, "2026-01-01T00:00:00");
        company(owner, "2025-12-01T00:00:00"); company(other, "2025-12-01T00:00:00");
        var six = service.overview(owner.getId(), 6);
        assertEquals(LocalDate.of(2026, 1, 15), six.asOf());
        assertEquals(4, six.summary().contacts()); assertEquals(1, six.summary().companies());
        assertEquals(6, six.growth().size());
        assertEquals("2025-08", six.growth().get(0).month()); assertEquals(1, six.growth().get(0).contacts());
        assertEquals("2026-01", six.growth().get(5).month()); assertEquals(1, six.growth().get(5).contacts());
        assertEquals(2, six.growth().stream().mapToLong(row -> row.contacts()).sum());
        assertEquals(0, six.growth().get(1).contacts()); assertEquals(1, six.growth().get(4).companies());
        var twelve = service.overview(owner.getId(), 12);
        assertEquals("2025-02", twelve.growth().get(0).month()); assertEquals(12, twelve.growth().size());
        assertEquals(3, twelve.growth().stream().mapToLong(row -> row.contacts()).sum());
        assertEquals(six.summary(), twelve.summary());
    }

    @Test
    void emptyUserGetsZeroesAndUnsupportedPeriodsAreRejected() {
        User owner = user("empty@example.com");
        var result = service.overview(owner.getId(), 6);
        assertEquals(0, result.summary().deals()); assertEquals(0, result.summary().contacts());
        assertEquals(0, result.summary().companies()); assertEquals(0, result.summary().pendingTasks());
        assertEquals(0, result.summary().completedTasks());
        assertTrue(result.growth().stream().allMatch(row -> row.contacts() == 0 && row.companies() == 0));
        assertTrue(result.dealStages().stream().allMatch(row -> row.count() == 0 && row.value().signum() == 0));
        for (int months : new int[]{-1, 0, 1, 13, 100})
            assertThrows(IllegalArgumentException.class, () -> service.overview(owner.getId(), months));
    }
}
