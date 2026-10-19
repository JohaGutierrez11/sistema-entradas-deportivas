package ec.edu.uce.tickets.match.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stadium")
@Getter
@Setter
@NoArgsConstructor
public class Stadium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 250)
    private String description;

    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

}
