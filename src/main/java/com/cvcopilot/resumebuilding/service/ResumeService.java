package com.cvcopilot.resumebuilding.service;

import com.cvcopilot.resumebuilding.repository.ProfileRepository;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.image.CreateImageRequest;

@Service
public class ResumeService {
    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ModificationService modificationService;

    @Value("${openai.api-key}")
    private String openAIKey;

    @Value("${openai.model}")
    private String openAIModel;

    private String prompt = "Based on the user's experiences, write a optimized resume according to the job description. Emit the personal information.";

    private static final Logger logger = LoggerFactory.getLogger(ResumeService.class);


    @KafkaListener(topics = "resume", groupId = "test-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload String message){

        logger.debug(String.format("$$ -> Consumed Message -> %s", message));
        String userId = message.substring(0, 19);
        String modificationId = message.substring(19, 55);

        modificationService.addOrUpdateModification(userId, modificationId, "in_progress", "#");

        OpenAiService service = new OpenAiService(openAIKey, Duration.ofSeconds(120));
        System.out.println("chat completion...");
        List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are a hr from big tech company.");
        final ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), message.substring(56) + prompt);
        messages.add(systemMessage);
        messages.add(userMessage);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
            .builder()
            .model("gpt-3.5-turbo")
            .messages(messages)
            .n(1)
            .maxTokens(600)
            .logitBias(new HashMap<>())
            .build();

        List<ChatCompletionChoice> res = service.createChatCompletion(chatCompletionRequest)
            .getChoices();
//        for (ChatCompletionChoice re : res) {
//            System.out.println(re.getMessage().getContent());
//            System.out.println(re.getFinishReason());
//        }

        service.shutdownExecutor();
        modificationService.addOrUpdateModification(userId, modificationId, "finished", res.get(0).getMessage().getContent());
    }



}
