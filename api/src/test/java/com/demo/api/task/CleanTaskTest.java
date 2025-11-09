package com.demo.api.task;

import com.demo.api.model.EmailToken;
import com.demo.api.repository.EmailTokenRepository;
import com.demo.api.repository.TripAttractionRepository;
import com.demo.api.repository.TripBookingQuoteRepository;
import com.demo.api.repository.TripDailySummaryRepository;
import com.demo.api.repository.TripHotelRepository;
import com.demo.api.repository.TripInsightRepository;
import com.demo.api.repository.TripRepository;
import com.demo.api.repository.TripTransportationRepository;
import com.demo.api.repository.TripWeatherRepository;
import com.demo.api.repository.UserRepository;
import com.demo.api.utils.AliyunOSSUtils;
import com.demo.api.utils.AwsS3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CleanTaskTest {

    @Mock
    private AwsS3Utils awsS3Utils;
    @Mock
    private AliyunOSSUtils aliyunOSSUtils;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailTokenRepository emailTokenRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripAttractionRepository attractionRepository;
    @Mock
    private TripHotelRepository hotelRepository;
    @Mock
    private TripTransportationRepository transportationRepository;
    @Mock
    private TripDailySummaryRepository summaryRepository;
    @Mock
    private TripBookingQuoteRepository bookingQuoteRepository;
    @Mock
    private TripInsightRepository insightRepository;
    @Mock
    private TripWeatherRepository weatherRepository;

    @InjectMocks
    private CleanTask task;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(task, "dirName", "root");
    }

    @DisplayName("fileClean removes S3 objects not referenced in database")
    @Test
    void fileClean_deletesOrphanedFiles() throws Exception {
        when(userRepository.findAllAvatar()).thenReturn(List.of("https://cdn.example.com/root/avatars/2025/05/a.jpg"));
        when(aliyunOSSUtils.listPageAllFiles("root/avatars/", 1000))
                .thenReturn(List.of("root/avatars/2025/05/a.jpg", "root/avatars/2025/05/b.jpg"));

        task.fileClean();

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(aliyunOSSUtils).batchDeleteFiles(captor.capture());
        assertThat(captor.getValue()).containsExactly("root/avatars/2025/05/b.jpg");
    }

    @DisplayName("fileClean skips deletion when S3 has no files")
    @Test
    void fileClean_noFiles() throws Exception {
        when(userRepository.findAllAvatar()).thenReturn(List.of());
        when(aliyunOSSUtils.listPageAllFiles("root/avatars/", 1000)).thenReturn(List.of());

        task.fileClean();

        verify(aliyunOSSUtils, never()).batchDeleteFiles(any());
    }

    @DisplayName("cleanup deletes expired tokens")
    @Test
    void cleanup_removesExpiredTokens() {
        task.cleanup();
        verify(emailTokenRepository).deleteAllByExpireTimeBefore(any(Instant.class));
    }

    @DisplayName("cleanRedundantData removes redundant trip hierarchy")
    @Test
    void cleanRedundantData_removesTripData() {
        when(tripRepository.findRedundantUserIds()).thenReturn(List.of(1L, 2L));
        when(tripRepository.findIdsByUserIdIn(List.of(1L, 2L))).thenReturn(List.of(10L, 20L));

        task.cleanRedundantData();

        verify(weatherRepository).deleteByTripIdIn(List.of(10L, 20L));
        verify(insightRepository).deleteByTripIdIn(List.of(10L, 20L));
        verify(bookingQuoteRepository).deleteByTripIdIn(List.of(10L, 20L));
        verify(summaryRepository).deleteByTripIdIn(List.of(10L, 20L));
        verify(transportationRepository).deleteByTripIdIn(List.of(10L, 20L));
        verify(hotelRepository).deleteByTripIdIn(List.of(10L, 20L));
        verify(attractionRepository).deleteByTripIdIn(List.of(10L, 20L));
        verify(tripRepository).deleteByUserIdIn(List.of(1L, 2L));
    }

    @DisplayName("cleanRedundantData logs when no redundant users found")
    @Test
    void cleanRedundantData_whenNoUsers() {
        when(tripRepository.findRedundantUserIds()).thenReturn(List.of());

        task.cleanRedundantData();

        verify(tripRepository).findRedundantUserIds();
        verify(tripRepository, never()).findIdsByUserIdIn(any());
        verifyNoInteractions(weatherRepository, insightRepository, bookingQuoteRepository,
                summaryRepository, transportationRepository, hotelRepository, attractionRepository);
    }

    @DisplayName("cleanRedundantData skips deletions when no trip ids")
    @Test
    void cleanRedundantData_whenNoTrips() {
        when(tripRepository.findRedundantUserIds()).thenReturn(List.of(5L));
        when(tripRepository.findIdsByUserIdIn(List.of(5L))).thenReturn(List.of());

        task.cleanRedundantData();

        verify(tripRepository).findIdsByUserIdIn(List.of(5L));
        verifyNoInteractions(weatherRepository, insightRepository, bookingQuoteRepository,
                summaryRepository, transportationRepository, hotelRepository, attractionRepository);
    }
}
