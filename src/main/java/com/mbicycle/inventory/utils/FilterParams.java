package com.mbicycle.inventory.utils;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FilterParams {
    private List<String> validationStatus;
    private List<String> confirmationStatus;
    private List<String> region;
    private List<String> country;
    private List<String> group;
    private List<String> cedant;
    private List<String> companyType;
    private List<String> branchName;
    private String publishedDate;
    private String confirmationDate;
    private String editedPeriod;
}
