package gui;

import model.*;
import service.GoogleCalendarService;
import storage.DataManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Ebeveyn dashboard ekrani - tam yeniden tasarim.
 * Sidebar navigasyonu CardLayout ile icerik panellerini degistirir.
 */
public class ParentDashboard extends JFrame {

    private final Parent      parent;
    private final DataManager dataManager;

    // ---- Renk Paleti ----
    private static final Color BG        = new Color(240, 242, 249);
    private static final Color SIDEBAR   = new Color(36, 48, 110);
    private static final Color PRIMARY   = new Color(67, 97, 238);
    private static final Color SUCCESS   = new Color(40, 167, 69);
    private static final Color DANGER    = new Color(220, 53, 69);
    private static final Color ORANGE    = new Color(255, 152, 0);
    private static final Color TEXT      = new Color(33, 37, 41);
    private static final Color MUTED     = new Color(108, 117, 130);
    private static final Color NAV_IDLE  = new Color(55, 72, 160);
    private static final Color NAV_ACT   = new Color(80, 100, 220);
    private static final Color TOP_BORDER= new Color(220, 225, 240);

    // ---- CardLayout / navigasyon ----
    private CardLayout cardLayout;
    private JPanel     mainContent;
    private JButton[]  navBtns;          // [0]=Görevlerim [1]=Takvim [2]=Çocuklarım

    // ---- Görev listesi ----
    private DefaultListModel<Task> taskListModel;
    private JList<Task>            taskJList;
    private JLabel                 statsTotalLbl, statsPendingLbl, statsDoneLbl;

    // ---- Takvim paneli ----
    private CalendarGridPanel calendarPanel;

    // ---- Tablo modelleri ----
    private DefaultTableModel childrenTableModel;
    private DefaultTableModel rewardRuleTableModel;

    // ---- Google Takvim butonu ----
    private JButton gcalBtn;

    // ====================================================================
    public ParentDashboard(Parent parent, DataManager dataManager) {
        this.parent      = parent;
        this.dataManager = dataManager;
        initUI();
    }

    // ====================================================================
    // INIT
    // ====================================================================

