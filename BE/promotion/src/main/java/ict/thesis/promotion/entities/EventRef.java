package ict.thesis.promotion.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "events_ref")
public class EventRef {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "status", length = 50)
    private String status;
}
