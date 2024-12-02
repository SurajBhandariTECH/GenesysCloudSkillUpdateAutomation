package com.example.Bulk_Skill_Creation.service;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.Configuration;
import com.mypurecloud.sdk.v2.api.RoutingApi;
import com.mypurecloud.sdk.v2.model.Language;

@Service
public class LanguageSkillServices {
	
	@Autowired
	private OrgConfigService orgConfigService;
	
	
	public List<String> createLanguageSkillFromCsv(MultipartFile csvFile, String organizationName, String environment)throws IOException{//An InputStreamReader is a bridge from byte streams to character streams: It reads bytes and decodes them into characters using a specified charset. The charset that it uses may be specified by name or may be given explicitly, or the platform's default charset may be accepted.
		
		List<String> results = new ArrayList<>();
		
		//retrieve clientId and clientSecret for the specified organization
		Map<String, String> credentials = orgConfigService.getCredentials(organizationName);
		String clientId = credentials.get("clientId");
		String clientSecret = credentials.get("clientSecret");
		
//		System.out.println("clientId"+clientId+" client Secret"+clientSecret);
		
		
		if(clientId==null || clientSecret==null) {
			results.add("Error: Client Credentials not found for organization"+organizationName);
			return results;
		}
		
		
		try {
			//initialize the Genesys API Client
			ApiClient apiClient = ApiClient.Builder.standard().withBasePath("https://api."+environment).build();
			apiClient.authorizeClientCredentials(clientId, clientSecret);
			Configuration.setDefaultApiClient(apiClient);
			
			RoutingApi routingApi = new RoutingApi(apiClient);
			
			//parse the csv to extract skill names starting from second row
			
			List<String> languageSkillNames = parseCSVForLanguageSkills(csvFile);
			
			//handle validation error from parsing
			for (String languageSkillName: languageSkillNames) {
				
				if(languageSkillName.startsWith("Error: ")) {
						results.add(languageSkillName);
						continue;
				}
				
				Language languageSkill = new Language();  // language skill object
				try {
					// create language
					languageSkill.setName(languageSkillName);
					routingApi.postRoutingLanguages(languageSkill);
					results.add("Successfully created Skill:"+languageSkillName);
				}catch (ApiException e) {
					results.add("Falied to create skill: "+languageSkillName+" (already exist or other "+e.getMessage()+" )");
				}
			}
			
		}catch(Exception e) {
			results.add("Error initializing Genesys API client: "+e.getMessage());	
		}
		
		return results;
	}
	
	private List<String> parseCSVForLanguageSkills(MultipartFile file )throws IOException{
		
		List <String> languageSkillNames = new ArrayList<>();
			//allowed: letters, digits, underscore and space
			String languageSkillNamePattern = "^[a-zA-Z0-9_ ]+$";
		
		try(InputStream inputStream = file.getInputStream();
				Scanner scanner = new Scanner(inputStream,"UTF-8")){
			
			//skip and validate the header row
			if(scanner.hasNextLine()) {
				String headerLine = scanner.nextLine();
				headerLine = headerLine.replace("\uFEFF","");
				String[] headers = headerLine.split(",");
				System.out.println(headers[0].trim());
				
				//validate the header
				if(headers.length==0 || !headers[0].equalsIgnoreCase("LanguageSkill Name")) {
					throw new IllegalArgumentException("Invalid File Format: The first cell of first row must be 'LanguageSkill Name'.");
					
				}
			}
			
			//read the validate each subsequent row
			int rowNumber = 1;
			while(scanner.hasNextLine()) {
				rowNumber++;
				String line = scanner.nextLine();
				String[] columns = line.split(",");
				
				if(columns.length>0 && !columns[0].trim().isEmpty()) {
					String languageSkillName = columns[0].trim();
					
					if(languageSkillName.isEmpty()) {
						continue;
					}
					
					if(!languageSkillName.matches(languageSkillNamePattern)) {
						languageSkillNames.add("Error: Invalid Language skill Name '"+languageSkillName+"' at row "+rowNumber);
						continue;
					}
					languageSkillNames.add(languageSkillName); //add valid skill name
				
				}
			}
			
		}
		return languageSkillNames;
	
	}
}
