package econo.project1.group;

public record GroupResponse(
        Long id,
        String name,
        String description,
        String inviteCode,
        Long ownerId
) {
    public static GroupResponse from(Group group) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getInviteCode(),
                group.getOwner() == null ? null : group.getOwner().getId());
    }
}
