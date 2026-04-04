package com.celfinder.Procesos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${llm.provider:ollama}")
    private String llmProvider;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3.2:1b}")
    private String ollamaModel;

    @Value("${openrouter.api.key:}")
    private String openRouterApiKey;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;

    @Value("${openrouter.model:meta-llama/llama-3.3-70b-instruct}")
    private String openRouterModel;

    public AIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateText(String prompt) {
        logger.info("Generando texto con proveedor: {}", llmProvider);
        try {
            if ("openrouter".equalsIgnoreCase(llmProvider)) {
                return callOpenRouter(prompt);
            } else {
                return callOllama(prompt);
            }
        } catch (Exception e) {
            logger.error("Error en AIService ({}): {}", llmProvider, e.getMessage());
            return "Error al generar texto: " + e.getMessage();
        }
    }

    private String callOllama(String prompt) throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", ollamaModel);
        payload.put("stream", false);

        ArrayNode messages = payload.putArray("messages");
        addMessage(messages, "system", "Eres 'gorge droyd', un asistente útil dentro de Celfinder. Responde solo en español.");
        addMessage(messages, "user", prompt);

        String rawResponse = webClient.post()
            .uri(ollamaBaseUrl + "/api/chat")
            .header("Content-Type", "application/json")
            .bodyValue(payload.toString())
            .retrieve()
            .bodyToMono(String.class)
            .block();

        JsonNode json = mapper.readTree(rawResponse);
        if (json.has("message")) {
            return json.get("message").get("content").asText();
        }
        return "Respuesta inválida de Ollama: " + rawResponse;
    }

    private String callOpenRouter(String prompt) throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", openRouterModel);

        ArrayNode messages = payload.putArray("messages");
        addMessage(messages, "system", "Eres 'gorge droyd', un asistente útil de la plataforma CelFinder. Responde amablemente y en español.");
        addMessage(messages, "user", prompt);

        String rawResponse = webClient.post()
            .uri(openRouterBaseUrl + "/chat/completions")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + openRouterApiKey)
            .header("X-Title", "CelFinder Assistant")
            .bodyValue(payload.toString())
            .retrieve()
            .bodyToMono(String.class)
            .block();

        JsonNode json = mapper.readTree(rawResponse);
        if (json.has("choices") && json.get("choices").get(0).has("message")) {
            return json.get("choices").get(0).get("message").get("content").asText();
        }
        return "Respuesta inválida de OpenRouter: " + rawResponse;
    }

    private void addMessage(ArrayNode messages, String role, String content) {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", role);
        msg.put("content", content);
        messages.add(msg);
    }
}

