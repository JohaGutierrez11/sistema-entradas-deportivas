package ec.edu.uce.tickets.reservation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parameter")
@Getter
@Setter
@NoArgsConstructor
public class Parameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "value", nullable = false, length = 100)
    private String value;

    @Column(name = "description", length = 250)
    private String description;
}