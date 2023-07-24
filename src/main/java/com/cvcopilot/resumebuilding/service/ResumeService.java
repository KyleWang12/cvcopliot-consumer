package com.cvcopilot.resumebuilding.service;

import com.cvcopilot.resumebuilding.models.Modification;
import com.cvcopilot.resumebuilding.repository.ModificationRepository;
import com.cvcopilot.resumebuilding.repository.ProfileRepository;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

@Service
public class ResumeService {
    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private StateService stateService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private HashOperations<String, String, String> hashOperations;
    private ZSetOperations<String, String> zSetOperations;

    @Autowired
    private ModificationRepository modificationRepository;

    @Value("${openai.api-key}")
    private String openAIKey;

    @Value("${openai.model}")
    private String openAIModel;

    @PostConstruct
    private void init() {
        hashOperations = redisTemplate.opsForHash();
        zSetOperations = redisTemplate.opsForZSet();
    }

    private String prompt = "Based on the user's experiences, write a optimized resume according to the job description. Emit the personal information.";

    private static final Logger logger = LoggerFactory.getLogger(ResumeService.class);


    @KafkaListener(topics = "resume", groupId = "test-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload String message) {

        String userId = message.substring(0, 19);
        String modificationId = message.substring(19, 55);
        List<ChatCompletionChoice> res;

        try {
            stateService.addOrUpdateState(userId, modificationId, "in_progress");

            OpenAiService service = new OpenAiService(openAIKey, Duration.ofSeconds(120));
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

            res = service.createChatCompletion(chatCompletionRequest).getChoices();

            service.shutdownExecutor();
        } catch (RuntimeException e) {
            logger.error("RuntimeException: " + e.getMessage());
            stateService.addOrUpdateState(userId, modificationId, "failed");
            return;
        }

        try {
            // write to postgres
            modificationRepository.save(new Modification(modificationId, res.get(0).getMessage().getContent(), Long.valueOf(userId), System.currentTimeMillis()));
        } catch (RuntimeException e) {
            logger.error("Failed to write to Postgres: " + e.getMessage());
            stateService.addOrUpdateState(userId, modificationId, "failed_db_error");
            return;
        }

        // write state to redis
        stateService.addOrUpdateState(userId, modificationId, "finished");

        // invalidate cache of all results of this user
        zSetOperations.remove(userId);
    }

}