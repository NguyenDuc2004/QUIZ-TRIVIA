import { theme } from 'antd'
import type { ThemeConfig } from 'antd'

/**
 * Token giao diện — nguồn duy nhất cho màu/bo góc/kiểu chữ của Ant Design.
 * Quy ước đầy đủ ở docs/ui-design-system.md. KHÔNG hardcode màu trong component.
 */
export const colors = {
  ink: '#1c1d1f',
  inkSoft: '#6a6f73',
  brand: '#a435f0',
  brandStrong: '#5624d0',
  line: '#d1d7dc',
  surfaceSubtle: '#f7f9fa',
  star: '#e59819',
  rating: '#b4690e',
  badge: '#eceb98',
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
  line: '#34363b',
  surface: '#202126',
  surfaceSubtle: '#17181c',
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
    colorBorder: colors.line,
    colorBorderSecondary: colors.line,
    colorBgLayout: '#ffffff',

    fontFamily: FONT_FAMILY,
    fontSize: 14,

    // Vuông vắn kiểu Udemy: mọi bo góc 4px
    borderRadius: 4,
    borderRadiusLG: 4,
    borderRadiusSM: 2,
    borderRadiusXS: 2,

    controlHeight: 40,
    controlHeightLG: 48,

    // Dùng viền thay đổ bóng
    boxShadow: 'none',
    boxShadowSecondary: '0 2px 8px rgba(0, 0, 0, 0.08)',
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
      headerBg: '#ffffff',
      headerHeight: 72,
      headerPadding: '0 24px',
      bodyBg: '#ffffff',
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
    },
    Card: {
      colorBorderSecondary: colors.line,
      paddingLG: 20,
    },
    Input: {
      colorBorder: colors.line,
      activeBorderColor: colors.brand,
      hoverBorderColor: colors.inkSoft,
    },
    Select: {
      colorBorder: colors.line,
    },
    Tag: {
      defaultBg: colors.surfaceSubtle,
      defaultColor: colors.ink,
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
    colorBgLayout: darkColors.surfaceSubtle,
    colorBgContainer: darkColors.surface,
    colorBgElevated: darkColors.surface,
    colorBgBase: darkColors.surfaceSubtle,
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
      bodyBg: darkColors.surfaceSubtle,
    },
    /* Bảng: nền tiêu đề cột và nền khi rê chuột phải tối theo, nếu không thì hàng tiêu đề sáng trưng */
    Table: {
      ...appTheme.components?.Table,
      headerBg: darkColors.surfaceSubtle,
      headerColor: darkColors.ink,
      rowHoverBg: '#2a2b31',
      borderColor: darkColors.line,
    },
    /* Menu xổ xuống và ngăn kéo dùng `colorBgElevated`, nhưng mục đang chọn cần nền riêng */
    Menu: {
      ...appTheme.components?.Menu,
      itemSelectedBg: '#2a2b31',
      itemHoverBg: '#26272c',
      itemColor: darkColors.ink,
      itemSelectedColor: darkColors.brand,
    },
  },
}
