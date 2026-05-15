package com.hospital.triage.storage;

import com.hospital.triage.enums.AppointmentStatus;
import com.hospital.triage.model.Appointment;
import com.hospital.triage.model.Department;
import com.hospital.triage.model.Doctor;
import com.hospital.triage.model.Patient;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataStorage {
    public final Map<String, Department> departments = new ConcurrentHashMap<>();
    public final Map<String, Doctor> doctors = new ConcurrentHashMap<>();
    public final Map<String, Patient> patients = new ConcurrentHashMap<>();
    public final Map<String, Appointment> appointments = new ConcurrentHashMap<>();
    public final Map<String, PriorityQueue<Appointment>> doctorQueues = new ConcurrentHashMap<>();
    public final Map<String, Integer> dailyNumberCounter = new ConcurrentHashMap<>();
    public final Map<String, Set<String>> dailyPatientDepartment = new ConcurrentHashMap<>();

    public DataStorage() {
        initData();
    }

    private void initData() {
        Department dept1 = new Department("D001", "内科", "内科诊疗");
        Department dept2 = new Department("D002", "外科", "外科诊疗");
        Department dept3 = new Department("D003", "儿科", "儿科诊疗");
        Department dept4 = new Department("D004", "急诊科", "急诊急救");
        departments.put(dept1.getId(), dept1);
        departments.put(dept2.getId(), dept2);
        departments.put(dept3.getId(), dept3);
        departments.put(dept4.getId(), dept4);

        List<DayOfWeek> workDays = Arrays.asList(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );

        Doctor doc1 = new Doctor("DOC001", "张医生", "D001", "内科", "主任医师", 20, workDays, true, null);
        Doctor doc2 = new Doctor("DOC002", "李医生", "D001", "内科", "副主任医师", 15, workDays, true, null);
        Doctor doc3 = new Doctor("DOC003", "王医生", "D002", "外科", "主任医师", 18, workDays, true, null);
        Doctor doc4 = new Doctor("DOC004", "赵医生", "D003", "儿科", "主治医师", 25, workDays, true, null);
        Doctor doc5 = new Doctor("DOC005", "刘医生", "D004", "急诊科", "副主任医师", 30, workDays, true, null);
        doctors.put(doc1.getId(), doc1);
        doctors.put(doc2.getId(), doc2);
        doctors.put(doc3.getId(), doc3);
        doctors.put(doc4.getId(), doc4);
        doctors.put(doc5.getId(), doc5);
    }

    public String generateQueueNumber(String doctorId, LocalDate date) {
        String key = doctorId + "_" + date.toString();
        dailyNumberCounter.putIfAbsent(key, 0);
        int number = dailyNumberCounter.merge(key, 1, Integer::sum);
        return doctorId.substring(doctorId.length() - 3) + String.format("%03d", number);
    }

    public boolean isDuplicateAppointment(String patientId, String departmentId, LocalDate date) {
        String key = patientId + "_" + date.toString();
        Set<String> depts = dailyPatientDepartment.get(key);
        return depts != null && depts.contains(departmentId);
    }

    public void recordPatientDepartment(String patientId, String departmentId, LocalDate date) {
        String key = patientId + "_" + date.toString();
        dailyPatientDepartment.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(departmentId);
    }

    public void removePatientDepartmentRecord(String patientId, String departmentId, LocalDate date) {
        String key = patientId + "_" + date.toString();
        Set<String> depts = dailyPatientDepartment.get(key);
        if (depts != null) {
            depts.remove(departmentId);
        }
    }

    public int getTodayAppointmentCount(String doctorId, LocalDate date) {
        return (int) appointments.values().stream()
            .filter(a -> a.getDoctorId().equals(doctorId)
                && a.getAppointmentDate().equals(date)
                && a.getStatus() != AppointmentStatus.CANCELLED
                && a.getStatus() != AppointmentStatus.MISSED)
            .count();
    }

    public List<Appointment> getDoctorAppointments(String doctorId, LocalDate date) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : appointments.values()) {
            if (a.getDoctorId().equals(doctorId) && a.getAppointmentDate().equals(date)) {
                result.add(a);
            }
        }
        result.sort(Appointment::compareTo);
        return result;
    }

    public List<Appointment> getWaitingAppointments(String doctorId) {
        List<Appointment> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Appointment a : appointments.values()) {
            if (a.getDoctorId().equals(doctorId)
                && a.getAppointmentDate().equals(today)
                && (a.getStatus() == AppointmentStatus.WAITING
                || a.getStatus() == AppointmentStatus.LATE)) {
                result.add(a);
            }
        }
        result.sort(Appointment::compareTo);
        return result;
    }

    public List<Appointment> getMissedAppointments() {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : appointments.values()) {
            if (a.getStatus() == AppointmentStatus.MISSED) {
                result.add(a);
            }
        }
        return result;
    }
}
