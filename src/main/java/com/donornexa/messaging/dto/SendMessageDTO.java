package com.donornexa.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageDTO {

    @NotBlank(message = "Message text cannot be empty")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String content;
}
