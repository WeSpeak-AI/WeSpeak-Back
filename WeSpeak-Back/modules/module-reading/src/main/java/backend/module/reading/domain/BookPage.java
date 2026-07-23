package backend.module.reading.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "book_pages", indexes = @Index(columnList = "book_id, page_number"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class BookPage {

    @Id
    private Long bookPageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private int pageNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column
    private String imageUrl;

    public void update(int pageNumber, String content) {
        this.pageNumber = pageNumber;
        this.content = content;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
