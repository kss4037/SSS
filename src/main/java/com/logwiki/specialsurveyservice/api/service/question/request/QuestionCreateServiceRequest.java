package com.logwiki.specialsurveyservice.api.service.question.request;

import com.logwiki.specialsurveyservice.api.utils.ApiError;
import com.logwiki.specialsurveyservice.domain.question.Question;
import com.logwiki.specialsurveyservice.domain.questioncategory.QuestionCategoryType;
import com.logwiki.specialsurveyservice.exception.BaseException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuestionCreateServiceRequest {

    private Long questionNumber;

    private String content;

    private String imgAddress;

    private QuestionCategoryType type;

    private List<MultipleChoiceCreateServiceRequest> multipleChoices;

    @Builder
    public QuestionCreateServiceRequest(Long questionNumber, String content,
            String imgAddress,
            QuestionCategoryType type, List<MultipleChoiceCreateServiceRequest> multipleChoices) {
        if (type == QuestionCategoryType.MULTIPLE_CHOICE && multipleChoices != null) {
            throw new BaseException(new ApiError("주관식은 보기를 가질수 없습니다.", 2001));
        }
        if (type == QuestionCategoryType.SHORT_FORM && multipleChoices == null) {
            throw new BaseException(new ApiError("객관식은 보기를 가져야합니다.", 2002));
        }
        this.questionNumber = questionNumber;
        this.content = content;
        this.imgAddress = imgAddress;
        this.type = type;
        this.multipleChoices = multipleChoices;
    }

    public Question toEntity() {
        return Question.builder()
                .questionNumber(questionNumber)
                .content(content)
                .imgAddress(imgAddress)
                .type(type)
                .multipleChoice(multipleChoices.stream()
                        .map(MultipleChoiceCreateServiceRequest::toEntity)
                        .collect(Collectors.toList()))
                .build();
    }
}
