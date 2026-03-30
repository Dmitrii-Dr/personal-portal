package com.dmdr.personal.portal.admin.observability;

import java.util.concurrent.ThreadPoolExecutor;
import com.dmdr.personal.portal.admin.observability.capture.RequestLoggingFilter;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Binds {@link RequestLogObservabilityProperties} and provides the async executor used for request-log writes.
 * Scheduled retention/rollup jobs should use {@code zone = "UTC"} on {@code @Scheduled} with the cron strings from properties.
 */
@AutoConfiguration
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@EnableConfigurationProperties(RequestLogObservabilityProperties.class)
@EnableAsync
@EnableScheduling
@Slf4j
public class RequestLogObservabilityAutoConfiguration {

	public static final String REQUEST_LOG_TASK_EXECUTOR_BEAN_NAME = "requestLogTaskExecutor";
	public static final int REQUEST_LOGGING_FILTER_ORDER_AFTER_SECURITY = -90;

	@Bean(name = REQUEST_LOG_TASK_EXECUTOR_BEAN_NAME)
	public ThreadPoolTaskExecutor requestLogTaskExecutor(RequestLogObservabilityProperties properties) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.getAsyncCorePoolSize());
		executor.setMaxPoolSize(properties.getAsyncMaxPoolSize());
		executor.setQueueCapacity(properties.getAsyncQueueCapacity());
		executor.setThreadNamePrefix("request-log-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(false);
		executor.setAcceptTasksAfterContextClose(false);
		executor.initialize();
		log.info(
			"Request-log async executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
			properties.getAsyncCorePoolSize(),
			properties.getAsyncMaxPoolSize(),
			properties.getAsyncQueueCapacity()
		);
		return executor;
	}

	@Bean
	public Clock requestLogClock() {
		return Clock.systemUTC();
	}

	@Bean
	@DependsOn("entityManagerFactory")
	public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration(RequestLoggingFilter requestLoggingFilter) {
		FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>(requestLoggingFilter);
		registration.setName("requestLoggingFilter");
		registration.addUrlPatterns("/*");
		/*
		 * Ordered after Spring Security's delegated filter (default order -100),
		 * while still running early enough to include the rest of the servlet chain.
		 */
		registration.setOrder(REQUEST_LOGGING_FILTER_ORDER_AFTER_SECURITY);
		registration.setMatchAfter(false);
		return registration;
	}
}
