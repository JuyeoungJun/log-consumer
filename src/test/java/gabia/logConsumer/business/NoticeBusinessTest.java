package gabia.logConsumer.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

import gabia.logConsumer.dto.ParsedLogDTO;
import gabia.logConsumer.entity.Enum.NoticeType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
@AutoConfigureMockMvc
class NoticeBusinessTest {

    @Value("${spring.notice.url}")
    private String url;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NoticeBusiness noticeBusiness;


    @Test
    void postNotice_성공() {

        // given
        openMocks(this);

        UUID uuid = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(Instant.now());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("noticeMessage", "test");
        request.put("noticeType", "Start");
        request.put("cronJobId", uuid.toString());
        request.put("noticeCreateDateTime", timestamp.toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(request);

        Mockito.when(
            restTemplate.exchange(url, HttpMethod.POST,
                entity, String.class))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // when
        ParsedLogDTO parsedLogDTO = new ParsedLogDTO();
        parsedLogDTO.setNoticeType(NoticeType.Start);
        parsedLogDTO.setContent("test");
        parsedLogDTO.setCronJobId(uuid);
        parsedLogDTO.setTimestamp(timestamp);
        parsedLogDTO.setPid("1");

        Boolean notice = noticeBusiness.postNotice(parsedLogDTO);

        // then
        assertThat(notice).isEqualTo(true);

    }

    @Test
    void postNotice_실패_크론_잡이_존재하지_않는_경우() {

        // given
        openMocks(this);
        UUID uuid = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(Instant.now());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("noticeMessage", "test");
        request.put("noticeType", "Start");
        request.put("cronJobId", uuid.toString());
        request.put("noticeCreateDateTime", timestamp.toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(request);

        Mockito.when(
            restTemplate.exchange(url, HttpMethod.POST,
                entity, String.class))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // when
        ParsedLogDTO parsedLogDTO = new ParsedLogDTO();
        parsedLogDTO.setNoticeType(NoticeType.Start);
        parsedLogDTO.setContent("test");
        parsedLogDTO.setCronJobId(uuid);
        parsedLogDTO.setTimestamp(timestamp);
        parsedLogDTO.setPid("1");

        Boolean notice = noticeBusiness.postNotice(parsedLogDTO);

        // then
        assertThat(notice).isEqualTo(false);

    }


}