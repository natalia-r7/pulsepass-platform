package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de {@link Artist} (PRD Persistencia §14, FR-ART-001).
 * findById se hereda de JpaRepository.
 */
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /** Artista por nombre artístico exacto (P1-D8). */
    Optional<Artist> findByStageName(String stageName);

    /**
     * Artista por nombre artístico, ignorando mayúsculas (PRD Servicios §32, P2-D6). Se añade
     * junto a {@link #findByStageName(String)}, que se conserva exacto (P1-D8).
     */
    Optional<Artist> findByStageNameIgnoreCase(String stageName);

    /** Artistas activos ordenados por nombre artístico (PRD Servicios §32, P2-D6). */
    List<Artist> findByActiveTrueOrderByStageNameAsc();
}
