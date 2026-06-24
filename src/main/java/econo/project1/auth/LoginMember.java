package econo.project1.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 Authorization 헤더의 JWT 에서 추출한 로그인 memberId 를 주입한다.
 * 토큰이 없거나 유효하지 않으면 401.
 * 사용: public Xxx method(@LoginMember Long memberId)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {
}
