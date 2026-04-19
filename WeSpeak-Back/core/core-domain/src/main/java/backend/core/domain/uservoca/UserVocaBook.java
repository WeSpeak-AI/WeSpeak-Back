package backend.core.domain.uservoca;

import backend.core.domain.user.User;
import backend.core.domain.voca.VocaBook;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_voca_books")
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
}
