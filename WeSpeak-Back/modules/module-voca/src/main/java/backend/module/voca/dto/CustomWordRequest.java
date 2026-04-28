package backend.module.voca.dto;

public record CustomWordRequest(
        String term,
        String meaning,
        String phonetic,
        String example,
        String imageUrl
) {
}
