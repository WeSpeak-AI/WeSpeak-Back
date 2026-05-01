package backend.core.domain.uservoca;

import backend.core.domain.user.User;
import backend.core.domain.voca.VocaBook;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_voca_books", indexes = {@Index(columnList = "user_id, voca_book_id"), @Index(columnList = "email, voca_book_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserVocaBook {

    @Id
    private Long userBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voca_book_id", nullable = false)
    private VocaBook vocaBook;

    @Builder.Default
    private int currentDay = 1;

    public void updateCurrentDay(int completedToDay) {
        this.currentDay = Math.max(this.currentDay, completedToDay + 1);
    }
}
