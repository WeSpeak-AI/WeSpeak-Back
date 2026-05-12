package backend.core.domain.reading;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reading_books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Book {

    @Id
    private Long bookId;

    @OneToMany(mappedBy = "book", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<BookPage> pages = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    private String imageUrl;

    @Column(nullable = false)
    private String author;

    @Enumerated(EnumType.STRING)
    private Level level;

    private String category;
    private int totalPages;

    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public void update(String title, String author, Level level, String category) {
        this.title = title;
        this.author = author;
        this.level = level;
        this.category = category;
    }

    public void incrementTotalPages() {
        this.totalPages++;
    }
}
