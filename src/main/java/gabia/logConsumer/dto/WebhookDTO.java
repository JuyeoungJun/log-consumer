package gabia.logConsumer.dto;

import gabia.logConsumer.entity.Enum.WebhookEndpoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class WebhookDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {

        String url;
        String text;
        WebhookEndpoint endpoint;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SendText {

        String text;
    }

}
