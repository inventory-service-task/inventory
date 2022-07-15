package com.mbicycle.inventory.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
public class Output {
    private String reference;
    private Map<String, Object> cedant;
    private Map<String, Object> country;
    private Map<String, Object> group;
    private Map<String, Object> region;
    private Map<String, Object> branches;
    private List<Map<String, Object>> cases;
    @Field("validation_status")
    private String validationStatus;
    @Field("confirmation_status")
    private String confirmationStatus;
    @Field("published_date")
    private String publicationDate;
    @Field("confirmation_date")
    private String confirmationDate;
    private Map<String, BigDecimal> calculatedREC = new HashMap<>();
    @Field("edited_period")
    private String editedPeriod;
    @Field("cedants.groups_cedants_id")
    private String groupCedantsId;
    @Field("cedants.name")
    private String cedantsName;
    @Field("cedants.countries_id")
    private String cedantsCountriesId;
}
