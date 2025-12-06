package com.dmdr.personal.portal.core.model;

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

	public String getDisplayName() {
		return displayName;
	}

	public static boolean isSupported(String currency) {
		return SUPPORTED_CURRENCIES.contains(currency);
	}
}

