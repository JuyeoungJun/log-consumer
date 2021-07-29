package gabia.logConsumer.business;

import gabia.logConsumer.dto.NoticeDTO;
import gabia.logConsumer.dto.NoticeDTO.Request;
import gabia.logConsumer.dto.ParsedLogDTO;
import gabia.logConsumer.entity.CronLog;
import gabia.logConsumer.repository.CronLogRepository;
import gabia.logConsumer.repository.NoticeSubscriptionRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.protocol.types.Field.Str;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Log4j2
@RequiredArgsConstructor
public class NoticeBusiness {

    private final RestTemplate restTemplate;

    @Value("${spring.notice.url}")
    private String url;

    /**
     * Notice 생성
     *
     * @param parsedLogDTO
     * @return NoticeDTO.Response
     */
    public Boolean postNotice(ParsedLogDTO parsedLogDTO) {

        // request 생성
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("noticeMessage", parsedLogDTO.getContent());
        request.put("noticeType", parsedLogDTO.getNoticeType().toString());
        request.put("cronJobId", parsedLogDTO.getCronJobId().toString());
        request.put("noticeCreateDateTime", parsedLogDTO.getTimestamp().toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(
            request);

        // 생성한 Request Cron Monitoring 서버로 Post
        try {
            ResponseEntity<String> response = restTemplate
                .exchange(url, HttpMethod.POST, entity, String.class);
            log.info("Success to make Notice about {} : {}", parsedLogDTO.getCronJobId().toString(),
                parsedLogDTO.getContent());
            return true;
        } catch (HttpClientErrorException e) {
            log.error("Fail to send message : {} ", e.getStatusCode().toString());
            return false;
        }
    }


}
