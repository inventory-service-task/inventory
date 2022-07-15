package com.mbicycle.inventory.controller;

import com.mbicycle.inventory.domain.InventoryResponseDTO;
import com.mbicycle.inventory.service.InventoryService;
import com.mbicycle.inventory.utils.FilterParams;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public List<InventoryResponseDTO> findSlipsPremium(FilterParams filterParams) {
        return inventoryService.findSlips(filterParams);
    }
}
