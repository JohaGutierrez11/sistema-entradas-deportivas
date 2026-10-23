package ec.edu.uce.tickets.match.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "section")
@Getter
@Setter
@NoArgsConstructor
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 250)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 15)
    private SectionType sectionType;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount;

    @Column(name = "seats_per_row", nullable = false)
    private Integer seatsPerRow;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    /** Recuadro sobre el plano. null = la sección todavía no está ubicada. */
    @Embedded
    private SectionBox box;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("letter ASC")
    private List<SeatRow> rows = new ArrayList<>();

    public void addRow(SeatRow row) {
        rows.add(row);
        row.setSection(this);
    }
}