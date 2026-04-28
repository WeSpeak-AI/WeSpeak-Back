package backend.module.searchword.dto;

public record WordSearchResponse(
        String term,
        String meaning,
        String phonetic,
        String example
) {
}
