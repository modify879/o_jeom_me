package com.ojeomme.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ojeomme.domain.user.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginCheckResponseDto {

    private final boolean result;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Long id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String nickname;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String profile;

    @Builder
    public LoginCheckResponseDto(boolean result, Long id, String nickname, String profile) {
        this.result = result;
        this.id = id;
        this.nickname = nickname;
        this.profile = profile;
    }

    public static LoginCheckResponseDto success(User user) {
        return LoginCheckResponseDto.builder()
                .result(true)
                .id(user.getId())
                .nickname(user.getNickname())
                .profile(user.getProfile())
                .build();
    }

    public static LoginCheckResponseDto fail() {
        return LoginCheckResponseDto.builder()
                .result(false)
                .build();
    }
}
