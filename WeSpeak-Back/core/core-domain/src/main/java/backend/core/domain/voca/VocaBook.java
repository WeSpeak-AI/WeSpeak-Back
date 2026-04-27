package backend.core.domain.voca;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "voca_books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class VocaBook {

    @Id
    private Long vocaBookId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private String description;

    private int totalDays;

    public enum Category {
        BASIC, INTERMEDIATE, HARD, TOEIC
    }

    public void update(String title, Category category, String description) {
        this.title = title;
        this.category = category;
        this.description = description;
    }

    public void incrementTotalDays() { this.totalDays++; }

    public void decrementTotalDays() { this.totalDays--; }
}
