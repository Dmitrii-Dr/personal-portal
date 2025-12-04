package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.SessionTypeResponse;
import com.dmdr.personal.portal.booking.dto.CreateSessionTypeRequest;
import com.dmdr.personal.portal.booking.dto.UpdateSessionTypeRequest;
import com.dmdr.personal.portal.booking.model.SessionType;
import com.dmdr.personal.portal.booking.repository.BookingRepository;
import com.dmdr.personal.portal.booking.repository.SessionTypeRepository;
import com.dmdr.personal.portal.booking.service.SessionTypeService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionTypeServiceImpl implements SessionTypeService {

	private final SessionTypeRepository repository;
	private final BookingRepository bookingRepository;

	public SessionTypeServiceImpl(SessionTypeRepository repository, BookingRepository bookingRepository) {
		this.repository = repository;
		this.bookingRepository = bookingRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SessionTypeResponse> getAll() {
		return repository.findAll().stream()
			.map(SessionTypeServiceImpl::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public SessionTypeResponse create(CreateSessionTypeRequest request) {
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
		entity.setPrices(request.getPrices());
		SessionType saved = repository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		SessionType sessionType = repository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("SessionType not found: " + id));

		long bookingCount = bookingRepository.countBySessionTypeId(id);
		if (bookingCount > 0) {
			throw new IllegalArgumentException(
				String.format("Cannot delete session type '%s' (ID: %d) because it is referenced by %d booking(s). " +
					"Please delete or update all related bookings first.", sessionType.getName(), id, bookingCount)
			);
		}

		repository.deleteById(id);
	}

	private static SessionTypeResponse toResponse(SessionType entity) {
		SessionTypeResponse resp = new SessionTypeResponse();
		resp.setId(entity.getId());
		resp.setName(entity.getName());
		resp.setDescription(entity.getDescription());
		resp.setDurationMinutes(entity.getDurationMinutes());
		resp.setBufferMinutes(entity.getBufferMinutes());
		resp.setPrices(entity.getPrices());
		return resp;
	}
}

