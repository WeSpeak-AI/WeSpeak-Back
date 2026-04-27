package backend.core.domain.voca;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "words", indexes = {@Index(columnList = "voca_book_day_id"), @Index(columnList = "term")})
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

    public void update(String term, String meaning, String phonetic, String example, String imageUrl) {
        this.term = term;
        this.meaning = meaning;
        this.phonetic = phonetic;
        this.example = example;
        this.imageUrl = imageUrl;
    }
}
