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

/**
 * Çocuk dashboard ekranı.
 * Kendi görevlerini, ebeveynden gelen görevleri ve takvimi görüntüler.
 */
public class ChildDashboard extends JFrame {

    private final Child child;
    private final DataManager dataManager;

    private static final Color BG      = new Color(245, 247, 251);
    private static final Color SIDEBAR = new Color(72, 149, 239);
    private static final Color PRIMARY = new Color(72, 149, 239);
    private static final Color SUCCESS = new Color(40, 167, 69);
    private static final Color DANGER  = new Color(220, 53, 69);
    private static final Color TEXT    = new Color(33, 37, 41);

    private DefaultTableModel myTaskModel;
    private DefaultTableModel assignedTaskModel;
    private DefaultTableModel eventModel;

    public ChildDashboard(Child child, DataManager dataManager) {
        this.child = child;
        this.dataManager = dataManager;
        initUI();
    }

    private void initUI() {
        setTitle("FamilySync - " + child.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 550));

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(),  BorderLayout.WEST);
        root.add(buildContent(),  BorderLayout.CENTER);

        add(root);
        setVisible(true);
    }

    // ----------------------------------------------------------------
    // SIDEBAR
    // ----------------------------------------------------------------

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(30, 20, 20, 20));

        JLabel icon = new JLabel("🧒", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel(child.getFullName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roleLabel = new JLabel("Çocuk • " + child.getAge() + " yaş", SwingConstants.CENTER);
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roleLabel.setForeground(new Color(200, 225, 255));
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Bekleyen görev rozeti
        int pending = child.getPendingAssignedTaskCount();
        if (pending > 0) {
            JLabel badge = new JLabel("⚠ " + pending + " görev bekliyor!", SwingConstants.CENTER);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            badge.setForeground(new Color(255, 220, 80));
            badge.setAlignmentX(Component.CENTER_ALIGNMENT);
            sidebar.add(icon);
            sidebar.add(Box.createVerticalStrut(6));
            sidebar.add(nameLabel);
            sidebar.add(roleLabel);
            sidebar.add(Box.createVerticalStrut(6));
            sidebar.add(badge);
        } else {
            sidebar.add(icon);
            sidebar.add(Box.createVerticalStrut(6));
            sidebar.add(nameLabel);
            sidebar.add(roleLabel);
        }

        sidebar.add(Box.createVerticalStrut(30));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(120, 180, 255));
        sep.setMaximumSize(new Dimension(170, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(20));

        sidebar.add(sidebarBtn("📋  Görevlerim"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sidebarBtn("📌  Atanan Görevler"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sidebarBtn("📅  Takvimim"));
        sidebar.add(Box.createGlue());

        JButton logoutBtn = new JButton("🚪  Çıkış");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBackground(new Color(90, 160, 245));
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(170, 36));
        logoutBtn.addActionListener(e -> { dispose(); new LoginFrame(dataManager); });
        sidebar.add(logoutBtn);

        return sidebar;
    }

    // ----------------------------------------------------------------
    // ANA İÇERİK
    // ----------------------------------------------------------------

    private JTabbedPane buildContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        tabs.addTab("📋  Kendi Görevlerim",    buildMyTasksTab());
        tabs.addTab("📌  Atanan Görevler",      buildAssignedTasksTab());
        tabs.addTab("📅  Takvimim",             buildCalendarTab());

        return tabs;
    }

    // ----------------------------------------------------------------
    // KENDİ GÖREVLERİM SEKMESİ
    // ----------------------------------------------------------------

    private JPanel buildMyTasksTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG);
        topBar.add(sectionTitle("📋 Kendi Görevlerim"), BorderLayout.WEST);

        JButton addBtn = actionBtn("+ Görev Ekle", SUCCESS);
        addBtn.addActionListener(e -> showAddMyTaskDialog());
        topBar.add(addBtn, BorderLayout.EAST);

        String[] cols = {"Başlık", "Öncelik", "Bitiş", "Durum"};
        myTaskModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshMyTaskTable();

        JTable table = new JTable(myTaskModel);
        styleTable(table);

        JButton doneBtn = actionBtn("✅ Tamamlandı İşaretle", PRIMARY);
        doneBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showInfo("Lütfen bir görev seçin."); return; }
            String taskTitle = (String) myTaskModel.getValueAt(row, 0);
            child.getTasks().stream()
                .filter(t -> t.getTitle().equals(taskTitle))
                .findFirst()
                .ifPresent(t -> t.setCompleted(true));
            dataManager.updateChild(child);
            refreshMyTaskTable();
        });

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.setBackground(BG);
        bottomBar.add(doneBtn);

        panel.add(topBar,                     BorderLayout.NORTH);
        panel.add(new JScrollPane(table),     BorderLayout.CENTER);
        panel.add(bottomBar,                  BorderLayout.SOUTH);

        return panel;
    }

    private void showAddMyTaskDialog() {
        JTextField titleField = new JTextField(20);
        JTextField descField  = new JTextField(20);
        String[] priorities   = {"Yüksek", "Orta", "Düşük"};
        JComboBox<String> priorityBox = new JComboBox<>(priorities);
        JTextField dateField  = new JTextField(LocalDate.now().plusDays(1).toString(), 20);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Başlık:"));            form.add(titleField);
        form.add(new JLabel("Açıklama:"));          form.add(descField);
        form.add(new JLabel("Öncelik:"));           form.add(priorityBox);
        form.add(new JLabel("Bitiş (YYYY-MM-DD):")); form.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, form, "Görev Ekle",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            Task.Priority p = switch (priorityBox.getSelectedIndex()) {
                case 0 -> Task.Priority.HIGH;
                case 1 -> Task.Priority.MEDIUM;
                default -> Task.Priority.LOW;
            };
            Task task = new Task(DataManager.generateId(), titleField.getText().trim(),
                descField.getText().trim(), p, LocalDate.parse(dateField.getText().trim()));
            child.addTask(task);
            dataManager.updateChild(child);
            refreshMyTaskTable();
        } catch (Exception ex) {
            showError("Geçersiz tarih formatı!");
        }
    }

    private void refreshMyTaskTable() {
        myTaskModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        for (Task t : child.getTasks()) {
            myTaskModel.addRow(new Object[]{
                t.getTitle(), t.getPriority().getLabel(),
                t.getDueDate() != null ? t.getDueDate().format(fmt) : "-",
                t.getStatusText()
            });
        }
    }

    // ----------------------------------------------------------------
    // ATANAN GÖREVLER SEKMESİ
    // ----------------------------------------------------------------

    private JPanel buildAssignedTasksTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = sectionTitle("📌 Ebeveynimden Gelen Görevler");

        String[] cols = {"Başlık", "Öncelik", "Bitiş", "Durum"};
        assignedTaskModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshAssignedTaskTable();

        JTable table = new JTable(assignedTaskModel);
        styleTable(table);

        JButton doneBtn = actionBtn("✅ Tamamlandı", SUCCESS);
        doneBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showInfo("Lütfen bir görev seçin."); return; }
            String taskTitle = (String) assignedTaskModel.getValueAt(row, 0);
            boolean ok = child.getAssignedTasks().stream()
                .filter(t -> t.getTitle().equals(taskTitle))
                .findFirst()
                .map(t -> { t.setCompleted(true); return true; })
                .orElse(false);
            if (ok) {
                dataManager.updateChild(child);
                refreshAssignedTaskTable();
                showInfo("🎉 Harika! Görevi tamamladın.");
            }
        });

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.setBackground(BG);
        bottomBar.add(doneBtn);

        panel.add(title,                      BorderLayout.NORTH);
        panel.add(new JScrollPane(table),     BorderLayout.CENTER);
        panel.add(bottomBar,                  BorderLayout.SOUTH);

        return panel;
    }

    private void refreshAssignedTaskTable() {
        assignedTaskModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        for (Task t : child.getAssignedTasks()) {
            assignedTaskModel.addRow(new Object[]{
                t.getTitle(), t.getPriority().getLabel(),
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
        topBar.add(sectionTitle("📅 Takvimim"), BorderLayout.WEST);

        JButton addBtn = actionBtn("+ Etkinlik Ekle", PRIMARY);
        addBtn.addActionListener(e -> showAddEventDialog());
        topBar.add(addBtn, BorderLayout.EAST);

        String[] cols = {"Tür", "Başlık", "Başlangıç", "Bitiş"};
        eventModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshEventTable();

        JTable table = new JTable(eventModel);
        styleTable(table);

        panel.add(topBar,                 BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private void showAddEventDialog() {
        JTextField titleField = new JTextField(20);
        String[] types = {"Toplantı", "Randevu", "Sınav", "Hatırlatıcı", "Diğer"};
        JComboBox<String> typeBox = new JComboBox<>(types);
        JTextField startField = new JTextField(LocalDateTime.now().toString().substring(0, 16), 20);
        JTextField endField   = new JTextField(LocalDateTime.now().plusHours(1).toString().substring(0, 16), 20);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Başlık:")); form.add(titleField);
        form.add(new JLabel("Tür:"));   form.add(typeBox);
        form.add(new JLabel("Başlangıç (YYYY-MM-DDTHH:MM):")); form.add(startField);
        form.add(new JLabel("Bitiş (YYYY-MM-DDTHH:MM):")); form.add(endField);

        int result = JOptionPane.showConfirmDialog(this, form, "Etkinlik Ekle",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            CalendarEvent.EventType[] eventTypes = CalendarEvent.EventType.values();
            CalendarEvent event = new CalendarEvent(
                DataManager.generateId(), titleField.getText().trim(), "",
                LocalDateTime.parse(startField.getText().trim()),
                LocalDateTime.parse(endField.getText().trim()),
                eventTypes[typeBox.getSelectedIndex()]
            );
            child.addEvent(event);
            dataManager.updateChild(child);
            refreshEventTable();
        } catch (Exception ex) {
            showError("Geçersiz tarih/saat formatı!");
        }
    }

    private void refreshEventTable() {
        eventModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        for (CalendarEvent e : child.getEvents()) {
            eventModel.addRow(new Object[]{
                e.getEventType().getLabel(), e.getTitle(),
                e.getStartTime().format(fmt), e.getEndTime().format(fmt)
            });
        }
    }

    // ----------------------------------------------------------------
    // YARDIMCI UI
    // ----------------------------------------------------------------

    private JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(TEXT);
        return lbl;
    }

    private JButton actionBtn(String text, Color color) {
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

    private JButton sidebarBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(90, 160, 245));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(170, 36));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return btn;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(200, 225, 255));
        table.setGridColor(new Color(230, 235, 245));
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Bilgi", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Hata", JOptionPane.ERROR_MESSAGE);
    }
}
