package com.example.core.service;


import com.example.core.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GeoRefService {

    private static final String GEOREF_BASE_URL = "https://apis.datos.gob.ar/georef/api";

    private final RestTemplate restTemplate;

    public GeoRefService() {
        this.restTemplate = new RestTemplate();
    }

    // ========== PROVINCIAS ==========

    /**
     * Obtiene todas las provincias de Argentina
     */
    public List<ProvinceDTO> getAllProvinces() {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(GEOREF_BASE_URL + "/provincias")
                    .queryParam("campos", "id,nombre")
                    .queryParam("max", "24") // Argentina tiene 23 provincias + CABA
                    .toUriString();

            ResponseEntity<GeoRefResponse<GeoRefProvince>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<GeoRefResponse<GeoRefProvince>>() {}
            );

            if (response.getBody() != null && response.getBody().getResultados() != null) {
                return response.getBody().getResultados().stream()
                        .map(p -> new ProvinceDTO(p.getId(), p.getNombre()))
                        .collect(Collectors.toList());
            }

            log.warn("GeoRef API retornó respuesta vacía para provincias");
            return Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Error al consultar provincias en GeoRef API", e);
            return Collections.emptyList();
        }
    }

    // ========== MUNICIPIOS ==========

    /**
     * Obtiene municipios de una provincia específica
     */
    public List<MunicipalityDTO> getMunicipalitiesByProvince(String provinceId) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(GEOREF_BASE_URL + "/municipios")
                    .queryParam("provincia", provinceId)
                    .queryParam("campos", "id,nombre,provincia.id,provincia.nombre")
                    .queryParam("max", "500")
                    .toUriString();

            ResponseEntity<GeoRefResponse<GeoRefMunicipality>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<GeoRefResponse<GeoRefMunicipality>>() {}
            );

            if (response.getBody() != null && response.getBody().getResultados() != null) {
                return response.getBody().getResultados().stream()
                        .map(m -> new MunicipalityDTO(
                                m.getId(),
                                m.getNombre(),
                                m.getProvincia().getId(),
                                m.getProvincia().getNombre()
                        ))
                        .collect(Collectors.toList());
            }

            log.warn("GeoRef API retornó respuesta vacía para municipios de provincia {}", provinceId);
            return Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Error al consultar municipios en GeoRef API para provincia {}", provinceId, e);
            return Collections.emptyList();
        }
    }

    // ========== LOCALIDADES ==========

    /**
     * Obtiene localidades de un municipio específico
     */
    public List<LocalityDTO> getLocalitiesByMunicipality(String municipalityId) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(GEOREF_BASE_URL + "/localidades")
                    .queryParam("municipio", municipalityId)
                    .queryParam("campos", "id,nombre,municipio.id,municipio.nombre,provincia.id,provincia.nombre")
                    .queryParam("max", "500")
                    .toUriString();

            ResponseEntity<GeoRefResponse<GeoRefLocality>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<GeoRefResponse<GeoRefLocality>>() {}
            );

            if (response.getBody() != null && response.getBody().getResultados() != null) {
                return response.getBody().getResultados().stream()
                        .map(l -> new LocalityDTO(
                                l.getId(),
                                l.getNombre(),
                                l.getMunicipio() != null ? l.getMunicipio().getId() : null,
                                l.getMunicipio() != null ? l.getMunicipio().getNombre() : null,
                                l.getProvincia().getId(),
                                l.getProvincia().getNombre()
                        ))
                        .collect(Collectors.toList());
            }

            log.warn("GeoRef API retornó respuesta vacía para localidades de municipio {}", municipalityId);
            return Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Error al consultar localidades en GeoRef API para municipio {}", municipalityId, e);
            return Collections.emptyList();
        }
    }

    // ========== BÚSQUEDA POR NOMBRE ==========

    /**
     * Busca municipios por nombre (autocomplete)
     */
    public List<MunicipalityDTO> searchMunicipalitiesByName(String query) {
        if (query == null || query.trim().length() < 3) {
            return Collections.emptyList();
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(GEOREF_BASE_URL + "/municipios")
                    .queryParam("nombre", query)
                    .queryParam("campos", "id,nombre,provincia.id,provincia.nombre")
                    .queryParam("max", "20")
                    .toUriString();

            ResponseEntity<GeoRefResponse<GeoRefMunicipality>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<GeoRefResponse<GeoRefMunicipality>>() {}
            );

            if (response.getBody() != null && response.getBody().getResultados() != null) {
                return response.getBody().getResultados().stream()
                        .map(m -> new MunicipalityDTO(
                                m.getId(),
                                m.getNombre(),
                                m.getProvincia().getId(),
                                m.getProvincia().getNombre()
                        ))
                        .collect(Collectors.toList());
            }

            return Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Error al buscar municipios por nombre: {}", query, e);
            return Collections.emptyList();
        }
    }

    // ========== VALIDACIÓN ==========

    /**
     * Valida que exista una provincia con el ID dado
     */
    public boolean isValidProvinceId(String provinceId) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(GEOREF_BASE_URL + "/provincias")
                    .queryParam("id", provinceId)
                    .queryParam("campos", "id")
                    .toUriString();

            ResponseEntity<GeoRefResponse<GeoRefProvince>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<GeoRefResponse<GeoRefProvince>>() {}
            );

            return response.getBody() != null &&
                    response.getBody().getCantidad() > 0;

        } catch (RestClientException e) {
            log.error("Error al validar provincia {}", provinceId, e);
            return false;
        }
    }

    /**
     * Valida que exista un municipio con el ID dado
     */
    public boolean isValidMunicipalityId(String municipalityId) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(GEOREF_BASE_URL + "/municipios")
                    .queryParam("id", municipalityId)
                    .queryParam("campos", "id")
                    .toUriString();

            ResponseEntity<GeoRefResponse<GeoRefMunicipality>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<GeoRefResponse<GeoRefMunicipality>>() {}
            );

            return response.getBody() != null &&
                    response.getBody().getCantidad() > 0;

        } catch (RestClientException e) {
            log.error("Error al validar municipio {}", municipalityId, e);
            return false;
        }
    }
}