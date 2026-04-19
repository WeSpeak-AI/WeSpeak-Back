package backend.core.domain.reading;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reading_books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Book {

    @Id
    private Long bookId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private Level level;

    private String category;
    private int estimatedMinutes;

    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}
