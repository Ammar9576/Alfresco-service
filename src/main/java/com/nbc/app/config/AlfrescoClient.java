package com.nbc.app.config;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.enums.*;
import org.apache.chemistry.opencmis.commons.exceptions.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;



/**
*
* @author Ammar
* @version 1.0
*/

@Component
public class AlfrescoClient {
	private static final Folder Document = null;

	private static Log logger = LogFactory.getLog(AlfrescoClient.class);
	
	@Value("${alfresco.url}")
	String ALFRESCO_URL;

	// Map with all open connections, will only be one for now
	private static Map<String, Session> connections = new ConcurrentHashMap<String, Session>();

	// Constructor
	public AlfrescoClient() {

	}

  /**
   * Get an Open CMIS session to use when talking to the Alfresco repo.
   * Will check if there is already a connection to the Alfresco repo
   * and re-use that session.
   *
   * @param connectionName the name of the new connection to be created
   * @param username       the Alfresco username to connect with
   * @param pwd            the Alfresco password to connect with
   * @return an Open CMIS Session object
   */
	public Session getSession(String connectionName, String username, String pwd) {
		Session session = connections.get(connectionName);
		if (session == null) {
			logger.info("Not connected, creating new connection to Alfresco with the connection id ("
                  + connectionName + ")");

			// No connection to Alfresco available, create a new one
			SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(SessionParameter.USER, username);
			parameters.put(SessionParameter.PASSWORD, pwd);
			parameters.put(SessionParameter.ATOMPUB_URL, ALFRESCO_URL);

			
			parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
			parameters.put(SessionParameter.COMPRESSION, "true");
			parameters.put(SessionParameter.CACHE_TTL_OBJECTS, "0");

			List<Repository> repositories = sessionFactory.getRepositories(parameters);
			Repository alfrescoRepository = null;
			if (repositories != null && repositories.size() > 0) {
				logger.info("Found (" + repositories.size() + ") Alfresco repositories");
				alfrescoRepository = repositories.get(0);
				logger.info("Info about the first Alfresco repo [ID=" + alfrescoRepository.getId() +
						"][name=" + alfrescoRepository.getName() +
						"][CMIS ver supported=" + alfrescoRepository.getCmisVersionSupported() + "]");
			} else {
				throw new CmisConnectionException(
						"Could not connect to the Alfresco Server, no repository found!");
			}

			// Create a new session with the Alfresco repository
			session = alfrescoRepository.createSession();

			// Save connection for reuse
			connections.put(connectionName, session);
		} else {
			logger.info("Already connected to Alfresco with the connection id (" + connectionName + ")");
		}

		return session;
	}
  
	/**
	 * List the repository info. Repository info is retrieved from the current session object. 
	 * 
	 * @param repositoryInfo	The repository info retrieved from session.getRepositoryInfo()
	 */
	public void listRepoCapabilities(RepositoryInfo repositoryInfo) {
		RepositoryCapabilities repoCapabilities = repositoryInfo.getCapabilities();
		logger.info("aclCapability = " + repoCapabilities.getAclCapability().name());
		logger.info("changesCapability = " + 
				repoCapabilities.getChangesCapability().name());
		logger.info("contentStreamUpdatable = " + 
				repoCapabilities.getContentStreamUpdatesCapability().name());
		logger.info("joinCapability = " + 
				repoCapabilities.getJoinCapability().name());
		logger.info("queryCapability = " + 
				repoCapabilities.getQueryCapability().name());
		logger.info("renditionCapability = " +  
				repoCapabilities.getRenditionsCapability().name());
		logger.info("allVersionsSearchable? = " + 
				repoCapabilities.isAllVersionsSearchableSupported());
		logger.info("getDescendantSupported? = " + 
				repoCapabilities.isGetDescendantsSupported());
		logger.info("getFolderTreeSupported? = " + 
				repoCapabilities.isGetFolderTreeSupported());
		logger.info("multiFilingSupported? = " + 
				repoCapabilities.isMultifilingSupported());
		logger.info("privateWorkingCopySearchable? = " + 
				repoCapabilities.isPwcSearchableSupported());
		logger.info("pwcUpdateable? = " + 
				repoCapabilities.isPwcUpdatableSupported());
		logger.info("unfilingSupported? = " + 
				repoCapabilities.isUnfilingSupported());
		logger.info("versionSpecificFilingSupported? = " + 
				repoCapabilities.isVersionSpecificFilingSupported());
	}

