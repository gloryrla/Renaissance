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
    private static final Double X = 126.9071166;
    private static final Double Y = 35.1755091;
    private static final Integer RADIUS = 1500;

    public KakaoLocalResponseDto getRestaurantList(String menu, Integer page) {
        KakaoLocalResponseDto kakaoLocalResponseDto = WebClient.create("https://dapi.kakao.com").get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword")
                        .queryParam("query", menu)
                        .queryParam("category_group_code", CATEGORY)
                        .queryParam("x", X)
                        .queryParam("y", Y)
                        .queryParam("radius", RADIUS)
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




}
