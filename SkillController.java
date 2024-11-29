package com.example.Bulk_Skill_Creation.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.Bulk_Skill_Creation.service.GenesysServices;
import com.example.Bulk_Skill_Creation.service.OrgConfigService;
import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.Configuration;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/skills")
public class SkillController {
	
	@Autowired
	private OrgConfigService orgConfigService;
	
	@Autowired
	private GenesysServices genesysService;
	
	public SkillController(GenesysServices genesysService) {
		this.genesysService = genesysService;
	}
	
	
	// Show login form
	@GetMapping("/login")
	public String showLoginForm(Model model) {
//		model.addAttribute("environment",List.of("mypurecloud.com","usw2.pure.cloud","eu.pure.cloud"));
		model.addAttribute("organizationName",orgConfigService.getAvailableOrganizations());
		return "login";
	}
	
	// Handle login
	
	@PostMapping("/login")
	public String connectTogenesys(
			@RequestParam("organizationName")String organizationName,
			@RequestParam("environment")String environment,
			RedirectAttributes redirectAttributes,
			HttpSession session,
			Model model) {
		
		try {
			
			Map<String, String> credentials = orgConfigService.getCredentials(organizationName);
			String clientId = credentials.get("clientId");
			String clientSecret = credentials.get("clientSecret");
			
			if(clientId==null || clientSecret==null || clientId.isEmpty() ||clientSecret.isEmpty()) {
				throw new IllegalArgumentException("Error: Client Credentials not found for organization "+ organizationName);
				
			}
			
			
				//initialize the Genesys API Client
			ApiClient apiClient = ApiClient.Builder.standard().withBasePath("https://api."+environment).build();
			apiClient.authorizeClientCredentials(clientId, clientSecret);
			Configuration.setDefaultApiClient(apiClient);
//			System.out.println("clientId"+clientId+" client Secret"+clientSecret);
			
			//Store credentials in the session
			session.setAttribute("organizationName", organizationName);
			session.setAttribute("environment", environment);
			session.setAttribute("credentials", credentials);
			
		//add confirmation message and redirect to the upload page
			 redirectAttributes.addFlashAttribute("confirmationMessage", "Connected to Genesys org: " + organizationName);
			 
		//save organization and environment in session or attributes for next page
		redirectAttributes.addFlashAttribute("organizationName",organizationName);
		redirectAttributes.addFlashAttribute("environment",environment);
		return "redirect:/skills/importOptions";
		
	}catch(IllegalArgumentException e) {
		model.addAttribute("errorMessage",e.getMessage());
		//re-add the available organizations and environment option after error
		 model.addAttribute("organizationName", orgConfigService.getAvailableOrganizations());
		return "login";
	}
		catch(Exception e) {
			model.addAttribute("errorMessage", "Unable to connect to Genesys: " + e.getMessage());
			 // Re-add the available organizations and environment options after error
			model.addAttribute("organizationName", orgConfigService.getAvailableOrganizations());
			model.addAttribute("errorMessage","Unable to connect to genesys:"+e.getMessage()+" Organization or Selected Environment is incorrect "+environment);
			return "login";
	}
	
		
}
	
	 // Show choose import options page
    @GetMapping("/importOptions")
    public String showChooseImportPage(@ModelAttribute("organizationName") String organizationName,
                                       @ModelAttribute("environment") String environment,
                                       @ModelAttribute("confirmationMessage") String confirmationMessage, Model model) {
        model.addAttribute("organizationName", organizationName);
        model.addAttribute("environment", environment);
        model.addAttribute("confirmationMessage", confirmationMessage); // Add confirmation message to model
        return "importOptions";
    }
	//show upload page for bulk skill
	@GetMapping("/upload")
	public String showUploadPage(@ModelAttribute("organizationName")String organizationName,
								@ModelAttribute("environment")String environment,
								@ModelAttribute("confirmationMessage")String confirmationMessage,Model model) {
		
		model.addAttribute("organizationName",organizationName);
		model.addAttribute("environment",environment);
		model.addAttribute("confirmationMessage",confirmationMessage);
		return "upload";
	}
	
	 // Show upload page for Bulk Language Skill
    @GetMapping("/uploadLanguageSkill")
    public String showUploadLanguageSkillPage(@ModelAttribute("organizationName") String organizationName,
                                              @ModelAttribute("environment") String environment,
                                              @ModelAttribute("confirmationMessage") String confirmationMessage, Model model) {
        model.addAttribute("organizationName", organizationName);
        model.addAttribute("environment", environment);
        model.addAttribute("confirmationMessage", confirmationMessage); // Add confirmation message to model
        return "uploadLanguageSkill"; // Create this page for Language Skill upload
    }