	/**
	 * Logs the top folder(s) of the repository.
	 * @param session Session object created when accessing the alfresco repository.
	 */
	public void listTopFolder(Session session) {
		Folder root = session.getRootFolder();
		ItemIterable<CmisObject> contentItems = root.getChildren();

		for(CmisObject contentItem : contentItems) {
			if(contentItem instanceof Document) {
				Document docMetadata = (Document) contentItem;
				ContentStream docContent = docMetadata.getContentStream();
				logger.info(docMetadata.getName() + " [size=" + docContent.getLength() + "][Mimetype=" +
						docContent.getMimeType() +"][type=" + ((CmisObjectProperties) docContent).getType().getDisplayName() + "]"
						);
			}else {
				logger.info(contentItem.getName() + "[type=" + contentItem.getType().getDisplayName()+"]");
			}
		}
	}
  
	/**
	 * Logs the top folder with Paging and Property Filters
	 * @param session Session object created to access the alfresco repository.
	 */
	public void listTopFolderWithPagingAndPropFilter(Session session) {
		Folder root = session.getRootFolder();
		OperationContext operationContext = new OperationContextImpl();
		int maxItemsPerPage = 5;
		operationContext.setMaxItemsPerPage(maxItemsPerPage);
		ItemIterable<CmisObject> contentItems = root.getChildren(operationContext);
		long numberOfPages = Math.abs(contentItems.getTotalNumItems() / maxItemsPerPage);
		int pageNumber = 1;
		boolean finishedPaging = false;
		int count = 0;
		ArrayList<CmisObject> cmisObjects = new ArrayList<CmisObject>();
		while(!finishedPaging) {
			logger.info("Page " + pageNumber + " (" + numberOfPages + ")");
			ItemIterable<CmisObject> currentPage = contentItems.skipTo(count).getPage();

			for(CmisObject contentItem : currentPage) {
				logger.info(contentItem.getName() + " [type=" + 
						contentItem.getType().getDisplayName() + "]");
				count++;
				//cmisObjects.add(contentItem);

			}
			pageNumber++;
			if(!currentPage.getHasMoreItems()) {
				finishedPaging = true;
			}
		}
	}
	
	/**
	 * Log the properties of the Cmis object passed in.
	 * @param cmistObject Alfresco object. i.e. Folder, Document, ...
	 */
	public void listProperties(CmisObject cmisObject) {
		for(Property<?> p : cmisObject.getProperties()) {
			if(PropertyType.DATETIME == p.getType()) {
				Calendar calValue = (Calendar) p.getValue();
				logger.info("  - " +p.getId()+ " = "+ (calValue != null ?
						new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"). 
						format(calValue.getTime()) : ""));
			}else {
				System.out.println("-------------------------------------------------------");
				logger.info("  - " + p.getId() + " = " + p.getValue());
				logger.info("  - " + p.getDisplayName() + " = " + p.getLocalName());
			}
		}
	}
  
	/**
	 * Log Types and Subtypes of all the session Cmis objects.
	 * @param session Session object created to access the alfresco repository.
	 */
	public void listTypesAndSubtypes(Session session) {
		boolean includePropertyDefinitions = false;
		List<Tree<ObjectType>> typeTrees = session.getTypeDescendants(null, -1, includePropertyDefinitions);
		for(Tree<ObjectType> typeTree: typeTrees) {
			logTypes(typeTree, "");
		}
	}

