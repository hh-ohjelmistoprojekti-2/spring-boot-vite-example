package fi.haagahelia.messenger.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fi.haagahelia.messenger.dto.CreateMessageDto;
import fi.haagahelia.messenger.model.Message;
import fi.haagahelia.messenger.model.User;
import fi.haagahelia.messenger.repository.MessageRepository;
import fi.haagahelia.messenger.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
@Tag(name = "Message", description = "Operations for accessing and managing the messages")
public class MessageRestController {
	private final MessageRepository messageRepository;
	private final UserService userService;

	public MessageRestController(MessageRepository messageRepository, UserService userService) {
		this.messageRepository = messageRepository;
		this.userService = userService;
	}

	@Operation(
        summary = "Get all messages",
        description = "Returns all messages"
    )
	@GetMapping("")
	public List<Message> getAllMessages() {
		return messageRepository.findAll();
	}

	@Operation(
        summary = "Get message by id",
        description = "Returns the message associated with the provided id"
    )
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successful operation"),
		@ApiResponse(responseCode = "404", description = "Message with the provided id does not exist")
	})
	@GetMapping("/{id}")
	public Message getMessageById(@PathVariable Long id) {
		return messageRepository.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message with id " + id + " does not exist"));
	}

	@Operation(
        summary = "Create a message",
        description = "Creates a new message and returns the created message"
    )
	@PostMapping("")
	public Message createMessage(@Valid @RequestBody CreateMessageDto message, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					bindingResult.getAllErrors().get(0).getDefaultMessage());
		}

		User user = userService.getAuthenticatedUser()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));

		Message newMessage = new Message(message.content(), user);
		return messageRepository.save(newMessage);
	}
}
