package backend.core.domain.userbook;

import backend.core.domain.reading.Book;
import backend.core.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_book")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserBook {

    @Id
    private Long userBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    private int currentPage;

    public void updateCurrentPage(int pageNumber) {
        this.currentPage = pageNumber;
    }
}
