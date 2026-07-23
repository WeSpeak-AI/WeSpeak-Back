package backend.module.voca.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test", indexes = @Index(columnList = "user_email, voca_book_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Test {

    @Id
    private Long testId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

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
