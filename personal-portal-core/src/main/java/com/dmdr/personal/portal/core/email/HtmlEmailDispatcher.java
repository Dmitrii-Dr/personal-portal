package com.dmdr.personal.portal.core.email;

/**
 * Sends a single-part HTML email using configured from-address properties.
 */
public interface HtmlEmailDispatcher {

    /**
     * @param to recipient address
     * @param subject email subject (plain text, Russian for this application)
     * @param htmlBody HTML body
     */
    void sendHtml(String to, String subject, String htmlBody) throws Exception;
}
