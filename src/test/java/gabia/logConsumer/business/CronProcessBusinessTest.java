package gabia.logConsumer.business;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import gabia.logConsumer.dto.ParsedLogDTO;
import gabia.logConsumer.entity.CronJob;
import gabia.logConsumer.entity.Enum.NoticeType;
import gabia.logConsumer.exception.CronJobNotFoundException;
import gabia.logConsumer.repository.CronJobRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
@AutoConfigureMockMvc
class CronProcessBusinessTest {

    @Value("${spring.cron.server.url}")
    private String serverUrl;

    @Mock
    CronJobRepository cronJobRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    CronProcessBusiness cronProcessBusiness;

    @Test
    void postCronProcess_POST_성공() {

        // given
        openMocks(this);
        UUID uuid = UUID.randomUUID();

        CronJob cronJob = CronJob.builder()
            .cronExpr("test")
            .cronName("test")
            .id(uuid)
            .server("127.0.0.1")
            .build();

        given(cronJobRepository.findById(uuid)).willReturn(Optional.of(cronJob));

        Timestamp timestamp = Timestamp.from(Instant.now());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("pid", "1");
        request.put("startTime", timestamp.toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(request);

        String url = String.format(serverUrl + "cron-servers/%s/cron-jobs/%s/process/",
            cronJob.getServer(), cronJob.getId().toString(), "1");

        Mockito.when(
            restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // when
        ParsedLogDTO parsedLogDTO = new ParsedLogDTO();
        parsedLogDTO.setNoticeType(NoticeType.Start);
        parsedLogDTO.setContent("test");
        parsedLogDTO.setCronJobId(uuid);
        parsedLogDTO.setTimestamp(timestamp);
        parsedLogDTO.setPid("1");

        Boolean cronProcess = cronProcessBusiness.postCronProcess(parsedLogDTO);

        // then
        Assertions.assertEquals(cronProcess, true);
    }

    @Test
    void postCronProcess_PATCH_성공() {

        // given
        openMocks(this);
        UUID uuid = UUID.randomUUID();

        CronJob cronJob = CronJob.builder()
            .cronExpr("test")
            .cronName("test")
            .id(uuid)
            .server("127.0.0.1")
            .build();

        given(cronJobRepository.findById(uuid)).willReturn(Optional.of(cronJob));

        Timestamp timestamp = Timestamp.from(Instant.now());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("pid", "1");
        request.put("endTime", timestamp.toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(request);

        String url = String.format(serverUrl + "cron-servers/%s/cron-jobs/%s/process/",
            cronJob.getServer(), cronJob.getId().toString());

        Mockito.when(
            restTemplate.exchange(
                url + "1", HttpMethod.PATCH, entity, String.class))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // when
        ParsedLogDTO parsedLogDTO = new ParsedLogDTO();
        parsedLogDTO.setNoticeType(NoticeType.End);
        parsedLogDTO.setContent("test");
        parsedLogDTO.setCronJobId(uuid);
        parsedLogDTO.setTimestamp(timestamp);
        parsedLogDTO.setPid("1");

        Boolean cronProcess = cronProcessBusiness.postCronProcess(parsedLogDTO);

        // then
        Assertions.assertEquals(cronProcess, true);
    }

    @Test
    void postCronProcess_크론잡이_없는_경우() {

        // given
        openMocks(this);
        UUID uuid = UUID.randomUUID();

        CronJob cronJob = CronJob.builder()
            .cronExpr("test")
            .cronName("test")
            .id(uuid)
            .server("127.0.0.1")
            .build();

        given(cronJobRepository.findById(uuid)).willReturn(Optional.empty());

        Timestamp timestamp = Timestamp.from(Instant.now());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("pid", "1");
        request.put("startTime", timestamp.toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(request);

        String url = String.format(serverUrl + "cron-servers/%s/cron-jobs/%s/process/",
            cronJob.getServer(), cronJob.getId().toString());

        Mockito.when(
            restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // when
        // then
        ParsedLogDTO parsedLogDTO = new ParsedLogDTO();
        parsedLogDTO.setNoticeType(NoticeType.Start);
        parsedLogDTO.setContent("test");
        parsedLogDTO.setCronJobId(uuid);
        parsedLogDTO.setTimestamp(timestamp);
        parsedLogDTO.setPid("1");

        assertThrows(CronJobNotFoundException.class, () -> {
            cronProcessBusiness.postCronProcess(parsedLogDTO);
        });
    }

    @Test
    void postCronProcess_POST_실패() {

        // given
        openMocks(this);
        UUID uuid = UUID.randomUUID();

        CronJob cronJob = CronJob.builder()
            .cronExpr("test")
            .cronName("test")
            .id(uuid)
            .server("127.0.0.1")
            .build();

        given(cronJobRepository.findById(uuid)).willReturn(Optional.of(cronJob));

        Timestamp timestamp = Timestamp.from(Instant.now());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("pid", "1");
        request.put("startTime", timestamp.toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(request);

        String url = String.format(serverUrl + "cron-servers/%s/cron-jobs/%s/process/",
            cronJob.getServer(), cronJob.getId().toString());

        Mockito.when(
            restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // when
        ParsedLogDTO parsedLogDTO = new ParsedLogDTO();
        parsedLogDTO.setNoticeType(NoticeType.Start);
        parsedLogDTO.setContent("test");
        parsedLogDTO.setCronJobId(uuid);
        parsedLogDTO.setTimestamp(timestamp);
        parsedLogDTO.setPid("1");

        Boolean cronProcess = cronProcessBusiness.postCronProcess(parsedLogDTO);

        // then
        Assertions.assertEquals(cronProcess, false);
    }

    @Test
    void postCronProcess_PATCH_실패() {

        // given
        openMocks(this);
        UUID uuid = UUID.randomUUID();

        CronJob cronJob = CronJob.builder()
            .cronExpr("test")
            .cronName("test")
            .id(uuid)
            .server("127.0.0.1")
            .build();

        given(cronJobRepository.findById(uuid)).willReturn(Optional.of(cronJob));

        Timestamp timestamp = Timestamp.from(Instant.now());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("pid", "1");
        request.put("endTime", timestamp.toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(request);

        String url = String.format(serverUrl + "cron-servers/%s/cron-jobs/%s/process/",
            cronJob.getServer(), cronJob.getId().toString());

        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        Mockito.when(
            restTemplate.exchange(
                url + "1", HttpMethod.PATCH, entity, String.class))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // when
        ParsedLogDTO parsedLogDTO = new ParsedLogDTO();
        parsedLogDTO.setNoticeType(NoticeType.End);
        parsedLogDTO.setContent("test");
        parsedLogDTO.setCronJobId(uuid);
        parsedLogDTO.setTimestamp(timestamp);
        parsedLogDTO.setPid("1");

        Boolean cronProcess = cronProcessBusiness.postCronProcess(parsedLogDTO);

        // then
        Assertions.assertEquals(cronProcess, false);
    }
}