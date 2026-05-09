package gui;

import model.Task;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Uygulama genelinde kullanılan programatik ikon ve görsel yardımcı sınıfı.
 * Harici PNG/SVG dosyası gerekmez; tüm ikonlar Graphics2D ile çizilir.
 */
public class AppUI {

    // ================================================================
    // PENCERE / TASKBAR İKONU
    // ================================================================

    /**
     * Tüm boyutlarda (16/32/48/64 px) uygulama ikonlarını döndürür.
     * JFrame.setIconImages() ile kullanılır.
     */
    public static List<Image> createWindowIconImages() {
        return List.of(
            drawWindowIcon(16),
            drawWindowIcon(32),
            drawWindowIcon(48),
            drawWindowIcon(64)
        );
    }

    /** Giriş ekranı için büyük logo ikonu (programatik, emoji yok). */
    public static ImageIcon createLogoIcon(int size) {
        return new ImageIcon(drawWindowIcon(size));
    }

    private static Image drawWindowIcon(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Arka plan dairesi
        g.setColor(new Color(67, 97, 238));
        g.fillOval(0, 0, s, s);

        // Beyaz ev silüeti
        g.setColor(Color.WHITE);
        int pad = s / 7;

        // Çatı (üçgen)
        int[] rx = { s / 2, pad,      s - pad };
        int[] ry = { pad,   s / 2 + 1, s / 2 + 1 };
        g.fillPolygon(rx, ry, 3);

        // Gövde
        int bx = pad + s / 8;
        g.fillRect(bx, s / 2 + 1, s - 2 * bx, s / 2 - pad);

        // Kapı (çıkarma)
        g.setColor(new Color(67, 97, 238));
        int dw = s / 5, dh = s / 4;
        g.fillRect(s / 2 - dw / 2, s - pad - dh, dw, dh);

        g.dispose();
        return img;
    }

    // ================================================================
    // KULLANICI AVATAR DAİRESİ
    // ================================================================

    /**
     * Ad/soyad baş harflerinden oluşan gradient daire avatar paneli döndürür.
     * Sidebar'a doğrudan eklenebilir (BoxLayout uyumlu).
     */
    public static JPanel createAvatarPanel(String fullName, Color baseColor, int size) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Hafif gölge
                g2.setColor(new Color(0, 0, 0, 35));
                g2.fillOval(2, 3, size, size);

                // Gradient daire
                RadialGradientPaint gradient = new RadialGradientPaint(
                    new Point(size / 3, size / 3), size * 0.65f,
                    new float[]{ 0f, 1f },
                    new Color[]{ baseColor.brighter(), baseColor.darker() }
                );
                g2.setPaint(gradient);
                g2.fillOval(0, 0, size, size);

