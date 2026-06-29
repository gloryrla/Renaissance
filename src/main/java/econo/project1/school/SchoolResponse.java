package econo.project1.school;

import econo.project1.common.School;

/**
 * 학교 목록 응답. 프론트 드롭다운용 — 좌표/반경은 노출하지 않는다.
 * code 는 enum name()(요청 시 식별자), displayName 은 표시명.
 */
public record SchoolResponse(
        String code,
        String displayName
) {
    public static SchoolResponse from(School school) {
        return new SchoolResponse(school.name(), school.getDisplayName());
    }
}
