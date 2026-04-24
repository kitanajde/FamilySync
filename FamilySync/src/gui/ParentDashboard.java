package gui;

import model.*;
import storage.DataManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Ebeveyn dashboard ekranı.
 * Görev yönetimi, çocuklara görev atama ve takvim işlemleri içerir.
 */
public class ParentDashboard extends JFrame {

    private final Parent parent;
    private final DataManager dataManager;

    // Renkler
    private static final Color BG       = new Color(245, 247, 251);
    private static final Color SIDEBAR  = new Color(67, 97, 238);
    private static final Color CARD_BG  = Color.WHITE;
    private static final Color PRIMARY  = new Color(67, 97, 238);
    private static final Color SUCCESS  = new Color(40, 167, 69);
    private static final Color DANGER   = new Color(220, 53, 69);
    private static final Color TEXT     = new Color(33, 37, 41);

    // Tablo modelleri (dinamik güncelleme için field'da tutuyoruz)
    private DefaultTableModel taskTableModel;
    private DefaultTableModel eventTableModel;
    private DefaultTableModel childrenTableModel;

    public ParentDashboard(Parent parent, DataManager dataManager) {
        this.parent = parent;
        this.dataManager = dataManager;
        initUI();
    }

    private void initUI() {
        setTitle("FamilySync - Ebeveyn Paneli");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));

        // Ana düzen: Sidebar + İçerik
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContent(), BorderLayout.CENTER);

        add(root);
        setVisible(true);
    }

    // ----------------------------------------------------------------
    // SIDEBAR
    // ----------------------------------------------------------------

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(30, 20, 20, 20));

        // Profil
        JLabel icon = new JLabel("👨‍👩‍👧", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel(parent.getFullName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roleLabel = new JLabel("Ebeveyn", SwingConstants.CENTER);
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roleLabel.setForeground(new Color(180, 200, 255));
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebar.add(icon);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(nameLabel);
        sidebar.add(roleLabel);
        sidebar.add(Box.createVerticalStrut(30));
        sidebar.add(createSeparator());
        sidebar.add(Box.createVerticalStrut(20));

        // Navigasyon butonları (sekmeli panel için)
        sidebar.add(sidebarButton("📋  Görevlerim"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sidebarButton("📅  Takviim"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sidebarButton("👶  Çocuklarım"));
        sidebar.add(Box.createGlue());

        // Çıkış
        JButton logoutBtn = new JButton("🚪  Çıkış Yap");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBackground(new Color(100, 120, 255));
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(180, 36));
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame(dataManager);
        });
        sidebar.add(logoutBtn);

        return sidebar;
    }

    // ----------------------------------------------------------------
    // ANA İÇERİK (sekmeli)
    // ----------------------------------------------------------------

    private JTabbedPane buildContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        tabs.addTab("📋  Görevlerim",    buildTasksTab());
        tabs.addTab("📅  Takvim",         buildCalendarTab());
        tabs.addTab("👶  Çocuklarım",     buildChildrenTab());

        return tabs;
    }

    // ----------------------------------------------------------------
    // GÖREVLER SEKMESİ
    // ----------------------------------------------------------------

    private JPanel buildTasksTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Başlık + Ekle butonu
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG);

        JLabel title = sectionTitle("📋 Görevlerim");
        JButton addBtn = actionButton("+ Yeni Görev", SUCCESS);
        addBtn.addActionListener(e -> showAddTaskDialog());

        topBar.add(title,  BorderLayout.WEST);
        topBar.add(addBtn, BorderLayout.EAST);

        // Tablo
        String[] cols = {"Başlık", "Öncelik", "Bitiş Tarihi", "Durum"};
        taskTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshTaskTable();

        JTable table = new JTable(taskTableModel);
        styleTable(table);

        JButton deleteBtn = actionButton("🗑 Sil", DANGER);
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showInfo("Lütfen bir görev seçin."); return; }
            String taskTitle = (String) taskTableModel.getValueAt(row, 0);
            parent.getTasks().removeIf(t -> t.getTitle().equals(taskTitle));
            dataManager.updateParent(parent);
            refreshTaskTable();
        });

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.setBackground(BG);
        bottomBar.add(deleteBtn);

        panel.add(topBar,                     BorderLayout.NORTH);
        panel.add(new JScrollPane(table),     BorderLayout.CENTER);
        panel.add(bottomBar,                  BorderLayout.SOUTH);

        return panel;
    }

    private void showAddTaskDialog() {
        JTextField titleField = new JTextField(20);
        JTextField descField  = new JTextField(20);
        String[] priorities   = {"Yüksek", "Orta", "Düşük"};
        JComboBox<String> priorityBox = new JComboBox<>(priorities);
        JTextField dateField  = new JTextField(LocalDate.now().plusDays(1).toString(), 20);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Başlık:"));        form.add(titleField);
        form.add(new JLabel("Açıklama:"));      form.add(descField);
        form.add(new JLabel("Öncelik:"));       form.add(priorityBox);
        form.add(new JLabel("Bitiş (YYYY-MM-DD):")); form.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, form, "Yeni Görev Ekle",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            Task.Priority p = switch (priorityBox.getSelectedIndex()) {
                case 0 -> Task.Priority.HIGH;
                case 1 -> Task.Priority.MEDIUM;
                default -> Task.Priority.LOW;
            };
            Task task = new Task(
                DataManager.generateId(),
                titleField.getText().trim(),
                descField.getText().trim(),
                p,
                LocalDate.parse(dateField.getText().trim())
            );
            parent.addTask(task);
            dataManager.updateParent(parent);
            refreshTaskTable();
        } catch (Exception ex) {
            showError("Geçersiz tarih formatı! Lütfen YYYY-MM-DD kullanın.");
        }
    }

    private void refreshTaskTable() {
        taskTableModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        for (Task t : parent.getTasks()) {
            taskTableModel.addRow(new Object[]{
                t.getTitle(),
                t.getPriority().getLabel(),
                t.getDueDate() != null ? t.getDueDate().format(fmt) : "-",
                t.getStatusText()
            });
        }
    }

    // ----------------------------------------------------------------
    // TAKVİM SEKMESİ
    // ----------------------------------------------------------------

    private JPanel buildCalendarTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG);

        JLabel title = sectionTitle("📅 Takvim Etkinlikleri");
        JButton addBtn = actionButton("+ Yeni Etkinlik", PRIMARY);
        addBtn.addActionListener(e -> showAddEventDialog());

        topBar.add(title, BorderLayout.WEST);
        topBar.add(addBtn, BorderLayout.EAST);

        String[] cols = {"Tür", "Başlık", "Başlangıç", "Bitiş"};
        eventTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshEventTable();

        JTable table = new JTable(eventTableModel);
        styleTable(table);

        panel.add(topBar,                 BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private void showAddEventDialog() {
        JTextField titleField = new JTextField(20);
        JTextField descField  = new JTextField(20);
        String[] types = {"Toplantı", "Randevu", "Sınav", "Hatırlatıcı", "Diğer"};
        JComboBox<String> typeBox = new JComboBox<>(types);
        JTextField startField = new JTextField(LocalDateTime.now().toString().substring(0, 16), 20);
        JTextField endField   = new JTextField(LocalDateTime.now().plusHours(1).toString().substring(0, 16), 20);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Başlık:"));         form.add(titleField);
        form.add(new JLabel("Açıklama:"));       form.add(descField);
        form.add(new JLabel("Tür:"));            form.add(typeBox);
        form.add(new JLabel("Başlangıç (YYYY-MM-DDTHH:MM):")); form.add(startField);
        form.add(new JLabel("Bitiş (YYYY-MM-DDTHH:MM):")); form.add(endField);

        int result = JOptionPane.showConfirmDialog(this, form, "Yeni Etkinlik Ekle",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            CalendarEvent.EventType[] eventTypes = CalendarEvent.EventType.values();
            CalendarEvent event = new CalendarEvent(
                DataManager.generateId(),
                titleField.getText().trim(),
                descField.getText().trim(),
                LocalDateTime.parse(startField.getText().trim()),
                LocalDateTime.parse(endField.getText().trim()),
                eventTypes[typeBox.getSelectedIndex()]
            );
            parent.addEvent(event);
            dataManager.updateParent(parent);
            refreshEventTable();
        } catch (Exception ex) {
            showError("Geçersiz tarih/saat formatı!");
        }
    }

    private void refreshEventTable() {
        eventTableModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        for (CalendarEvent e : parent.getEvents()) {
            eventTableModel.addRow(new Object[]{
                e.getEventType().getLabel(),
                e.getTitle(),
                e.getStartTime().format(fmt),
                e.getEndTime().format(fmt)
            });
        }
    }

    // ----------------------------------------------------------------
    // ÇOCUKLAR SEKMESİ
    // ----------------------------------------------------------------

    private JPanel buildChildrenTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG);
        JLabel title = sectionTitle("👶 Çocuklarım");

        JButton assignBtn = actionButton("📌 Görev Ata", new Color(255, 152, 0));
        assignBtn.addActionListener(e -> showAssignTaskDialog());

        topBar.add(title,     BorderLayout.WEST);
        topBar.add(assignBtn, BorderLayout.EAST);

        String[] cols = {"Ad Soyad", "Kullanıcı Adı", "Yaş", "Bekleyen Görev"};
        childrenTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshChildrenTable();

        JTable table = new JTable(childrenTableModel);
        styleTable(table);

        panel.add(topBar,                 BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private void showAssignTaskDialog() {
        List<Child> myChildren = dataManager.getChildrenByParentId(parent.getId());
        if (myChildren.isEmpty()) {
            showInfo("Henüz kayıtlı çocuğunuz bulunmuyor.");
            return;
        }

        String[] childNames = myChildren.stream()
            .map(c -> c.getFullName() + " (" + c.getUsername() + ")")
            .toArray(String[]::new);

        JComboBox<String> childBox    = new JComboBox<>(childNames);
        JTextField titleField         = new JTextField(20);
        JTextField descField          = new JTextField(20);
        String[] priorities           = {"Yüksek", "Orta", "Düşük"};
        JComboBox<String> priorityBox = new JComboBox<>(priorities);
        JTextField dateField          = new JTextField(LocalDate.now().plusDays(1).toString(), 20);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Çocuk:"));           form.add(childBox);
        form.add(new JLabel("Görev Başlığı:"));   form.add(titleField);
        form.add(new JLabel("Açıklama:"));        form.add(descField);
        form.add(new JLabel("Öncelik:"));         form.add(priorityBox);
        form.add(new JLabel("Bitiş (YYYY-MM-DD):")); form.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, form, "Çocuğa Görev Ata",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            Child selectedChild = myChildren.get(childBox.getSelectedIndex());
            Task.Priority p = switch (priorityBox.getSelectedIndex()) {
                case 0 -> Task.Priority.HIGH;
                case 1 -> Task.Priority.MEDIUM;
                default -> Task.Priority.LOW;
            };
            Task task = new Task(
                DataManager.generateId(),
                titleField.getText().trim(),
                descField.getText().trim(),
                p,
                LocalDate.parse(dateField.getText().trim())
            );
            parent.assignTaskToChild(task, selectedChild.getId());
            selectedChild.receiveAssignedTask(task);
            dataManager.updateChild(selectedChild);
            refreshChildrenTable();
            showInfo("✅ Görev başarıyla atandı: " + selectedChild.getFullName());
        } catch (Exception ex) {
            showError("Geçersiz tarih formatı! Lütfen YYYY-MM-DD kullanın.");
        }
    }

    private void refreshChildrenTable() {
        childrenTableModel.setRowCount(0);
        List<Child> myChildren = dataManager.getChildrenByParentId(parent.getId());
        for (Child c : myChildren) {
            childrenTableModel.addRow(new Object[]{
                c.getFullName(),
                c.getUsername(),
                c.getAge(),
                c.getPendingAssignedTaskCount() + " görev bekliyor"
            });
        }
    }

    // ----------------------------------------------------------------
    // YARDIMCI UI METOTları
    // ----------------------------------------------------------------

    private JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(TEXT);
        return lbl;
    }

    private JButton actionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private JButton sidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(80, 110, 250));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(180, 36));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return btn;
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(120, 140, 255));
        sep.setMaximumSize(new Dimension(180, 1));
        return sep;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(200, 210, 255));
        table.setGridColor(new Color(230, 235, 245));
        table.setShowGrid(true);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Bilgi", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Hata", JOptionPane.ERROR_MESSAGE);
    }
}
