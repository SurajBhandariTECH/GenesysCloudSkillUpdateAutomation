package com.bulkupdateuser.bulkupdate.service;


import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.Configuration;
import com.mypurecloud.sdk.v2.api.UsersApi;
import com.mypurecloud.sdk.v2.model.EmployerInfo;
import com.mypurecloud.sdk.v2.model.UpdateUser;
import com.mypurecloud.sdk.v2.model.User;
import com.mypurecloud.sdk.v2.model.UserEntityListing;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class UserService {

    private final String clientId = "d1e380d5-aa10-4eed-908f-5625fd342f79";
    private final String clientSecret = "M6khOP5e-RpVex3GKNglaR6OqKAInZJF45NLrNDb_jY";
    private final String environment = "mypurecloud.com";

    public UserService() {
        try {
            ApiClient apiClient = ApiClient.Builder.standard()
                    .withBasePath("https://api." + environment)
                    .build();
            apiClient.authorizeClientCredentials(clientId, clientSecret);
            Configuration.setDefaultApiClient(apiClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Genesys API Client: " + e.getMessage());
        }
    }

    public String updateUsersFromCsv(MultipartFile file) throws Exception {
        UsersApi usersApi = new UsersApi();
        StringBuilder result = new StringBuilder();

        // Step 1: Fetch all users from Genesys Cloud
        Map<String, User> usersMap = fetchAllUsers(usersApi);

        // Step 2: Parse the CSV file
        List<String[]> csvData = parseCsv(file, result);

        int rowIndex = 1;
        // Step 3: Process updates sequentially
        for (String[] row : csvData) {
            String email = row[0].trim().toLowerCase();
            String officialName = row[1];
            String employeeId = row[2];

            // Check if the user exists in the map
            User matchUser = usersMap.get(email);
            System.out.println("excel email "+email);
//            System.out.println(matchUser);
           System.out.println(matchUser.getEmail());

            if (matchUser != null) {
                try {
                    // Retrieve the user version
                    Integer version = matchUser.getVersion();

                    // Prepare the user update object
                    UpdateUser updateUser = new UpdateUser();
                    updateUser.version(version);
                    if(matchUser.getEmployerInfo()==null){
                        EmployerInfo employerInfo = new EmployerInfo();
                        employerInfo.setOfficialName("");
                        employerInfo.setEmployeeId("");
                        matchUser.setEmployerInfo(employerInfo);
                    }
                    updateUser.setEmployerInfo(matchUser.getEmployerInfo());
                    updateUser.getEmployerInfo().setOfficialName(officialName);
                    updateUser.getEmployerInfo().setEmployeeId(employeeId);


                    usersApi.patchUser(matchUser.getId(),updateUser);

                    result.append("Row ").append(rowIndex).append("- Update User: ").append(matchUser.getName()).append(" Email: ").append(email).append(".\n");

                    }catch(ApiException | IOException e){
                        result.append("\nRow ").append(rowIndex).append("- Failed to update User: ").append(matchUser.getName()).append(" Email: ").append(email).append(" (Error: ").append(e.getMessage()).append("\n");
                    }
                }else {
                        result.append("\nRow ").append(rowIndex).append("- User not found for email: ").append(email).append(".\n");

                }
            rowIndex++;

        }

        return result.toString();
    }

    private Map<String, User> fetchAllUsers(UsersApi usersApi) throws ApiException, IOException {
        Map<String, User> usersMap = new HashMap<>();
        int pageSize = 10; // Max allowed page size
        int pageNumber = 1;
        List<String> employerInfo = Arrays.asList("employerInfo");

        while (true) {
            UserEntityListing userListing = usersApi.getUsers(pageSize, pageNumber, null, null, null, employerInfo, null, null);
            if (userListing.getEntities() != null) {
                for (User user : userListing.getEntities()) {
                    if (user.getEmail() != null) {
                        usersMap.put(user.getEmail().trim().toLowerCase(), user); // Use email as key
                    }
                }
            }
            if (userListing.getEntities() == null || userListing.getEntities().isEmpty()) {
                break; // No more users to fetch
            }
            pageNumber++;




        }
        return usersMap;
    }

    private List<String[]> parseCsv(MultipartFile file, StringBuilder result) throws Exception {
        List<String[]> csvData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            headerLine = headerLine.replace("\uFFFF","");

            // Validate headers
            if (headerLine == null || !validateHeaders(headerLine)) {
                throw new IllegalArgumentException("Invalid File Format: Headers must be 'email', 'officialName', 'employeeId'.");
            }

            String line;
            int rowCount = 1;
            while ((line = reader.readLine()) != null) {
                rowCount++;
                String[] columns = line.split(",");
                if (columns.length >= 3 && !columns[0].trim().isEmpty() && !columns[1].trim().isEmpty() && !columns[2].trim().isEmpty()) {
                    csvData.add(new String[]{columns[0].trim(), columns[1].trim(), columns[2].trim()});
                } else {
                    result.append("\nRow ").append(rowCount).append(": Skipped due to missing values or invalid format.\n");
                }
            }
        }
        return csvData;
    }

//    private User getUserByUsername(UsersApi usersApi, String username) throws ApiException, IOException {
//        UserEntityListing userList = usersApi.getUsers(25, 1, null, null, "username:" + username, null, null, null);
//        if (userList.getEntities() != null && !userList.getEntities().isEmpty()) {
//            return userList.getEntities().get(0); // Return the first matched user
//        }
//        return null; // User not found
//    }

    private boolean validateHeaders(String headerLine) {
        headerLine = headerLine.replace("\uFFFF","");
        String[] headers = headerLine.trim().split(",");
        return headers.length >= 3 &&
                headers[0].equalsIgnoreCase("email") &&
                headers[1].equalsIgnoreCase("officialName") &&
                headers[2].equalsIgnoreCase("employeeId");
    }
}

