package backend.core.domain.voca;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "words")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Word {

    @Id
    private Long wordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voca_book_day_id", nullable = false)
    private VocaBookDay vocaBookDay;

    @Column(nullable = false)
    private String term;

    @Column(nullable = false)
    private String meaning;

    @Column(nullable = false)
    private String phonetic;

    @Column(nullable = false)
    private String example;

    private String imageUrl;
}
