package econo.project1.school;

import econo.project1.common.School;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 지원 학교(장소) 목록 조회 API. 투표 생성 시 school 식별자를 고르는 데 사용한다.
 */
@Tag(name = "School", description = "지원 학교(장소) 목록 조회 API")
@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    // 지원 학교 목록 (code + 표시명)
    @Operation(
            summary = "학교 목록 조회",
            description = "투표 생성 시 선택 가능한 지원 학교(장소) 목록을 조회하는 엔드포인트"
    )
    @ApiResponse(responseCode = "200", description = "지원 학교 목록(List<SchoolResponse>)")
    @GetMapping
    public List<SchoolResponse> list() {
        return Arrays.stream(School.values())
                .map(SchoolResponse::from)
                .toList();
    }
}
