package econo.project1.common;

/** 로그인은 했지만 권한이 없을 때(예: 그룹원이 아님). -> HTTP 403 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
