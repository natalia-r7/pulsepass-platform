package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.service.ArtistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de artistas (PRD Controladores §7.3, §8.3; FR-CTRL-ART-001..003).
 * <p>
 * Controlador delgado: solo traduce HTTP a llamadas de {@link ArtistService} y devuelve sus DTO
 * (CTRL-001..004, CTRL-009). Los parámetros llegan al servicio sin transformarse: ignorar
 * mayúsculas en el nombre artístico (P2-08 A-1a) y filtrar los activos (BR-ARTIST-002) son
 * reglas del servicio. Los errores los traduce {@code GlobalExceptionHandler}: un artista
 * inexistente responde 404 (BR-ARTIST-001); un {@code id} no numérico o la falta de
 * {@code stageName}, 400 (P3-D3).
 * <p>
 * Las rutas literales {@code /active} y {@code /by-stage-name} tienen prioridad sobre
 * {@code /{id}}.
 */
@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    /** {@code GET /api/artists/{id}} → 200 con el artista (FR-CTRL-ART-001). */
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(artistService.findById(id));
    }

    /** {@code GET /api/artists/by-stage-name?stageName=...} → 200 con el artista (FR-CTRL-ART-002). */
    @GetMapping("/by-stage-name")
    public ResponseEntity<ArtistResponse> findByStageName(@RequestParam String stageName) {
        return ResponseEntity.ok(artistService.findByStageName(stageName));
    }

    /** {@code GET /api/artists/active} → 200 con los artistas activos (FR-CTRL-ART-003). */
    @GetMapping("/active")
    public ResponseEntity<List<ArtistResponse>> findActiveArtists() {
        return ResponseEntity.ok(artistService.findActiveArtists());
    }
}
