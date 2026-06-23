package econo.project1.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoTokenResponseDto {

    @JsonProperty("token_type")
    public String tokenType;
    @JsonProperty("access_token")
    public String accessToken;
    @JsonProperty("id_token")
    public String idToken;
    @JsonProperty("expires_in")
    public Integer expiresIn;
    @JsonProperty("refresh_token")
    public String refreshToken;
    @JsonProperty("refresh_token_expires_in")
    public Integer refreshTokenExpiresIn;
    @JsonProperty("scope")
    public String scope;
}


/*
token_type	String	토큰 타입, bearer로 고정	O
access_token	String	사용자 액세스 토큰 값	O
id_token	String	ID 토큰 값
OpenID Connect 확장 기능으로 발급하는 ID 토큰, Base64 인코딩 된 사용자 인증 정보 포함

제공 조건: OpenID Connect가 활성화 된 앱의 토큰 발급 요청인 경우
또는 scope에 openid를 포함한 동의항목 추가 동의 요청을 거친 토큰 발급 요청인 경우	X
expires_in	Integer	액세스 토큰과 ID 토큰의 만료 시간(초)

참고: 액세스 토큰과 ID 토큰의 만료 시간은 동일	O
refresh_token	String	사용자 리프레시 토큰 값	O
refresh_token_expires_in	Integer	리프레시 토큰 만료 시간(초)	O
scope
 */