package com.dmdr.personal.portal.users.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserProfileRequest {

    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;

    @PositiveOrZero(message = "Avatar id must be greater than or equal to 0")
    @Max(value = 32767, message = "Avatar id must be less than or equal to 32767")
    private Short avatarId;
}
