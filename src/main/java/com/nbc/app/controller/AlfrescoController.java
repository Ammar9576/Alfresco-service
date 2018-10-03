package com.nbc.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import com.nbc.app.service.AlfrescoService;

@RestController
public class AlfrescoController {

	public static final String BLANK = "";

	@Autowired
	AlfrescoService alfrescoService;

	@RequestMapping("/")
	@ApiIgnore
	public String welcome() {
		return "Welcome to the Alfresco Microservice";
	}
	
	@PostMapping(value = "/processData", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public void getGithubPayload(@RequestParam(value="ticketNumber",required=true)String ticketNumber,
									@RequestParam(value="folderPath",required=true)String folderPath ,
									@RequestParam(value="files",required=true) MultipartFile[]  files) {		
		if(files.length>0) {
			for(MultipartFile file : files) {	
				alfrescoService.uploadFolderToAlfresco(file,ticketNumber,folderPath);
	        }
		}

	}

	@PostMapping(value = "/test", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public void getGithubTest(@RequestParam(value="ticketNumber",required=false)String ticketNumber,
									@RequestParam(value="folderPath",required=false)String folderPath ,
									@RequestParam(value="files",required=false) MultipartFile[]  files) {
		 ticketNumber="454444";
		 folderPath="/CI/Test-ammar";
		
		if(files.length>0) {
			for(MultipartFile file : files) {	
				alfrescoService.uploadFolderToAlfresco(file,ticketNumber,folderPath);
	        }
		}

	}
}
