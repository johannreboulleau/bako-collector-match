package com.bakoconsigne.bako_collector_match.services;

import android.util.Log;
import com.bakoconsigne.bako_collector_match.clients.BakoAdminClient;
import com.bakoconsigne.bako_collector_match.dto.BoxTypeDto;
import com.bakoconsigne.bako_collector_match.dto.DepositFormDto;
import com.bakoconsigne.bako_collector_match.dto.LoginDto;
import com.bakoconsigne.bako_collector_match.dto.LoginResponseDto;
import com.bakoconsigne.bako_collector_match.dto.MonitoringDto;
import com.bakoconsigne.bako_collector_match.dto.StockCollectorDTO;
import com.bakoconsigne.bako_collector_match.dto.UserDto;
import com.bakoconsigne.bako_collector_match.exceptions.BoxTypeSettingsException;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bakoconsigne.bako_collector_match.MainActivity.LOGGER_TAG;

/**
 * Collector main service
 */
public class CollectorService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static final CollectorService INSTANCE = new CollectorService();

    private BakoAdminClient client;

    private final List<BoxTypeDto> boxTypeList = new ArrayList<>();

    private final Map<String, Integer> mapBox = new HashMap<>();

    private String siteId;

    private StockCollectorDTO stockCollector;

    /**
     * number of opened drawer
     */
    @Getter
    @Setter
    private Integer numDrawer;

    /**
     * private constructor
     */
    private CollectorService() {
    }

    /**
     * Get list fo box
     *
     * @return List of {@link BoxTypeDto}
     *
     * @throws IOException
     *     I/O Exception
     */
    public List<BoxTypeDto> loadListBoxType() throws IOException {
        if (this.boxTypeList.isEmpty()) {
            this.boxTypeList.addAll(client.getListTypeBox());
        }
        return this.boxTypeList;
    }

    /**
     * Return a singleton of {@link CollectorService}
     *
     * @return an instance of {@link CollectorService}
     */
    public static CollectorService getInstance() {
        return INSTANCE;
    }

    /**
     * Return a singleton of {@link CollectorService}
     *
     * @param token
     *     Token for API
     * @param siteId
     *     Site ID
     *
     * @return an instance of {@link CollectorService}
     */
    public static CollectorService getInstance(final String token, final String siteId) {

        if (INSTANCE.client == null) {
            INSTANCE.client = new BakoAdminClient(token);
        }
        INSTANCE.siteId = siteId;

        return getInstance();
    }

    /**
     * Set a new token to call API
     * siteId
     *
     * @param token
     *     The new token
     */
    public void setToken(final String token) {
        if (this.client != null) {
            this.client.setToken(token);
        }
    }

    /**
     * Set a new token to call API
     *
     * @param siteId
     *     The new token
     */
    public void setSiteId(final String siteId) {
        this.siteId = siteId;
    }

    /**
     * Check if box reference exists
     *
     * @param boxTypeId
     *     ID of reference
     *
     * @return True or false
     *
     * @throws IOException
     *     I/O Exception
     */
    public boolean isBoxReferenceExist(final String boxTypeId) throws IOException {
        boolean found = this.boxTypeList.stream().anyMatch(boxTypeDto -> boxTypeDto.getId().equals(boxTypeId));
        if (found) {
            return true;
        } else {
            this.boxTypeList.clear();
            this.boxTypeList.addAll(client.getListTypeBox());
            return this.boxTypeList.stream().anyMatch(boxTypeDto -> boxTypeDto.getId().equals(boxTypeId));
        }
    }

    public void addBoxInMemory(final String boxTypId) {
        this.mapBox.computeIfPresent(boxTypId, (key, total) -> total + 1);
        this.mapBox.putIfAbsent(boxTypId, 1);
    }

    public void clear() {
        this.mapBox.clear();
        this.stockCollector = null;
        this.numDrawer = null;
    }

    /**
     * Get total of boxes in cart
     *
     * @return Total of boxes
     */
    public Integer getTotalBoxes() {
        return this.mapBox.values().stream().reduce(Integer::sum).orElse(0);
    }

    /**
     * Get total of weight in cart
     *
     * @return Total of weight
     */
    public Integer getTotalWeight() {
        AtomicInteger totalWeight = new AtomicInteger(0);
        this.mapBox.keySet().forEach(boxTypeId -> {
            Integer quantityOfBoxType = this.mapBox.getOrDefault(boxTypeId, 0);
            Log.d(LOGGER_TAG, "weightOfBoxType " + quantityOfBoxType);

            if (this.boxTypeList.stream().anyMatch(boxTypeDto -> boxTypeDto.getWeight() == null)) {
                throw new BoxTypeSettingsException();
            }
            if (quantityOfBoxType != null) {
                this.boxTypeList.stream()
                                .filter(boxTypeDto -> boxTypeDto.getId().equals(boxTypeId))
                                .findFirst()
                                .ifPresent(boxTypeDto -> totalWeight.addAndGet(boxTypeDto.getWeight() * quantityOfBoxType));
            }
        });

        return totalWeight.get();
    }

    /**
     * Deposit box in collector
     *
     * @return True if ok
     *
     * @throws IOException
     *     I/O Exception
     */
    public boolean postDepositBoxes() throws IOException {

        final DepositFormDto formDto = new DepositFormDto();
        formDto.setMapBoxes(mapBox);
        formDto.setSiteId(siteId);

        return client.depositBoxes(formDto);
    }

    /**
     * Monitor collector status
     *
     * @return True if ok
     *
     * @throws IOException
     *     I/O Exception
     */
    public boolean monitoring(final float batteryPercent, final String status) throws IOException {

        final MonitoringDto monitoringDto = new MonitoringDto();
        monitoringDto.setSiteId(siteId);
        monitoringDto.setBatteryPercent(batteryPercent);
        monitoringDto.setStatus(status);

        if (client != null) {
            return client.monitoring(monitoringDto);
        }
        return false;
    }

    /**
     * Login and check if the user has admin role
     *
     * @param login
     *     The login
     * @param password
     *     The password
     *
     * @return True if ok or false
     *
     * @throws IOException
     *     I/O Exception
     */
    public boolean loginAndIsAdmin(final String login, final String password) throws IOException {

        final LoginDto loginDto = new LoginDto();
        loginDto.setUsername(login);
        loginDto.setPassword(password);
        LoginResponseDto loginResponseDto = this.client.login(loginDto);

        final UserDto userDto = this.client.account(loginResponseDto.getId_token());

        return userDto.getAuthorities().stream().anyMatch(ROLE_ADMIN::equals);
    }

    /**
     * Get stock of collector
     *
     * @return StockCollectorDTO
     *
     * @throws IOException
     *     I/O Exception
     */
    public StockCollectorDTO getStockCollector() throws IOException {
        if (this.stockCollector == null) {
            this.stockCollector = this.client.getStock(siteId);
        }
        return this.stockCollector;
    }
}
