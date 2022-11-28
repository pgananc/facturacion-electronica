package com.wallparisoft.ebill.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@FieldDefaults(level = PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class MailDto {

	 String from;
	 String to;
	 String subject;
	 String templateHtml;
	 Map<String, Object> model;
	

}
