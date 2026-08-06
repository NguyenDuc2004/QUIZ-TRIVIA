package com.datn.quizai.realtime.dto;

import com.datn.quizai.realtime.domain.PlayerAvatar;

import java.util.List;

/** Một lựa chọn avatar cho giao diện — frontend vẽ emoji trên nền màu, không tải ảnh. */
public record AvatarOption(String code, String emoji, String color) {

    public static List<AvatarOption> catalog() {
        return PlayerAvatar.all().stream()
                .map(avatar -> new AvatarOption(avatar.name(), avatar.emoji(), avatar.color()))
                .toList();
    }
}