	private void logTypes(Tree<ObjectType> typeTree, String tab) {
		ObjectType objType = typeTree.getItem();
		String docInfo = "";
		if(objType instanceof DocumentType) {
			DocumentType docType = (DocumentType) objType;
			docInfo = "[versionable=" + docType.isVersionable() +
					"][content=" + docType.getContentStreamAllowed() + "]";
			logger.info(tab + objType.getDisplayName() +
					objType.getId() + "][fileable=" + objType.isFileable() + 
					"][queryable=" + objType.isQueryable() + "]" + docInfo);
			for (Tree<ObjectType> subTypeTree : typeTree.getChildren()) {
				logTypes(subTypeTree, tab + " ");
			}
		}
	}
	
	
	public boolean checkFolderExists(Session session,String folderName,String path) {
				
		// Check if the folder already exists
		Folder newFolder = (Folder) getObject(session, path, folderName);
		if(newFolder == null) {					
			return true;
		} else {
			logger.info("Folder already exist: " + newFolder.getPath());
			
			return false;
		}	
	}
	
	
	
	/**
	 * Create a new folder. If the folder already exists it will not create it.
	 * To create a root folder pass in the "/" for the path.
	 * To create a subfolders pass in the path i.e "folder1/folder2/folder3"
	 * @param session 		The current session for the alfresco object.
	 * @param folderName	The new folder you want to have created.
	 * @param path			The path of the folder where you want to put your new folder.
	 * @return				The new folder.
	 */
	public boolean createFolder(Session session, String folderName, String path) {
		
		Folder parentFolder = getFolder(session, path);
		
		// If the path does not exist return or is duplicate folder.
		//if(parentFolder == null) 
		//	return;
		
		// Make sure the user is allowed to create a folder
		// under the root folder
		if(parentFolder.getAllowableActions().getAllowableActions().contains(Action.CAN_CREATE_FOLDER)==false){
			throw new CmisUnauthorizedException("Current user does not have permission to create " +
					"a sub-folder in " + parentFolder.getPath());
		}

		// Check if the folder already exists, if not create it
		Folder newFolder = (Folder) getObject(session, path, folderName);
		if(newFolder == null) {
			Map<String, Object> newFolderProps = new HashMap<String, Object>();
			newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
			newFolderProps.put(PropertyIds.NAME, folderName);
			newFolder = parentFolder.createFolder(newFolderProps);
			logger.info("Created new folder: " + newFolder.getPath() + " [creator=" + newFolder.getCreatedBy() + "][created=" +
						date2String(newFolder.getCreationDate().getTime()) + "]");
			
			return true;
		} else {
			logger.info("Folder already exist: " + newFolder.getPath());
			
			return false;
		}
	}
	
