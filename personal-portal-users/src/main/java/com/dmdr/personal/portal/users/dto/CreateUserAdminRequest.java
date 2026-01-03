package com.dmdr.personal.portal.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserAdminRequest {

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	private String email;

	@Size(max = 100, message = "First name must be at most 100 characters")
	private String firstName;

	@Size(max = 100, message = "Last name must be at most 100 characters")
	private String lastName;

	@Size(max = 20, message = "Phone number must be at most 20 characters")
	private String phoneNumber;

	@NotBlank(message = "Timezone is required")
	@Size(max = 50, message = "Timezone must be at most 50 characters")
	private String timezone;

	private Boolean emailNotificationEnabled;
}
