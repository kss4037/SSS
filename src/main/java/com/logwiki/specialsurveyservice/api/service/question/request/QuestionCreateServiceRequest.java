package com.logwiki.specialsurveyservice.api.service.question.request;

import com.logwiki.specialsurveyservice.domain.question.Question;
import com.logwiki.specialsurveyservice.domain.questioncategory.QuestionCategoryType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

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
        this.questionNumber = questionNumber;
        this.content = content;
        this.imgAddress = imgAddress;
        this.type = type;
        this.multipleChoices = multipleChoices;
    }

    public Question toEntity() {
        if (multipleChoices != null) {
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
        return Question.builder()
                .questionNumber(questionNumber)
                .content(content)
                .imgAddress(imgAddress)
                .type(type)
                .build();
    }

}