	/**
	 * Returns the folder for the particular path if it exists. Otherwise the method returns null.
	 * @param session 	The current Alfresco session.
	 * @param path		The path of the folder that you are trying to get.
	 * @return			The folder for the path you passed in. Otherwise null.
	 */
	public Folder getFolder(Session session, String path) {
		Folder folder = null;
		try {
			// Get the path for the folder.
			folder = (Folder) session.getObjectByPath(path);
		}catch(CmisObjectNotFoundException e) {
			logger.info("Folder already exists: " + path);
		}
		
		return folder;
	}
	
	
	/**
	 * Upload a document on a particular path.
	 * @param session The current session with the Alfresco object.
	 * @param fileName		The name of the file retrieved from the Part object in HTTP servlet post.
	 * @param mimeType		The type of the file retrieved from the Part object in HTTP servlet post.
	 * @param fileContent	The inputstream of the file retrieved from the Part object in HTTP servlet.
	 * @param fileSize		The size of the file retrieved from the Part object in HTTP servlet.
	 * @param description	The description of the file. From a form parameter in the post request.
	 * @param path			The folder path to place the file.		
	 * @throws IOException
	 */
	public void uploadDocument(Session session, String fileName, String mimeType, InputStream fileContent, 
						long fileSize, String description, String path) throws IOException {

		Folder parentFolder = getFolder(session, path);
		
		
		// Make sure the user is allowed to create a document
		// in the passed in folder
		if(parentFolder.getAllowableActions().getAllowableActions().contains(Action.CAN_CREATE_DOCUMENT)==false){
			throw new CmisUnauthorizedException("Current user does not have permission to " +
					"create a document in " + parentFolder.getPath());
		}

		// Check if document already exists, if not create it
		Document newDocument = (Document) getObject(session, path, fileName);
		if(newDocument == null) {
			// Setup document metadata
			Map<String, Object> newDocumentProps = new HashMap<String, Object>();
			newDocumentProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
			newDocumentProps.put(PropertyIds.NAME, fileName);
			newDocumentProps.put(PropertyIds.DESCRIPTION, description);
			

			ContentStream contentStream = session.getObjectFactory().createContentStream(fileName, fileSize, mimeType, fileContent);


			// Create versioned document object
			newDocument = parentFolder.createDocument(newDocumentProps, contentStream, VersioningState.MAJOR);
			logger.info("Created new document: " + 
					getDocumentPath(newDocument) + " [version=" + 
					newDocument.getVersionLabel() + "][creator=" + 
					newDocument.getCreatedBy() + "][created=" + 
					date2String(newDocument.getCreationDate().getTime())+"]");
		} else {
			logger.info("Document already exist: " + 
					getDocumentPath(newDocument));
		}


	}
  
	/**
	 * Update the name of the folder. 
	 * @param session 		The current session opened on the Alfresco repository.
	 * @param path			The path to the current folder.
	 * @param newFolderName	The new name of the folder.
	 * @return
	 */
	public void updateFolder(Session session, String path, String newFolderName) {
		Folder updatedFolder = null;
		Folder folder = getFolder(session, path);
		
		// If we got a folder update the name of it
		if (folder != null) {
			// Make sure the user is allowed to update folder properties
			if (folder.getAllowableActions().getAllowableActions().contains(Action.CAN_UPDATE_PROPERTIES) == false) {
				throw new CmisUnauthorizedException(
						"Current user does not have permission to update " + 
					    "folder properties for " + folder.getPath());
			}

			// Update the folder with a new name
			String oldName = folder.getName();
			Map<String, Object> newFolderProps = new HashMap<String, Object>();
			newFolderProps.put(PropertyIds.NAME, newFolderName);
			updatedFolder = (Folder) folder.updateProperties(newFolderProps);

			logger.info("Updated " + oldName + " with new name: " + 
					updatedFolder.getPath() + " [creator=" + updatedFolder.getCreatedBy() + "][created=" +
					date2String(updatedFolder.getCreationDate().getTime()) + "][modifier=" + updatedFolder.getLastModifiedBy() + "][modified="+date2String(updatedFolder.
							getLastModificationDate().getTime()) + "]");
		} else {
			logger.error("Folder to update is null!");
		}

	}
  	
