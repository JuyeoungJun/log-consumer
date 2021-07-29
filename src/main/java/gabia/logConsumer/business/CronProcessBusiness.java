package gabia.logConsumer.business;

import gabia.logConsumer.dto.ParsedLogDTO;
import gabia.logConsumer.entity.CronJob;
import gabia.logConsumer.entity.Enum.NoticeType;
import gabia.logConsumer.exception.CronJobNotFoundException;
import gabia.logConsumer.repository.CronJobRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Log4j2
@RequiredArgsConstructor
public class CronProcessBusiness {

    private final RestTemplate restTemplate;
    private final CronJobRepository cronJobRepository;

    @Value("${spring.cron.server.url}")
    private String serverUrl;

    public Boolean postCronProcess(ParsedLogDTO parsedLogDTO) {

        // 해당 cronJob 이 존재하는 지 확인
        CronJob cronJob = cronJobRepository.findById(parsedLogDTO.getCronJobId())
            .orElseThrow(() -> new CronJobNotFoundException("해당 크론 잡이 존재하지 않습니다."));

        // Cron Process 생성 url
        String url = String.format(serverUrl + "cron-servers/%s/cron-jobs/%s/process/",
            cronJob.getServer(), cronJob.getId().toString(), parsedLogDTO.getPid());

        // request 생성
        Map<String, Object> cronProcessRequest = new HashMap<String, Object>();
        cronProcessRequest.put("pid", parsedLogDTO.getPid());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(
            cronProcessRequest);

        if (parsedLogDTO.getNoticeType() == NoticeType.Start) {

            // 시작하는 Log 인 경우
            cronProcessRequest.put("startTime", parsedLogDTO.getTimestamp().toString());
            try {
                ResponseEntity<String> response = restTemplate
                    .exchange(url, HttpMethod.POST, entity, String.class);
                log.info("Success make cron process (cronJobId: {}, pid: {})",
                    parsedLogDTO.getCronJobId().toString(), parsedLogDTO.getPid());
                return true;
            } catch (HttpClientErrorException e) {
                log.error("Fail to make cron process (cronJobId: {}, pid: {}) : {}",
                    parsedLogDTO.getCronJobId().toString(), parsedLogDTO.getPid(), e.getMessage());
                return false;
            }
        } else if (parsedLogDTO.getNoticeType() == NoticeType.End) {

            // 끝나는 Log 인 경우
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            url = url + parsedLogDTO.getPid();
            cronProcessRequest.put("endTime", parsedLogDTO.getTimestamp().toString());
            try {
                ResponseEntity<String> response = restTemplate
                    .exchange(url, HttpMethod.PATCH, entity, String.class);
                log.info("Success update cron process endtime (cronJobId: {}, pid: {})",
                    parsedLogDTO.getCronJobId().toString(), parsedLogDTO.getPid());
                return true;
            } catch (HttpClientErrorException e) {
                log.error("Fail to update cron process endtime (cronJobId: {}, pid: {}) : {}",
                    parsedLogDTO.getCronJobId().toString(), parsedLogDTO.getPid(), e.getMessage());
                return false;
            }
        }
        return true;
    }

}
