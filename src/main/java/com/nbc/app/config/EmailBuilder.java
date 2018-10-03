package com.nbc.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.jayway.jsonpath.PathNotFoundException;
import com.nbc.app.domain.Email;

import java.util.Arrays;
import java.util.List;

@Component
public class EmailBuilder {

	@Value("${notification.email.recipient}")
	private String recipient;

	@Value("${notification.email.sender}")
	private String sender;

	@Value("${notification.email.message}")
	private String message;

	@Value("${notification.email.subject}")
	String mailsubject;

	public Email build() {
		try {
			List<String> to = Arrays.asList(recipient.split(","));
			String from = sender;
			String subject = mailsubject;
			String templatedMessage = message;
			return new Email(to, from, templatedMessage, subject);
		} catch (PathNotFoundException pathNotFoundException) {
			throw new IllegalArgumentException("A required field was missing from the JSON payload");
		} catch (ClassCastException classNotFoundException) {
			throw new IllegalArgumentException("The wrong type was supplied for a required field");
		}
	}
}