  	/**
  	 * Update a text document with new text.
  	 * @param session			The current alfresco session.
  	 * @param path				The path of the document you would like to update.
  	 * @param documentName		The name of the document you want to update.
  	 * @param newDocumentText	The new text of the document.
  	 * @throws IOException
  	 */
  	public void updateDocument(Session session, String path, String documentName, String newDocumentText) throws IOException {
  		RepositoryInfo repoInfo = session.getRepositoryInfo();
  		if (!repoInfo.getCapabilities().getContentStreamUpdatesCapability().equals(CapabilityContentStreamUpdates.ANYTIME)) {
  			    logger.warn("Updating content stream without a checkout is" +
  			    " not supported by this repository [repoName=" +
  			    repoInfo.getProductName() + "][repoVersion=" + 
  			    repoInfo.getProductVersion() + "]");
  		}

  		Document document = (Document) getObject(session, path, documentName);
  		// Make sure we got a document, then update it
  		Document updatedDocument = null;
  		if (document != null) {
  			// Make sure the user is allowed to update the content 
  			// for this document
  			if (document.getAllowableActions().getAllowableActions().contains(Action.CAN_SET_CONTENT_STREAM) == false) {
  				throw new CmisUnauthorizedException("Current user does not" 
  						+ " have permission to set/update content stream for " +
  						getDocumentPath(document));
  			}

  			String mimetype = "text/plain; charset=UTF-8";
  			byte[] bytes = newDocumentText.getBytes("UTF-8");
  			ByteArrayInputStream input = new ByteArrayInputStream(bytes);
  			ContentStream contentStream = session.getObjectFactory().createContentStream(document.getName(), bytes.length, mimetype, input);


  			boolean overwriteContent = true;
  			updatedDocument = document.setContentStream(contentStream, overwriteContent);
  			if (updatedDocument == null) {
  				logger.info("No new version was created when content stream was updated for " + getDocumentPath(document));
  				updatedDocument = document;
  			}

  			logger.info("Updated content for document: " + getDocumentPath(updatedDocument) +
  					" [version=" + updatedDocument.getVersionLabel() + "][modifier=" + updatedDocument.getLastModifiedBy() +
  					"][modified=" + date2String(updatedDocument.
  							getLastModificationDate().getTime()) + "]");
  		} else {
  			logger.info("Document is null, cannot update it!");
  		}
  	}

  	/**
  	 * Delete the document passed in.
  	 * @param session		The current alfresco session.
  	 * @param documentName	The name of the document to be deleted.
  	 * @param path			The path of the document to be deleted.
  	 */
  	public void deleteDocument(Session session, String documentName, String path) {
  		
  		Document document = (Document) getObject(session, path, documentName);

  		// If we got a document try and delete it
  		if (document != null) {
  			// Make sure the user is allowed to delete the document
  			if (document.getAllowableActions().getAllowableActions().
  					contains(Action.CAN_DELETE_OBJECT) == false) {
  				throw new CmisUnauthorizedException("Current user does " + 
  						"not have permission to delete document " +
  						document.getName()+" with Object ID "+document.getId());
  			}

  			String docPath = getDocumentPath(document);
  			boolean deleteAllVersions = true;
  			document.delete(deleteAllVersions);
  			logger.info("Deleted document: " + docPath);
  		} else {
  			logger.info("Cannot delete document as it is null!");
  		}
  	}

  	/**
  	 * Delete the folder passed in.
  	 * @param session	The current alfresco session.
  	 * @param path		The path of the folder to be deleted. The last part of the path is the folder to be deleted. 
  	 */
  	public void deleteFolder(Session session, String path) {
  		Folder folder = getFolder(session, path);
  		UnfileObject unfileMode = UnfileObject.UNFILE;
  		RepositoryInfo repoInfo = session.getRepositoryInfo();
  		if (!repoInfo.getCapabilities().isUnfilingSupported()) {
  			logger.warn("The repository does not support unfiling a document from a folder, documents will " +
  					"be deleted completely from all associated folders " + "[repoName=" + repoInfo.getProductName() + "][repoVersion=" + repoInfo.getProductVersion() + "]");
  			unfileMode = UnfileObject.DELETE;
  		}

  		
  		if (folder != null) {
  			// Make sure the user is allowed to delete the folder
  			if (folder.getAllowableActions().getAllowableActions().contains(Action.CAN_DELETE_TREE) == false) {
  				throw new CmisUnauthorizedException("Current user does" + 
  						" not have permission to delete folder tree" + path);
  			}

  			boolean deleteAllVersions = true;
  			boolean continueOnFailure = true;
  			List<String> failedObjectIds = folder.deleteTree(deleteAllVersions, unfileMode, continueOnFailure);
  			logger.info("Deleted folder and all its content: " + folder.getName());
  			
  			if (failedObjectIds != null && failedObjectIds.size() > 1) {
  				for (String failedObjectId : failedObjectIds) {
  					logger.info("Could not delete Alfresco node with Node Ref: " + failedObjectId);
  				}
  			}
  		} else {
  			logger.info("Did not delete folder as it does not exist: " + path);
  		}
  	}
  	
