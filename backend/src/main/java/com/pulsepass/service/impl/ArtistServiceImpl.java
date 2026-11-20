package com.pulsepass.service.impl;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.service.ArtistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de {@link ArtistService} (PRD Servicios §17).
 *
 * <ul>
 *   <li>Dependencias {@code final} recibidas por constructor (SRV-002, NFR-002).</li>
 *   <li>Los tres métodos son lecturas: {@code @Transactional(readOnly = true)} en cada uno
 *       (SRV-004, §38, P2-D14).</li>
 *   <li>El filtro {@code active = true} solo se aplica en {@link #findActiveArtists()}, y lo
 *       hace la consulta del repositorio (BR-ARTIST-002, SRV-005, P2-08 A-4a).</li>
 *   <li>La conversión a DTO la hace {@link ArtistMapper} (NFR-004).</li>
 *   <li>Los valores {@code null} no se validan en esta iteración (P2-08 A-3a, P2-D18).</li>
 * </ul>
 */
@Service
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;

    private final ArtistMapper artistMapper;

    public ArtistServiceImpl(ArtistRepository artistRepository, ArtistMapper artistMapper) {
        this.artistRepository = artistRepository;
        this.artistMapper = artistMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code findById} se hereda de {@code JpaRepository}. El mensaje sigue P2-D17 y
     * P2-08 A-2a.
     */
    @Override
    @Transactional(readOnly = true)
    public ArtistResponse findById(Long id) {
        return artistRepository.findById(id)
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + id));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Usa {@code findByStageNameIgnoreCase}, la operación que el PRD indica para los
     * servicios (§32, P2-08 A-1a). El {@code findByStageName} exacto del repositorio se
     * conserva para la Fase 1 (P1-D8) y no se usa aquí. El mensaje sigue P2-D17 y P2-08 A-2a.
     */
    @Override
    @Transactional(readOnly = true)
    public ArtistResponse findByStageName(String stageName) {
        return artistRepository.findByStageNameIgnoreCase(stageName)
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + stageName));
    }

    /** {@inheritDoc} Conserva el orden por nombre artístico que da el repositorio. */
    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponse> findActiveArtists() {
        return artistRepository.findByActiveTrueOrderByStageNameAsc()
                .stream()
                .map(artistMapper::toResponse)
                .toList();
    }
}
