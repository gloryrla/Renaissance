package econo.project1.group;

public record GroupMemberResponse(
        Long memberId,
        String name,
        GroupRole role
) {
    public static GroupMemberResponse from(GroupMember gm) {
        return new GroupMemberResponse(
                gm.getMember().getId(),
                gm.getMember().getName(),
                gm.getRole());
    }
}
