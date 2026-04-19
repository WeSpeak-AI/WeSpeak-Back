package backend.core.domain.test;

import backend.core.domain.voca.Word;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "incorrect_word")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class IncorrectWord {

    @Id
    private Long incorrectWordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private Test test;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id")
    private Word word;
}
