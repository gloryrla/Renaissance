package econo.project1.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
public class KakaoLocalResponseDto {

    @JsonProperty("meta")
    private Meta meta;  //응답 관련 정보
    @JsonProperty("documents")
    private Document[] documents;  //응답 결과

    @Getter
    public static class Meta {
        @JsonProperty("is_end")
        private boolean is_end;  //현재 페이지가 마지맥 페이지인지
        @JsonProperty("pageable_count")
        private int pageable_count;  //노출가능 문서
        @JsonProperty("total_count")
        private int total_count;  //검색된 문서 수
        @JsonProperty("same_name")
        private SameName sameName;  //질의어의 지역 및 키워드 분석 정보
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SameName {
        @JsonProperty("region")
        private String[] region;  //질의어에서 인식된 지역 리스트
        @JsonProperty("keyword")
        private String keyword;  //지역 정보를 제외한 키워드
        @JsonProperty("selected_region")
        private String selectRegion;  //인식된 지역 중 사용된 지역 정보
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        @JsonProperty("id")
        private String id;  //장소 id
        @JsonProperty("place_name")
        private String placeName;  //장소명, 업체명
        @JsonProperty("category_name")
        private String categoryName;  //장소명, 업체명
        @JsonProperty("category_group_name")
        private String categoryGroupName; //그룹핑한 카테고리 그룹명
        @JsonProperty("category_group_code")
        private String categoryGroupCode; //그룹핑한 카테고리 그룹코드
        @JsonProperty("phone")
        private String phone;  //전화번호
        @JsonProperty("address_name")
        private String addressName;  //전체 지번 주소
        @JsonProperty("road_address_name")
        private String roadAddressName; //전체 도로명 주소
        @JsonProperty("x")
        private String x;
        @JsonProperty("y")
        private String y;
        @JsonProperty("place_url")
        private String place_url;  //장소 상세 페이지
        @JsonProperty("distance")
        private String distance;   //중심좌표까지 거리
    }
}


/*
응답
헤더
이름	설명	필수
Content-Type	응답 데이터 타입
content-type: application/json;charset=UTF-8 또는
content-type: text/xml;charset=UTF-8	O
본문
이름	타입	설명
meta
Meta
응답 관련 정보
documents
Document[]
응답 결과
Meta
이름	타입	설명
total_count	Integer	검색어에 검색된 문서 수
pageable_count	Integer	total_count 중 노출 가능 문서 수 (최대: 45)
is_end	Boolean	현재 페이지가 마지막 페이지인지 여부
값이 false면 다음 요청 시 page 값을 증가시켜 다음 페이지 요청 가능
same_name
SameName
질의어의 지역 및 키워드 분석 정보
SameName
이름	타입	설명
region	String[]	질의어에서 인식된 지역의 리스트
예: '중앙로 맛집' 에서 중앙로에 해당하는 지역 리스트
keyword	String	질의어에서 지역 정보를 제외한 키워드
예: '중앙로 맛집' 에서 '맛집'
selected_region	String	인식된 지역 리스트 중, 현재 검색에 사용된 지역 정보
Document
이름	타입	설명
id	String	장소 ID
place_name	String	장소명, 업체명
category_name	String	카테고리 이름
category_group_code	String	중요 카테고리만 그룹핑한 카테고리 그룹 코드
category_group_name	String	중요 카테고리만 그룹핑한 카테고리 그룹명
phone	String	전화번호
address_name	String	전체 지번 주소
road_address_name	String	전체 도로명 주소
x	String	X 좌표값, 경위도인 경우 longitude (경도)
y	String	Y 좌표값, 경위도인 경우 latitude(위도)
place_url	String	장소 상세페이지 URL
distance	String	중심좌표까지의 거리 (단, x,y 파라미터를 준 경우에만 존재)
단위 meter
 */