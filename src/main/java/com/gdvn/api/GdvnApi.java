package com.gdvn.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GdvnApi {
    private static final String BASE_URL = "https://api.gdvn.net";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    public static JsonObject createOtp() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/otp"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to create OTP: HTTP " + response.statusCode());
        }
        return GSON.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject verifyOtp(String code) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/otp/" + code))
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to verify OTP: HTTP " + response.statusCode());
        }
        return GSON.fromJson(response.body(), JsonObject.class);
    }

    public static PlayerData getPlayerInfo(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/me"))
                .GET()
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get player info: HTTP " + response.statusCode());
        }
        return GSON.fromJson(response.body(), PlayerData.class);
    }

    public static void deleteApiKey(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/APIKey"))
                .DELETE()
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new Exception("Failed to delete API key: HTTP " + response.statusCode());
        }
    }
}
