package com.github.stella.springaoplab.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTitleRequest(
        @NotBlank @Size(max = 200) String title
) {}
