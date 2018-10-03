package com.nbc.app.service;

import org.springframework.web.multipart.MultipartFile;

public interface AlfrescoService {
	
	public void uploadFolderToAlfresco(MultipartFile file,String ticketNumber,String folderPath);

}
