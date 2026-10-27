package ec.edu.uce.tickets.match.service;

import ec.edu.uce.tickets.match.entity.MatchStatus;
import ec.edu.uce.tickets.match.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Avanza el estado de los partidos según la hora, sin depender de que el admin presione un botón:
 *   PROGRAMADO → EN_CURSO   cuando llega la hora de inicio
 *   EN_CURSO   → FINALIZADO cuando pasa la duración configurada (app.match.duration-minutes)
 * El admin puede adelantarse (iniciar o finalizar antes), nunca atrasarse. CANCELADO no se toca.
 */
@Service
public class MatchLifecycleService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final MatchRepository matchRepository;
    private final int durationMinutes;

    public MatchLifecycleService(MatchRepository matchRepository,
                                 @Value("${app.match.duration-minutes:120}") int durationMinutes) {
        this.matchRepository = matchRepository;
        this.durationMinutes = durationMinutes;
    }

    /** Devuelve cuántos partidos iniciaron y cuántos finalizaron en esta pasada. */
    @Transactional
    public Result advance() {
        LocalDateTime now = LocalDateTime.now(ECUADOR);

        // Primero finalizar y después iniciar: un partido olvidado de hace 3 horas pasa en dos vueltas
        // (PROGRAMADO → EN_CURSO → FINALIZADO) y nunca se salta EN_CURSO de la máquina de estados.
        LocalDateTime finishedIfStartedBefore = now.minusMinutes(durationMinutes);
        int finished = matchRepository.moveStartedBefore(MatchStatus.EN_CURSO, MatchStatus.FINALIZADO,
                finishedIfStartedBefore.toLocalDate(), finishedIfStartedBefore.toLocalTime());
        int started = matchRepository.moveStartedBefore(MatchStatus.PROGRAMADO, MatchStatus.EN_CURSO,
                now.toLocalDate(), now.toLocalTime());
        return new Result(started, finished);
    }

    public record Result(int started, int finished) {
        public boolean changedAnything() {
            return started > 0 || finished > 0;
        }
    }
}
