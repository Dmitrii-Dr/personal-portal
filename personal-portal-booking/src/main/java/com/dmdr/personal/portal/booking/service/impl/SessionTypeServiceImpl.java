package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.SessionTypeResponse;
import com.dmdr.personal.portal.booking.dto.CreateSessionTypeRequest;
import com.dmdr.personal.portal.booking.dto.UpdateSessionTypeRequest;
import com.dmdr.personal.portal.booking.model.SessionType;
import com.dmdr.personal.portal.booking.repository.SessionTypeRepository;
import com.dmdr.personal.portal.booking.service.SessionTypeService;
import com.dmdr.personal.portal.core.model.Currency;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionTypeServiceImpl implements SessionTypeService {

	private final SessionTypeRepository repository;

	public SessionTypeServiceImpl(SessionTypeRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SessionTypeResponse> getAll() {
		return repository.findByActiveTrue().stream()
			.map(SessionTypeServiceImpl::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<SessionTypeResponse> getAllIncludingInactive() {
		return repository.findAll().stream()
			.sorted(Comparator.comparing(SessionType::isActive).reversed())
			.map(SessionTypeServiceImpl::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public SessionTypeResponse create(CreateSessionTypeRequest request) {
		validatePrices(request.getPrices());

		SessionType entity = new SessionType();
		entity.setName(request.getName());
		entity.setDescription(request.getDescription());
		entity.setDurationMinutes(request.getDurationMinutes());
		entity.setBufferMinutes(request.getBufferMinutes());
		entity.setPrices(request.getPrices());
		SessionType saved = repository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public SessionTypeResponse update(Long id, UpdateSessionTypeRequest request) {
		SessionType entity = repository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("SessionType not found: " + id));
		entity.setName(request.getName());
		entity.setDescription(request.getDescription());
		entity.setDurationMinutes(request.getDurationMinutes());
		entity.setBufferMinutes(request.getBufferMinutes());
		validatePrices(request.getPrices());
		entity.setPrices(request.getPrices());
		if (request.getActive() != null) {
			entity.setActive(request.getActive());
		}
		SessionType saved = repository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		repository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("SessionType not found: " + id));
		repository.deleteById(id);
	}

	private static void validatePrices(Map<String, BigDecimal> prices) {
		// Prices map must not be null
		if (prices == null) {
			throw new IllegalArgumentException("Prices map is required and cannot be null");
		}

		// Get all required currency display names
		Set<String> requiredCurrencies = Arrays.stream(Currency.values())
			.map(Currency::getDisplayName)
			.collect(Collectors.toSet());

		// Check that all currencies are present
		Set<String> missingCurrencies = requiredCurrencies.stream()
			.filter(currency -> !prices.containsKey(currency))
			.collect(Collectors.toSet());

		if (!missingCurrencies.isEmpty()) {
			throw new IllegalArgumentException(
				"All currencies must be specified. Missing currencies: " + missingCurrencies
			);
		}

		// Check that all provided currencies are valid
		Set<String> invalidCurrencies = prices.keySet().stream()
			.filter(currency -> !Currency.isSupported(currency))
			.collect(Collectors.toSet());

		if (!invalidCurrencies.isEmpty()) {
			throw new IllegalArgumentException(
				"Invalid currencies: " + invalidCurrencies
			);
		}

		// Check that no null values are present (0 is allowed)
		for (Map.Entry<String, BigDecimal> entry : prices.entrySet()) {
			if (entry.getValue() == null) {
				throw new IllegalArgumentException(
					"Price value cannot be null for currency: " + entry.getKey() + ". Use 0 instead."
				);
			}
		}
	}

	private static SessionTypeResponse toResponse(SessionType entity) {
		SessionTypeResponse resp = new SessionTypeResponse();
		resp.setId(entity.getId());
		resp.setName(entity.getName());
		resp.setDescription(entity.getDescription());
		resp.setDurationMinutes(entity.getDurationMinutes());
		resp.setBufferMinutes(entity.getBufferMinutes());
		resp.setPrices(entity.getPrices());
		resp.setActive(entity.isActive());
		return resp;
	}
}

