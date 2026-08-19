package backend.module.reading.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_book", indexes = {
        @Index(columnList = "user_email, book_id"),
        @Index(columnList = "book_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserBook {

    @Id
    private Long userBookId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    private int currentPage;

    public void updateCurrentPage(int pageNumber) {
        this.currentPage = pageNumber;
    }
}
