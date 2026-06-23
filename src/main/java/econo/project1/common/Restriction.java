package econo.project1.common;


import lombok.Getter;


/**
 * 2단계 음식 제한 항목(알레르기 + 식이 제한).
 * 하드필터
 */
@Getter
public enum Restriction {
    NUTS("견과루"),
    CRUSTACEAN("갑각류"),
    DAIRY("유제품"),
    GLUTEN("글루텐"),
    EGG("계란"),
    PORK("돼지고기"),
    BEEF("소고기");


    private String label;

    Restriction(String label) {
        this.label = label;
    }


}
