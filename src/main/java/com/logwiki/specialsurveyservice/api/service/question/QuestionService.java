package com.logwiki.specialsurveyservice.api.service.question;

import com.logwiki.specialsurveyservice.api.service.question.request.QuestionCreateServiceRequest;
import com.logwiki.specialsurveyservice.api.service.question.request.QuestionModifyServiceRequest;
import com.logwiki.specialsurveyservice.api.utils.ApiError;
import com.logwiki.specialsurveyservice.domain.question.Question;
import com.logwiki.specialsurveyservice.domain.question.QuestionRepository;
import com.logwiki.specialsurveyservice.domain.questioncategory.QuestionCategoryRepository;
import com.logwiki.specialsurveyservice.exception.BaseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionCategoryRepository questionCategoryRepository;

    public Question addQuestion(QuestionCreateServiceRequest questionCreateServiceRequest) {
        return questionCreateServiceRequest.toEntity();
    }

    @Transactional
    public void modifyQuestion(QuestionModifyServiceRequest dto) {
        Question question = questionRepository.findById(dto.getId())
                .orElseThrow(() -> new BaseException(new ApiError("문항을 찾을 수 없습니다.", 2003)));
        question.updateQuestion(dto);
    }
}