    private void initUI() {
        setTitle("FamilySync - Ebeveyn Paneli");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 720);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(950, 620));
        setIconImages(AppUI.createWindowIconImages());

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(),      BorderLayout.WEST);
        root.add(buildRightPanel(),   BorderLayout.CENTER);

        add(root);
        setActiveNav(0);
        setVisible(true);
    }

    // ====================================================================
    // SIDEBAR
    // ====================================================================

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(28, 18, 18, 18));

        // Avatar
        JPanel avatar = AppUI.createAvatarPanel(parent.getFullName(), new Color(120, 145, 255), 60);
        sidebar.add(avatar);
        sidebar.add(Box.createVerticalStrut(10));

        // İsim
        JLabel nameLbl = new JLabel(parent.getFullName(), SwingConstants.CENTER);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(nameLbl);

        // Rol
        JLabel roleLbl = new JLabel("Ebeveyn", SwingConstants.CENTER);
        roleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roleLbl.setForeground(new Color(180, 195, 240));
        roleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(roleLbl);

        sidebar.add(Box.createVerticalStrut(22));
        sidebar.add(makeSep());
        sidebar.add(Box.createVerticalStrut(18));

        // Nav buttons
        String[] labels = {"Görevlerim", "Takvim", "Çocuklarım"};
        String[] cards  = {"tasks", "calendar", "children"};
        navBtns = new JButton[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            final String card = cards[i];
            JButton btn = makeNavBtn(labels[i]);
            btn.addActionListener(e -> { cardLayout.show(mainContent, card); setActiveNav(idx); });
            navBtns[i] = btn;
            sidebar.add(btn);
            if (i < labels.length - 1) sidebar.add(Box.createVerticalStrut(6));
        }

        sidebar.add(Box.createVerticalStrut(22));
        sidebar.add(makeSep());
        sidebar.add(Box.createVerticalStrut(12));

        // Google Takvim butonu
        boolean connected = GoogleCalendarService.getInstance().isAuthenticated();
        gcalBtn = new JButton(connected ? "Google Takvim ✓" : "Google Takvim");
        gcalBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gcalBtn.setForeground(Color.WHITE);
        gcalBtn.setBackground(connected ? new Color(40, 150, 80) : new Color(70, 90, 180));
        gcalBtn.setContentAreaFilled(false);
        gcalBtn.setOpaque(true);
        gcalBtn.setBorderPainted(false);
        gcalBtn.setFocusPainted(false);
        gcalBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gcalBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        gcalBtn.setMaximumSize(new Dimension(184, 30));
        gcalBtn.setIcon(AppUI.createIcon("calendar", 13));
        gcalBtn.setIconTextGap(6);
        gcalBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        gcalBtn.addActionListener(e -> showGoogleCalendarDialog());
        sidebar.add(gcalBtn);

        sidebar.add(Box.createGlue());

        // Çıkış
        JButton logoutBtn = makeNavBtn("Çıkış Yap");
        logoutBtn.setIcon(AppUI.createIcon("logout", 14));
        logoutBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        logoutBtn.addActionListener(e -> { dispose(); new LoginFrame(dataManager); });
        sidebar.add(logoutBtn);

        return sidebar;
    }

    // ====================================================================
    // SAG PANEL (welcome bar + cardlayout)
    // ====================================================================

    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(BG);
        right.add(buildWelcomeBar(), BorderLayout.NORTH);

        cardLayout  = new CardLayout();
        mainContent = new JPanel(cardLayout);
        mainContent.setBackground(BG);
        mainContent.add(buildTasksCard(),    "tasks");
        mainContent.add(buildCalendarCard(), "calendar");
        mainContent.add(buildChildrenCard(), "children");

        right.add(mainContent, BorderLayout.CENTER);
        return right;
    }

    // Welcome bar
    private JPanel buildWelcomeBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, TOP_BORDER),
            new EmptyBorder(12, 20, 12, 20)
        ));

        JLabel greet = new JLabel("Merhaba, " + parent.getFullName());
        greet.setFont(new Font("Segoe UI", Font.BOLD, 17));
        greet.setForeground(TEXT);

        String dateStr = LocalDate.now().format(
            DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR")));
        JLabel dateLbl = new JLabel(dateStr);
        dateLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateLbl.setForeground(MUTED);

        bar.add(greet,   BorderLayout.WEST);
        bar.add(dateLbl, BorderLayout.EAST);
        return bar;
    }

    // ====================================================================
    // GOREVLER KARTI
    // ====================================================================

    private JPanel buildTasksCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG);

        // Stats row
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        statsRow.setBackground(BG);
        statsTotalLbl   = buildStatCard("0", "Toplam Görev",    new Color(67, 97, 238),  statsRow);
        statsPendingLbl = buildStatCard("0", "Bekleyen",        ORANGE,                  statsRow);
        statsDoneLbl    = buildStatCard("0", "Tamamlanan",      SUCCESS,                 statsRow);
        updateTaskStats();

        // Task JList
        taskListModel = new DefaultListModel<>();
        taskJList     = new JList<>(taskListModel);
        AppUI.applyListStyle(taskJList, BG);
        populateTaskList();

        JScrollPane scroll = new JScrollPane(taskJList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setBackground(BG);
        listWrapper.setBorder(new EmptyBorder(0, 16, 8, 16));
        listWrapper.add(scroll, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);
        center.add(statsRow,    BorderLayout.NORTH);
        center.add(listWrapper, BorderLayout.CENTER);

        // Action bar
        JPanel actionBar = buildActionBar();
        JButton newBtn  = actionButton("Yeni Görev",           SUCCESS,  "plus");
        JButton doneBtn = actionButton("Tamamlandı İşaretle", PRIMARY,  "check");
        JButton delBtn  = actionButton("Sil",                  DANGER,  "trash");

        newBtn.addActionListener(e -> showAddTaskDialog());
        doneBtn.addActionListener(e -> markSelectedTaskDone());
        delBtn.addActionListener(e  -> deleteSelectedTask());

        actionBar.add(newBtn);
        actionBar.add(doneBtn);
        actionBar.add(delBtn);

        panel.add(center,    BorderLayout.CENTER);
        panel.add(actionBar, BorderLayout.SOUTH);
        return panel;
    }

    private void populateTaskList() {
        taskListModel.clear();
        for (Task t : parent.getTasks()) taskListModel.addElement(t);
    }

    private void updateTaskStats() {
        if (statsTotalLbl == null) return;
        List<Task> tasks = parent.getTasks();
        long pending = tasks.stream().filter(t -> !t.isCompleted()).count();
        long done    = tasks.stream().filter(Task::isCompleted).count();
        statsTotalLbl.setText(String.valueOf(tasks.size()));
        statsPendingLbl.setText(String.valueOf(pending));
        statsDoneLbl.setText(String.valueOf(done));
    }

    private void refreshTaskList() {
        populateTaskList();
        updateTaskStats();
    }

    private void markSelectedTaskDone() {
        int idx = taskJList.getSelectedIndex();
        if (idx < 0) { showInfo("Lütfen bir görev seçin."); return; }
        Task task = taskListModel.getElementAt(idx);
        if (task.isCompleted()) { showInfo("Bu görev zaten tamamlandı."); return; }
        task.setCompleted(true);
        dataManager.updateParent(parent);
        refreshTaskList();
        if (task.getGoogleEventId() != null && !task.getGoogleEventId().isEmpty()) {
            final String eid   = task.getGoogleEventId();
            final String title = task.getTitle();
            new Thread(() -> GoogleCalendarService.getInstance().markEventCompleted(eid, title)).start();
        }
    }

    private void deleteSelectedTask() {
        int idx = taskJList.getSelectedIndex();
        if (idx < 0) { showInfo("Lütfen bir görev seçin."); return; }
        Task task = taskListModel.getElementAt(idx);
        if (task.getGoogleEventId() != null && !task.getGoogleEventId().isEmpty()) {
            final String eid = task.getGoogleEventId();
            new Thread(() -> GoogleCalendarService.getInstance().deleteEvent(eid)).start();
        }
        parent.getTasks().remove(task);
        dataManager.updateParent(parent);
        refreshTaskList();
    }

    private void showAddTaskDialog() {
        JTextField titleField = new JTextField();
        JTextField descField  = new JTextField();
        JComboBox<String> priorityBox = new JComboBox<>(new String[]{"Yüksek", "Orta", "Düşük"});
        JTextField dateField  = new JTextField(LocalDate.now().plusDays(1).toString());

        JDialog dlg  = AppUI.createFormDialog(this, "Yeni Görev Ekle");
        JPanel  body = AppUI.buildDialogBody();
        body.add(AppUI.formRow("Başlık", titleField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Açıklama", descField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Öncelik", priorityBox));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Bitiş Tarihi  (YYYY-MM-DD)", dateField));

        boolean[] ok = {false};
        dlg.add(AppUI.dialogHeader("Yeni Görev Ekle"), BorderLayout.NORTH);
        dlg.add(body, BorderLayout.CENTER);
        dlg.add(AppUI.buildDialogFooter(dlg, ok, "Ekle"), BorderLayout.SOUTH);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(440, dlg.getHeight()));
        dlg.setVisible(true);

        if (!ok[0]) return;
        try {
            Task.Priority p = switch (priorityBox.getSelectedIndex()) {
                case 0 -> Task.Priority.HIGH;
                case 1 -> Task.Priority.MEDIUM;
                default -> Task.Priority.LOW;
            };
            Task task = new Task(DataManager.generateId(),
                titleField.getText().trim(), descField.getText().trim(),
                p, LocalDate.parse(dateField.getText().trim()));
            parent.addTask(task);
            dataManager.updateParent(parent);
            refreshTaskList();
            syncParentTaskToGoogle(task);
        } catch (Exception ex) {
            showError("Geçersiz tarih formatı! Lütfen YYYY-MM-DD kullanın.");
        }
    }

    // ====================================================================
    // TAKVİM KARTI
    // ====================================================================

    private JPanel buildCalendarCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);

        calendarPanel = new CalendarGridPanel(parent.getEvents(), new CalendarGridPanel.EventCallback() {
            @Override public void onAdd() { showAddEventDialog(); }
            @Override public void onDelete(CalendarEvent event) {
                if (event.getGoogleEventId() != null && !event.getGoogleEventId().isEmpty()) {
                    final String gid = event.getGoogleEventId();
                    new Thread(() -> GoogleCalendarService.getInstance().deleteEvent(gid)).start();
                }
                parent.getEvents().remove(event);
                dataManager.updateParent(parent);
                calendarPanel.setEvents(parent.getEvents());
            }
        });
        syncEventsFromGoogle();

        panel.add(calendarPanel, BorderLayout.CENTER);
        return panel;
    }

    private void showAddEventDialog() {
        JTextField titleField = new JTextField();
        JTextField descField  = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Toplantı", "Randevu", "Sınav", "Hatırlatıcı", "Diğer"});
        JTextField startField = new JTextField(LocalDateTime.now().toString().substring(0, 16));
        JTextField endField   = new JTextField(LocalDateTime.now().plusHours(1).toString().substring(0, 16));

        JDialog dlg  = AppUI.createFormDialog(this, "Yeni Etkinlik Ekle");
        JPanel  body = AppUI.buildDialogBody();
        body.add(AppUI.formRow("Başlık", titleField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Açıklama", descField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Tür", typeBox));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Başlangıç  (YYYY-MM-DDTHH:MM)", startField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Bitiş  (YYYY-MM-DDTHH:MM)", endField));

        boolean[] ok = {false};
        dlg.add(AppUI.dialogHeader("Yeni Etkinlik Ekle"), BorderLayout.NORTH);
        dlg.add(body, BorderLayout.CENTER);
        dlg.add(AppUI.buildDialogFooter(dlg, ok, "Ekle"), BorderLayout.SOUTH);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(440, dlg.getHeight()));
        dlg.setVisible(true);

        if (!ok[0]) return;
        try {
            CalendarEvent.EventType[] eventTypes = CalendarEvent.EventType.values();
            CalendarEvent event = new CalendarEvent(DataManager.generateId(),
                titleField.getText().trim(), descField.getText().trim(),
                LocalDateTime.parse(startField.getText().trim()),
                LocalDateTime.parse(endField.getText().trim()),
                eventTypes[typeBox.getSelectedIndex()]);
            parent.addEvent(event);
            dataManager.updateParent(parent);
            refreshCalendarView();
            pushEventToGoogle(event);
        } catch (Exception ex) {
            showError("Geçersiz tarih/saat formatı!");
        }
    }

    private void pushEventToGoogle(CalendarEvent event) {
        GoogleCalendarService gcal = GoogleCalendarService.getInstance();
        if (!gcal.isAuthenticated()) return;
        new Thread(() -> {
            String gid = gcal.createCalendarEvent(
                event.getTitle(), event.getDescription(),
                event.getStartTime(), event.getEndTime());
            if (gid != null) {
                event.setGoogleEventId(gid);
                SwingUtilities.invokeLater(() -> dataManager.updateParent(parent));
            }
        }).start();
    }

    private void syncEventsFromGoogle() {
        GoogleCalendarService gcal = GoogleCalendarService.getInstance();
        if (!gcal.isAuthenticated()) return;
        new Thread(() -> {
            List<model.CalendarEvent> fetched = gcal.fetchGoogleEvents();
            SwingUtilities.invokeLater(() -> {
                java.util.Set<String> existingGids = new java.util.HashSet<>();
                for (CalendarEvent ev : parent.getEvents()) {
                    if (ev.getGoogleEventId() != null) existingGids.add(ev.getGoogleEventId());
                }
                boolean changed = false;
                for (CalendarEvent ev : fetched) {
                    if (!existingGids.contains(ev.getGoogleEventId())) {
                        parent.addEvent(ev);
                        changed = true;
                    }
                }
                if (changed) {
                    dataManager.updateParent(parent);
                    refreshCalendarView();
                }
            });
        }).start();
    }

    private void refreshCalendarView() {
        if (calendarPanel != null) calendarPanel.setEvents(parent.getEvents());
    }

    // ====================================================================
    // ÇOCUKLAR KARTI
    // ====================================================================

    private JPanel buildChildrenCard() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(16, 20, 0, 20));

        // Ust: cocuklar tablosu
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG);
        topBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        topBar.add(sectionTitle("Çocuklarım"), BorderLayout.WEST);

        JButton assignBtn = actionButton("Görev Ata", ORANGE, "assign");
        assignBtn.addActionListener(e -> showAssignTaskDialog());
        topBar.add(assignBtn, BorderLayout.EAST);

        String[] cols = {"Ad Soyad", "Kullanıcı Adı", "Yaş", "Puan", "Bekleyen Görev"};
        childrenTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshChildrenTable();

        JTable childTable = new JTable(childrenTableModel);
        styleTable(childTable);

        JPanel upperPanel = new JPanel(new BorderLayout(0, 8));
        upperPanel.setBackground(BG);
        upperPanel.add(topBar,                          BorderLayout.NORTH);
        upperPanel.add(styledScrollPane(childTable),    BorderLayout.CENTER);

        // Alt: odül kuralları
        JPanel rewardPanel = buildRewardRulesSection();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upperPanel, rewardPanel);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        splitPane.setBackground(BG);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRewardRulesSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG);
        topBar.add(sectionTitle("Ödül Kuralları"), BorderLayout.WEST);

        JButton addBtn = actionButton("Ekle", SUCCESS, "plus");
        topBar.add(addBtn, BorderLayout.EAST);

        String[] cols = {"Ödül Açıklaması", "Gerekli Puan"};
        rewardRuleTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshRewardRuleTable();

        JTable table = new JTable(rewardRuleTableModel);
        styleTable(table);

        addBtn.addActionListener(e -> showAddRewardRuleDialog());

        JButton deleteBtn = actionButton("Sil", DANGER, "trash");
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showInfo("Lütfen bir kural seçin."); return; }
            List<RewardRule> rules = parent.getRewardRules();
            if (row < rules.size()) {
                parent.getRewardRules().remove(rules.get(row));
                dataManager.updateParent(parent);
                refreshRewardRuleTable();
            }
        });

        JPanel actionBar = buildActionBar();
        actionBar.add(deleteBtn);

        panel.add(topBar,                 BorderLayout.NORTH);
        panel.add(styledScrollPane(table), BorderLayout.CENTER);
        panel.add(actionBar,              BorderLayout.SOUTH);
        return panel;
    }

    private void showAddRewardRuleDialog() {
        JTextField descField  = new JTextField();
        JTextField pointField = new JTextField("50");

        JDialog dlg  = AppUI.createFormDialog(this, "Ödül Kuralı Ekle");
        JPanel  body = AppUI.buildDialogBody();
        body.add(AppUI.formRow("Ödül Açıklaması", descField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Gerekli Puan", pointField));

        boolean[] ok = {false};
        dlg.add(AppUI.dialogHeader("Ödül Kuralı Ekle"), BorderLayout.NORTH);
        dlg.add(body, BorderLayout.CENTER);
        dlg.add(AppUI.buildDialogFooter(dlg, ok, "Kaydet"), BorderLayout.SOUTH);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(400, dlg.getHeight()));
        dlg.setVisible(true);

        if (!ok[0]) return;
        try {
            int threshold = Integer.parseInt(pointField.getText().trim());
            if (threshold <= 0) throw new NumberFormatException();
            String desc = descField.getText().trim();
            if (desc.isEmpty()) { showError("Açıklama boş olamaz."); return; }
            parent.addRewardRule(new RewardRule(DataManager.generateId(), desc, threshold));
            dataManager.updateParent(parent);
            refreshRewardRuleTable();
        } catch (NumberFormatException ex) {
            showError("Geçersiz puan değeri! Pozitif bir sayı girin.");
        }
    }

    private void refreshRewardRuleTable() {
        rewardRuleTableModel.setRowCount(0);
        for (RewardRule r : parent.getRewardRules()) {
            rewardRuleTableModel.addRow(new Object[]{ r.getDescription(), r.getPointThreshold() });
        }
    }

    private void refreshChildrenTable() {
        childrenTableModel.setRowCount(0);
        for (Child c : dataManager.getChildrenByParentId(parent.getId())) {
            childrenTableModel.addRow(new Object[]{
                c.getFullName(), c.getUsername(), c.getAge(),
                c.getPoints() + " puan", c.getPendingAssignedTaskCount() + " bekliyor"
            });
        }
    }

    private void showAssignTaskDialog() {
        List<Child> myChildren = dataManager.getChildrenByParentId(parent.getId());
        if (myChildren.isEmpty()) { showInfo("Henüz kayıtlı çocuğunuz bulunmuyor."); return; }

        String[] childNames = myChildren.stream()
            .map(c -> c.getFullName() + " (" + c.getUsername() + ")")
            .toArray(String[]::new);

        JComboBox<String> childBox    = new JComboBox<>(childNames);
        JTextField titleField         = new JTextField();
        JTextField descField          = new JTextField();
        JComboBox<String> priorityBox = new JComboBox<>(new String[]{"Yüksek", "Orta", "Düşük"});
        JTextField dateField          = new JTextField(LocalDate.now().plusDays(1).toString());

        JDialog dlg  = AppUI.createFormDialog(this, "Çocuğa Görev Ata");
        JPanel  body = AppUI.buildDialogBody();
        body.add(AppUI.formRow("Çocuk", childBox));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Görev Başlığı", titleField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Açıklama", descField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Öncelik", priorityBox));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Bitiş Tarihi  (YYYY-MM-DD)", dateField));

        boolean[] ok = {false};
        dlg.add(AppUI.dialogHeader("Çocuğa Görev Ata"), BorderLayout.NORTH);
        dlg.add(body, BorderLayout.CENTER);
        dlg.add(AppUI.buildDialogFooter(dlg, ok, "Ata"), BorderLayout.SOUTH);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(440, dlg.getHeight()));
        dlg.setVisible(true);

        if (!ok[0]) return;
        try {
            Child selectedChild = myChildren.get(childBox.getSelectedIndex());
            Task.Priority p = switch (priorityBox.getSelectedIndex()) {
                case 0 -> Task.Priority.HIGH;
                case 1 -> Task.Priority.MEDIUM;
                default -> Task.Priority.LOW;
            };
            Task task = new Task(DataManager.generateId(),
                titleField.getText().trim(), descField.getText().trim(),
                p, LocalDate.parse(dateField.getText().trim()));
            parent.assignTaskToChild(task, selectedChild.getId());
            selectedChild.receiveAssignedTask(task);
            syncTaskToGoogleCalendar(task, selectedChild);
            dataManager.updateChild(selectedChild);
            refreshChildrenTable();
            showInfo("Görev başarıyla atandı: " + selectedChild.getFullName());
        } catch (Exception ex) {
            showError("Geçersiz tarih formatı! Lütfen YYYY-MM-DD kullanın.");
        }
    }

    private void syncParentTaskToGoogle(Task task) {
        GoogleCalendarService gcal = GoogleCalendarService.getInstance();
        if (!gcal.isAuthenticated()) return;
        new Thread(() -> {
            String eventId = gcal.createTaskEvent(task.getTitle(), task.getDescription(),
                task.getDueDate(), parent.getFullName());
            if (eventId != null) {
                task.setGoogleEventId(eventId);
                SwingUtilities.invokeLater(() -> dataManager.updateParent(parent));
            }
        }).start();
    }

    private void syncTaskToGoogleCalendar(Task task, Child child) {
        GoogleCalendarService gcal = GoogleCalendarService.getInstance();
        if (!gcal.isAuthenticated()) return;
        new Thread(() -> {
            String eventId = gcal.createTaskEvent(task.getTitle(), task.getDescription(),
                task.getDueDate(), child.getFullName());
            if (eventId != null) {
                task.setGoogleEventId(eventId);
                SwingUtilities.invokeLater(() -> {
                    dataManager.updateChild(child);
                    dataManager.updateParent(parent);
                });
            }
        }).start();
    }

    // ====================================================================
    // GOOGLE TAKVİM DİYALOĞU
    // ====================================================================

    private void showGoogleCalendarDialog() {
        GoogleCalendarService gcal = GoogleCalendarService.getInstance();
        if (!gcal.isConfigured()) {
            JTextField clientIdField     = new JTextField(32);
            JTextField clientSecretField = new JTextField(32);
            JPanel form = new JPanel();
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.add(new JLabel("<html><b>Google Cloud Console adimlari:</b><br>"
                + "1. console.cloud.google.com - Proje olustur<br>"
                + "2. APIler - Google Calendar APIyi etkinlestir<br>"
                + "3. Kimlik Bilgileri - OAuth 2.0 Istemci Kimligi (Masaustu Uygulamasi)<br><br></html>"));
            form.add(new JLabel("Client ID:"));
            form.add(clientIdField);
            form.add(Box.createVerticalStrut(6));
            form.add(new JLabel("Client Secret:"));
            form.add(clientSecretField);

            if (JOptionPane.showConfirmDialog(this, form, "Google Takvim Baglantisi",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                String cid  = clientIdField.getText().trim();
                String csec = clientSecretField.getText().trim();
                if (cid.isEmpty() || csec.isEmpty()) { showError("Client ID ve Secret bos olamaz."); return; }
                gcal.setCredentials(cid, csec);
                startOAuthFlow();
            }
        } else if (!gcal.isAuthenticated()) {
            startOAuthFlow();
        } else {
            if (JOptionPane.showConfirmDialog(this,
                    "Google Takvim bagli!\n\nBaglantiy kesmek istiyor musunuz?",
                    "Google Takvim", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                gcal.disconnect();
                updateGcalButton(false);
                showInfo("Google Takvim bağlantısı kesildi.");
            }
        }
    }

    private void startOAuthFlow() {
        showInfo("Tarayici aciliyor... Google hesabinizla yetkilendirin.\n(2 dakika icinde tamamlayin)");
        new Thread(() -> {
            try {
                GoogleCalendarService.getInstance().authenticate();
                SwingUtilities.invokeLater(() -> {
                    updateGcalButton(true);
                    showInfo("Google Takvim başarıyla bağlandı!\nAtanan görevler artık otomatik takvime eklenir.");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> showError("Baglanti hatasi: " + ex.getMessage()));
            }
        }).start();
    }

    private void updateGcalButton(boolean connected) {
        if (gcalBtn == null) return;
        gcalBtn.setText(connected ? "Google Takvim ✓" : "Google Takvim");
        gcalBtn.setBackground(connected ? new Color(40, 150, 80) : new Color(70, 90, 180));
    }

    // ====================================================================
    // NAVIGASYON YARDIMCILARI
    // ====================================================================

    /** Aktif nav butonunu parlak yapar, diğerlerini soluklaştırır. */
    private void setActiveNav(int activeIdx) {
        for (int i = 0; i < navBtns.length; i++) {
            boolean active = i == activeIdx;
            navBtns[i].setBackground(active ? NAV_ACT : NAV_IDLE);
            navBtns[i].setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        }
    }

    // ====================================================================
    // STATS KARTI YARDIMCISI
    // ====================================================================

    /**
     * Küçük istatistik kartı oluşturur; büyük renkli sayı + küçük label.
     * Kartı parent'a ekler ve sayı label'ını döndürür (güncelleme için).
     */
    private JLabel buildStatCard(String value, String label, Color color, JPanel parent) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Left color strip
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 10, getHeight(), 12, 12);
                g2.fillRect(5, 0, 5, getHeight());
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 18, 10, 18));
        card.setPreferredSize(new Dimension(130, 70));

        JLabel numLbl = new JLabel(value);
        numLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        numLbl.setForeground(color);
        numLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLbl = new JLabel(label);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLbl.setForeground(MUTED);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(numLbl);
        card.add(descLbl);
        parent.add(card);
        return numLbl;
    }

    // ====================================================================
    // YARDIMCI UI
    // ====================================================================

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bar.setBackground(Color.WHITE);
        bar.setBorder(new MatteBorder(1, 0, 0, 0, TOP_BORDER));
        return bar;
    }

    private JButton makeNavBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(NAV_IDLE);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(184, 36));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return btn;
    }

    private JButton actionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private JButton actionButton(String text, Color color, String iconType) {
        JButton btn = actionButton(text, color);
        btn.setIcon(AppUI.createIcon(iconType, 14));
        btn.setIconTextGap(6);
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        return btn;
    }

    private JSeparator makeSep() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(70, 90, 160));
        sep.setMaximumSize(new Dimension(184, 1));
        return sep;
    }

    private JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lbl.setForeground(TEXT);
        return lbl;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setRowHeight(30);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(200, 210, 255));
        table.setSelectionForeground(TEXT);
        table.setGridColor(new Color(230, 235, 245));
        table.setShowGrid(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                        tbl, value, isSelected, hasFocus, row, col);
                lbl.setBackground(PRIMARY);
                lbl.setForeground(Color.WHITE);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                lbl.setOpaque(true);
                return lbl;
            }
        });
    }

    private JScrollPane styledScrollPane(java.awt.Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 240)));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Bilgi", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Hata", JOptionPane.ERROR_MESSAGE);
    }
}