    // Show upload page for Bulk Wrap-Up Code
    @GetMapping("/uploadWrapUpCode")
    public String showUploadWrapUpCodePage(@ModelAttribute("organizationName") String organizationName,
                                           @ModelAttribute("environment") String environment,
                                           @ModelAttribute("confirmationMessage") String confirmationMessage, Model model) {
        model.addAttribute("organizationName", organizationName);
        model.addAttribute("environment", environment);
        model.addAttribute("confirmationMessage", confirmationMessage); // Add confirmation message to model
        return "uploadWrapUpCode"; // Create this page for Wrap-Up Code upload
    }
    
    @PostMapping("/upload")
    public String handleSkillFileUpload(@RequestParam("file") MultipartFile file,
                                        HttpSession session, Model model) {
        return handleFileUpload(file, session, model,"");
    }

    // Handle File Upload for Bulk Language Skills
    @PostMapping("/uploadLanguageSkill")
    public String handleLanguageSkillFileUpload(@RequestParam("file") MultipartFile file,
                                                HttpSession session, Model model) {
        return handleFileUpload(file, session, model, "LanguageSkill");
    }
	
 // Handle File Upload for Bulk Wrap-Up Code (with different validation and functionality)
    @PostMapping("/uploadWrapUpCode")
    public String handleWrapUpCodeFileUpload(@RequestParam("file") MultipartFile file,
                                              HttpSession session, Model model) {
        return handleWrapUpCodeUpload(file, session, model);
    }
    
	// Handle File Upload
	
	
	public String handleFileUpload(MultipartFile file,
			HttpSession httpSession,
			Model model,String type){
		
		

		try {
			
				String organizationName= (String) httpSession.getAttribute("organizationName");
				String environment = (String)httpSession.getAttribute("environment");
			
				if(organizationName==null || environment==null) {
					model.addAttribute("errorMessage","Session expired. Please login in again.");
					return "login";
					}
			
				@SuppressWarnings("unchecked")
				Map<String, String> credentials = (Map<String,String>) httpSession.getAttribute("credentials");
				if(credentials ==null || !orgConfigService.validateCredentials(credentials,environment)) {
						throw new IllegalArgumentException("Client credentials have changed or are invalid. Please log in again:");
					}
			
		//process the CSV file and create Skill
				List<String> results = genesysService.createSkillFromCsv(file, organizationName,environment);
		//add the confirmation message and results to the redirect attributes so it can be displayed in the success message
				model.addAttribute("confirmationMessage","Connect to Genesys org:"+organizationName);
				model.addAttribute("results",results);
				model.addAttribute("successMessage"," Upload successfully");
		
//				redirectAttributes.addFlashAttribute("results",results);
				
			}catch(IllegalArgumentException e) {
				model.addAttribute("errorMessage"+ e.getLocalizedMessage());
				return "login";
			}catch(IOException e) {
				model.addAttribute("errorMessage","File process error: "+e.getMessage());
				return "upload"+type;
			}catch(Exception e) {
				model.addAttribute("errorMessage","Error processing the files:"+e.getMessage());
				return "upload"+type;
			}
		return "upload"+type;
		}
		
	private String handleWrapUpCodeUpload(MultipartFile file, HttpSession session, Model model) {
        try {
            String organizationName = (String) session.getAttribute("organizationName");
            String environment = (String) session.getAttribute("environment");

            if (organizationName == null || environment == null) {
                model.addAttribute("errorMessage", "Session expired. Please log in again.");
                return "login";
            }

            @SuppressWarnings("unchecked")
            Map<String, String> credentials = (Map<String, String>) session.getAttribute("credentials");
            if (credentials == null || !orgConfigService.validateCredentials(credentials, environment)) {
                throw new IllegalArgumentException("Client credentials have changed or are invalid. Please log in again.");
            }

            // Custom validation and processing for Wrap-Up Code
            List<String> results = genesysService.createWrapUpCodeFromCsv(file, organizationName, environment);

            model.addAttribute("confirmationMessage", "Connected to Genesys org: " + organizationName);
            model.addAttribute("results", results);
            model.addAttribute("successMessage", "Wrap-Up Code uploaded successfully.");

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error processing the file: " + e.getMessage());
            return "uploadWrapUpCode";
        }

        return "uploadWrapUpCode";
    }
	
		
	
}
