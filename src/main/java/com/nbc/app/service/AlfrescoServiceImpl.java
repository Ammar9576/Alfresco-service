package com.nbc.app.service;

import java.io.IOException;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nbc.app.config.AlfrescoClient;

@Service
public class AlfrescoServiceImpl implements AlfrescoService {

	private static Log logger = LogFactory.getLog(AlfrescoServiceImpl.class);

	@Value("${alfresco.userName}")
	String ALFRESCO_USERNAME;

	@Value("${alfresco.connectionName}")
	String ALFRESCO_CONNECTION_NAME;

	@Value("${alfresco.password}")
	String ALFRESCO_PASSWORD;

	@Value("${alfresco.fileDescption}")
	String ALFRESCO_FILE_DESC;

	@Autowired
	AlfrescoClient alfrescoClient;

	@Override
	public void uploadFolderToAlfresco(MultipartFile file,String ticketNumber,String folderPath) {

		Session session = alfrescoClient.getSession(ALFRESCO_CONNECTION_NAME, ALFRESCO_USERNAME, ALFRESCO_PASSWORD);
		try {
			logger.info("Uploading file to Alfresco");
			
			boolean folderFlag = alfrescoClient.checkFolderExists(session, ticketNumber, folderPath);
			if(folderFlag) {
				alfrescoClient.createFolder(session, ticketNumber, folderPath);
			}
					
			alfrescoClient.uploadDocument(session, file.getOriginalFilename(),file.getContentType(), file.getInputStream(), file.getSize(),ALFRESCO_FILE_DESC,folderPath+"/"+ticketNumber);
			
			logger.info("Document uploaded successfully");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
