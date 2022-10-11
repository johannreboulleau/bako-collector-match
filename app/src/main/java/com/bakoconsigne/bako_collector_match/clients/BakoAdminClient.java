package com.bakoconsigne.bako_collector_match.clients;

import android.util.Log;
import com.bakoconsigne.bako_collector_match.MainActivity;
import com.bakoconsigne.bako_collector_match.dto.BoxTypeDto;
import com.bakoconsigne.bako_collector_match.dto.ConsumerUserDto;
import com.bakoconsigne.bako_collector_match.dto.DepositFormDto;
import com.bakoconsigne.bako_collector_match.dto.LoginDto;
import com.bakoconsigne.bako_collector_match.dto.LoginResponseDto;
import com.bakoconsigne.bako_collector_match.dto.StockCollectorDTO;
import com.bakoconsigne.bako_collector_match.dto.UserDto;
import com.bakoconsigne.bako_collector_match.exceptions.BadRequestException;
import com.bakoconsigne.bako_collector_match.exceptions.InternalServerException;
import com.bakoconsigne.bako_collector_match.exceptions.UnauthorizedException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.List;

public class BakoAdminClient {

    private static final String ADMIN_URL = "https://administration.bako-consigne.fr/api";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String BEARER = "Bearer ";

    private String token;

    public BakoAdminClient(final String token) {
        this.token = token;
    }

    /**
     * Set a new token to call API
     *
     * @param token The new token
     */
    public void setToken(final String token) {
        this.token = token;
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public List<BoxTypeDto> getListTypeBox() throws IOException {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .header(HEADER_AUTHORIZATION, BEARER + token)
            .url(ADMIN_URL + "/box-types")
            .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            return objectMapper.readValue(response.body().string(), new TypeReference<List<BoxTypeDto>>() {
            });
        } else if (response.code() == 400) {
            throw new BadRequestException();
        } else if (response.code() == 401) {
            throw new UnauthorizedException();
        } else {
            Log.e(MainActivity.LOGGER_TAG, "Error to getListTypeBox - response = " + response.code() + " - " + response.body().string());
            throw new InternalServerException();
        }
    }

    public boolean depositBoxes(final DepositFormDto depositFormDto) throws IOException {

        OkHttpClient client = new OkHttpClient();

        String json = objectMapper.writeValueAsString(depositFormDto);

        Request request = new Request.Builder()
            .header(HEADER_AUTHORIZATION, BEARER + token)
            .url(ADMIN_URL + "/collector-sites/deposit")
            .post(RequestBody.create(MediaType.parse("application/json"), json))
            .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            return true;
        } else if (response.code() == 400) {
            throw new BadRequestException(response.body().string());
        } else if (response.code() == 401) {
            throw new UnauthorizedException();
        } else {
            Log.e(MainActivity.LOGGER_TAG, "Error to depositBoxes - response = " + response.code() + " - " + response.body().string());
            throw new InternalServerException();
        }
    }

    public StockCollectorDTO getStock(final String id) throws IOException {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .header(HEADER_AUTHORIZATION, BEARER + token)
            .url(ADMIN_URL + "/collector-sites/" + id + "/stock")
            .get()
            .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            return objectMapper.readValue(response.body().string(), new TypeReference<StockCollectorDTO>() {
            });
        } else if (response.code() == 400) {
            throw new BadRequestException(response.body().string());
        } else if (response.code() == 401) {
            throw new UnauthorizedException();
        } else {
            Log.e(MainActivity.LOGGER_TAG, "Error to getStock - response = " + response.code() + " - " + response.body().string());
            throw new InternalServerException();
        }
    }

    /**
     * Login with username, password and then get token
     *
     * @param loginDto {@link LoginDto}
     * @return LoginResponseDto
     * @throws IOException I/O Exception
     */
    public LoginResponseDto login(final LoginDto loginDto) throws IOException {

        OkHttpClient client = new OkHttpClient();

        String json = objectMapper.writeValueAsString(loginDto);

        Request request = new Request.Builder()
            .url(ADMIN_URL + "/authenticate")
            .post(RequestBody.create(MediaType.parse("application/json"), json))
            .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            return objectMapper.readValue(response.body().string(), LoginResponseDto.class);
        } else if (response.code() == 400) {
            throw new BadRequestException();
        } else if (response.code() == 401) {
            throw new UnauthorizedException();
        } else {
            Log.e(MainActivity.LOGGER_TAG, "Error to login - response = " + response.code() + " - " + response.body().string());
            throw new InternalServerException();
        }
    }

    /**
     * Get current User with token
     *
     * @param token Token
     * @return UserDto
     * @throws IOException I/O Exception
     */
    public UserDto account(final String token) throws IOException {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .header(HEADER_AUTHORIZATION, BEARER + token)
            .url(ADMIN_URL + "/account")
            .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            return objectMapper.readValue(response.body().string(), UserDto.class);
        } else if (response.code() == 400) {
            throw new BadRequestException();
        } else if (response.code() == 401) {
            throw new UnauthorizedException();
        } else {
            Log.e(MainActivity.LOGGER_TAG, "Error to Get current User with token - response = " + response.code() + " - " + response.body().string());
            throw new InternalServerException();
        }
    }
}
