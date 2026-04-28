package backend.core.domain.test;

import backend.core.domain.voca.Word;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "correct_word", indexes = @Index(columnList = "test_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CorrectWord {

    @Id
    private Long correctWordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id")
    private Word word;
}
