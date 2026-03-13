package fi.haagahelia.messenger.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.haagahelia.messenger.dto.CreateMessageDto;
import fi.haagahelia.messenger.dto.RegisterUserDto;
import fi.haagahelia.messenger.model.Message;
import fi.haagahelia.messenger.model.User;
import fi.haagahelia.messenger.repository.MessageRepository;
import fi.haagahelia.messenger.repository.UserRepository;
import fi.haagahelia.messenger.service.JwtService;
import fi.haagahelia.messenger.service.UserService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
public class MessageRestControllerTest {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();
    private User authenticatedUser;
    private String authorizationHeader;

    @Autowired
    public MessageRestControllerTest(MessageRepository messageRepository, UserRepository userRepository,
            UserService userService, JwtService jwtService, MockMvc mockMvc) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtService = jwtService;
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() throws Exception {
        messageRepository.deleteAll();
        userRepository.deleteAll();

        this.authenticatedUser = userService.registerUser(new RegisterUserDto("johndoe", "john123"));
        this.authorizationHeader = "Bearer "
                + jwtService.getAccessToken(authenticatedUser.getUsername()).accessToken();
    }

    @Test
    public void getAllMessagesReturnsEmptyListWhenNoMessagesExist() throws Exception {
        this.mockMvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getAllMessagesReturnsListOfMessagesWhenMessagesExist() throws Exception {
        Message firstMessage = new Message("First message", null);
        Message secondMessage = new Message("Second message", null);
        messageRepository.saveAll(List.of(firstMessage, secondMessage));

        this.mockMvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("First message"))
                .andExpect(jsonPath("$[0].id").value(firstMessage.getId()))
                .andExpect(jsonPath("$[1].content").value("Second message"))
                .andExpect(jsonPath("$[1].id").value(secondMessage.getId()));
    }

    @Test
    public void getMessageByIdReturnsMessageWhenMessageExists() throws Exception {
        Message message = new Message("Message", null);
        messageRepository.save(message);

        this.mockMvc.perform(get("/api/messages/" + message.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Message"))
                .andExpect(jsonPath("$.id").value(message.getId()));
    }

    @Test
    public void getMessageByIdReturnsNotFoundWhenMessageDoesNotExist() throws Exception {
        this.mockMvc.perform(get("/api/messages/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void createMessageSavesValidMessage() throws Exception {
        CreateMessageDto message = new CreateMessageDto("Hello world!");
        String requestBody = mapper.writeValueAsString(message);

        this.mockMvc
                .perform(post("/api/messages").header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello world!"));

        List<Message> messages = messageRepository.findAll();
        assertEquals(1, messages.size());
        assertEquals("Hello world!", messages.get(0).getContent());
        assertEquals(authenticatedUser.getId(), messages.get(0).getUser().getId());
    }

    @Test
    public void createMessageDoesNotSaveInvalidMessage() throws Exception {
        CreateMessageDto message = new CreateMessageDto("");
        String requestBody = mapper.writeValueAsString(message);

        this.mockMvc
                .perform(post("/api/messages").header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest());

        List<Message> messages = messageRepository.findAll();
        assertEquals(0, messages.size());
    }
}
