package com.bulkupdateuser.bulkupdate.service;


import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.Configuration;
import com.mypurecloud.sdk.v2.api.UsersApi;
import com.mypurecloud.sdk.v2.model.UpdateUser;
import com.mypurecloud.sdk.v2.model.User;
import com.mypurecloud.sdk.v2.model.UserEntityListing;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();

            // Validate headers
            if (headerLine == null || !validateHeaders(headerLine)) {
                throw new IllegalArgumentException("Invalid File Format: Headers must be 'username', 'officialName', 'employeeId'.");
            }

            List<String[]> csvData = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns.length >= 3 && !columns[0].trim().isEmpty() && !columns[1].trim().isEmpty() && !columns[2].trim().isEmpty()) {
                    csvData.add(new String[]{columns[0].trim(), columns[1].trim(), columns[2].trim()});
                } else {
                    result.append("Skipped row: Missing values or incorrect format.\n");
                }
            }

            // Process updates concurrently
            ExecutorService executorService = Executors.newFixedThreadPool(10); // Allow 10 parallel threads
            for (String[] userData : csvData) {
                executorService.execute(() -> {
                    try {
                        String username = userData[0];
                        String officialName = userData[1];
                        String employeeId = userData[2];

                        // Fetch user by username
                        User user = getUserByUsername(usersApi, username);
                        System.out.println(user);
                        if (user == null) {
                            synchronized (result) {
                                result.append("User not found for username ").append(username).append(".\n");
                            }
                            return;
                        }

                        // Update employerInfo
                        UpdateUser updateUser = new UpdateUser();
                        updateUser.setEmployerInfo(user.getEmployerInfo());
                        updateUser.getEmployerInfo().setOfficialName(officialName);
                        updateUser.getEmployerInfo().setEmployeeId(employeeId);

                        usersApi.patchUser(user.getId(), updateUser);
                        synchronized (result) {
                            result.append("Updated user ").append(username).append(".\n");
                        }

                    } catch (ApiException | IOException e) {
                        synchronized (result) {
                            result.append("Failed to update user ").append(userData[0]).append(" (Error: ").append(e.getMessage()).append(").\n");
                        }
                    }
                });
            }

            executorService.shutdown();
            while (!executorService.isTerminated()) {
                // Wait for all tasks to finish
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing CSV file: " + e.getMessage());
        }

        return result.toString();
    }

    private User getUserByUsername(UsersApi usersApi, String username) throws ApiException, IOException {
        UserEntityListing userList = usersApi.getUsers(25, 1, null, null, "username:" + username, null, null, null);
        if (userList.getEntities() != null && !userList.getEntities().isEmpty()) {
            return userList.getEntities().get(0); // Return the first matched user
        }
        return null; // User not found
    }

    private boolean validateHeaders(String headerLine) {
        String[] headers = headerLine.split(",");
        return headers.length >= 3 &&
                headers[0].equalsIgnoreCase("username") &&
                headers[1].equalsIgnoreCase("officialName") &&
                headers[2].equalsIgnoreCase("employeeId");
    }
}

