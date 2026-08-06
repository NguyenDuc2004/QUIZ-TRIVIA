package com.datn.quizai.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Optional;

/**
 * Tìm địa chỉ IPv4 mà <b>máy khác trong cùng mạng LAN</b> gọi tới được.
 * <p>
 * Cần cho mã QR phòng đấu: điện thoại quét QR không hiểu {@code localhost} (với nó, localhost là
 * chính chiếc điện thoại). QR phải mang địa chỉ LAN thật.
 * <p>
 * <b>Cách tìm:</b> mở một UDP socket rồi {@code connect} tới một địa chỉ ngoài Internet. UDP
 * connect <i>không gửi gói nào</i> — nó chỉ khiến hệ điều hành tra bảng định tuyến và chọn card
 * mạng sẽ dùng. Đọc địa chỉ cục bộ của socket đó là ra đúng card mạng đang thật sự nối ra ngoài.
 * <p>
 * Cách này ăn hơn việc liệt kê {@code NetworkInterface} rồi đoán: máy dev thường có thêm card ảo
 * của VMware, VirtualBox, WSL, Docker — tất cả đều là IPv4 riêng "trông hợp lệ" nhưng điện thoại
 * không gọi tới được.
 */
@Component
public class NetworkAddressResolver {

    private static final Logger log = LoggerFactory.getLogger(NetworkAddressResolver.class);

    /** Chỉ dùng để hệ điều hành tra bảng định tuyến — không có gói tin nào được gửi tới đây. */
    private static final String ROUTE_PROBE_HOST = "8.8.8.8";
    private static final int ROUTE_PROBE_PORT = 53;

    /** Kết quả không đổi trong một lần chạy, tra một lần rồi giữ lại. */
    private final Optional<String> lanAddress;

    public NetworkAddressResolver() {
        this.lanAddress = detect();
        log.info("Địa chỉ LAN dùng cho mã QR: {}", lanAddress.orElse("(không tìm được)"));
    }

    /** @return địa chỉ IPv4 trong LAN, rỗng nếu máy không nối mạng nào */
    public Optional<String> lanAddress() {
        return lanAddress;
    }

    private Optional<String> detect() {
        return routedAddress().or(this::firstNonVirtualAddress);
    }

    private Optional<String> routedAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName(ROUTE_PROBE_HOST), ROUTE_PROBE_PORT);
            InetAddress local = socket.getLocalAddress();

            if (local != null && !local.isAnyLocalAddress() && !local.isLoopbackAddress()) {
                return Optional.of(local.getHostAddress());
            }
        } catch (Exception e) {
            log.debug("Không tra được card mạng qua bảng định tuyến: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Dự phòng khi máy không có đường ra Internet: quét card mạng và bỏ những card ảo.
     * Kém chính xác hơn cách trên nên chỉ dùng khi cách trên thất bại.
     */
    private Optional<String> firstNonVirtualAddress() {
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual() || isVirtualName(nic)) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address.getAddress().length == 4 && !address.isLoopbackAddress()) {
                        return Optional.of(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            log.debug("Không liệt kê được card mạng: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /** Card ảo của máy ảo/WSL/Docker: IPv4 trông hợp lệ nhưng điện thoại không gọi tới được. */
    private boolean isVirtualName(NetworkInterface nic) {
        String name = (nic.getDisplayName() == null ? "" : nic.getDisplayName()).toLowerCase();
        return name.contains("vmware") || name.contains("virtual") || name.contains("vethernet")
                || name.contains("hyper-v") || name.contains("wsl") || name.contains("docker")
                || name.contains("loopback") || name.contains("tap") || name.contains("tunnel");
    }
}
