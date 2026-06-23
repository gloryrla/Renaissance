package econo.project1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:ctxtest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.session.store-type=none",
		"kakao.client_id=test", "kakao.secret_id=test",
		"naver.client_id=test", "naver.secret_id=test"
})
class Project1ApplicationTests {

	@Test
	void contextLoads() {
	}

}
