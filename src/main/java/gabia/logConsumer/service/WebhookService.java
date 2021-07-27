package gabia.logConsumer.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gabia.logConsumer.business.CronLogBusiness;
import gabia.logConsumer.business.CronProcessBusiness;
import gabia.logConsumer.business.NoticeBusiness;
import gabia.logConsumer.business.WebhookBusiness;
import gabia.logConsumer.dto.HiworksDTO;
import gabia.logConsumer.dto.ParsedLogDTO;
import gabia.logConsumer.dto.SlackDTO;
import gabia.logConsumer.dto.SlackDTO.Attachment;
import gabia.logConsumer.dto.WebhookDTO;
import gabia.logConsumer.dto.WebhookMessage;
import gabia.logConsumer.entity.Enum.WebhookEndpoint;
import gabia.logConsumer.util.WebhookMessageFactory;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Log4j2
@Service
public class WebhookService {

    private final NoticeBusiness noticeBusiness;
    private final WebhookBusiness webhookBusiness;
    private final CronProcessBusiness cronProcessBusiness;
    private final CronLogBusiness cronLogBusiness;

    /**
     * Notice 생성 Post 및 웹훅 기능
     *
     * @param message Log message
     */
    @KafkaListener(topics = "statusLog", groupId = "log_consumer")
    public void processLog(String message) {

        // 토픽에서 가져온 카프카 메시지 파싱
        JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
        String logMessage = jsonObject.get("message").getAsString();
        ParsedLogDTO parsedLogDTO = new ParsedLogDTO().fromMessage(logMessage);

        // Cron Monitoring 서버에 Notice Post
        String noticeResponse = noticeBusiness.postNotice(parsedLogDTO);

        // 해당하는 notice를 생성하지 못한 경우
        if (noticeResponse.equals("404")) {
            log.error("Fail to make notice");
            return;
        }

        // Cron Monitoring 서버에 CronProcess 생성 Post
        String cronProcessResponse = cronProcessBusiness.postCronProcess(parsedLogDTO);

        // 해당하는 CronProcess를 생성하지 못한 경우
        if (cronProcessResponse.equals("404")) {
            log.error("Fail to make Cron Process");
            return;
        }

        // 웹훅 전송
        webhookBusiness.produceWebhook(parsedLogDTO);

        // Influx DB 에 로그 저장
        cronLogBusiness.saveLog(parsedLogDTO);
    }

    /**
     * Kafka Webhook Listener
     *
     * @param request
     * @throws IOException
     */
    @KafkaListener(topics = "webhook", groupId = "webhook_consumer", containerFactory = "webhookListener")
    public void webhookConsumer(WebhookDTO.Request request) throws IOException {

        WebhookMessage webhookMessage = WebhookMessageFactory.getMessage(request);

        webhookBusiness.sendMessage(request, webhookMessage);
        log.info("Send message to {} in {}", request.getUrl(), request.getEndpoint().toString());

    }

}
