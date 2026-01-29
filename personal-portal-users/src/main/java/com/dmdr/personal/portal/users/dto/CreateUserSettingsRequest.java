package com.dmdr.personal.portal.users.dto;

import com.dmdr.personal.portal.core.model.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserSettingsRequest {

	@NotNull(message = "Timezone ID is required")
	private Integer timezoneId;

	private Currency currency;

	private Boolean emailNotificationEnabled;
}
