package com.dmdr.personal.portal.users.dto;

import com.dmdr.personal.portal.core.model.Currency;
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

	private Currency currency;

	private Boolean emailNotificationEnabled;
}
