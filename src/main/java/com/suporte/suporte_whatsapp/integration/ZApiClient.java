package com.suporte.suporte_whatsapp.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP para a API da Z-API (gateway WhatsApp).
 *
 * Responsável exclusivamente por realizar as chamadas de envio de mensagens.
 * Toda falha de comunicação é tratada internamente — nunca propaga exceção
 * para o chamador, garantindo que uma falha no WhatsApp não reverta a
 * persistência da mensagem no banco de dados.
 *
 * Configuração necessária em application.yml:
 * 
 * <pre>
 * zapi:
 *   base-url: https://api.z-api.io
 *   instance-id: SEU_INSTANCE_ID
 *   token: SEU_TOKEN
 *   phone-number: "5565999990000"
 *   client-token: SEU_CLIENT_TOKEN  # opcional
 * </pre>
 */
@Slf4j
@Service
public class ZApiClient {

    private static final String PATH_SEND_TEXT = "/instances/{instanceId}/token/{token}/send-text";
    private static final String PATH_SEND_IMAGE = "/instances/{instanceId}/token/{token}/send-image";
    private static final String PATH_SEND_AUDIO = "/instances/{instanceId}/token/{token}/send-audio";
    private static final String PATH_SEND_VIDEO = "/instances/{instanceId}/token/{token}/send-video";

    @Value("${zapi.base-url}")
    private String baseUrl;

    @Value("${zapi.instance-id}")
    private String instanceId;

    @Value("${zapi.token}")
    private String token;

    /**
     * Client-Token: header adicional de segurança exigido pela Z-API
     * em contas que habilitaram a verificação de segurança avançada.
     * Pode ser deixado vazio se não utilizado na conta.
     */
    @Value("${zapi.client-token:}")
    private String clientToken;

    private final RestTemplate restTemplate;

    /**
     * No Spring Boot 3.x, connectTimeout() e readTimeout() foram removidos do
     * RestTemplateBuilder. A configuração de timeouts é feita diretamente via
     * SimpleClientHttpRequestFactory (valores em milissegundos).
     */
    public ZApiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000); // 5 segundos para estabelecer conexão
        factory.setReadTimeout(10_000); // 10 segundos aguardando resposta
        this.restTemplate = new RestTemplate(factory);
    }

    // =========================================================================
    // Métodos públicos de envio
    // =========================================================================

    /**
     * Envia uma mensagem de texto para o número informado.
     *
     * @param telefone Número do destinatário no formato internacional sem "+":
     *                 "5565999990000"
     * @param texto    Conteúdo da mensagem
     */
    public void enviarTexto(String telefone, String texto) {
        ZApiEnvioRequest.Texto payload = ZApiEnvioRequest.Texto.builder()
                .phone(telefone)
                .message(texto)
                .build();

        enviar(PATH_SEND_TEXT, payload, "texto", telefone);
    }

    /**
     * Envia uma imagem com legenda opcional.
     *
     * @param telefone Número do destinatário
     * @param imageUrl URL pública da imagem (HTTPS)
     * @param caption  Legenda exibida abaixo da imagem (pode ser null)
     */
    public void enviarImagem(String telefone, String imageUrl, String caption) {
        ZApiEnvioRequest.Imagem payload = ZApiEnvioRequest.Imagem.builder()
                .phone(telefone)
                .image(imageUrl)
                .caption(caption)
                .build();

        enviar(PATH_SEND_IMAGE, payload, "imagem", telefone);
    }

    /**
     * Envia um áudio que aparece como mensagem de voz no WhatsApp.
     *
     * @param telefone Número do destinatário
     * @param audioUrl URL pública do arquivo de áudio (mp3 ou ogg)
     */
    public void enviarAudio(String telefone, String audioUrl) {
        ZApiEnvioRequest.Audio payload = ZApiEnvioRequest.Audio.builder()
                .phone(telefone)
                .audio(audioUrl)
                .build();

        enviar(PATH_SEND_AUDIO, payload, "audio", telefone);
    }

    /**
     * Envia um vídeo com legenda opcional.
     *
     * @param telefone Número do destinatário
     * @param videoUrl URL pública do arquivo de vídeo (mp4)
     * @param caption  Legenda exibida abaixo do vídeo (pode ser null)
     */
    public void enviarVideo(String telefone, String videoUrl, String caption) {
        ZApiEnvioRequest.Video payload = ZApiEnvioRequest.Video.builder()
                .phone(telefone)
                .video(videoUrl)
                .caption(caption)
                .build();

        enviar(PATH_SEND_VIDEO, payload, "video", telefone);
    }

    // =========================================================================
    // Método interno de despacho
    // =========================================================================

    /**
     * Realiza o POST para a Z-API e trata todos os cenários de falha.
     *
     * A falha é apenas logada — nunca relançada — para que o contexto
     * transacional do ChatService não seja revertido por um problema
     * externo à aplicação (rede, Z-API indisponível, etc.).
     *
     * @param pathTemplate Caminho do endpoint com placeholders {instanceId} e
     *                     {token}
     * @param payload      Objeto que será serializado como JSON no body
     * @param tipoMidia    Nome amigável do tipo (usado apenas no log)
     * @param telefone     Número do destinatário (truncado no log por LGPD)
     */
    private void enviar(String pathTemplate, Object payload, String tipoMidia, String telefone) {
        String url = baseUrl + pathTemplate
                .replace("{instanceId}", instanceId)
                .replace("{token}", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (clientToken != null && !clientToken.isBlank()) {
            headers.set("Client-Token", clientToken);
        }

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[Z-API] Mensagem de {} enviada com sucesso para número terminado em {}",
                        tipoMidia, ultimos4Digitos(telefone));
            } else {
                log.warn("[Z-API] Resposta inesperada ao enviar {} para número terminado em {}: status={}",
                        tipoMidia, ultimos4Digitos(telefone), response.getStatusCode());
            }

        } catch (RestClientException ex) {
            // Loga sem expor o telefone completo (LGPD) e sem relançar a exceção
            log.error("[Z-API] Falha ao enviar mensagem de {} para número terminado em {}: {}",
                    tipoMidia, ultimos4Digitos(telefone), ex.getMessage());
        }
    }

    /**
     * Retorna apenas os 4 últimos dígitos do telefone para os logs,
     * evitando exposição de dados pessoais conforme a LGPD.
     */
    private String ultimos4Digitos(String telefone) {
        if (telefone == null || telefone.length() < 4)
            return "????";
        return "..." + telefone.substring(telefone.length() - 4);
    }
}