# TESTING

## Unit tests
Run `mvn test` from `api/` to execute the Mockito-based unit suite. Each class uses `@ExtendWith(MockitoExtension.class)` and either pure mocks or standalone MockMvc, so the Spring container is not started.

## Integration tests
Run `mvn verify` to execute integration tests (`*IT`) alongside unit tests. Pass `-DskipITs=true` if you explicitly want to skip them.

*Note:* if you're running the tests for the first time, you'll need to run `mvn clean verify`. Maven will automatically download any required dependencies.

## In-memory database
`IntegrationTestSupport` registers H2 via `@DynamicPropertySource`, booting a fresh `jdbc:h2:mem:` database per run with `spring.jpa.hibernate.ddl-auto=create-drop`. Tests seed data through repositories in their `@BeforeEach` methods; no `application.properties` override is used.

## External stubs
Outbound dependencies are replaced by deterministic fakes: `StubBookingClient`, `StubAwsS3Utils`, and `StubSendGridUtils` are defined inside `IntegrationTestSupport`, while HTTP-based services (e.g., Google Maps) rely on `MockRestServiceServer` within individual unit tests.

## Coverage
JaCoCo runs during `mvn clean verify`. The latest run (on 2025-11-02) produced 93.43 % line coverage (1905/2039) and 69.12 % branch coverage (429/621). Key low-coverage areas remain `BookingServiceImpl`, `TripGenerationServiceImpl`, `MapServiceImpl`, and `UserServiceImpl`. HTML artifacts live at `target/site/jacoco/index.html`.