                // Baş harfler
                String initials = toInitials(fullName);
                g2.setColor(new Color(255, 255, 255, 225));
                g2.setFont(new Font("Segoe UI", Font.BOLD, size * 2 / 5));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (size - fm.stringWidth(initials)) / 2;
                int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, tx, ty);

                g2.dispose();
            }
        };
        int total = size + 3;
        panel.setPreferredSize(new Dimension(total, total));
        panel.setMaximumSize(new Dimension(total, total));
        panel.setMinimumSize(new Dimension(total, total));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return panel;
    }

    private static String toInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    // ================================================================
    // BUTON İKONLARI (beyaz, şeffaf arka plan)
    // ================================================================

    /**
     * Buton üzerinde gösterilmek üzere beyaz vektörel ikon üretir.
     *
     * @param type  "plus" | "trash" | "check" | "assign" | "logout" | "calendar" | "connect"
     * @param size  piksel (önerilen: 14–18)
     */
    public static ImageIcon createIcon(String type, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        float sw = Math.max(1.8f, size / 7f);
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int p  = Math.max(1, size / 6);  // kenar boşluğu
        int h  = size / 2;

        switch (type) {
            case "plus" -> {
                g.drawOval(p, p, size - 2 * p, size - 2 * p);
                g.drawLine(h, p + 2, h, size - p - 2);
                g.drawLine(p + 2, h, size - p - 2, h);
            }
            case "trash" -> {
                int lp = p + 1;
                g.drawLine(lp, p + 3, size - lp, p + 3);
                g.drawLine(p + 3, p + 1, size - p - 3, p + 1);
                g.drawRect(lp, p + 3, size - 2 * lp, size - p - 4);
                g.drawLine(h - 3, p + 6, h - 3, size - p - 2);
                g.drawLine(h,     p + 6, h,     size - p - 2);
                g.drawLine(h + 3, p + 6, h + 3, size - p - 2);
            }
            case "check" -> {
                g.drawOval(p, p, size - 2 * p, size - 2 * p);
                g.drawLine(p + 3, h + 1, h - 1, size - p - 2);
                g.drawLine(h - 1, size - p - 2, size - p - 2, p + 3);
            }
            case "assign" -> {
                // ok (sağa)
                g.drawLine(p + 1, h, size - p - 2, h);
                g.drawLine(size - p - 4, p + 2, size - p - 2, h);
                g.drawLine(size - p - 4, size - p - 2, size - p - 2, h);
                // kişi başı (küçük daire solda)
                int r = size / 4;
                g.drawOval(p, p, r * 2, r * 2);
            }
            case "logout" -> {
                g.drawRect(p, p, h - 1, size - 2 * p);
                int ax = h + 2;
                g.drawLine(ax, h, size - p - 1, h);
                g.drawLine(size - p - 3, p + 3,     size - p - 1, h);
                g.drawLine(size - p - 3, size - p - 3, size - p - 1, h);
            }
            case "calendar" -> {
                g.drawRoundRect(p, p + 2, size - 2 * p, size - 2 * p - 2, 3, 3);
                g.drawLine(p, p + 6, size - p, p + 6);
                g.drawLine(p + 4, p, p + 4, p + 4);
                g.drawLine(size - p - 4, p, size - p - 4, p + 4);
            }
            case "connect" -> {
                // zincir halkası gibi iki birbirine geçmiş daire
                int r2 = size / 3;
                g.drawOval(p,         h - r2, r2 * 2, r2 * 2);
                g.drawOval(size - p - r2 * 2, h - r2, r2 * 2, r2 * 2);
                g.drawLine(p + r2, h, size - p - r2, h);
            }
        }

        g.dispose();
        return new ImageIcon(img);
    }

    // ================================================================
    // TABLO HÜCRE RENDERER'LARI
    // ================================================================

    /**
     * Görev tablosuna öncelik ve durum renderer'larını uygular.
     * Emoji yerine renkli metin kullanır — kare görünümünü ortadan kaldırır.
     */
    public static void applyTaskRenderers(JTable table, int priorityCol, int statusCol) {
        table.getColumnModel().getColumn(priorityCol).setCellRenderer(new PriorityRenderer());
        table.getColumnModel().getColumn(statusCol).setCellRenderer(new StatusRenderer());
    }

    /** Öncelik sütunu: renkli arka plan */
    public static class PriorityRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, col);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            String text = value != null ? value.toString() : "";
            if (!isSelected) {
                if (text.contains("Yüksek")) {
                    lbl.setBackground(new Color(255, 218, 218));
                    lbl.setForeground(new Color(170, 25, 25));
                } else if (text.contains("Orta")) {
                    lbl.setBackground(new Color(255, 243, 205));
                    lbl.setForeground(new Color(155, 95, 0));
                } else {
                    lbl.setBackground(new Color(209, 242, 215));
                    lbl.setForeground(new Color(25, 120, 45));
                }
            }
            return lbl;
        }
    }

    /** Durum sütunu: emoji yok, renkli metin + unicode sembol */
    public static class StatusRenderer extends DefaultTableCellRenderer {
        private static final Color DONE = new Color(30, 140, 60);
        private static final Color WAIT = new Color(200, 100, 0);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            String raw  = value != null ? value.toString() : "";
            boolean done = raw.contains("Tamamland");
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                table, done ? "✓  Tamamlandı" : "●  Bekliyor",
                isSelected, hasFocus, row, col);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (!isSelected) lbl.setForeground(done ? DONE : WAIT);
            return lbl;
        }
    }

    // ================================================================
    // GÖREV KARTI RENDERER (JList<Task> için)
    // ================================================================

    /**
     * JList<Task> için kart görünümü sağlayan renderer.
     * Her kart: renkli sol şerit (önceliğe göre), kalın başlık, açıklama/öncelik,
     * sağda tarih ve durum göstergesi içerir.
     */
    public static class TaskCardRenderer implements ListCellRenderer<Task> {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM");

        private static final Color HIGH_COLOR   = new Color(0xDC3545);
        private static final Color MEDIUM_COLOR = new Color(0xFF9800);
        private static final Color LOW_COLOR    = new Color(0x28A745);
        private static final Color DONE_COLOR   = new Color(40, 167, 69);
        private static final Color PENDING_COLOR= new Color(255, 152, 0);
        private static final Color SEL_BG       = new Color(230, 236, 255);

        @Override
        public Component getListCellRendererComponent(
                JList<? extends Task> list, Task task,
                int index, boolean isSelected, boolean cellHasFocus) {

            // Outer wrapper: adds 3px top/bottom padding between cards
            JPanel outer = new JPanel(new BorderLayout());
            outer.setOpaque(false);
            outer.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

            // Card panel with custom painting for rounded corners
            Color stripColor = task.getPriority() == Task.Priority.HIGH   ? HIGH_COLOR
                             : task.getPriority() == Task.Priority.MEDIUM ? MEDIUM_COLOR
                             : LOW_COLOR;

            JPanel card = new JPanel(new BorderLayout(0, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Card background
                    g2.setColor(isSelected ? SEL_BG : Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                    // Selection border highlight
                    if (isSelected) {
                        g2.setColor(new Color(100, 130, 240, 120));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    }

                    // Left colored strip (5px wide, rounded on left, square on right)
                    g2.setColor(stripColor);
                    g2.fillRoundRect(0, 0, 10, getHeight(), 12, 12);
                    g2.fillRect(5, 0, 5, getHeight()); // square right side of strip

                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 12));

            // ---- Left content (title + description) ----
            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.setOpaque(false);

            // Title: bold, strikethrough if completed
            String titleText = task.getTitle() != null ? task.getTitle() : "";
            JLabel titleLbl;
            if (task.isCompleted()) {
                titleLbl = new JLabel("<html><strike>" + escapeHtml(titleText) + "</strike></html>");
                titleLbl.setForeground(new Color(160, 165, 175));
            } else {
                titleLbl = new JLabel(titleText);
                titleLbl.setForeground(new Color(33, 37, 41));
            }
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Description + priority line
            String desc = task.getDescription() != null && !task.getDescription().isBlank()
                ? task.getDescription() : "";
            String priorityLabel = task.getPriority() != null ? task.getPriority().getLabel() : "";
            String subText = desc.isEmpty() ? priorityLabel : desc + "  •  " + priorityLabel;
            JLabel subLbl = new JLabel(subText);
            subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            subLbl.setForeground(new Color(130, 138, 155));
            subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            leftPanel.add(titleLbl);
            leftPanel.add(Box.createVerticalStrut(3));
            leftPanel.add(subLbl);

            // ---- Right content (date + status indicator) ----
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
            rightPanel.setOpaque(false);

            // Date label
            String dateStr = task.getDueDate() != null ? task.getDueDate().format(FMT) : "";
            JLabel dateLbl = new JLabel(dateStr);
            dateLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            dateLbl.setForeground(new Color(130, 138, 155));
            dateLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);

            // Status dot/check
            JLabel statusLbl;
            if (task.isCompleted()) {
                statusLbl = new JLabel("✓");
                statusLbl.setForeground(DONE_COLOR);
                statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            } else {
                statusLbl = new JLabel("●");
                statusLbl.setForeground(PENDING_COLOR);
                statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            }
            statusLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);

            rightPanel.add(dateLbl);
            rightPanel.add(Box.createVerticalStrut(4));
            rightPanel.add(statusLbl);

            card.add(leftPanel, BorderLayout.CENTER);
            card.add(rightPanel, BorderLayout.EAST);

            outer.add(card, BorderLayout.CENTER);
            return outer;
        }

        private static String escapeHtml(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    /**
     * JList<Task> bileşenine standart kart stilini uygular.
     * fixedCellHeight=78, null border, TaskCardRenderer, SINGLE_SELECTION.
     */
    public static void applyListStyle(JList<Task> list, Color bg) {
        list.setBackground(bg);
        list.setBorder(null);
        list.setFixedCellHeight(78);
        list.setCellRenderer(new TaskCardRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    // ================================================================
    // STYLED FORM DIALOG YARDIMCILARI
    // ================================================================

    /** Standart modal JDialog oluşturur. */
    public static JDialog createFormDialog(JFrame owner, String title) {
        JDialog dlg = new JDialog(owner, title, true);
        dlg.setLayout(new BorderLayout());
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(owner);
        return dlg;
    }

    /** Mavi başlık paneli (diyalog üstü). */
    public static JPanel dialogHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(67, 97, 238));
        header.setBorder(new EmptyBorder(14, 22, 14, 22));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(Color.WHITE);
        header.add(lbl, BorderLayout.WEST);
        return header;
    }

    /** Beyaz, padding'li dikey BoxLayout gövde paneli. */
    public static JPanel buildDialogBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(20, 24, 8, 24));
        return body;
    }

    /**
     * İptal + onay butonu içeren diyalog alt paneli.
     * ok[0] = true olursa onay tuşuna basılmış demektir.
     */
    public static JPanel buildDialogFooter(JDialog dlg, boolean[] ok, String confirmLabel) {
        JButton cancelBtn = dialogButton("İptal",      new Color(140, 145, 155));
        JButton okBtn     = dialogButton(confirmLabel, new Color(67, 97, 238));
        cancelBtn.addActionListener(e -> dlg.dispose());
        okBtn.addActionListener(e -> { ok[0] = true; dlg.dispose(); });
        dlg.getRootPane().setDefaultButton(okBtn);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(220, 225, 240)),
            new EmptyBorder(10, 20, 14, 20)
        ));
        footer.add(cancelBtn);
        footer.add(okBtn);
        return footer;
    }

    /** Etiket üstte, alan altta şeklinde form satırı. */
    public static JPanel formRow(String labelText, JComponent field) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(Color.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(new Color(80, 90, 110));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        styleDialogComponent(field);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(label);
        row.add(Box.createVerticalStrut(4));
        row.add(field);
        return row;
    }

    private static void styleDialogComponent(JComponent c) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        c.setPreferredSize(new Dimension(380, 36));
        if (c instanceof JTextField tf) {
            tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 230), 1),
                new EmptyBorder(6, 10, 6, 10)
            ));
            tf.setBackground(Color.WHITE);
        } else if (c instanceof JComboBox<?> cb) {
            cb.setBackground(Color.WHITE);
        }
    }

    /** Yuvarlak köşeli, renk dolgulu diyalog butonu. */
    public static JButton dialogButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(90, 36));
        return btn;
    }
}
