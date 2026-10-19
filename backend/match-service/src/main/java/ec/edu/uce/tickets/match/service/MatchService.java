package ec.edu.uce.tickets.match.service;

import ec.edu.uce.tickets.match.dto.MatchRequest;
import ec.edu.uce.tickets.match.dto.MatchResponse;
import ec.edu.uce.tickets.match.dto.MatchUpdateRequest;
import ec.edu.uce.tickets.match.entity.MatchStatus;
import ec.edu.uce.tickets.match.entity.SportMatch;
import ec.edu.uce.tickets.match.entity.Stadium;
import ec.edu.uce.tickets.match.exception.BusinessRuleException;
import ec.edu.uce.tickets.match.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.match.repository.MatchRepository;
import ec.edu.uce.tickets.match.repository.StadiumRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MatchRepository matchRepository;
    private final StadiumRepository stadiumRepository;

    public MatchService(MatchRepository matchRepository, StadiumRepository stadiumRepository) {
        this.matchRepository = matchRepository;
        this.stadiumRepository = stadiumRepository;
    }

    @Transactional
    public MatchResponse create(MatchRequest request) {
        // 1. El estadio debe existir
        Stadium stadium = stadiumRepository.findById(request.stadiumId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el estadio con el id " + request.stadiumId()));

        // 2. La fecha y hora del partido no pueden haber pasado (hora de Ecuador)
        LocalDateTime start = LocalDateTime.of(request.matchDate(), request.matchTime());
        if (start.isBefore(LocalDateTime.now(ECUADOR))) {
            throw new BusinessRuleException(
                    "La fecha del partido (" + start.format(DATE_TIME_FORMAT) + ") ya pasó");
        }

        // 3. Crear el partido: siempre nace PROGRAMADO
        SportMatch match = new SportMatch();
        match.setStadium(stadium);
        match.setName(request.name().trim());
        match.setDescription(request.description());
        match.setMatchDate(request.matchDate());
        match.setMatchTime(request.matchTime());
        match.setStatus(MatchStatus.PROGRAMADO);

        // 4. Guardar y devolver
        return MatchResponse.from(matchRepository.save(match));
    }

    @Transactional(readOnly = true)
    public MatchResponse findById(Long id) {
        return MatchResponse.from(findMatchOrThrow(id));
    }

    private SportMatch findMatchOrThrow(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el partido con el id " + id));
    }

    @Transactional
    public MatchResponse update(Long id, MatchUpdateRequest request) {
        SportMatch match = findMatchOrThrow(id);
        MatchStatus current = match.getStatus();
        MatchStatus next = request.status();

        // 1. Un partido finalizado o cancelado ya es historia: no se modifica
        if (current.isFinal()) {
            throw new BusinessRuleException("El partido está " + current + " y ya no se puede modificar");
        }

        // 2. La transición debe estar permitida
        if (!current.canTransitionTo(next)) {
            throw new BusinessRuleException("No se puede pasar un partido de " + current + " a " + next);
        }

        // 3. El estadio no se cambia: los asientos reservados pertenecen al estadio original
        if (!match.getStadium().getId().equals(request.stadiumId())) {
            throw new BusinessRuleException(
                    "No se puede cambiar el estadio de un partido; cancélelo y cree uno nuevo");
        }

        // 4. Si sigue programado, la nueva fecha y hora no pueden haber pasado
        if (next == MatchStatus.PROGRAMADO) {
            LocalDateTime start = LocalDateTime.of(request.matchDate(), request.matchTime());
            if (start.isBefore(LocalDateTime.now(ECUADOR))) {
                throw new BusinessRuleException(
                        "La fecha del partido (" + start.format(DATE_TIME_FORMAT) + ") ya pasó");
            }
        }

        match.setName(request.name().trim());
        match.setDescription(request.description());
        match.setMatchDate(request.matchDate());
        match.setMatchTime(request.matchTime());
        match.setStatus(next);

        return MatchResponse.from(match);   // dirty checking: no hace falta save()
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> search(LocalDate date, String name, Long stadiumId, MatchStatus status) {
        MatchStatus effectiveStatus = (status != null) ? status : MatchStatus.PROGRAMADO;

        Specification<SportMatch> spec = (root, query, cb) -> {
            root.fetch("stadium");   // trae el estadio en la MISMA consulta (evita N+1)

            List<Predicate> filters = new ArrayList<>();

            // Estado: siempre se filtra (por defecto PROGRAMADO)
            filters.add(cb.equal(root.get("status"), effectiveStatus));

            // Fecha exacta; si no viene y buscamos programados, solo desde hoy
            if (date != null) {
                filters.add(cb.equal(root.get("matchDate"), date));
            } else if (effectiveStatus == MatchStatus.PROGRAMADO) {
                filters.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("matchDate"), LocalDate.now(ECUADOR)));
            }

            // Nombre: "contiene", sin distinguir mayúsculas
            if (name != null && !name.isBlank()) {
                filters.add(cb.like(cb.lower(root.<String>get("name")),
                        "%" + name.trim().toLowerCase() + "%"));
            }

            // Estadio
            if (stadiumId != null) {
                filters.add(cb.equal(root.get("stadium").get("id"), stadiumId));
            }

            return cb.and(filters.toArray(new Predicate[0]));
        };

        return matchRepository.findAll(spec, Sort.by("matchDate", "matchTime")).stream()
                .map(MatchResponse::from)
                .toList();
    }
}