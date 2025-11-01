package com.demo.api.support;

import com.demo.api.repository.UserRepository;
import com.demo.api.utils.AwsS3Utils;
import com.demo.api.utils.SendGridUtils;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(IntegrationTestSupport.TestFakesConfiguration.class)
public abstract class IntegrationTestSupport {

    private static MockWebServer bookingServer;

    protected MockWebServer bookingServer() {
        return ensureBookingServer();
    }

    private static synchronized MockWebServer ensureBookingServer() {
        if (bookingServer == null) {
            bookingServer = new MockWebServer();
            try {
                bookingServer.start();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to start booking MockWebServer", e);
            }
        }
        return bookingServer;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String dbName = "it_" + UUID.randomUUID().toString().replace("-", "");
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:%s;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP"
                        .formatted(dbName));
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
        registry.add("spring.task.scheduling.enabled", () -> "false");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.main.allow-bean-definition-overriding", () -> "true");
        registry.add("booking.base-url", () -> ensureBookingServer().url("/").toString());
        registry.add("aws.accesskeyId", () -> "test");
        registry.add("aws.secretAccessKey", () -> "test");
        registry.add("aws.region", () -> "us-east-1");
        registry.add("aws.s3.bucket-name", () -> "test-bucket");
        registry.add("aws.s3.dir-name", () -> "test-dir");
        registry.add("aws.s3.cdn", () -> "cdn.test");
        registry.add("sendgrid.api-key", () -> "test-key");
        registry.add("sendgrid.from", () -> "test@example.com");
    }

    @AfterAll
    static void shutdownMockServers() throws IOException {
        if (bookingServer != null) {
            bookingServer.shutdown();
            bookingServer = null;
        }
    }

    @TestConfiguration
    static class TestFakesConfiguration {

        @Bean
        @Primary
        StubAwsS3Utils stubAwsS3Utils() {
            return new StubAwsS3Utils();
        }

        @Bean
        @Primary
        StubSendGridUtils stubSendGridUtils() {
            return new StubSendGridUtils();
        }

        @Bean(name = "initDatabase")
        @Primary
        CommandLineRunner disableDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
            return args -> {};
        }

        @Bean
        static org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor initDatabaseRemover() {
            return new org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor() {
                @Override
                public void postProcessBeanDefinitionRegistry(org.springframework.beans.factory.support.BeanDefinitionRegistry registry) {
                    if (registry.containsBeanDefinition("initDatabase")) {
                        registry.removeBeanDefinition("initDatabase");
                    }
                }

                @Override
                public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
                    // no-op
                }
            };
        }
    }

    public static class StubAwsS3Utils extends AwsS3Utils {
        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        public StubAwsS3Utils() {
            super("test", "test", "us-east-1");
            ReflectionTestUtils.setField(this, "bucketName", "test-bucket");
            ReflectionTestUtils.setField(this, "dirName", "test-dir");
            ReflectionTestUtils.setField(this, "cdn", "cdn.test");
        }

        @Override
        public String upload(InputStream in, String originalFilename, boolean isAvatar) throws Exception {
            byte[] bytes = in.readAllBytes();
            String key = (isAvatar ? "avatars" : "files") + "/" + Instant.now().toEpochMilli() + "-" + originalFilename;
            objects.put(key, bytes);
            return "https://cdn.test/" + key;
        }

        @Override
        public String uploadFromUrl(String imageUrl, boolean isAvatar) {
            byte[] body = imageUrl.getBytes(StandardCharsets.UTF_8);
            String key = (isAvatar ? "avatars" : "files") + "/url-" + Instant.now().toEpochMilli();
            objects.put(key, body);
            return "https://cdn.test/" + key;
        }

        @Override
        public List<String> listFiles(String preFix, int size) {
            return objects.keySet().stream()
                    .filter(k -> k.startsWith(preFix))
                    .limit(size)
                    .toList();
        }

        @Override
        public void deleteFile(String objectKey) {
            objects.remove(objectKey);
        }

        @Override
        public void batchDeleteFiles(List<String> objectKeys) {
            objectKeys.forEach(objects::remove);
        }
    }

    public static class StubSendGridUtils extends SendGridUtils {
        private final List<MailRecord> sentEmails = new ArrayList<>();

        public StubSendGridUtils() {
            super("test-key");
        }

        @Override
        public void sendHtml(String to, String subject, String html) {
            sentEmails.add(new MailRecord(to, subject, html));
        }

        public List<MailRecord> getSentEmails() {
            return List.copyOf(sentEmails);
        }
    }

    public record MailRecord(String to, String subject, String body) {}
}
