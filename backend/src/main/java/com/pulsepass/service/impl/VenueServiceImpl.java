package com.pulsepass.service.impl;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.VenueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de {@link VenueService} (PRD Servicios §10).
 *
 * <ul>
 *   <li>Dependencias {@code final} recibidas por constructor (SRV-002, NFR-002).</li>
 *   <li>Los dos métodos son lecturas: {@code @Transactional(readOnly = true)} en cada uno
 *       (SRV-004, §38, P2-D14).</li>
 *   <li>El filtro {@code active = true} lo hace la consulta del repositorio; el servicio no
 *       filtra en memoria (BR-VENUE-002, SRV-005).</li>
 *   <li>La conversión a DTO la hace {@link VenueMapper} (NFR-004).</li>
 * </ul>
 */
@Service
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;

    private final VenueMapper venueMapper;

    public VenueServiceImpl(VenueRepository venueRepository, VenueMapper venueMapper) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>El {@code Optional} se resuelve con {@code orElseThrow}, nunca con {@code get()} (§36).
     * El mensaje va en inglés e identifica el recurso (P2-D17, NFR-005, P2-07 V-2a). Un
     * {@code code} nulo no se valida en esta iteración (P2-07 V-3a, P2-D18).
     */
    @Override
    @Transactional(readOnly = true)
    public VenueResponse findByCode(String code) {
        return venueRepository.findByCode(code)
                .map(venueMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + code));
    }

    /** {@inheritDoc} Conserva el orden por nombre que da el repositorio. */
    @Override
    @Transactional(readOnly = true)
    public List<VenueResponse> findActiveVenues() {
        return venueRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(venueMapper::toResponse)
                .toList();
    }
}
