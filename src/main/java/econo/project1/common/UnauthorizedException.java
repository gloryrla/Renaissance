package econo.project1.common;

/** 로그인이 필요할 때. -> HTTP 401 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
