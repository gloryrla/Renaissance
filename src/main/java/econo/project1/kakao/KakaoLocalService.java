package econo.project1.kakao;


import econo.project1.kakao.KakaoLocalResponseDto;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class KakaoLocalService {

    @Value("${kakao.client_id}")
    private String clientId;


    private static final String CATEGORY = "FD6";
    private static final double DEFAULT_X = 126.9071166;
    private static final double DEFAULT_Y = 35.1755091;
    private static final int DEFAULT_RADIUS = 1500;

    // 학교 좌표 등 호출부가 지정한 위치 기준으로 검색
    public KakaoLocalResponseDto getRestaurantList(String menu, Integer page,
                                                   double x, double y, int radius) {
        KakaoLocalResponseDto kakaoLocalResponseDto = WebClient.create("https://dapi.kakao.com").get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword")
                        .queryParam("query", menu)
                        .queryParam("category_group_code", CATEGORY)
                        .queryParam("x", x)
                        .queryParam("y", y)
                        .queryParam("radius", radius)
                        .queryParam("sort", "distance")  // 중심 좌표에서 가까운 순
                        .queryParam("page", page)
                        .queryParam("size", 10)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + clientId)
                .retrieve()
                //TODO : Custom Exception
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new RuntimeException("Invalid Parameter")))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new RuntimeException("Internal Server Error")))
                .bodyToMono(KakaoLocalResponseDto.class)
                .block();

        return kakaoLocalResponseDto;
    }

    // 위치 미지정 시 기본 좌표(하위 호환)
    public KakaoLocalResponseDto getRestaurantList(String menu, Integer page) {
        return getRestaurantList(menu, page, DEFAULT_X, DEFAULT_Y, DEFAULT_RADIUS);
    }




}
