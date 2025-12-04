package com.dmdr.personal.portal.booking.service;

import com.dmdr.personal.portal.booking.dto.SessionTypeResponse;
import com.dmdr.personal.portal.booking.dto.CreateSessionTypeRequest;
import com.dmdr.personal.portal.booking.dto.UpdateSessionTypeRequest;
import java.util.List;

public interface SessionTypeService {
	List<SessionTypeResponse> getAll();
	List<SessionTypeResponse> getAllIncludingInactive();
	SessionTypeResponse create(CreateSessionTypeRequest request);
	SessionTypeResponse update(Long id, UpdateSessionTypeRequest request);
	void delete(Long id);
}

