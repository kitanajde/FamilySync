package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Ebeveyn kullanıcı sınıfı.
 * Çocuklara görev atayabilir, aile takvimini yönetebilir.
 */
public class Parent extends User {

    // Ebeveynin yönettiği çocukların ID listesi
    private List<String> childrenIds;

    public Parent(String id, String username, String password, String fullName) {
        super(id, username, password, fullName);
        this.childrenIds = new ArrayList<>();
    }

    /**
     * Polimorfizm: Ebeveyn için özel dashboard başlığı
     */
    @Override
    public String getDashboardTitle() {
        return "Ebeveyn Paneli - Hoş geldiniz, " + getFullName();
    }

    /**
     * Polimorfizm: Ebeveyn görev eklerken öncelik seviyesi kontrolü yapabilir
     */
    @Override
    public void addTask(Task task) {
        // Ebeveynler yüksek öncelikli görevleri listenin başına ekler
        if (task.getPriority() == Task.Priority.HIGH) {
            getTasks().add(0, task);
        } else {
            super.addTask(task);
        }
    }

    /**
     * Belirli bir çocuğa görev atar (childId ile eşleşen Child bulunur dışarıda)
     */
    public void assignTaskToChild(Task task, String childId) {
        task.setAssignedToChildId(childId);
        task.setAssignedByParentId(this.getId());
    }

    public void addChildId(String childId) {
        if (!childrenIds.contains(childId)) {
            childrenIds.add(childId);
        }
    }

    public void removeChildId(String childId) {
        childrenIds.remove(childId);
    }

    // --- Getter / Setter ---

    public List<String> getChildrenIds() { return childrenIds; }
    public void setChildrenIds(List<String> childrenIds) { this.childrenIds = childrenIds; }
}
