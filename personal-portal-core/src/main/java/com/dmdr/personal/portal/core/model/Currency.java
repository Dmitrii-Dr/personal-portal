package com.dmdr.personal.portal.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum Currency {

	RUB("Rubles"),
	TENGE("Tenge"),
	USD("USD");

	private static final Set<String> SUPPORTED_CURRENCIES = Arrays.stream(Currency.values())
			.map(Currency::getDisplayName)
			.collect(Collectors.toSet());

	private final String displayName;

	Currency(String displayName) {
		this.displayName = displayName;
	}

	@JsonValue
	public String getDisplayName() {
		return displayName;
	}

	public static boolean isSupported(String currency) {
		return SUPPORTED_CURRENCIES.contains(currency);
	}

	@JsonCreator
	public static Currency fromString(String value) {
		if (value == null) {
			return null;
		}

		// Try to match by enum name first (RUB, TENGE, USD)
		try {
			return Currency.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			// If that fails, try to match by display name (Rubles, Tenge, USD)
			for (Currency currency : Currency.values()) {
				if (currency.displayName.equalsIgnoreCase(value)) {
					return currency;
				}
			}
			throw new IllegalArgumentException(
					"Invalid currency: " + value + ". Accepted values: " +
							Arrays.toString(Currency.values()) + " or their display names: " +
							SUPPORTED_CURRENCIES);
		}
	}
}
