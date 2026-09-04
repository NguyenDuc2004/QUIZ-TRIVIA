import { theme } from 'antd'
import type { ThemeConfig } from 'antd'

/**
 * Token giao diện — nguồn duy nhất cho màu/bo góc/kiểu chữ của Ant Design.
 * Quy ước đầy đủ ở docs/ui-design-system.md. KHÔNG hardcode màu trong component.
 */
export const colors = {
  ink: '#0f172a',
  inkSoft: '#64748b',
  brand: '#a435f0',
  brandStrong: '#5624d0',
  /** Viền thẻ/panel — siêu mờ. Xem chú thích cùng tên trong `index.css`. */
  line: 'rgba(226, 232, 240, 0.6)',
  /** Viền ô nhập/ô chọn — đậm hơn, vì mất ranh giới ở đây là mất chỗ để bấm vào mà gõ. */
  lineStrong: '#e2e8f0',
  canvas: '#f8fafc',
  surface: '#ffffff',
  surfaceSubtle: '#f1f5f9',
  star: '#e59819',
  rating: '#b4690e',
  badge: '#eceb98',
} as const

/**
 * Thang bo góc và bóng đổ — giữ **cùng giá trị** với các token `--radius-*` / `--shadow-*` trong
 * `index.css`. Hai nơi vì hai hệ khác nhau (Ant Design nhận qua JavaScript, Tailwind qua biến CSS),
 * nhưng lệch nhau thì một thẻ Ant Design đứng cạnh một thẻ dựng bằng Tailwind sẽ bo khác nhau.
 */
export const shape = {
  radiusPanel: 24,
  radiusCard: 16,
  radiusControl: 12,
  radiusSmall: 8,
  shadowSoft: '0 1px 2px rgb(15 23 42 / 0.04), 0 8px 30px rgb(15 23 42 / 0.04)',
  shadowLifted: '0 2px 4px rgb(15 23 42 / 0.05), 0 12px 32px rgb(15 23 42 / 0.09)',
} as const

/**
 * Màu cho chế độ tối. Giữ **cùng tên khoá** với `colors` ở trên để chỗ dùng không phải phân biệt.
 *
 * Giá trị khớp với khối `:root[data-theme='dark']` trong `index.css`. Hai nơi vì hai hệ khác nhau —
 * Ant Design nhận màu qua JavaScript, Tailwind qua biến CSS — nhưng phải cùng giá trị, nếu không thẻ
 * của Ant Design và thẻ dựng bằng Tailwind cạnh nhau sẽ khác màu nền.
 */
export const darkColors = {
  ink: '#e8eaed',
  inkSoft: '#9aa0a6',
  brand: '#c084fc',
  brandStrong: '#b07af0',
  line: 'rgba(255, 255, 255, 0.1)',
  lineStrong: 'rgba(255, 255, 255, 0.18)',
  canvas: '#0f172a',
  surface: '#1e293b',
  surfaceSubtle: '#172033',
  star: '#f0b429',
  rating: '#e0a020',
  badge: '#4a4a2a',
} as const

const FONT_FAMILY = "'Inter Variable', 'Inter', system-ui, -apple-system, 'Segoe UI', sans-serif"

