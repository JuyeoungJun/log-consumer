package gabia.logConsumer.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gabia.logConsumer.dto.ParsedLogDTO;
import gabia.logConsumer.dto.WebhookDTO;
import gabia.logConsumer.dto.WebhookMessage;
import gabia.logConsumer.entity.WebhookSubscription;
import gabia.logConsumer.repository.NoticeSubscriptionRepository;
import gabia.logConsumer.repository.WebhookSubscriptionRepository;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Log4j2
@RequiredArgsConstructor
public class WebhookBusiness {

    private final RestTemplate restTemplate;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final KafkaTemplate<String, WebhookDTO.Request> kafkaTemplate;
    private final NoticeSubscriptionRepository noticeSubscriptionRepository;

    /**
     * 웹훅 생성
     *
     * @param parsedLogDTO
     */
    public List<WebhookDTO.Request> produceWebhook(ParsedLogDTO parsedLogDTO) {

        // Post 할 Request 생성
        List<Long> noticeSubscriptionIdList = noticeSubscriptionRepository
            .findByCronJobId(parsedLogDTO.getCronJobId()).stream()
            .map(dto -> dto.getId())
            .collect(Collectors.toList());

        List<WebhookSubscription> webhookSubscriptionList = webhookSubscriptionRepository
            .findByNoticeSubscriptionIdIn(noticeSubscriptionIdList);

        String text = String.format("[%s] (%s) %s: %s", parsedLogDTO.getTimestamp(),
            parsedLogDTO.getNoticeType().toString(), parsedLogDTO.getCronJobId(),
            parsedLogDTO.getContent());

        List<WebhookDTO.Request> result = new LinkedList<>();

        // kafka message produce
        for (WebhookSubscription webhookSubscription : webhookSubscriptionList) {
            WebhookDTO.Request request = new WebhookDTO.Request();
            request.setUrl(webhookSubscription.getUrl());
            request.setEndpoint(webhookSubscription.getEndpoint());
            request.setText(text);
            result.add(request);

            kafkaTemplate.send("webhook", request);
        }

        return result;
    }

    /**
     * Webhook message 전송
     *
     * @param webhookDTO
     * @param webhookMessage
     * @throws JsonProcessingException
     */
    public boolean sendMessage(WebhookDTO.Request webhookDTO, WebhookMessage webhookMessage)
        throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = mapper.writeValueAsString(webhookMessage);
        Map<String, Object> slackMessage = mapper.readValue(json,
            new TypeReference<Map<String, Object>>() {
            });

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(
            slackMessage);

        try {
            restTemplate.exchange(webhookDTO.getUrl(), HttpMethod.POST, entity, String.class);
            log.info("Send Message to {} (url: {})", webhookDTO.getEndpoint(), webhookDTO.getUrl());
            return true;
        } catch (HttpClientErrorException e) {
            log.error("Fail to send message : {}", e.getMessage());
            return false;
        }

    }
}
