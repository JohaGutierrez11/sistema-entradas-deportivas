package ec.edu.uce.tickets.match.dto;

import ec.edu.uce.tickets.match.entity.*;

import java.util.List;

/** Todo lo que el front necesita para dibujar el estadio (HU-38). Sin disponibilidad. */
public record StadiumLayoutResponse(Long stadiumId, String stadiumName, BoxResponse field,
                                    List<SectionLayout> sections) {

    public record SectionLayout(Long id, String name, SectionType sectionType, Integer capacity,
                                BoxResponse box, List<RowLayout> rows) {
        static SectionLayout from(Section section) {
            return new SectionLayout(section.getId(), section.getName(), section.getSectionType(),
                    section.getCapacity(), BoxResponse.from(section.getBox()),
                    section.getRows().stream().map(RowLayout::from).toList());
        }
    }

    public record RowLayout(Long id, String letter, List<SeatLayout> seats) {
        static RowLayout from(SeatRow row) {
            return new RowLayout(row.getId(), row.getLetter(),
                    row.getSeats().stream().map(SeatLayout::from).toList());
        }
    }

    /** El id es el que se envía a reservation-service al reservar. */
    public record SeatLayout(Long id, Integer number) {
        static SeatLayout from(Seat seat) {
            return new SeatLayout(seat.getId(), seat.getSeatNumber());
        }
    }

    public static StadiumLayoutResponse from(Stadium stadium, List<Section> sections) {
        return new StadiumLayoutResponse(stadium.getId(), stadium.getName(),
                BoxResponse.from(SectionBox.FIELD),
                sections.stream().map(SectionLayout::from).toList());
    }
}