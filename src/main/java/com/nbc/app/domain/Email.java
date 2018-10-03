package com.nbc.app.domain;

import java.util.List;

public class Email {

	private List<String> to;
	private String from;
	private String msgBody;
	private String subject;

	public Email(List<String> to, String from, String msgBody, String subject) {
		super();
		this.to = to;
		this.from = from;
		this.msgBody = msgBody;
		this.subject = subject;
	}

	public List<String> getTo() {
		return to;
	}

	public void setTo(List<String> to) {
		this.to = to;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getMsgBody() {
		return msgBody;
	}

	public void setMsgBody(String msgBody) {
		this.msgBody = msgBody;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

}