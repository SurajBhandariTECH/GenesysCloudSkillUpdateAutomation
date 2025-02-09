package com.surajDev.Bulk_Import_Feature.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.Configuration;
import com.mypurecloud.sdk.v2.api.UsersApi;
import com.mypurecloud.sdk.v2.model.UserEntityListing;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class AgentExportService {
	@Autowired 
	private HttpSession session;
	
	public void exportAgents(HttpServletResponse response) throws IOException, ApiException {
		//set response header for csv file
		response.setContentType("text/csv");
		response.setHeader("content-Disposition", "attachment; filename = agent_export.csv");
		
		//Get access Token for session
		
		String accessToken = (String)session.getAttribute("accesstoken");
		String environment = (String)session.getAttribute("environment");
		if(accessToken == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"no access token found. Please login in");
			return;
		}
		
		
		//Initialize Genesys API Client
		ApiClient apiClient = ApiClient.Builder.standard().withAccessToken(accessToken).withBasePath("https://api." + environment).build();
		Configuration.setDefaultApiClient(apiClient);
		
		//fetch agent list from genesys cloud
		int pageSize = 10; // Max allowed page size
		int pageNumber = 1;
		List<String> employerInfo = Arrays.asList("locations","skills","languages","authorization");
		
		 UsersApi usersApi = new UsersApi(apiClient);
		 UserEntityListing agents = usersApi.getUsers(pageSize, pageNumber, null, null, null, employerInfo,
					null, null);
		 
		 String[] header = { "Name", "Email", "Title", "Department",
                  "Location Work", "Role Divisions", "Queues", "Division", "Skill" };
		 
		 //write each agent details
		 for(var user:agents.getEntities()) {
			 String[] row = {
					 user.getName(),
					 user.getEmail(),
					 user.getTitle(),
					 user.getDepartment(),
					 user.getLocations().toString(),
					 user.getAuthorization().roles()
					 
					 
			 }
		 }
	}
}
