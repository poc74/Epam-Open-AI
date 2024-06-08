package com.ravi;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.google.gson.Gson;
import com.ravi.request.Message;
import com.ravi.request.OpenAIMessages;
import com.ravi.response.ChatCompletionChunk;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;

@Controller
@Component
public class OpenAIController {

    private  OpenAIClient openAIClient;

    public OpenAIController(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    private final Logger LOGGER = LoggerFactory.getLogger(OpenAIController.class);

    @Value("${azure.openai.url}")
    private String OPENAI_URL;

    @Value  ("${azure.openai.model.path}")
    private String MODEL_URI_PATH;

    @Value("${azure.openai.api.key}")
    private String OPENAI_API_KEY;

    private static Map<UUID, Sinks.Many<String>> userSinks;

    private WebClient webClient;

    static {
        userSinks = new ConcurrentHashMap<>();
    }

    @PostConstruct
    private void init() {
        // WebClient
        webClient = WebClient.builder().baseUrl(OPENAI_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .defaultHeader("api-key", OPENAI_API_KEY).build();
    }

    // index.html
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping(path = "/openai-gpt4-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<String> sseStream(@RequestParam UUID userId) {
        Sinks.Many<String> userSink = getUserSink(userId);
        if (userSink == null) {
            userSink = createUserSink(userId);
        }
        LOGGER.debug("USER ID IS ADDED: {}}", userId);
        return userSink.asFlux().delayElements(Duration.ofMillis(10));
    }

    @PostMapping("/openai-gpt4-submit1234")
    @ResponseBody
    public Mono<ResponseEntity<String>> openaiGpt4Sse(@RequestBody String inputText, @RequestParam UUID userId) {
        LOGGER.debug(inputText);
        OpenAIMessages messages = createMessages(inputText);

        Sinks.Many<String> userSink = getUserSink(userId);

        return webClient.post()
                .uri(MODEL_URI_PATH)
                .body(BodyInserters.fromValue(messages))
                .retrieve()
                .bodyToMono(String.class)
                .map(data -> {
                    sleepPreventFromOverflow();

                    if (data.contains("[DONE]")) {
                        LOGGER.debug("DONE");
                    } else {
                        invokeOpenAIAndSendMessageToClient(userSink, data);
                    }

                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(data);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/openai-gpt4-submit234")
    @ResponseBody
    public ResponseEntity<String> openaiGpt4Sse243(@RequestBody String inputText, @RequestParam UUID userId) {
        LOGGER.debug(inputText);
        OpenAIMessages messages = createMessages(inputText);

        Sinks.Many<String> userSink = getUserSink(userId);

        String responseData = webClient.post()
                .uri(MODEL_URI_PATH)
                .body(BodyInserters.fromValue(messages))
                .retrieve()
                .bodyToMono(String.class)
                .map(data -> {
                    sleepPreventFromOverflow();

                    if (data.contains("[DONE]")) {
                        LOGGER.debug("DONE");
                    } else {
                        invokeOpenAIAndSendMessageToClient(userSink, data);
                    }

                    return data;
                })
                .block(); // This will block until the response is available

        if (responseData != null) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(responseData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/openai-gpt4-submit")
    @ResponseBody
    public void openaiGpt4Sse1(@RequestBody String inputText, @RequestParam UUID userId) {
        LOGGER.debug(inputText);
        OpenAIMessages messages = createMessages(inputText);

        Sinks.Many<String> userSink = getUserSink(userId);
        webClient.post().uri(MODEL_URI_PATH).body(BodyInserters.fromValue(messages)).retrieve()
                .bodyToFlux(String.class).subscribe(data -> {
                    sleepPreventFromOverflow();

                    if (data.contains("[DONE]")) {
                        LOGGER.debug("DONE");
                    } else {
                        invokeOpenAIAndSendMessageToClient(userSink, data);
                    }
                });
    }

    private void invokeOpenAIAndSendMessageToClient(Sinks.Many<String> userSink, String data) {
        Gson gson = new Gson();
        ChatCompletionChunk inputData = gson.fromJson(data, ChatCompletionChunk.class);

        if (inputData.getChoices().get(0).getFinish_reason() != null
                && inputData.getChoices().get(0).getFinish_reason().equals("stop")) {
        } else {
            String returnValue = inputData.getChoices().get(0).getDelta().getContent();
            if (returnValue != null) {
                LOGGER.debug(returnValue);
                returnValue = returnValue.replace(" ", "<SPECIAL_WHITE_SPACEr>");
                returnValue = returnValue.replace("\n", "<SPECIAL_LINE_SEPARATOR>");
                LOGGER.debug(returnValue);
                EmitResult result = userSink.tryEmitNext(returnValue);
                showDetailErrorReasonForSSE(result, returnValue, data);
            } else {
                userSink.tryEmitNext("");
            }
        }
    }

    private void showDetailErrorReasonForSSE(EmitResult result, String returnValue, String data) {
        if (result.isFailure()) {
            LOGGER.error("Failure: {}", returnValue + " " + data);
            if (result == EmitResult.FAIL_OVERFLOW) {
                LOGGER.error("Overflow: {}", returnValue + " " + data);
            } else if (result == EmitResult.FAIL_NON_SERIALIZED) {
                LOGGER.error("Non-serialized: {}", returnValue + " " + data);
            } else if (result == EmitResult.FAIL_ZERO_SUBSCRIBER) {
                LOGGER.error("Zero subscriber: {}", returnValue + " " + data);
            } else if (result == EmitResult.FAIL_TERMINATED) {
                LOGGER.error("Terminated: {}", returnValue + " " + data);
            } else if (result == EmitResult.FAIL_CANCELLED) {
                LOGGER.error("Cancelled: {}", returnValue + " " + data);
            }
        }
    }

    private void sleepPreventFromOverflow() {
        try {
            TimeUnit.MILLISECONDS.sleep(20);
        } catch (InterruptedException e) {
            LOGGER.warn("Thread Intrrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private Sinks.Many<String> createUserSink(UUID userId) {
        Sinks.Many<String> userSink = Sinks.many().multicast().directBestEffort();
        userSinks.put(userId, userSink);
        return userSink;
    }

    private Sinks.Many<String> getUserSink(UUID userId) {
        return userSinks.get(userId);
    }

    private OpenAIMessages createMessages(String inputText) {

        List<Message> messages = new ArrayList<>();
        Message message = new Message();
        message.setRole("system");
        Message message6 = new Message();
        message6.setRole("user");
     //   message6.setContent();
        message6.setContent(openAIClient.callIndo() + "Read above code and find out the probable exception logic method and class name"+inputText);
//        message6.setContent(OpenAIClient.callIndo() + "Read above code and GZIP With decompressing and decoding these strings and find out the probable exception/error logic method and class name "+inputText);

        messages.add(message6);

        OpenAIMessages oaimessage = new OpenAIMessages();
        oaimessage.setMessages(messages);
        oaimessage.setMax_tokens(3000);
        oaimessage.setTemperature(0.1);
        oaimessage.setFrequency_penalty(0);
        oaimessage.setPresence_penalty(0);
        oaimessage.setTop_p(0.95);
        oaimessage.setStop(null);
        oaimessage.setStream(true);

        Gson gson = new Gson();
        LOGGER.debug(gson.toJson("REQUESTED JSON: " + messages));

        return oaimessage;
    }
}
