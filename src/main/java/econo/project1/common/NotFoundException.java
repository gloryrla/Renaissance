package econo.project1.common;

/** 요청한 리소스가 없을 때. -> HTTP 404 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
