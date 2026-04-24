package gui;

import model.User;
import model.Parent;
import model.Child;
import storage.DataManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Giriş ekranı.
 * Kullanıcı adı ve şifre doğrulamasını gerçekleştirir.
 * Doğrulama sonucuna göre ilgili dashboard'u açar.
 */
public class LoginFrame extends JFrame {

    private final DataManager dataManager;

    // Arayüz bileşenleri
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;

    // Renkler
    private static final Color BG_COLOR     = new Color(245, 247, 251);
    private static final Color PRIMARY      = new Color(67, 97, 238);
    private static final Color PRIMARY_DARK = new Color(50, 75, 200);
    private static final Color ERROR_COLOR  = new Color(220, 53, 69);
    private static final Color TEXT_COLOR   = new Color(33, 37, 41);

    public LoginFrame(DataManager dataManager) {
        this.dataManager = dataManager;
        initUI();
    }

    private void initUI() {
        setTitle("FamilySync - Giriş");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 520);
        setLocationRelativeTo(null); // Ekranın ortasında aç
        setResizable(false);

        // Ana panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(40, 50, 40, 50));

        // --- Üst: Logo ve başlık ---
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(BG_COLOR);

        JLabel logoLabel = new JLabel("🏠", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("FamilySync", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Aile takviminize giriş yapın", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(100, 110, 130));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(logoLabel);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(30));

        // --- Orta: Form ---
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(BG_COLOR);

        // Kullanıcı adı
        formPanel.add(createFieldLabel("👤  Kullanıcı Adı"));
        formPanel.add(Box.createVerticalStrut(5));
        usernameField = createTextField("Kullanıcı adınızı girin");
        formPanel.add(usernameField);
        formPanel.add(Box.createVerticalStrut(16));

        // Şifre
        formPanel.add(createFieldLabel("🔒  Şifre"));
        formPanel.add(Box.createVerticalStrut(5));
        passwordField = new JPasswordField();
        styleTextField(passwordField, "Şifrenizi girin");
        formPanel.add(passwordField);
        formPanel.add(Box.createVerticalStrut(20));

        // Giriş butonu
        JButton loginButton = createPrimaryButton("Giriş Yap");
        loginButton.addActionListener(e -> handleLogin());
        formPanel.add(loginButton);
        formPanel.add(Box.createVerticalStrut(12));

        // Durum / hata mesajı
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(statusLabel);

        // --- Alt: Demo bilgisi ---
        JPanel demoPanel = new JPanel();
        demoPanel.setBackground(new Color(230, 235, 255));
        demoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 255), 1),
            new EmptyBorder(10, 14, 10, 14)
        ));
        demoPanel.setLayout(new BoxLayout(demoPanel, BoxLayout.Y_AXIS));

        JLabel demoTitle = new JLabel("Demo Hesaplar", SwingConstants.CENTER);
        demoTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        demoTitle.setForeground(PRIMARY);
        demoTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel demoInfo = new JLabel("<html><center>Ebeveyn: <b>ebeveyn</b> / <b>1234</b><br>Çocuk: <b>cocuk</b> / <b>1234</b></center></html>", SwingConstants.CENTER);
        demoInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        demoInfo.setForeground(TEXT_COLOR);
        demoInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        demoPanel.add(demoTitle);
        demoPanel.add(Box.createVerticalStrut(4));
        demoPanel.add(demoInfo);

        // Enter tuşu ile giriş
        getRootPane().setDefaultButton(loginButton);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel,   BorderLayout.CENTER);
        mainPanel.add(demoPanel,   BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    /**
     * Giriş işlemini yönetir.
     */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Lütfen tüm alanları doldurun.", ERROR_COLOR);
            return;
        }

        User user = dataManager.login(username, password);

        if (user == null) {
            showStatus("❌ Kullanıcı adı veya şifre hatalı.", ERROR_COLOR);
            passwordField.setText("");
            return;
        }

        // Başarılı giriş - ilgili dashboard'u aç
        dispose();

        if (user instanceof Parent) {
            new ParentDashboard((Parent) user, dataManager);
        } else if (user instanceof Child) {
            new ChildDashboard((Child) user, dataManager);
        }
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    // ----------------------------------------------------------------
    // YARDIMCI UI METOTları
    // ----------------------------------------------------------------

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        styleTextField(field, placeholder);
        return field;
    }

    private void styleTextField(JTextField field, String placeholder) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 230), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(Color.WHITE);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Placeholder davranışı
        field.setForeground(new Color(160, 170, 190));
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_COLOR);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(160, 170, 190));
                }
            }
        });
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? PRIMARY_DARK : PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
