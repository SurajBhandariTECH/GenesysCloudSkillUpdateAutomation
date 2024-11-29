package com.example.Bulk_Skill_Creation.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mypurecloud.sdk.v2.ApiClient;

import jakarta.annotation.PostConstruct;

@Service
public class OrgConfigService {
	
	private final Map<String, Map<String, String>> orgConfig= new HashMap<>();
	
	public List<String> getAvailableOrganizations(){
		List<String> organizationName = new ArrayList<>();
		try {
				//load and parse the xml file
			ClassPathResource resource = new ClassPathResource("org-config.xml");
			if(!resource.exists()) {
					throw new IllegalStateException("Org-config.xml not found in the classpath");
				}
			InputStream inputStream = resource.getInputStream();
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(inputStream);
			
			NodeList organizations = document.getElementsByTagName("organization");
			
			for(int i=0; i<organizations.getLength();i++) {
				Element orgElement = (Element) organizations.item(i);
				
				String name = orgElement.getElementsByTagName("name").item(0).getTextContent().trim();
				organizationName.add(name);
				}
			}catch(Exception e) {
			 e.printStackTrace();
			}
		return organizationName;
	}
	
	@PostConstruct
	public void loadConfig() throws Exception {
		
		
		//use classpathresource to load from resource folder
		ClassPathResource resource = new ClassPathResource("org-config.xml");
		if(!resource.exists()) {
			throw new IllegalStateException("Org-config.xml not found in the classpath");
		}
		InputStream inputStream = resource.getInputStream();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(inputStream);
		
		NodeList organizations = document.getElementsByTagName("organization");
		
		for(int i=0; i<organizations.getLength();i++) {
			Element orgElement = (Element) organizations.item(i);
			
			String name = orgElement.getElementsByTagName("name").item(0).getTextContent().trim();
			String clientId = orgElement.getElementsByTagName("clientId").item(0).getTextContent().trim();
			String clientSecret = orgElement.getElementsByTagName("clientSecret").item(0).getTextContent().trim();
			
//			System.out.println("sssss"+clientId);
			Map<String, String> credentials = new HashMap<>();
			credentials.put("clientId", clientId);
			credentials.put("clientSecret", clientSecret);
			orgConfig.put(name, credentials);
		}
	}
	
	public boolean validateCredentials(Map<String,String> credentials, String environment) {
		try {
			String clientId = credentials.get("clientId");
			String clientSecret = credentials.get("clientSecret");
			
			ApiClient apiClient=ApiClient.Builder.standard().withBasePath("https://api."+environment).build();
			apiClient.authorizeClientCredentials(clientId, clientSecret);
			
			//if no exception occurs, credentials are valid
			return true;
		}catch(Exception e) {
			//if exception occur, credentials are invalid
			return false;
		}
	}
	
	public Map<String,String> getCredentials(String orgName){
		return orgConfig.get(orgName);
	}
}
