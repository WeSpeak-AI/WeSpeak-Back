package backend.module.voca.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "custom_words", indexes = @Index(columnList = "custom_voca_book_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CustomWord {

    @Id
    private Long customWordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_voca_book_id", nullable = false)
    private CustomVocaBook customVocaBook;

    @Column(nullable = false)
    private String term;

    @Column(nullable = false)
    private String meaning;

    private String phonetic;

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
