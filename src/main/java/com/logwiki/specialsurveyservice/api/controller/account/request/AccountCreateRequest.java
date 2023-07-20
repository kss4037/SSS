package com.logwiki.specialsurveyservice.api.controller.account.request;

import com.logwiki.specialsurveyservice.api.service.account.request.AccountCreateServiceRequest;
import com.logwiki.specialsurveyservice.domain.sex.Sex;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@NoArgsConstructor
public class AccountCreateRequest {

  @NotEmpty(message = "이메일은 필수입니다.")
  @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+.[A-Za-z]{2,6}$", message = "이메일 형식에 맞지 않습니다.")
  private String email;

  @NotEmpty(message = "패스워드는 필수입니다.")
  @Size(min = 3, max = 30, message = "패스워드의 길이는 3이상 30이하 입니다.")
  private String password;

  @NotEmpty(message = "성별은 필수입니다.")
  private Sex sex;

  @NotEmpty(message = "이름은 필수입니다.")
  private String name;

  @NotEmpty(message = "휴대폰 번호는 필수입니다.")
  @Pattern(regexp = "[0-9]{10,11}", message = "10~11자리의 숫자만 입력가능합니다")
  private String phoneNumber;

  @NotEmpty(message = "생년월일은 필수입니다.")
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate birthday;

  @Builder
  private AccountCreateRequest(String email, String password, Sex sex, String name, String phoneNumber, LocalDate birthday) {
    this.email = email;
    this.password = password;
    this.sex = sex;
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.birthday = birthday;
  }

  public AccountCreateServiceRequest toServiceRequest() {
    return AccountCreateServiceRequest.builder()
        .email(email)
        .password(password)
        .sex(sex)
        .name(name)
        .phoneNumber(phoneNumber)
        .birthday(birthday)
        .build();
  }
}
