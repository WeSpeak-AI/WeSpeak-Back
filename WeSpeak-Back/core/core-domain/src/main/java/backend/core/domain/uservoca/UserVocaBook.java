package backend.core.domain.userbook;

import backend.core.domain.user.User;
import backend.core.domain.voca.VocaBook;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_voca_book")
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserVocaBook {

    @Id
    private Long userBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocaBook_id", nullable = false)
    private VocaBook vacaBook;
}
