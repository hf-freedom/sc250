package com.hospital.triage.model;

import com.hospital.triage.enums.PatientPriority;

public class Patient {
    private String id;
    private String name;
    private String idCard;
    private String phone;
    private int age;
    private String gender;
    private PatientPriority priority;

    public Patient() {}

    public Patient(String id, String name, String idCard, String phone, int age, String gender, PatientPriority priority) {
        this.id = id;
        this.name = name;
        this.idCard = idCard;
        this.phone = phone;
        this.age = age;
        this.gender = gender;
        this.priority = priority;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public PatientPriority getPriority() { return priority; }
    public void setPriority(PatientPriority priority) { this.priority = priority; }
}
