package gui;

import model.*;
import storage.DataManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Cocuk dashboard ekrani - tam yeniden tasarim.
 * Sidebar navigasyonu CardLayout ile icerik panellerini degistirir.
 */
public class ChildDashboard extends JFrame {

    private final Child       child;
    private final DataManager dataManager;

    // ---- Renk Paleti ----
    private static final Color BG         = new Color(240, 242, 249);
    private static final Color SIDEBAR    = new Color(36, 48, 110);
    private static final Color PRIMARY    = new Color(67, 97, 238);
    private static final Color SUCCESS    = new Color(40, 167, 69);
    @SuppressWarnings("unused")
    private static final Color DANGER     = new Color(220, 53, 69);
    private static final Color TEXT       = new Color(33, 37, 41);
    private static final Color MUTED      = new Color(108, 117, 130);
    private static final Color GOLD       = new Color(255, 193, 7);
    private static final Color NAV_IDLE   = new Color(55, 72, 160);
    private static final Color NAV_ACT    = new Color(80, 100, 220);
    private static final Color TOP_BORDER = new Color(220, 225, 240);

    // ---- CardLayout / navigasyon ----
    private CardLayout cardLayout;
    private JPanel     mainContent;
    private JButton[]  navBtns;          // [0]=Gorevlerim [1]=Atanan [2]=Takvim [3]=Rozetler

    // ---- Kendi gorevler listesi ----
    private DefaultListModel<Task> myTaskListModel;
    private JList<Task>            myTaskJList;
    private JLabel myStatsTotalLbl, myStatsPendingLbl, myStatsDoneLbl;

    // ---- Atanan gorevler listesi ----
    private DefaultListModel<Task> assignedListModel;
    private JList<Task>            assignedJList;
    private JLabel asgnStatsTotalLbl, asgnStatsPendingLbl, asgnStatsDoneLbl;

    // ---- Takvim ----
    private CalendarGridPanel calendarPanel;

    // ---- Rozetler / puanlar ----
    private JLabel pointsLabel;
    private JPanel badgeGridPanel;
    private JPanel rewardListPanel;

    // ====================================================================
    public ChildDashboard(Child child, DataManager dataManager) {
        this.child       = child;
        this.dataManager = dataManager;
        initUI();
    }

    // ====================================================================
    // INIT
    // ====================================================================