export const appTheme: ThemeConfig = {
  token: {
    colorPrimary: colors.brand,
    colorLink: colors.brandStrong,
    colorLinkHover: colors.brand,
    colorTextBase: colors.ink,
    colorTextSecondary: colors.inkSoft,
    colorBorder: colors.lineStrong,
    colorBorderSecondary: colors.line,
    colorBgLayout: colors.canvas,

    fontFamily: FONT_FAMILY,
    fontSize: 14,

    /*
     * Thang bo góc ba bậc (đổi 05/09/2026, thay cho "4px cho mọi thứ").
     *
     * `borderRadius` là bậc dùng cho THÀNH PHẦN ĐIỀU KHIỂN — nút, ô nhập, ô chọn, thẻ nhãn.
     * `borderRadiusLG` là bậc dùng cho KHỐI CHỨA — card, modal, drawer, popover.
     * Ant Design tự chọn bậc nào cho component nào, nên đặt đúng hai con số này là đủ cho cả thư viện.
     */
    borderRadius: shape.radiusControl,
    borderRadiusLG: shape.radiusCard,
    borderRadiusSM: shape.radiusSmall,
    borderRadiusXS: 6,

    controlHeight: 40,
    controlHeightLG: 48,

    // Bóng mờ toả rộng thay cho viền kẻ — xem `--shadow-soft` trong index.css
    boxShadow: shape.shadowSoft,
    boxShadowSecondary: shape.shadowLifted,
    boxShadowTertiary: shape.shadowSoft,
  },

  components: {
    /**
     * Nút hành động chính của Udemy là **nền đen**, còn tím chỉ dùng cho link/nhấn mạnh.
     * Vì vậy ghi đè colorPrimary riêng cho Button, giữ colorPrimary toàn cục là tím.
     */
    Button: {
      colorPrimary: colors.ink,
      colorPrimaryHover: '#3e4143',
      colorPrimaryActive: '#000000',
      defaultColor: colors.ink,
      defaultBorderColor: colors.ink,
      fontWeight: 700,
      primaryShadow: 'none',
      defaultShadow: 'none',
      dangerShadow: 'none',
    },
    /**
     * Thanh tiến độ lấy màu từ `colorInfo` (xanh mặc định của antd), KHÔNG phải `colorPrimary` — nên đặt
     * `colorPrimary` tím ở trên không đủ. Đặt ở đây thay vì truyền `strokeColor` ở từng chỗ dùng: một màu
     * lệch bảng màu thì lệch ở mọi trang, và sửa lẻ thì trang thêm sau lại lệch tiếp.
     */
    Progress: {
      defaultColor: colors.brand,
    },
    Layout: {
      /* Thanh trên vẫn TRẮNG, còn thân trang là nền kem: chính chênh lệch này làm thanh trên nổi lên
         như một lớp riêng, thay cho đường kẻ dưới chân nó. */
      headerBg: colors.surface,
      headerHeight: 72,
      headerPadding: '0 24px',
      bodyBg: colors.canvas,
    },
    Menu: {
      itemBg: 'transparent',
      itemColor: colors.ink,
      itemSelectedColor: colors.brandStrong,
      itemHoverColor: colors.brandStrong,
      horizontalItemSelectedColor: colors.brandStrong,
      activeBarBorderWidth: 0,
    },
    Table: {
      headerBg: colors.surfaceSubtle,
      headerColor: colors.ink,
      borderColor: colors.line,
      rowHoverBg: colors.surfaceSubtle,
      headerBorderRadius: shape.radiusCard,
    },
    Card: {
      colorBorderSecondary: colors.line,
      paddingLG: 20,
      boxShadowTertiary: shape.shadowSoft,
    },
    /*
     * Ô nhập và ô chọn dùng viền ĐẬM HƠN thẻ (`lineStrong`).
     *
     * Một cái thẻ mờ ranh giới thì chỉ hơi khó nhìn; một ô nhập mờ ranh giới thì người dùng không biết
     * bấm vào đâu để gõ. WCAG 1.4.11 đòi thành phần giao diện tương phản tối thiểu 3:1 với nền, mà
     * `slate-200/60` trên nền trắng không đạt — đây là chỗ duy nhất trong bản nâng cấp này cố ý đi
     * chệch khỏi "viền siêu mờ cho mọi thứ".
     */
    Input: {
      colorBorder: colors.lineStrong,
      activeBorderColor: colors.brand,
      hoverBorderColor: colors.inkSoft,
    },
    Select: {
      colorBorder: colors.lineStrong,
    },
    /* Thẻ nhãn bo tròn hẳn, cùng hình dáng với `<Pill>` để hai thứ không đá nhau trên cùng một bảng */
    Tag: {
      defaultBg: colors.surfaceSubtle,
      defaultColor: colors.ink,
      borderRadiusSM: 9999,
    },
    Typography: {
      titleMarginBottom: '0.4em',
      titleMarginTop: '0',
    },
    Modal: {
      titleFontSize: 19,
    },
  },
}

/**
 * Cấu hình Ant Design cho **chế độ tối**.
 *
 * Dựng bằng cách chồng lên `appTheme` chứ không viết lại từ đầu: bo góc 4px, chiều cao control, kiểu chữ
 * và toàn bộ phần `components` là quy ước của dự án, không liên quan tới sáng hay tối. Viết lại từ đầu
 * nghĩa là hai bản sẽ trôi xa nhau ngay lần sửa quy ước tiếp theo.
 *
 * `darkAlgorithm` lo phần còn lại — hàng chục màu nội bộ mà dự án không khai (nền dropdown, màu chữ khi
 * bị vô hiệu hoá, nền khi rê chuột…). Tự đặt tay từng cái là công việc không bao giờ xong.
 */
