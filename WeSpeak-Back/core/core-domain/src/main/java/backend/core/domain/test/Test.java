package backend.core.domain.test;

import backend.core.domain.user.User;
import backend.core.domain.voca.VocaBook;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test", indexes = @Index(columnList = "user_id, voca_book_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Test {

    @Id
    private Long testId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voca_book_id")
    private VocaBook vocaBook;

    @Column(nullable = false)
    private LocalDateTime testedAt;

    @Column(nullable = false)
    private int startDay;

    @Column(nullable = false)
    private int endDay;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int totalWords;
}
