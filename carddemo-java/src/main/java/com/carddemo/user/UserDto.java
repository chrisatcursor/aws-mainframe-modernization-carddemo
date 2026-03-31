package com.carddemo.user;

public record UserDto(String userId, String firstName, String lastName, String userType) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserType()
        );
    }
}
