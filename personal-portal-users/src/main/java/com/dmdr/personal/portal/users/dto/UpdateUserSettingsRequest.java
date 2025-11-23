package com.dmdr.personal.portal.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserSettingsRequest {
	
	@NotBlank(message = "Timezone is required")
	@Size(max = 50, message = "Timezone must be at most 50 characters")
	private String timezone;

	@NotBlank(message = "Language is required")
	@Size(max = 10, message = "Language must be at most 10 characters")
	private String language;
}
