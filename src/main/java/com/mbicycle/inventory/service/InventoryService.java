package com.mbicycle.inventory.service;

import com.mbicycle.inventory.domain.InventoryResponseDTO;
import com.mbicycle.inventory.domain.Output;
import com.mbicycle.inventory.repository.aggregation.CustomLookupAggregation;
import com.mbicycle.inventory.utils.FilterParams;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mbicycle.inventory.domain.InventoryConstants.*;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final MongoTemplate mongoTemplate;

    public List<InventoryResponseDTO> findSlips(FilterParams filterParams) {

        Aggregation aggregation = buildAggregation(filterParams);

        AggregationResults<Output> response
                = mongoTemplate.aggregate(aggregation, SLIPS_PREMIUM_COLLECTION, Output.class);

        return buildResponse(calculateResults(response.getMappedResults()));
    }

    private List<Output> calculateResults(List<Output> mappedResults) {
        mappedResults
                .forEach(result -> {
                    Map<String, BigDecimal> calculatedREC = result.getCalculatedREC();
                    if (calculatedREC.get(CALCULATED_REC) == null) {
                        calculatedREC.put(CALCULATED_REC, new BigDecimal(0));
                    }
                    result.getCases().stream()
                            .filter(caze -> caze.get(CEDANTS_ID).equals(result.getCedant().get(_ID))
                                    && caze.get(BRANCHES_ID).equals(result.getBranches().get(_ID)))
                            .forEach(caze -> {
                                BigDecimal calculatedREC1 = calculatedREC.get(CALCULATED_REC);
                                double premiumHt = Double.parseDouble(String.valueOf(caze.get(PREMIUM_HT)));
                                premiumHt *= PREMIUM_HT_RATE;
                                calculatedREC.put(CALCULATED_REC, calculatedREC1.add(BigDecimal.valueOf(premiumHt)));
                            });
                });
        return mappedResults;
    }

    private Aggregation buildAggregation(FilterParams filterParams) {
        MatchOperation matchOperation = buildMatchStage(filterParams);
        List<AggregationOperation> lookupOperations = buildLookupOperations(filterParams);
        MatchOperation filterStage = buildFilterStage(filterParams);
        ProjectionOperation projectionOperation = buildProjectionOperation();
        List<UnwindOperation> unwindOperations = buildUnwindOperation();
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        aggregationOperations.add(matchOperation);
        aggregationOperations.addAll(lookupOperations);
        aggregationOperations.add(filterStage);
        aggregationOperations.add(projectionOperation);
        aggregationOperations.addAll(unwindOperations);
        return Aggregation.newAggregation(aggregationOperations);
    }

    private MatchOperation buildMatchStage(FilterParams filterParams) {
        return Aggregation.match(new Criteria(CONFIRMATION_STATUS)
                .in(filterParams.getConfirmationStatus())
                .and(VALIDATION_STATUS)
                .in(filterParams.getValidationStatus()));
    }

    private List<AggregationOperation> buildLookupOperations(FilterParams filterParams) {
        String branches = buildQueryStrings(String.join(",", filterParams.getBranchName()));
        String companyTypes = buildQueryStrings(String.join(",", filterParams.getCompanyType()));
        String getBranchesQuery = "{ $lookup: { from: \"branches\", pipeline: [{ $match: { name: { $in: [\"" + branches + "\"] }, type: {$in: [\"" + companyTypes + "\"]} } }], as: \"branches\" } }";
        String getCasesNotLifePremiumQuery = "{ $lookup: { from: \"case_not_life_premium\", let: { cedant_id: \"$cedant._id\", slips_id: \"$_id\"}, pipeline: [{ $match: { $expr: { $and: [{$eq: [\"$slipes_prime_id\", \"$$slips_id\"]}]}}}], as: \"cases\"}}";
        CustomLookupAggregation getBranchesCustomLookupAggregation = new CustomLookupAggregation(getBranchesQuery);
        CustomLookupAggregation getCasesNotLifePremiumCustomLookupAggregation = new CustomLookupAggregation(getCasesNotLifePremiumQuery);
        LookupOperation cedantsLookupStage = Aggregation.lookup(CEDANTS, CEDANTS_ID, _ID, CEDANT);
        LookupOperation countriesLookupStage = Aggregation.lookup(COUNTRIES, CEDANT_COUNTRIES_ID, _ID, COUNTRY);
        LookupOperation groupLookupStage = Aggregation.lookup(GROUP_CEDANTS, CEDANT_GROUP_ID, _ID, GROUP);
        LookupOperation regionLookupStage = Aggregation.lookup(REGION, COUNTRY_REGIONS_ID, _ID, REGION);

        return List.of(
                getBranchesCustomLookupAggregation,
                cedantsLookupStage,
                countriesLookupStage,
                groupLookupStage,
                regionLookupStage,
                getCasesNotLifePremiumCustomLookupAggregation);
    }

    private MatchOperation buildFilterStage(FilterParams filterParams) {
        Criteria criteria = new Criteria();
        criteria.andOperator(
                !CollectionUtils.isEmpty(filterParams.getCedant()) ? Criteria.where(CEDANT_NAME).in(filterParams.getCedant())
                        : Criteria.where(""),
                !CollectionUtils.isEmpty(filterParams.getRegion()) ? Criteria.where(REGION_NAME).in(filterParams.getRegion())
                        : Criteria.where(""),
                !CollectionUtils.isEmpty(filterParams.getCountry()) ? Criteria.where(COUNTRY_NAME).in(filterParams.getCountry())
                        : Criteria.where(""),
                !CollectionUtils.isEmpty(filterParams.getGroup()) ? Criteria.where(GROUP_NAME).in(filterParams.getGroup())
                        : Criteria.where(""),
                StringUtils.hasLength(filterParams.getPublishedDate()) ? Criteria.where(PUBLISHED_DATE).regex(filterParams.getPublishedDate())
                        : Criteria.where(""),
                StringUtils.hasLength(filterParams.getConfirmationDate()) ? Criteria.where(CONFIRMATION_DATE).regex(filterParams.getConfirmationDate())
                        : Criteria.where(""),
                StringUtils.hasLength(filterParams.getEditedPeriod()) ? Criteria.where(EDITED_PERIOD).is(convertEditedPeriod(filterParams.getEditedPeriod()))
                        : Criteria.where("")
        );

        return Aggregation.match(criteria);
    }

    private List<UnwindOperation> buildUnwindOperation() {
        UnwindOperation cedantUnwind = Aggregation.unwind(DOL_SIGN + CEDANT);
        UnwindOperation countryUnwind = Aggregation.unwind(DOL_SIGN + COUNTRY);
        UnwindOperation groupUnwind = Aggregation.unwind(DOL_SIGN + GROUP);
        UnwindOperation regionUnwind = Aggregation.unwind(DOL_SIGN + REGION);
        UnwindOperation branchesUnwind = Aggregation.unwind(DOL_SIGN + BRANCHES);

        return List.of(
                cedantUnwind,
                countryUnwind,
                groupUnwind,
                regionUnwind,
                branchesUnwind);
    }

    private ProjectionOperation buildProjectionOperation() {
        return Aggregation.project(_ID, REFERENCE, CONFIRMATION_STATUS, VALIDATION_STATUS, EDITED_PERIOD, CONFIRMATION_DATE,
                PUBLISHED_DATE, CEDANT, COUNTRY, GROUP, REGION, BRANCHES, CASES);
    }

    private List<InventoryResponseDTO> buildResponse(List<Output> results) {

        return results.stream()
                .filter(result -> !result.getCalculatedREC().get(CALCULATED_REC).equals(new BigDecimal(0)))
                .map(result -> InventoryResponseDTO.builder()
                        .reference(result.getReference())
                        .country(String.valueOf(result.getCountry().get(NAME)))
                        .cedant(String.valueOf(result.getCedant().get(NAME)))
                        .validationStatus(result.getValidationStatus())
                        .confirmationStatus(result.getConfirmationStatus())
                        .publicationDate(result.getPublicationDate())
                        .branche(String.valueOf(result.getBranches().get(NAME)))
                        .calculatedREC(result.getCalculatedREC().get(CALCULATED_REC).setScale(2, RoundingMode.DOWN))
                        .build()
                ).collect(Collectors.toList());
    }

    private String convertEditedPeriod(String editedPeriod) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        DateTimeFormatter fullMonthAndYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        LocalDate ld = LocalDate.parse(editedPeriod, dtf);

        return fullMonthAndYearFormatter.format(ld);
    }

    private String buildQueryStrings(String params) {
        return params.replace(",", "\",\"");
    }
}
