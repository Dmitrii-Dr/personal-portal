package com.dmdr.personal.portal.controller.admin;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Builds non-sensitive request context strings for admin API INFO logs.
 */
final class AdminApiLogSupport {

	private AdminApiLogSupport() {
	}

	static String http(HttpServletRequest request) {
		return "method=" + request.getMethod() + " path=" + request.getRequestURI();
	}
}
