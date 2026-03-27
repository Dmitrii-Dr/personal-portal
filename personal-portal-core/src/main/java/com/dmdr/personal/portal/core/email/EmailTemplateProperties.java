package com.dmdr.personal.portal.core.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Email template configuration.
 *
 * See: .cursor/plans/email_template_os_override_3482bb38.plan.md
 */
@ConfigurationProperties(prefix = "spring.mail.templates")
@Component
@Getter
@Setter
public class EmailTemplateProperties {

	/**
	 * Optional absolute path to a directory containing custom email templates.
	 * When set, templates are loaded from this directory first; missing files fall back to classpath.
	 */
	private String directory;
}

