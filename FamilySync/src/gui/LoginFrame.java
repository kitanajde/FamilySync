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
    private static final Color BG_COLOR          = new Color(245, 247, 251);
    private static final Color PRIMARY            = new Color(67, 97, 238);
    private static final Color PRIMARY_DARK       = new Color(50, 75, 200);
    private static final Color ERROR_COLOR        = new Color(220, 53, 69);
    private static final Color TEXT_COLOR         = new Color(33, 37, 41);
    private static final Color PLACEHOLDER_COLOR  = new Color(160, 170, 190);

    private static final String USERNAME_PH = "Kullanıcı adınızı girin";
    private static final String PASSWORD_PH = "Şifrenizi girin";

    public LoginFrame(DataManager dataManager) {
        this.dataManager = dataManager;
        initUI();
    }

    private void initUI() {
        setTitle("FamilySync - Giriş");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImages(AppUI.createWindowIconImages());

        // Ana panel — tek sütun BoxLayout, bileşenler sıraya göre dizilir
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(36, 50, 24, 50));

        // --- Logo ve başlık ---
        JLabel logoLabel = new JLabel(AppUI.createLogoIcon(72), SwingConstants.CENTER);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("FamilySync", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Aile takviminize giriş yapın", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(100, 110, 130));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Form alanları ---
        JLabel userLabel = createFieldLabel("Kullanıcı Adı");
        usernameField = createTextField(USERNAME_PH);

        JLabel passLabel = createFieldLabel("Şifre");
        passwordField = new JPasswordField();
        stylePasswordField(passwordField);

        JButton loginButton = createPrimaryButton("Giriş Yap");
        loginButton.addActionListener(e -> handleLogin());

        // --- Durum / hata mesajı ---
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        statusLabel.setPreferredSize(new Dimension(320, 20));
        statusLabel.setMinimumSize(new Dimension(0, 20));

        // --- Demo hesaplar kutusu ---
        JPanel demoPanel = new JPanel();
        demoPanel.setLayout(new BoxLayout(demoPanel, BoxLayout.Y_AXIS));
        demoPanel.setBackground(new Color(230, 235, 255));
        demoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 255), 1),
            new EmptyBorder(10, 14, 10, 14)
        ));
        demoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        demoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel demoTitle = new JLabel("Demo Hesaplar", SwingConstants.CENTER);
        demoTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        demoTitle.setForeground(PRIMARY);
        demoTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel demoInfo = new JLabel(
            "<html><center>Ebeveyn: <b>ebeveyn</b> / <b>1234</b><br>Çocuk: <b>cocuk</b> / <b>1234</b></center></html>",
            SwingConstants.CENTER);
        demoInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        demoInfo.setForeground(TEXT_COLOR);
        demoInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        demoPanel.add(demoTitle);
        demoPanel.add(Box.createVerticalStrut(4));
        demoPanel.add(demoInfo);

        // --- Tüm bileşenleri sıraya diz ---
        mainPanel.add(logoLabel);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(28));
        mainPanel.add(userLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(passLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(passwordField);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(demoPanel);

        getRootPane().setDefaultButton(loginButton);

        add(mainPanel);
        setVisible(true);
        SwingUtilities.invokeLater(() -> getRootPane().requestFocusInWindow());
    }

    /**
     * Giriş işlemini yönetir.
     */
    private void handleLogin() {
        // Placeholder gösteriliyorsa boş say
        String rawUser = usernameField.getText();
        String username = rawUser.equals(USERNAME_PH) ? "" : rawUser.trim();
        String password = (passwordField.getEchoChar() == 0)
                ? "" : new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Lütfen tüm alanları doldurun.", ERROR_COLOR);
            return;
        }

        User user = dataManager.login(username, password);

        if (user == null) {
            showStatus("Kullanıcı adı veya şifre hatalı.", ERROR_COLOR);
            resetPasswordField();
            return;
        }

        dispose();
        if (user instanceof Parent) {
            new ParentDashboard((Parent) user, dataManager);
        } else if (user instanceof Child) {
            new ChildDashboard((Child) user, dataManager);
        }
    }

    private void resetPasswordField() {
        passwordField.setEchoChar((char) 0);
        passwordField.setText(PASSWORD_PH);
        passwordField.setForeground(PLACEHOLDER_COLOR);
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
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
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
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setForeground(PLACEHOLDER_COLOR);
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
                    field.setForeground(PLACEHOLDER_COLOR);
                }
            }
        });
    }

    private void stylePasswordField(JPasswordField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 230), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(Color.WHITE);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);

        // echoChar = 0 → placeholder düz metin olarak görünür
        field.setEchoChar((char) 0);
        field.setText(PASSWORD_PH);
        field.setForeground(PLACEHOLDER_COLOR);

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getEchoChar() == 0) {   // placeholder modunda
                    field.setText("");
                    field.setForeground(TEXT_COLOR);
                    field.setEchoChar('•'); // •
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getPassword().length == 0) {
                    resetPasswordField();
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
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