  	/**
  	 * Grabs the content of a document.
  	 * @param session		The current Alfresco session.
  	 * @param documentName	The name of the document you want retrieved. 
  	 * @param path			The path the document is on.
  	 * @return				The document you are retrieving from the alfresco server. 
  	 */
  	public InputStream getDocument(Session session, String documentName, String path) {

  		Document document = (Document) getObject(session, path, documentName);
  		InputStream input = null;
  		if (document != null) {
  			// Make sure the user is allowed to get the 
  			// content stream (bytes) for the document
  			if (document.getAllowableActions().getAllowableActions().contains(Action.CAN_GET_CONTENT_STREAM) == false) {
  				throw new CmisUnauthorizedException("Current user does not have permission to get the" + 
  						" content stream for " + path);
  			}

  			// Get the object content stream and write to 
  			input = document.getContentStream().getStream();
  			logger.info("Grabbing document stream and returning " + documentName);
  		} else {
  			logger.error("Template document could not be found: " +
  					path);
  		}
  		return input;
  	}

  	/**
  	 * Copy the document from one folder to the other. 
  	 * @param session
  	 * @param path
  	 * @param documentName
  	 * @param destinationFolder
  	 */
  	public void copyDocument(Session session, String path, String documentName, String destinationFolder) {
  	  Folder parentFolder = getFolder(session, path);
  	  Document document = (Document) getObject(session, path, documentName);
  	  Folder destFolder = getFolder(session, destinationFolder);

  	  if (destFolder == null) {
  	    logger.error("Cannot copy " + document.getName() + ", could not find folder with the name " +
  	    destFolder.getName().toString() + ", are you using Alfresco?");
  	    return;
  	  }

  	  // Check that we got the document, then copy
  	  if (document != null) {
  	    try {
  	      document.copy(destFolder);
  	      logger.info("Copied document " + document.getName() + "from folder " + parentFolder.getPath() +
  	      " to folder " + destFolder.getPath());
  	    } catch (CmisContentAlreadyExistsException e) {
  	      logger.error("Cannot copy document " + document.getName() +
  	      ", already exist in to folder " + 
  	      destFolder.getPath());
  	    }
  	  } else {
  	    logger.error("Document is null, cannot copy to " + destinationFolder);
  	  }
  	}
  	
  	public CmisObject getObject(Session session, String path, String objectName) {
  		CmisObject object = null;
  		try {
  			String path2Object = path;
  			if(!path2Object.endsWith("/")){
  				path2Object += "/";
  			}
  			path2Object += objectName;
  			object = session.getObjectByPath(path2Object);
  		}catch (CmisObjectNotFoundException nfe0){

  		}

  		return object;
  	}

  	private String date2String(Date date) {
  		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(date);
  	}
  	
	/*
	 * Finds the absolute repository path for a document.
	 */
	private String getDocumentPath(Document document) {
		String path2Doc = getParentFolderPath(document);
		if (!path2Doc.endsWith("/")) {
			path2Doc += "/";
		}
		path2Doc += document.getName();
		return path2Doc;
	}
  	
	  
	  private String getParentFolderPath(Document document) {
		  Folder parentFolder = getDocumentParentFolder(document);
		  return parentFolder == null ? "Un-filed": parentFolder.getPath();
	  }
	  
	  private Folder getDocumentParentFolder(Document document) {
		  // Get all the parent folders (could be more than on if multi-filed)
		  List<Folder> parentFolders = document.getParents();
		  
		  // Grab the first parent folder
		  if(parentFolders.size() > 0) {
			  if(parentFolders.size() > 1) {
				  logger.info("The " + document.getName() + "has more than one parent folder, it is multi-filed");
				  
			  }
			  return parentFolders.get(0);
		  }else {
			  logger.info("Document " + document.getName() + "is un-filed and does not have a parent folder");
			  return null;
		  }
	  }

}
