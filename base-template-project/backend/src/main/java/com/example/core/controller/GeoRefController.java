package com.example.core.controller;

import com.example.core.dto.LocalityDTO;
import com.example.core.dto.MunicipalityDTO;
import com.example.core.dto.ProvinceDTO;
import com.example.core.service.GeoRefService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/georef")
@RequiredArgsConstructor
public class GeoRefController {

    private final GeoRefService geoRefService;

    // ========== PROVINCIAS ==========

    /**
     * GET /api/georef/provinces
     * Obtiene todas las provincias de Argentina
     */
    @GetMapping("/provinces")
    public ResponseEntity<List<ProvinceDTO>> getAllProvinces() {
        List<ProvinceDTO> provinces = geoRefService.getAllProvinces();
        return ResponseEntity.ok(provinces);
    }

    // ========== MUNICIPIOS ==========

    /**
     * GET /api/georef/municipalities?provinceId=82
     * Obtiene municipios de una provincia específica
     */
    @GetMapping("/municipalities")
    public ResponseEntity<List<MunicipalityDTO>> getMunicipalitiesByProvince(
            @RequestParam String provinceId) {
        List<MunicipalityDTO> municipalities = geoRefService.getMunicipalitiesByProvince(provinceId);
        return ResponseEntity.ok(municipalities);
    }

    /**
     * GET /api/georef/municipalities/search?query=Venado
     * Busca municipios por nombre (autocomplete)
     */
    @GetMapping("/municipalities/search")
    public ResponseEntity<List<MunicipalityDTO>> searchMunicipalities(
            @RequestParam String query) {
        List<MunicipalityDTO> municipalities = geoRefService.searchMunicipalitiesByName(query);
        return ResponseEntity.ok(municipalities);
    }

    // ========== LOCALIDADES ==========

    /**
     * GET /api/georef/localities?municipalityId=822077
     * Obtiene localidades de un municipio específico
     */
    @GetMapping("/localities")
    public ResponseEntity<List<LocalityDTO>> getLocalitiesByMunicipality(
            @RequestParam String municipalityId) {
        List<LocalityDTO> localities = geoRefService.getLocalitiesByMunicipality(municipalityId);
        return ResponseEntity.ok(localities);
    }
}
