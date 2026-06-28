package econo.project1.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI(/swagger-ui.html) + OpenAPI 문서(/v3/api-docs) 설정.
 * 보호된 API는 Authorization: Bearer JWT 가 필요하므로, Swagger 의 "Authorize" 버튼으로 토큰을 넣을 수 있게 한다.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Renaissance API",
                version = "v1",
                description = "그룹 외식 메뉴 추천·투표 백엔드 REST API. 상세는 docs/api-spec.md, docs/feature-spec.md 참고."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
