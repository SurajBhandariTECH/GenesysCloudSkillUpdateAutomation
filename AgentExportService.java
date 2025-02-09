package com.surajDev.Bulk_Import_Feature.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.Configuration;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class AgentExportService {
	@Autowired 
	private HttpSession session;
	
	public void exportAgents(HttpServletResponse response) throws IOException {
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
	}
}
