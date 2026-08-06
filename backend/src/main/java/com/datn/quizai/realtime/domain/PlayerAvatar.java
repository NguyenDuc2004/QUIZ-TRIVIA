package com.datn.quizai.realtime.domain;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Bộ nhân vật vui nhộn cho phòng đấu.
 * <p>
 * Mỗi avatar là <b>một emoji + một màu nền</b>, không phải file ảnh. Lý do: không phải tải ảnh về,
 * không phụ thuộc dịch vụ ảnh bên ngoài, chạy được cả khi mất mạng, và emoji đã có sẵn trên mọi
 * hệ điều hành. Đổi lại, đây là biểu tượng vui chứ không phải tranh nhân vật vẽ tay.
 * <p>
 * Server giữ danh sách này để: (1) chặn client tự bịa mã avatar lạ, (2) frontend và các bản xuất
 * báo cáo đều nhìn cùng một nguồn.
 */
public enum PlayerAvatar {

    CAT("🐱", "#f59e0b"),
    DOG("🐶", "#d97706"),
    FOX("🦊", "#ea580c"),
    PANDA("🐼", "#1c1d1f"),
    KOALA("🐨", "#6b7280"),
    TIGER("🐯", "#f97316"),
    UNICORN("🦄", "#a435f0"),
    DRAGON("🐲", "#059669"),
    FROG("🐸", "#16a34a"),
    OWL("🦉", "#7c3aed"),
    PENGUIN("🐧", "#0ea5e9"),
    OCTOPUS("🐙", "#db2777"),
    ROBOT("🤖", "#475569"),
    ALIEN("👽", "#22c55e"),
    NINJA("🥷", "#111827"),
    WIZARD("🧙", "#5624d0"),
    ROCKET("🚀", "#dc2626"),
    GHOST("👻", "#94a3b8");

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String emoji;
    private final String color;

    PlayerAvatar(String emoji, String color) {
        this.emoji = emoji;
        this.color = color;
    }

    public String emoji() {
        return emoji;
    }

    public String color() {
        return color;
    }

    public static PlayerAvatar random() {
        PlayerAvatar[] all = values();
        return all[RANDOM.nextInt(all.length)];
    }

    /** Mã lạ hoặc bỏ trống thì bốc ngẫu nhiên, không báo lỗi — chọn avatar không đáng chặn người chơi. */
    public static PlayerAvatar parseOrRandom(String code) {
        if (code == null || code.isBlank()) {
            return random();
        }
        return Arrays.stream(values())
                .filter(avatar -> avatar.name().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseGet(PlayerAvatar::random);
    }

    public static List<PlayerAvatar> all() {
        return List.of(values());
    }
}
