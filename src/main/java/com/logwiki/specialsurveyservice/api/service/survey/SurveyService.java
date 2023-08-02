package com.logwiki.specialsurveyservice.api.service.survey;


import com.logwiki.specialsurveyservice.api.controller.sse.response.SurveyAnswerResponse;
import com.logwiki.specialsurveyservice.api.service.giveaway.GiveawayService;
import com.logwiki.specialsurveyservice.api.service.account.AccountService;
import com.logwiki.specialsurveyservice.api.service.survey.request.GiveawayAssignServiceRequest;
import com.logwiki.specialsurveyservice.api.service.survey.request.SurveyCreateServiceRequest;
import com.logwiki.specialsurveyservice.api.service.survey.response.SurveyDetailResponse;
import com.logwiki.specialsurveyservice.api.service.survey.response.SurveyResponse;
import com.logwiki.specialsurveyservice.api.service.targetnumber.TargetNumberService;
import com.logwiki.specialsurveyservice.api.service.targetnumber.request.TargetNumberCreateServiceRequest;
import com.logwiki.specialsurveyservice.domain.account.Account;
import com.logwiki.specialsurveyservice.domain.account.AccountRepository;
import com.logwiki.specialsurveyservice.domain.accountcode.AccountCode;
import com.logwiki.specialsurveyservice.domain.accountcode.AccountCodeRepository;
import com.logwiki.specialsurveyservice.domain.accountcode.AccountCodeType;
import com.logwiki.specialsurveyservice.domain.giveaway.GiveawayRepository;
import com.logwiki.specialsurveyservice.domain.survey.Survey;
import com.logwiki.specialsurveyservice.domain.survey.SurveyRepository;
import com.logwiki.specialsurveyservice.domain.surveycategory.SurveyCategory;
import com.logwiki.specialsurveyservice.domain.surveycategory.SurveyCategoryRepository;
import com.logwiki.specialsurveyservice.domain.surveycategory.SurveyCategoryType;
import com.logwiki.specialsurveyservice.domain.surveycategory.SurveyCategoryType;
import com.logwiki.specialsurveyservice.domain.surveygiveaway.SurveyGiveaway;
import com.logwiki.specialsurveyservice.domain.surveyresult.SurveyResult;
import com.logwiki.specialsurveyservice.domain.surveytarget.SurveyTarget;
import com.logwiki.specialsurveyservice.domain.targetnumber.TargetNumber;
import com.logwiki.specialsurveyservice.domain.targetnumber.TargetNumberRepository;
import com.logwiki.specialsurveyservice.exception.BaseException;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final AccountService accountService;
    private final GiveawayRepository giveawayRepository;
    private final TargetNumberService targetNumberService;
    private final AccountCodeRepository accountCodeRepository;
    private final SurveyCategoryRepository surveyCategoryRepository;

    private final GiveawayService giveawayService;

    private final TargetNumberRepository targetNumberRepository;

    private final AccountRepository accountRepository;
    private static final double MAXPROBABILITY = 100.0;
    private static final String LOSEPRODUCT = "꽝";



    public SurveyResponse addSurvey(SurveyCreateServiceRequest dto) {
        Account account = accountService.getCurrentAccountBySecurity();

        Survey survey = dto.toEntity(account.getId());

        for (AccountCodeType accountCodeType : dto.getSurveyTarget()) {
            AccountCode accountCode = accountCodeRepository.findAccountCodeByType(accountCodeType).orElseThrow(
                    () -> new BaseException("없는 나이,성별 코드 입니다.", 3007)
            );

            SurveyTarget surveyTarget = SurveyTarget.builder()
                    .survey(survey)
                    .accountCode(accountCode)
                    .build();
            survey.addSurveyTarget(surveyTarget);
        }

        SurveyCategory surveyCategoryByType = surveyCategoryRepository.findSurveyCategoryByType(
                dto.getType());
        survey.addSurveyCategory(surveyCategoryByType);

        List<GiveawayAssignServiceRequest> giveawayAssignServiceRequests = dto.getGiveaways();
        List<SurveyGiveaway> surveyGiveaways = getSurveyGiveaways(survey,
                giveawayAssignServiceRequests);
        survey.addSurveyGiveaways(surveyGiveaways);

        TargetNumberCreateServiceRequest targetNumberCreateServiceRequest = TargetNumberCreateServiceRequest.create(
                dto.getClosedHeadCount(), giveawayAssignServiceRequests, survey);
        List<TargetNumber> targetNumbers = targetNumberService.createTargetNumbers(
                targetNumberCreateServiceRequest);
        survey.addTargetNumbers(targetNumbers);
        surveyRepository.save(survey);


        return SurveyResponse.from(survey);
    }

    private List<SurveyGiveaway> getSurveyGiveaways(Survey survey,
            List<GiveawayAssignServiceRequest> giveawayAssignServiceRequests) {

        return giveawayAssignServiceRequests.stream()
                .map(giveaway -> SurveyGiveaway.create(giveaway.getCount(), survey,
                        giveawayRepository.findById(giveaway.getId())
                                .orElseThrow(
                                        () -> new BaseException("등록되어 있지 않은 당첨 상품을 포함하고 있습니다.",
                                                5003))))
                .collect(Collectors.toList());
    }

    public List<SurveyResponse> getRecommendNormalSurvey() {
        List<Survey> surveys = getRecommendSurveysBySurveyCategoryType(SurveyCategoryType.NORMAL);

        sortByEndTime(surveys);
        return surveys.stream()
                .map(SurveyResponse::from)
                .collect(Collectors.toList());
    }

    public List<SurveyResponse> getRecommendInstantSurvey() {
        List<Survey> surveys = getRecommendSurveysBySurveyCategoryType(SurveyCategoryType.INSTANT_WIN);

        sortByWinningPercent(surveys);

        return surveys.stream()
                .map(SurveyResponse::from)
                .collect(Collectors.toList());
    }

    public List<SurveyResponse> getRecommendShortTimeSurvey() {
        List<Survey> surveys = getAllRecommendSurveys();

        sortByRequiredTimeForSurvey(surveys);

        return surveys.stream()
                .map(SurveyResponse::from)
                .collect(Collectors.toList());
    }

    private List<Survey> getRecommendSurveysBySurveyCategoryType(SurveyCategoryType surveyCategoryType) {
        Account account = accountService.getCurrentAccountBySecurity();
        Long genderId = accountCodeRepository.findAccountCodeByType(account.getGender())
                .orElseThrow(() -> new BaseException("성별 코드가 올바르지 않습니다.", 2004))
                .getId();
        Long ageId = accountCodeRepository.findAccountCodeByType(account.getAge())
                .orElseThrow(() -> new BaseException("나이 코드가 올바르지 않습니다.", 2005))
                .getId();

        return surveyRepository.findRecommendSurvey(surveyCategoryType.toString(),
                genderId, ageId);
    }

    private List<Survey> getAllRecommendSurveys() {
        Account account = accountService.getCurrentAccountBySecurity();
        Long genderId = accountCodeRepository.findAccountCodeByType(account.getGender())
                .orElseThrow(() -> new BaseException("성별 코드가 올바르지 않습니다.", 2004))
                .getId();
        Long ageId = accountCodeRepository.findAccountCodeByType(account.getAge())
                .orElseThrow(() -> new BaseException("나이 코드가 올바르지 않습니다.", 2005))
                .getId();

        return surveyRepository.findRecommendSurvey(genderId, ageId);
    }

    private static void sortByEndTime(List<Survey> surveys) {
        surveys.sort((survey1, survey2) -> {
            LocalDateTime survey1EndTime = survey1.getEndTime();
            LocalDateTime survey2EndTime = survey2.getEndTime();
            return survey1EndTime.compareTo(survey2EndTime);
        });
    }

    private static void sortByWinningPercent(List<Survey> surveys) {
        surveys.sort((survey1, survey2) -> {
            int survey1GiveawayCount = survey1.getTotalGiveawayCount();
            int survey2GiveawayCount = survey2.getTotalGiveawayCount();
            double survey1WinningPercent =
                    (double) survey1GiveawayCount / survey1.getClosedHeadCount();
            double survey2WinningPercent =
                    (double) survey2GiveawayCount / survey2.getClosedHeadCount();
            return Double.compare(survey2WinningPercent, survey1WinningPercent);
        });
    }

    private static void sortByRequiredTimeForSurvey(List<Survey> surveys) {
        surveys.sort(Comparator.comparingInt(Survey::getRequiredTimeInSeconds));
    }




    public List<SurveyAnswerResponse> getSurveyAnswers(Long surveyId) {
        Optional<Survey> targetSurveyOptional = surveyRepository.findById(surveyId);
        if(targetSurveyOptional.isEmpty()) {
            throw new BaseException("없는 설문입니다.",3005);
        }
        Survey targetSurvey = targetSurveyOptional.get();
        SurveyResponse surveyResponse = SurveyResponse.from(targetSurvey);

        List<SurveyAnswerResponse> surveyResponseResults = new ArrayList<>();
        if(targetSurvey.getSurveyResults() != null) {
            for (SurveyResult surveyResult : targetSurvey.getSurveyResults()) {
                String giveawayName;
                boolean isWin = false;
                Optional<TargetNumber> tn = targetNumberRepository.findFirstBySurveyAndNumber(
                        targetSurvey, surveyResult.getSubmitOrder());
                if (tn.isPresent()) {
                    isWin = true;
                    giveawayName = tn.get().getGiveaway().getName();
                } else {
                    giveawayName = LOSEPRODUCT;
                }

                surveyResponseResults.add(SurveyAnswerResponse.from(surveyResult,giveawayName,isWin));

            }
        }
        return surveyResponseResults;
    }
    public SurveyDetailResponse getSurveyDetail(Long surveyId) {
        Optional<Survey> targetSurveyOptional =  surveyRepository.findById(surveyId);

        if(targetSurveyOptional.isEmpty()) {
            throw new BaseException("없는 설문입니다." , 3005);
        }
        Survey targetSurvey = targetSurveyOptional.get();
        SurveyResponse surveyResponse = SurveyResponse.from(targetSurvey);
        List<SurveyGiveaway> surveyGiveaways = targetSurvey.getSurveyGiveaways();
        List<String> giveawayNames = new ArrayList<>();
        for(SurveyGiveaway surveyGiveaway : surveyGiveaways) {
            giveawayNames.add(surveyGiveaway.getGiveaway().getName());
        }

        Optional<Account> writerAccount =  accountRepository.findById(targetSurvey.getWriter());
        if(writerAccount.isEmpty()){
            throw new BaseException("설문 작성자가 존재하지 않습니다.", 3013);
        }
        return SurveyDetailResponse.of(targetSurvey,surveyResponse.getWinningPercent(),giveawayNames,writerAccount.get().getName());
    }
}
