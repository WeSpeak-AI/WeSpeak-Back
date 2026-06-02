package backend.core.domain.customvoca;

import backend.core.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_voca_books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CustomVocaBook {

    @Id
    private Long customVocaBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "customVocaBook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomWord> words = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
