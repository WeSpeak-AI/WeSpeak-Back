package backend.module.voca.dto;

import backend.module.voca.domain.VocaBookDay;
import lombok.Getter;

@Getter
public class VocaBookDayResponse {

    private Integer dayNumber;
    private String dayTopic;

    public static VocaBookDayResponse from(VocaBookDay vocaBookDay) {
        VocaBookDayResponse vocaBookDayResponse = new VocaBookDayResponse();
        vocaBookDayResponse.dayTopic = vocaBookDay.getDayTopic();
        vocaBookDayResponse.dayNumber = vocaBookDay.getDay();
        return vocaBookDayResponse;
    }
}
