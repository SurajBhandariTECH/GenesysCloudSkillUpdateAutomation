package com.surajDev.Bulk_Import_Feature.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.Configuration;
import com.mypurecloud.sdk.v2.api.UsersApi;
import com.mypurecloud.sdk.v2.model.UserEntityListing;
import com.opencsv.CSVWriter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Service
public class AgentExportService {
	@Autowired 
	private HttpSession session;
	
	@Autowired
	private OrgConfigService orgConfigService;
	
	public List<String> exportAgents(HttpServletResponse response,String organizationName,String environment) throws IOException, ApiException {
		
		 List<String> results = new ArrayList<>();
		 
			Map<String, String> credentials = orgConfigService.getCredentials(organizationName);
			String clientId = credentials.get("clientId");
			String redirectUri = credentials.get("redirectUri");

			if (clientId == null || redirectUri == null || credentials.isEmpty()) {
				results.add("Error: Client credentials not found for organization" + organizationName);
				return results;
			}
		
		try {
			 String accessToken = (String) session.getAttribute("accessToken");
	            if (accessToken == null || accessToken.isEmpty()) {
	                results.add("❌ ERROR: Access token is missing. Please log in again.");
	                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No access token found. Please log in.");
	                return results;
	            }
	            
	          //Initialize Genesys API Client
	    		ApiClient apiClient = ApiClient.Builder.standard().withAccessToken(accessToken).withBasePath("https://api." + environment).build();
	    		Configuration.setDefaultApiClient(apiClient);
	    		
	    		//fetch agent list from genesys cloud
	    		int pageSize = 100; // Max allowed page size
	    		int pageNumber = 1;
	    		List<String> employerInfo = Arrays.asList("locations","skills","languages");
	    		
	    		
	    		 UsersApi usersApi = new UsersApi(apiClient);
	    		 UserEntityListing agents = usersApi.getUsers(pageSize, pageNumber, null, null, null, employerInfo,
	    					null, null);
	    		 
	    		 System.out.println(agents);
	    		 
	    		 writeAgentsToCSV(response, agents);
	             results.add("✅ Agent export successfully.");
		}catch (Exception e) {
            results.add("❌ ERROR: Failed to export agents. " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }
		
			
	private void writeAgentsToCSV(HttpServletResponse response, UserEntityListing agents) {
		
		try {
			
			//set response header for csv file
			response.setContentType("text/csv");
			response.setHeader("content-Disposition", "attachment; filename = agent_export.csv");
	        
			 //write data to CSV file
			 try(CSVWriter writer = new CSVWriter(new PrintWriter(response.getWriter()))){
				 
			 String[] header = { "Name", "Email", "Title", "Department",
	                  "Location Work", "Division"};
			 
			 writer.writeNext(header);
			 
			 //write each agent details
			 for(var user:agents.getEntities()) {
				 String[] row = {
						 user.getName(),
						 user.getEmail(),
						 user.getTitle(),
						 user.getDepartment(),
						 user.getLocations().toString(),
						 (user.getDivision() !=null)? user.getDivision().getName():""
						
				 };
					writer.writeNext(row);
					
						 
						 
				 }
			 }
		}catch(Exception e) {
			 e.printStackTrace();
			
		}
	}
}
