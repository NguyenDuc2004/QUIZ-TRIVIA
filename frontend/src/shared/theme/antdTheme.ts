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