export const darkTheme: ThemeConfig = {
  ...appTheme,
  algorithm: theme.darkAlgorithm,
  token: {
    ...appTheme.token,
    colorPrimary: darkColors.brand,
    colorLink: darkColors.brandStrong,
    colorLinkHover: darkColors.brand,
    // Khai CẢ `colorTextBase` LẪN `colorText`/`colorTextHeading`.
    //
    // `colorTextBase` là hạt giống để thuật toán suy ra hàng chục màu chữ khác nhau, nhưng bản thân
    // tiêu đề (`Typography.Title`) đọc `colorTextHeading`, còn thân bài đọc `colorText`. Chỉ đặt hạt
    // giống thì hai màu kia vẫn được suy ra — nhưng suy ra kèm độ mờ, nên tiêu đề trên nền tối trông
    // nhợt hẳn đi. Đặt thẳng thì tiêu đề rõ đúng như mong muốn.
    colorTextBase: darkColors.ink,
    colorText: darkColors.ink,
    colorTextHeading: darkColors.ink,
    colorTextSecondary: darkColors.inkSoft,
    colorTextDescription: darkColors.inkSoft,
    colorBorder: darkColors.line,
    colorBorderSecondary: darkColors.line,
    colorBgLayout: darkColors.canvas,
    colorBgContainer: darkColors.surface,
    colorBgElevated: darkColors.surface,
    colorBgBase: darkColors.canvas,
  },
  components: {
    ...appTheme.components,
    /**
     * Nút hành động chính ở chế độ tối KHÔNG dùng nền đen.
     *
     * Ở chế độ sáng nền đen là thứ nổi bật nhất trên trang trắng. Ở chế độ tối thì ngược hẳn: nút đen
     * trên nền xám than gần như biến mất, và nút quan trọng nhất màn hình lại là thứ khó thấy nhất.
     * Đảo lại — nền sáng, chữ tối — giữ đúng ý đồ "nút chính là thứ tương phản mạnh nhất".
     */
    Button: {
      ...appTheme.components?.Button,
      colorPrimary: darkColors.ink,
      colorPrimaryHover: '#ffffff',
      colorPrimaryActive: '#c8ccd1',
      primaryColor: '#17181c',
      defaultColor: darkColors.ink,
      defaultBorderColor: darkColors.line,
    },
    Tag: {
      defaultBg: darkColors.surfaceSubtle,
      defaultColor: darkColors.ink,
    },
    /*
     * BẮT BUỘC phải khai lại. `darkTheme` kế thừa `...appTheme.components`, mà trong đó `Layout` đặt
     * cứng `headerBg: '#ffffff'` và `bodyBg: '#ffffff'`. Kế thừa nguyên vẹn nghĩa là nền thanh trên và
     * nền thân trang vẫn TRẮNG ở chế độ tối, dù `colorBgLayout` đã đổi — token riêng của component
     * thắng token toàn cục.
     *
     * Đây là mảng trắng cuối cùng còn lại sau khi đổi toàn bộ `bg-white` sang token: nó không đến từ
     * Tailwind mà từ chính bảng token của Ant Design.
     */
    Layout: {
      ...appTheme.components?.Layout,
      headerBg: darkColors.surface,
      bodyBg: darkColors.canvas,
    },
    /* Bảng: nền tiêu đề cột và nền khi rê chuột phải tối theo, nếu không thì hàng tiêu đề sáng trưng */
    Table: {
      ...appTheme.components?.Table,
      headerBg: darkColors.surfaceSubtle,
      headerColor: darkColors.ink,
      rowHoverBg: '#243247',
      borderColor: darkColors.line,
    },
    /* Menu xổ xuống và ngăn kéo dùng `colorBgElevated`, nhưng mục đang chọn cần nền riêng */
    Menu: {
      ...appTheme.components?.Menu,
      itemSelectedBg: '#243247',
      itemHoverBg: '#1f2b3e',
      itemColor: darkColors.ink,
      itemSelectedColor: darkColors.brand,
    },
  },
}
