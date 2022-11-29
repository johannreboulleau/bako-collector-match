package com.bakoconsigne.bako_collector_match.clients;

import android.util.Log;
import com.bakoconsigne.bako_collector_match.MainActivity;
import com.bakoconsigne.bako_collector_match.dto.BoxTypeDto;
import com.bakoconsigne.bako_collector_match.dto.DepositFormDto;
import com.bakoconsigne.bako_collector_match.dto.LoginDto;
import com.bakoconsigne.bako_collector_match.dto.LoginResponseDto;
import com.bakoconsigne.bako_collector_match.dto.MonitoringDto;
import com.bakoconsigne.bako_collector_match.dto.ResetStockDto;
import com.bakoconsigne.bako_collector_match.dto.StockCollectorDTO;
import com.bakoconsigne.bako_collector_match.dto.UserDto;
import com.bakoconsigne.bako_collector_match.exceptions.BadRequestException;
import com.bakoconsigne.bako_collector_match.exceptions.InternalServerException;
import com.bakoconsigne.bako_collector_match.exceptions.UnauthorizedException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

import static com.bakoconsigne.bako_collector_match.MainActivity.LOGGER_TAG;

/**
 * Web client to call main API
 */
public class BakoAdminClient {

    private static final String ADMIN_URL = "https://administration.bako-consigne.fr/api";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String BEARER = "Bearer ";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private String token;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OkHttpClient client;

    /**
     * Constructor
     */
    public BakoAdminClient() {

        this.client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request();

                // try the request
                Response response = chain.proceed(request);

                int       tryCount = 0;
                final int maxLimit = 3; //Set your max limit here

                while (!response.isSuccessful() && tryCount < maxLimit && response.code() >= 500) {

                    Log.d("intercept", "Request failed - " + tryCount);

                    tryCount++;

                    // retry the request
                    response = chain.proceed(request);
                }

                // otherwise just pass the original response on
                return response;
            })
            .build();
    }

    public BakoAdminClient(final String token) {
        this();
        this.token = token;
    }

    /**
     * Set a new token to call API
     *
     * @param token
     *     The new token
     */
    public void setToken(final String token) {
        this.token = token;
    }


    public List<BoxTypeDto> getListTypeBox() throws IOException {

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
            Log.e(LOGGER_TAG, "Error to getListTypeBox - response = " + response.code() + " - " + response.body().string());
            throw new InternalServerException();
        }
    }

    public boolean depositBoxes(final DepositFormDto depositFormDto) throws IOException {

        String json = objectMapper.writeValueAsString(depositFormDto);

        Request request = new Request.Builder()
            .header(HEADER_AUTHORIZATION, BEARER + token)
            .url(ADMIN_URL + "/collector-sites/deposit")
            .post(RequestBody.create(json, JSON))
            .build();

        return getBooleanResponse(request);
    }

    public StockCollectorDTO getStock(final String id) throws IOException {

        Request request = new Request.Builder()
            .header(HEADER_AUTHORIZATION, BEARER + token)
            .url(ADMIN_URL + "/collector-sites/" + id + "/stock")
            .get()
            .build();

        return getResponse(request, StockCollectorDTO.class);
    }

    /**
     * Login with username, password and then get token
     *
     * @param loginDto
     *     {@link LoginDto}
     *
     * @return LoginResponseDto
     *
     * @throws IOException
     *     I/O Exception
     */
    public LoginResponseDto login(final LoginDto loginDto) throws IOException {

        String json = objectMapper.writeValueAsString(loginDto);

        Request request = new Request.Builder()
            .url(ADMIN_URL + "/authenticate")
            .post(RequestBody.create(json, JSON))
            .build();

        return getResponse(request, LoginResponseDto.class);
    }

    /**
     * Get current User with token
     *
     * @param token
     *     Token
     *
     * @return UserDto
     *
     * @throws IOException
     *     I/O Exception
     */
    public UserDto account(final String token) throws IOException {

        Request request = new Request.Builder()
            .header(HEADER_AUTHORIZATION, BEARER + token)
            .url(ADMIN_URL + "/account")
            .build();

        return getResponse(request, UserDto.class);
    }

    /**
     * Post status of collector for monitoring.
     *
     * @param monitoringDto
     *     the {@link MonitoringDto}
     *
     * @return true if ok
     *
     * @throws IOException
     *     I/O Exception
     */
    public boolean monitoring(final MonitoringDto monitoringDto) throws IOException {

        String json = objectMapper.writeValueAsString(monitoringDto);

        Request request = new Request.Builder()
            .header(HEADER_AUTHORIZATION, BEARER + token)
            .url(ADMIN_URL + "/collector-sites/monitoring")
            .post(RequestBody.create(json, JSON))
            .build();

        return getBooleanResponse(request);
    }

    /**
     * Post reset stock.
     *
     * @param resetStockDto
     *     the {@link ResetStockDto}
     *
     * @return true if ok
     *
     * @throws IOException
     *     I/O Exception
     */
    public boolean resetStock(final ResetStockDto resetStockDto) throws IOException {

        String json = objectMapper.writeValueAsString(resetStockDto);

        Request request = new Request.Builder()
            .header(HEADER_AUTHORIZATION, BEARER + token)
            .url(ADMIN_URL + "/collector-sites/resetStock")
            .post(RequestBody.create(json, JSON))
            .build();

        return getBooleanResponse(request);
    }

    private boolean getBooleanResponse(final Request request) throws IOException {
        try (final Response response = this.client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return true;
            } else if (response.code() == 400) {
                Log.e(MainActivity.LOGGER_TAG, getBody(response));
                throw new BadRequestException();
            } else if (response.code() == 401) {
                Log.e(MainActivity.LOGGER_TAG, getBody(response));
                throw new UnauthorizedException();
            } else {
                Log.e(MainActivity.LOGGER_TAG, getBody(response));
                throw new InternalServerException();
            }
        }
    }

    private <T> T getResponse(final Request request, Class<T> _class) throws IOException {
        try (final Response response = this.client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return this.objectMapper.readValue(getBody(response), _class);
            } else if (response.code() == 400) {
                throw new BadRequestException();
            } else if (response.code() == 401) {
                throw new UnauthorizedException();
            } else {
                throw new InternalServerException();
            }
        }
    }

    private String getBody(final Response response) {
        if (response.body() != null) {
            try {
                return response.body().string();
            } catch (IOException e) {
                return "";
            }
        }
        return "";
    }
}
