package com.mcardoso.srvcondominiopro.modules.notificacoes.canais;

import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Envio de email transacional via Resend (https://resend.com/docs/api-reference/emails/send-email).
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final RestClient restClient;
    private final ResendProperties properties;

    public EmailSender(RestClient.Builder builder, ResendProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public void enviar(String destinatario, String assunto, String conteudo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", properties.from());
        body.put("to", List.of(destinatario));
        body.put("subject", assunto != null && !assunto.isBlank() ? assunto : "Notificação CondomínioPro");
        body.put("text", conteudo);

        try {
            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Resend recusou o envio para {}: {} - {}",
                    destinatario, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new AppException("Resend recusou o envio: " + ex.getStatusCode().value(), HttpStatus.BAD_GATEWAY);
        } catch (RestClientException ex) {
            log.warn("Erro de comunicação com o Resend ao enviar para {}", destinatario, ex);
            throw new AppException("Falha de comunicação com o provedor de email", HttpStatus.BAD_GATEWAY);
        }
    }
}
