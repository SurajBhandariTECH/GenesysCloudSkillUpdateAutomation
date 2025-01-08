package com.bulkupdateuser.bulkupdate.UserController;


import com.bulkupdateuser.bulkupdate.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/upload")
    public String showUploadPage(Model model) {
        model.addAttribute("confirmationMessage", "Connected to Genesys");
        return "uploadUser";
    }

    @PostMapping("/upload")
    public String handleUserUpdate(@RequestParam("file") MultipartFile file, Model model) {
        try {
            // Process the Excel file and update users
            String results = userService.updateUsersFromCsv(file);
            model.addAttribute("successMessage", results);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "uploadUser";
    }
}
