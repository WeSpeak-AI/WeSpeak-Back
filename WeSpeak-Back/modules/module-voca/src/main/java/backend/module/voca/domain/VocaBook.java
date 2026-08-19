package backend.module.voca.domain;

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

    @OneToMany(mappedBy = "vocaBook", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<VocaBookDay> vocaBookDays = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private String description;

    private int totalDays;


    public enum Category {
        BASIC, INTERMEDIATE, HARD, TOEIC
    }

    public void updateImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void update(String title, Category category, String description) {
        this.title = title;
        this.category = category;
        this.description = description;
    }

    public void incrementTotalDays() { this.totalDays++; }

    public void decrementTotalDays() { this.totalDays--; }
}
