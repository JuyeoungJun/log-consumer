package gabia.logConsumer.util;

import gabia.logConsumer.dto.HiworksDTO;
import gabia.logConsumer.dto.SlackDTO;
import gabia.logConsumer.dto.SlackDTO.Attachment;
import gabia.logConsumer.dto.WebhookDTO;
import gabia.logConsumer.dto.WebhookMessage;
import gabia.logConsumer.entity.Enum.WebhookEndpoint;

public class WebhookMessageFactory {

    public static WebhookMessage getMessage(WebhookDTO.Request request) {
        WebhookMessage webhookMessage = null;

        if (request.getEndpoint().equals(WebhookEndpoint.SLACK)) {
            webhookMessage = new SlackDTO();
            webhookMessage.setText(request.getText());

            Attachment attachment = Attachment.builder()
                .title("Cron Status")
                .text(request.getText())
                .authorName("Cron Monitoring Server")
                .build();

            ((SlackDTO) webhookMessage).addAttachment(attachment);

        } else if (request.getEndpoint().equals(WebhookEndpoint.HIWORKS)) {

            webhookMessage = new HiworksDTO();
            webhookMessage.setText(request.getText());
        }

        return webhookMessage;
    }

}
