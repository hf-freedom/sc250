package com.hospital.triage.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class Doctor {
    private String id;
    private String name;
    private String departmentId;
    private String departmentName;
    private String title;
    private int maxDailyPatients;
    private List<DayOfWeek> workingDays;
    private boolean isAvailable;
    private LocalDate unavailableDate;

    public Doctor() {}

    public Doctor(String id, String name, String departmentId, String departmentName, String title,
                  int maxDailyPatients, List<DayOfWeek> workingDays, boolean isAvailable, LocalDate unavailableDate) {
        this.id = id;
        this.name = name;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.title = title;
        this.maxDailyPatients = maxDailyPatients;
        this.workingDays = workingDays;
        this.isAvailable = isAvailable;
        this.unavailableDate = unavailableDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getMaxDailyPatients() { return maxDailyPatients; }
    public void setMaxDailyPatients(int maxDailyPatients) { this.maxDailyPatients = maxDailyPatients; }
    public List<DayOfWeek> getWorkingDays() { return workingDays; }
    public void setWorkingDays(List<DayOfWeek> workingDays) { this.workingDays = workingDays; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    public LocalDate getUnavailableDate() { return unavailableDate; }
    public void setUnavailableDate(LocalDate unavailableDate) { this.unavailableDate = unavailableDate; }
}
