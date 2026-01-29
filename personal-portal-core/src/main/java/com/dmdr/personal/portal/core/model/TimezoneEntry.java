package com.dmdr.personal.portal.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TimezoneEntry {
    INTERNATIONAL_DATE_LINE_WEST(1, "International Date Line West", "GMT-12:00"),
    AMERICAN_SAMOA(2, "American Samoa", "GMT-11:00"),
    HAWAII(3, "Hawaii", "GMT-10:00"),
    ALASKA(4, "Alaska", "GMT-09:00"),
    PACIFIC_TIME(5, "Pacific Time (US & Canada)", "GMT-08:00"),
    MOUNTAIN_TIME(6, "Mountain Time (US & Canada)", "GMT-07:00"),
    CENTRAL_TIME(7, "Central Time (US & Canada)", "GMT-06:00"),
    EASTERN_TIME(8, "Eastern Time (US & Canada)", "GMT-05:00"),
    ATLANTIC_TIME(9, "Atlantic Time (Canada)", "GMT-04:00"),
    BUENOS_AIRES_BRASILIA(10, "Buenos Aires, Brasilia", "GMT-03:00"),
    MID_ATLANTIC(11, "Mid-Atlantic", "GMT-02:00"),
    AZORES(12, "Azores", "GMT-01:00"),
    LONDON_DUBLIN_LISBON(13, "London, Dublin, Lisbon", "GMT+00:00"),
    PARIS_BERLIN_ROME_MADRID(14, "Paris, Berlin, Rome, Madrid", "GMT+01:00"),
    ATHENS_CAIRO_ISTANBUL(15, "Athens, Cairo, Istanbul", "GMT+02:00"),
    MOSCOW_KUWAIT_RIYADH(16, "Moscow, Kuwait, Riyadh", "GMT+03:00"),
    DUBAI_ABU_DHABI_BAKU(17, "Dubai, Abu Dhabi, Baku", "GMT+04:00"),
    KARACHI_TASHKENT(18, "Karachi, Tashkent", "GMT+05:00"),
    MUMBAI_KOLKATA_NEW_DELHI(19, "Mumbai, Kolkata, New Delhi", "GMT+05:30"),
    DHAKA_ALMATY(20, "Dhaka, Almaty", "GMT+06:00"),
    BANGKOK_JAKARTA_HANOI(21, "Bangkok, Jakarta, Hanoi", "GMT+07:00"),
    SINGAPORE_HONG_KONG_BEIJING(22, "Singapore, Hong Kong, Beijing", "GMT+08:00"),
    TOKYO_SEOUL_OSAKA(23, "Tokyo, Seoul, Osaka", "GMT+09:00"),
    SYDNEY_MELBOURNE_BRISBANE(24, "Sydney, Melbourne, Brisbane", "GMT+10:00"),
    SOLOMON_ISLANDS_NEW_CALEDONIA(25, "Solomon Islands, New Caledonia", "GMT+11:00"),
    AUCKLAND_FIJI_WELLINGTON(26, "Auckland, Fiji, Wellington", "GMT+12:00"),
    TONGA_SAMOA(27, "Tonga, Samoa", "GMT+13:00");

    private final int id;
    private final String displayName;
    private final String gmtOffset;

    public static TimezoneEntry getById(int id) {
        return Arrays.stream(values())
                .filter(entry -> entry.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown timezone id: " + id));
    }

    public String getGmtOffset() {
        // Some regions may have summer/winter time -> we get offset for today
        Instant referenceTime = Instant.now();
        ZoneId zoneId = ZoneId.of(this.gmtOffset);
        ZoneOffset offset = zoneId.getRules().getOffset(referenceTime);
        return offset.toString();
    }

}
