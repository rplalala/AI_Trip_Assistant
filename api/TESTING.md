# TESTING

## Unit tests
- Run `mvn test` from `api/` to execute the Mockito-based unit suite. Each class uses `@ExtendWith(MockitoExtension.class)` and either pure mocks or standalone MockMvc, so the Spring container is not started.

## Integration tests
- Run `mvn verify` to execute integration tests (`*IT`) alongside unit tests. Pass `-DskipITs=true` if you explicitly want to skip them.

## In-memory database
- `IntegrationTestSupport` registers H2 via `@DynamicPropertySource`, booting a fresh `jdbc:h2:mem:` database per run with `spring.jpa.hibernate.ddl-auto=create-drop`. Tests seed data through repositories in their `@BeforeEach` methods; no `application.properties` override is used.

## External stubs
- Outbound dependencies are replaced by deterministic fakes: `StubBookingClient`, `StubAwsS3Utils`, and `StubSendGridUtils` are defined inside `IntegrationTestSupport`, while HTTP-based services (e.g., Google Maps) rely on `MockRestServiceServer` within individual unit tests.

## Coverage
- JaCoCo runs during `mvn verify`. The latest run (`./mvnw verify && ./mvnw jacoco:report` on 2025-11-01) produced 86.83% line coverage (1761/2028) and 56.91% branch coverage (354/622) across the module. For `com.demo.api.service.impl` specifically the report shows 89.75% line (1164/1297) and 59.84% branch (231/386). The HTML report lives at `target/site/jacoco/index.html`.
