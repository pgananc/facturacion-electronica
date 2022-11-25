package com.wallparisoft.ebill.utils.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MailDto {

	private String from;
	private String to;
	private String subject;
	private String templateHtml;
	private Map<String, Object> model;
	

}
