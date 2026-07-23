package backend.module.voca.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "custom_voca_books",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_email"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CustomVocaBook {

    @Id
    private Long customVocaBookId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "customVocaBook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomWord> words = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
