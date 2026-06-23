package econo.project1;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Project1Application {

    public static void main(String[] args) {
        SpringApplication.run(Project1Application.class, args);
    }

}


// 4/23 로그인, 그룹 생성 완료 -> 존재하지 않은 그룹 참여시, 이미 들어간 그룹 참여시 에외 처리
// 4/24 카카오맵 api 연동 -> 메뉴 검색시 식당 list 기능