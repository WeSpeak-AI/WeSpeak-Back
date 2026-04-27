package backend.core.domain.voca;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "voca_book_Days", indexes = @Index(columnList = "voca_book_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class VocaBookDay {

    @Id
    private Long vocaBookDayId;

    @Column(nullable = false)
    private Integer day;

    @Column(nullable = false)
    private String dayTopic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voca_book_id")
    private VocaBook vocaBook;

    public void update(Integer day, String dayTopic) {
        this.day = day;
        this.dayTopic = dayTopic;
    }
}
