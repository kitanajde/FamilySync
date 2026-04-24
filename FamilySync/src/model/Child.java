package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Çocuk kullanıcı sınıfı.
 * Ebeveyn tarafından atanan görevleri görüntüler ve tamamlayabilir.
 */
public class Child extends User {

    private int age;
    private String parentId; // Bağlı olduğu ebeveynin ID'si
    private List<Task> assignedTasks; // Ebeveyn tarafından atanan görevler

    public Child(String id, String username, String password, String fullName, int age, String parentId) {
        super(id, username, password, fullName);
        this.age = age;
        this.parentId = parentId;
        this.assignedTasks = new ArrayList<>();
    }

    /**
     * Polimorfizm: Çocuk için özel dashboard başlığı
     */
    @Override
    public String getDashboardTitle() {
        return "Merhaba, " + getFullName() + "! 👋";
    }

    /**
     * Ebeveynden gelen görevi kabul et
     */
    public void receiveAssignedTask(Task task) {
        assignedTasks.add(task);
    }

    /**
     * Atanan görevi tamamlandı olarak işaretle
     */
    public boolean completeAssignedTask(String taskId) {
        for (Task task : assignedTasks) {
            if (task.getId().equals(taskId)) {
                task.setCompleted(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Tamamlanmamış atanmış görevlerin sayısını döndürür
     */
    public int getPendingAssignedTaskCount() {
        return (int) assignedTasks.stream().filter(t -> !t.isCompleted()).count();
    }

    // --- Getter / Setter ---

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public List<Task> getAssignedTasks() { return assignedTasks; }
    public void setAssignedTasks(List<Task> assignedTasks) { this.assignedTasks = assignedTasks; }
}