    private void initUI() {
        setTitle("FamilySync - " + child.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(850, 580));
        setIconImages(AppUI.createWindowIconImages());

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(),     BorderLayout.WEST);
        root.add(buildRightPanel(),  BorderLayout.CENTER);

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
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(28, 16, 16, 16));

        // Avatar
        JPanel avatar = AppUI.createAvatarPanel(child.getFullName(), new Color(100, 140, 240), 60);
        sidebar.add(avatar);
        sidebar.add(Box.createVerticalStrut(8));

        // İsim
        JLabel nameLbl = new JLabel(child.getFullName(), SwingConstants.CENTER);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(nameLbl);

        // Yas
        JLabel ageLbl = new JLabel("Çocuk • " + child.getAge() + " yaş", SwingConstants.CENTER);
        ageLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ageLbl.setForeground(new Color(180, 195, 240));
        ageLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(ageLbl);

        sidebar.add(Box.createVerticalStrut(6));

        // Puan rozeti
        JLabel pointsBadge = new JLabel("💰 " + child.getPoints() + " puan", SwingConstants.CENTER);
        pointsBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pointsBadge.setForeground(GOLD);
        pointsBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(pointsBadge);

        // Bekleyen uyarisi
        int pending = child.getPendingAssignedTaskCount();
        if (pending > 0) {
            sidebar.add(Box.createVerticalStrut(4));
            JLabel warn = new JLabel("! " + pending + " görev bekliyor!", SwingConstants.CENTER);
            warn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            warn.setForeground(new Color(255, 220, 80));
            warn.setAlignmentX(Component.CENTER_ALIGNMENT);
            sidebar.add(warn);
        }

        sidebar.add(Box.createVerticalStrut(18));
        sidebar.add(makeSep());
        sidebar.add(Box.createVerticalStrut(14));

        // Nav buttons
        String[] labels = {"Görevlerim", "Atanan Görevler", "Takvim", "Rozetlerim"};
        String[] cards  = {"myTasks", "assigned", "calendar", "badges"};
        navBtns = new JButton[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int idx  = i;
            final String c = cards[i];
            JButton btn = makeNavBtn(labels[i]);
            btn.addActionListener(e -> { cardLayout.show(mainContent, c); setActiveNav(idx); });
            navBtns[i] = btn;
            sidebar.add(btn);
            if (i < labels.length - 1) sidebar.add(Box.createVerticalStrut(6));
        }

        sidebar.add(Box.createGlue());

        // Çıkış
        JButton logoutBtn = makeNavBtn("Çıkış");
        logoutBtn.setIcon(AppUI.createIcon("logout", 14));
        logoutBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        logoutBtn.addActionListener(e -> { dispose(); new LoginFrame(dataManager); });
        sidebar.add(logoutBtn);

        return sidebar;
    }

    // ====================================================================
    // SAG PANEL
    // ====================================================================

    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(BG);
        right.add(buildWelcomeBar(), BorderLayout.NORTH);

        cardLayout  = new CardLayout();
        mainContent = new JPanel(cardLayout);
        mainContent.setBackground(BG);
        mainContent.add(buildMyTasksCard(),    "myTasks");
        mainContent.add(buildAssignedCard(),   "assigned");
        mainContent.add(buildCalendarCard(),   "calendar");
        mainContent.add(buildBadgesTab(),      "badges");

        right.add(mainContent, BorderLayout.CENTER);
        return right;
    }

    private JPanel buildWelcomeBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, TOP_BORDER),
            new EmptyBorder(12, 20, 12, 20)
        ));

        JLabel greet = new JLabel("Merhaba, " + child.getFullName() + " 👋");
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
    // KENDİ GÖREVLERİM KARTI
    // ====================================================================

    private JPanel buildMyTasksCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG);

        // Stats row
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        statsRow.setBackground(BG);
        myStatsTotalLbl   = buildStatCard("0", "Toplam",     PRIMARY, statsRow);
        myStatsPendingLbl = buildStatCard("0", "Bekleyen",   new Color(255, 152, 0), statsRow);
        myStatsDoneLbl    = buildStatCard("0", "Tamamlanan", SUCCESS, statsRow);
        updateMyTaskStats();

        // JList
        myTaskListModel = new DefaultListModel<>();
        myTaskJList     = new JList<>(myTaskListModel);
        AppUI.applyListStyle(myTaskJList, BG);
        populateMyTaskList();

        JScrollPane scroll = new JScrollPane(myTaskJList);
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
        JButton addBtn  = actionButton("Görev Ekle",  SUCCESS, "plus");
        JButton doneBtn = actionButton("Tamamlandı",  PRIMARY, "check");

        addBtn.addActionListener(e  -> showAddMyTaskDialog());
        doneBtn.addActionListener(e -> completeMyTask());

        actionBar.add(addBtn);
        actionBar.add(doneBtn);

        panel.add(center,    BorderLayout.CENTER);
        panel.add(actionBar, BorderLayout.SOUTH);
        return panel;
    }

    private void populateMyTaskList() {
        myTaskListModel.clear();
        for (Task t : child.getTasks()) myTaskListModel.addElement(t);
    }

    private void updateMyTaskStats() {
        if (myStatsTotalLbl == null) return;
        List<Task> tasks = child.getTasks();
        long pending = tasks.stream().filter(t -> !t.isCompleted()).count();
        long done    = tasks.stream().filter(Task::isCompleted).count();
        myStatsTotalLbl.setText(String.valueOf(tasks.size()));
        myStatsPendingLbl.setText(String.valueOf(pending));
        myStatsDoneLbl.setText(String.valueOf(done));
    }

    private void refreshMyTaskList() {
        populateMyTaskList();
        updateMyTaskStats();
    }

    private void completeMyTask() {
        int idx = myTaskJList.getSelectedIndex();
        if (idx < 0) { showInfo("Lütfen bir görev seçin."); return; }
        Task task = myTaskListModel.getElementAt(idx);
        if (task.isCompleted()) { showInfo("Bu görev zaten tamamlandı."); return; }
        List<String> badgesBefore = new ArrayList<>(child.getEarnedBadgeNames());
        int earned = child.completeOwnTask(task.getId());
        dataManager.updateChild(child);
        refreshMyTaskList();
        refreshBadgesPanel();
        if (earned >= 0) showCompletionMessage(earned, badgesBefore);
    }

    private void showAddMyTaskDialog() {
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
            child.addTask(task);
            dataManager.updateChild(child);
            refreshMyTaskList();
        } catch (Exception ex) {
            showError("Geçersiz tarih formatı!");
        }
    }

    // ====================================================================
    // ATANAN GÖREVLER KARTI
    // ====================================================================

    private JPanel buildAssignedCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG);

        // Stats row
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        statsRow.setBackground(BG);
        asgnStatsTotalLbl   = buildStatCard("0", "Toplam Atanan",  PRIMARY, statsRow);
        asgnStatsPendingLbl = buildStatCard("0", "Bekleyen",       new Color(255, 152, 0), statsRow);
        asgnStatsDoneLbl    = buildStatCard("0", "Tamamlanan",     SUCCESS, statsRow);
        updateAssignedStats();

        // JList
        assignedListModel = new DefaultListModel<>();
        assignedJList     = new JList<>(assignedListModel);
        AppUI.applyListStyle(assignedJList, BG);
        populateAssignedList();

        JScrollPane scroll = new JScrollPane(assignedJList);
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
        JButton doneBtn = actionButton("Tamamlandı", SUCCESS, "check");
        doneBtn.addActionListener(e -> completeAssignedTask());
        actionBar.add(doneBtn);

        panel.add(center,    BorderLayout.CENTER);
        panel.add(actionBar, BorderLayout.SOUTH);
        return panel;
    }

    private void populateAssignedList() {
        assignedListModel.clear();
        for (Task t : child.getAssignedTasks()) assignedListModel.addElement(t);
    }

    private void updateAssignedStats() {
        if (asgnStatsTotalLbl == null) return;
        List<Task> tasks = child.getAssignedTasks();
        long pending = tasks.stream().filter(t -> !t.isCompleted()).count();
        long done    = tasks.stream().filter(Task::isCompleted).count();
        asgnStatsTotalLbl.setText(String.valueOf(tasks.size()));
        asgnStatsPendingLbl.setText(String.valueOf(pending));
        asgnStatsDoneLbl.setText(String.valueOf(done));
    }

    private void refreshAssignedList() {
        populateAssignedList();
        updateAssignedStats();
    }

    private void completeAssignedTask() {
        int idx = assignedJList.getSelectedIndex();
        if (idx < 0) { showInfo("Lütfen bir görev seçin."); return; }
        Task task = assignedListModel.getElementAt(idx);
        if (task.isCompleted()) { showInfo("Bu görev zaten tamamlandı."); return; }
        List<String> before = new ArrayList<>(child.getEarnedBadgeNames());
        int earned = child.completeAssignedTask(task.getId());
        if (earned >= 0) {
            if (task.getGoogleEventId() != null && !task.getGoogleEventId().isEmpty()) {
                final String eid = task.getGoogleEventId();
                new Thread(() -> service.GoogleCalendarService.getInstance()
                    .markEventCompleted(eid, task.getTitle())).start();
            }
            dataManager.updateChild(child);
            refreshAssignedList();
            refreshBadgesPanel();
            showCompletionMessage(earned, before);
        }
    }

    // ====================================================================
    // TAKVİM KARTI
    // ====================================================================

    private JPanel buildCalendarCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);

        calendarPanel = new CalendarGridPanel(child.getEvents(), new CalendarGridPanel.EventCallback() {
            @Override public void onAdd() { showAddEventDialog(); }
            @Override public void onDelete(CalendarEvent event) {
                if (event.getGoogleEventId() != null && !event.getGoogleEventId().isEmpty()) {
                    final String gid = event.getGoogleEventId();
                    new Thread(() -> service.GoogleCalendarService.getInstance().deleteEvent(gid)).start();
                }
                child.getEvents().remove(event);
                dataManager.updateChild(child);
                calendarPanel.setEvents(child.getEvents());
            }
        });
        syncEventsFromGoogle();

        panel.add(calendarPanel, BorderLayout.CENTER);
        return panel;
    }

    private void showAddEventDialog() {
        JTextField titleField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Toplantı", "Randevu", "Sınav", "Hatırlatıcı", "Diğer"});
        JTextField startField = new JTextField(LocalDateTime.now().toString().substring(0, 16));
        JTextField endField   = new JTextField(LocalDateTime.now().plusHours(1).toString().substring(0, 16));

        JDialog dlg  = AppUI.createFormDialog(this, "Etkinlik Ekle");
        JPanel  body = AppUI.buildDialogBody();
        body.add(AppUI.formRow("Başlık", titleField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Tür", typeBox));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Başlangıç  (YYYY-MM-DDTHH:MM)", startField));
        body.add(Box.createVerticalStrut(12));
        body.add(AppUI.formRow("Bitiş  (YYYY-MM-DDTHH:MM)", endField));

        boolean[] ok = {false};
        dlg.add(AppUI.dialogHeader("Etkinlik Ekle"), BorderLayout.NORTH);
        dlg.add(body, BorderLayout.CENTER);
        dlg.add(AppUI.buildDialogFooter(dlg, ok, "Ekle"), BorderLayout.SOUTH);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(440, dlg.getHeight()));
        dlg.setVisible(true);

        if (!ok[0]) return;
        try {
            CalendarEvent.EventType[] eventTypes = CalendarEvent.EventType.values();
            CalendarEvent event = new CalendarEvent(DataManager.generateId(),
                titleField.getText().trim(), "",
                LocalDateTime.parse(startField.getText().trim()),
                LocalDateTime.parse(endField.getText().trim()),
                eventTypes[typeBox.getSelectedIndex()]);
            child.addEvent(event);
            dataManager.updateChild(child);
            refreshCalendarView();
            pushEventToGoogle(event);
        } catch (Exception ex) {
            showError("Geçersiz tarih/saat formatı!");
        }
    }

    private void pushEventToGoogle(CalendarEvent event) {
        service.GoogleCalendarService gcal = service.GoogleCalendarService.getInstance();
        if (!gcal.isAuthenticated()) return;
        new Thread(() -> {
            String gid = gcal.createCalendarEvent(
                event.getTitle(), event.getDescription(),
                event.getStartTime(), event.getEndTime());
            if (gid != null) {
                event.setGoogleEventId(gid);
                SwingUtilities.invokeLater(() -> dataManager.updateChild(child));
            }
        }).start();
    }

    private void syncEventsFromGoogle() {
        service.GoogleCalendarService gcal = service.GoogleCalendarService.getInstance();
        if (!gcal.isAuthenticated()) return;
        new Thread(() -> {
            List<CalendarEvent> fetched = gcal.fetchGoogleEvents();
            SwingUtilities.invokeLater(() -> {
                java.util.Set<String> existingGids = new java.util.HashSet<>();
                for (CalendarEvent ev : child.getEvents()) {
                    if (ev.getGoogleEventId() != null) existingGids.add(ev.getGoogleEventId());
                }
                boolean changed = false;
                for (CalendarEvent ev : fetched) {
                    if (!existingGids.contains(ev.getGoogleEventId())) {
                        child.addEvent(ev);
                        changed = true;
                    }
                }
                if (changed) {
                    dataManager.updateChild(child);
                    refreshCalendarView();
                }
            });
        }).start();
    }

    private void refreshCalendarView() {
        if (calendarPanel != null) calendarPanel.setEvents(child.getEvents());
    }

    // ====================================================================
    // ROZETLER & PUANLAR KARTI
    // ====================================================================

    private JPanel buildBadgesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 16));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Puan basligi
        pointsLabel = new JLabel();
        updatePointsLabel();
        pointsLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        pointsLabel.setForeground(PRIMARY);
        pointsLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subLabel = new JLabel("Görevleri tamamlayarak puan ve rozet kazanıyorsun!", SwingConstants.CENTER);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(MUTED);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(BG);
        headerPanel.add(pointsLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subLabel);
        headerPanel.add(Box.createVerticalStrut(16));

        // Rozet grid
        badgeGridPanel = new JPanel(new GridLayout(0, 4, 12, 12));
        badgeGridPanel.setBackground(BG);
        rebuildBadgeGrid();

        JScrollPane badgeScroll = new JScrollPane(badgeGridPanel);
        badgeScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 230)),
            "  Rozetler  "));
        badgeScroll.getViewport().setBackground(BG);

        // Odul listesi
        rewardListPanel = new JPanel();
        rewardListPanel.setLayout(new BoxLayout(rewardListPanel, BoxLayout.Y_AXIS));
        rewardListPanel.setBackground(BG);
        rewardListPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 230)),
            "  Ebeveynimden Ödüller  "));
        rebuildRewardList();

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        centerPanel.setBackground(BG);
        centerPanel.add(badgeScroll);
        centerPanel.add(rewardListPanel);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private void updatePointsLabel() {
        if (pointsLabel != null) {
            pointsLabel.setText(child.getPoints() + " Puan  |  " + child.getCompletedTaskCount() + " Görev Tamamlandı");
        }
    }

    private void rebuildBadgeGrid() {
        if (badgeGridPanel == null) return;
        badgeGridPanel.removeAll();
        List<Badge> earned = child.getEarnedBadges();
        for (Badge badge : Badge.values()) {
            boolean isEarned = earned.stream().anyMatch(b -> b == badge);
            badgeGridPanel.add(createBadgeCard(badge, isEarned));
        }
        badgeGridPanel.revalidate();
        badgeGridPanel.repaint();
    }

    private JPanel createBadgeCard(Badge badge, boolean earned) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(earned ? new Color(255, 250, 230) : new Color(240, 242, 247));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(earned ? GOLD : new Color(210, 215, 225), 1),
            new EmptyBorder(10, 8, 10, 8)
        ));

        JLabel emojiLabel = new JLabel(badge.getLabel().substring(0, 2), SwingConstants.CENTER);
        emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        emojiLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel("<html><center>" + badge.getLabel().substring(2).trim() + "</center></html>", SwingConstants.CENTER);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        nameLabel.setForeground(earned ? new Color(120, 80, 0) : new Color(150, 155, 165));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String thresholdText = badge.getTaskThreshold() > 0
            ? badge.getTaskThreshold() + " görev"
            : badge.getPointThreshold() + " puan";
        JLabel threshLabel = new JLabel(thresholdText, SwingConstants.CENTER);
        threshLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        threshLabel.setForeground(earned ? new Color(160, 110, 0) : new Color(180, 185, 195));
        threshLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (!earned) {
            JLabel lockLabel = new JLabel("🔒", SwingConstants.CENTER);
            lockLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
            lockLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lockLabel);
        }

        card.add(emojiLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(threshLabel);
        return card;
    }

    private void rebuildRewardList() {
        if (rewardListPanel == null) return;
        rewardListPanel.removeAll();
        rewardListPanel.add(Box.createVerticalStrut(8));

        Parent parent = dataManager.getParentById(child.getParentId());
        List<RewardRule> rules = (parent != null) ? parent.getRewardRules() : new ArrayList<>();

        if (rules.isEmpty()) {
            JLabel emptyLabel = new JLabel("  Henüz ödül kuralı belirlenmemiş.", SwingConstants.LEFT);
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            emptyLabel.setForeground(new Color(150, 155, 165));
            rewardListPanel.add(emptyLabel);
        } else {
            for (RewardRule rule : rules) {
                boolean unlocked = child.getPoints() >= rule.getPointThreshold();
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setBackground(unlocked ? new Color(230, 255, 237) : BG);
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(unlocked ? SUCCESS : new Color(215, 220, 230), 1),
                    new EmptyBorder(8, 12, 8, 12)
                ));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

                JLabel descLabel = new JLabel((unlocked ? "✓ " : "- ") + rule.getDescription());
                descLabel.setFont(new Font("Segoe UI", Font.PLAIN + (unlocked ? Font.BOLD : 0), 13));
                descLabel.setForeground(unlocked ? new Color(20, 120, 50) : TEXT);

                JLabel ptLabel = new JLabel(rule.getPointThreshold() + " puan");
                ptLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                ptLabel.setForeground(unlocked ? SUCCESS : new Color(100, 110, 130));

                row.add(descLabel, BorderLayout.CENTER);
                row.add(ptLabel,   BorderLayout.EAST);
                rewardListPanel.add(row);
                rewardListPanel.add(Box.createVerticalStrut(6));
            }
        }

        rewardListPanel.revalidate();
        rewardListPanel.repaint();
    }

    /** Puan/rozet/odul panellerini yeniler. */
    private void refreshBadgesPanel() {
        updatePointsLabel();
        rebuildBadgeGrid();
        rebuildRewardList();
    }

    // ====================================================================
    // GÖREV TAMAMLAMA MESAJI
    // ====================================================================

    private void showCompletionMessage(int pointsEarned, List<String> badgesBefore) {
        List<String> newBadgeNames = child.getEarnedBadgeNames().stream()
            .filter(b -> !badgesBefore.contains(b))
            .collect(Collectors.toList());

        StringBuilder msg = new StringBuilder("Harika! Görevi tamamladın.\n");
        msg.append("+").append(pointsEarned).append(" puan kazandın! (Toplam: ").append(child.getPoints()).append(")\n");

        if (!newBadgeNames.isEmpty()) {
            msg.append("\nYeni Rozet Kazandin!\n");
            for (String name : newBadgeNames) {
                try { msg.append("  ").append(Badge.valueOf(name).getLabel()).append("\n"); }
                catch (Exception ignored) {}
            }
        }

        JOptionPane.showMessageDialog(this, msg.toString(), "Tebrikler!", JOptionPane.INFORMATION_MESSAGE);
    }

    // ====================================================================
    // NAVİGASYON YARDIMCILARI
    // ====================================================================

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

    private JLabel buildStatCard(String value, String label, Color color, JPanel parent) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
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
        btn.setMaximumSize(new Dimension(178, 36));
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
        sep.setMaximumSize(new Dimension(178, 1));
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
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(200, 225, 255));
        table.setGridColor(new Color(230, 235, 245));
        table.setBackground(Color.WHITE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Bilgi", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Hata", JOptionPane.ERROR_MESSAGE);
    }
}
