package com.dmdr.personal.portal.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class CorrelationIdConfiguration {

	@Bean
	public CorrelationIdFilter correlationIdFilter() {
		return new CorrelationIdFilter();
	}

	@Bean
	public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter correlationIdFilter) {
		FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(correlationIdFilter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		registration.addUrlPatterns("/*");
		return registration;
	}
}
