package com.hospital.triage.service;

import com.hospital.triage.dto.AppointmentRequest;
import com.hospital.triage.enums.AppointmentStatus;
import com.hospital.triage.enums.PatientPriority;
import com.hospital.triage.model.Appointment;
import com.hospital.triage.model.Department;
import com.hospital.triage.model.Doctor;
import com.hospital.triage.model.Patient;
import com.hospital.triage.storage.DataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TriageService {
    @Autowired
    private DataStorage dataStorage;

    @Value("${triage.late-threshold-minutes:15}")
    private int lateThresholdMinutes;

    @Value("${triage.consultation-minutes:15}")
    private int consultationMinutes;

    public Appointment createAppointment(AppointmentRequest request) {
        String patientId = findOrCreatePatient(request);
        
        if (dataStorage.isDuplicateAppointment(patientId, request.getDepartmentId(), request.getAppointmentDate())) {
            throw new RuntimeException("同一天不能重复预约同一科室");
        }

        String doctorId = request.getDoctorId();
        if (doctorId == null || doctorId.isEmpty()) {
            doctorId = findAvailableDoctor(request.getDepartmentId(), request.getAppointmentDate());
        }

        Doctor doctor = dataStorage.doctors.get(doctorId);
        if (doctor == null) {
            throw new RuntimeException("医生不存在");
        }

        if (!doctor.isAvailable()) {
            String alternativeDoctorId = findAlternativeDoctor(doctor);
            if (alternativeDoctorId != null) {
                doctorId = alternativeDoctorId;
                doctor = dataStorage.doctors.get(doctorId);
            } else {
                throw new RuntimeException("该医生今日停诊，暂无其他医生可用");
            }
        }

        int currentCount = dataStorage.getTodayAppointmentCount(doctorId, request.getAppointmentDate());
        if (currentCount >= doctor.getMaxDailyPatients()) {
            throw new RuntimeException("该医生今日号源已满");
        }

        Department dept = dataStorage.departments.get(request.getDepartmentId());
        
        Patient patient = dataStorage.patients.get(patientId);
        PatientPriority priority = request.isEmergency() ? PatientPriority.EMERGENCY : PatientPriority.NORMAL;
        patient.setPriority(priority);
        
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID().toString());
        appointment.setPatientId(patientId);
        appointment.setPatientName(request.getPatientName());
        appointment.setDoctorId(doctorId);
        appointment.setDoctorName(doctor.getName());
        appointment.setDepartmentId(request.getDepartmentId());
        appointment.setDepartmentName(dept.getName());
        appointment.setSymptoms(request.getSymptoms());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setPriority(priority);
        appointment.setStatus(AppointmentStatus.WAITING);
        appointment.setCreatedAt(LocalDateTime.now());
        
        String queueNumber = dataStorage.generateQueueNumber(doctorId, request.getAppointmentDate());
        appointment.setQueueNumber(queueNumber);
        
        calculateExpectedTime(appointment);
        
        dataStorage.appointments.put(appointment.getId(), appointment);
        dataStorage.recordPatientDepartment(patientId, request.getDepartmentId(), request.getAppointmentDate());
        
        updateQueuePositions(doctorId, request.getAppointmentDate());
        
        return appointment;
    }

    private String findOrCreatePatient(AppointmentRequest request) {
        for (Patient patient : dataStorage.patients.values()) {
            if (patient.getIdCard().equals(request.getIdCard())) {
                return patient.getId();
            }
        }
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        patient.setName(request.getPatientName());
        patient.setIdCard(request.getIdCard());
        patient.setPhone(request.getPhone());
        patient.setAge(request.getAge());
        patient.setGender(request.getGender());
        patient.setPriority(PatientPriority.NORMAL);
        dataStorage.patients.put(patient.getId(), patient);
        return patient.getId();
    }

    private String findAvailableDoctor(String departmentId, LocalDate date) {
        for (Doctor doctor : dataStorage.doctors.values()) {
            if (doctor.getDepartmentId().equals(departmentId)
                && doctor.isAvailable()
                && doctor.getWorkingDays().contains(date.getDayOfWeek())) {
                int count = dataStorage.getTodayAppointmentCount(doctor.getId(), date);
                if (count < doctor.getMaxDailyPatients()) {
                    return doctor.getId();
                }
            }
        }
        throw new RuntimeException("该科室今日无可用医生");
    }

    private String findAlternativeDoctor(Doctor unavailableDoctor) {
        for (Doctor doctor : dataStorage.doctors.values()) {
            if (doctor.getDepartmentId().equals(unavailableDoctor.getDepartmentId())
                && doctor.isAvailable()
                && !doctor.getId().equals(unavailableDoctor.getId())) {
                return doctor.getId();
            }
        }
        return null;
    }

    private void calculateExpectedTime(Appointment appointment) {
        List<Appointment> appointments = dataStorage.getDoctorAppointments(
            appointment.getDoctorId(), appointment.getAppointmentDate());
        
        appointments.sort(Appointment::compareTo);
        
        int position = 0;
        LocalTime startTime = LocalTime.of(8, 0);
        
        for (int i = 0; i < appointments.size(); i++) {
            Appointment a = appointments.get(i);
            if (a.getId().equals(appointment.getId())) {
                position = i;
                break;
            }
        }
        
        appointment.setQueuePosition(position + 1);
        appointment.setExpectedTime(startTime.plusMinutes((long) position * consultationMinutes));
        appointment.setEstimatedWaitMinutes(position * consultationMinutes);
    }

    private void updateQueuePositions(String doctorId, LocalDate date) {
        List<Appointment> appointments = dataStorage.getDoctorAppointments(doctorId, date);
        
        appointments.sort(Appointment::compareTo);
        
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime now = LocalTime.now();
        
        int waitingIndex = 0;
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.WAITING || a.getStatus() == AppointmentStatus.LATE) {
                a.setQueuePosition(waitingIndex + 1);
                a.setExpectedTime(startTime.plusMinutes((long) waitingIndex * consultationMinutes));
                
                int minutesUntilStart = (int) java.time.Duration.between(now, a.getExpectedTime()).toMinutes();
                a.setEstimatedWaitMinutes(Math.max(0, minutesUntilStart));
                waitingIndex++;
            }
        }
    }

    public Appointment cancelAppointment(String appointmentId) {
        Appointment appointment = dataStorage.appointments.get(appointmentId);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        
        appointment.setStatus(AppointmentStatus.CANCELLED);
        dataStorage.removePatientDepartmentRecord(
            appointment.getPatientId(),
            appointment.getDepartmentId(),
            appointment.getAppointmentDate()
        );
        
        updateQueuePositions(appointment.getDoctorId(), appointment.getAppointmentDate());
        
        return appointment;
    }

    public Appointment checkIn(String appointmentId) {
        Appointment appointment = dataStorage.appointments.get(appointmentId);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        
        LocalTime expectedTime = appointment.getExpectedTime();
        LocalTime now = LocalTime.now();
        
        if (now.isAfter(expectedTime.plusMinutes(lateThresholdMinutes))) {
            appointment.setStatus(AppointmentStatus.LATE);
            appointment.setPriority(PatientPriority.LATE);
        } else {
            appointment.setStatus(AppointmentStatus.CHECKED_IN);
        }
        
        appointment.setCheckInTime(LocalDateTime.now());
        
        updateQueuePositions(appointment.getDoctorId(), appointment.getAppointmentDate());
        
        return appointment;
    }
    
    public Appointment markLate(String appointmentId) {
        Appointment appointment = dataStorage.appointments.get(appointmentId);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        
        appointment.setStatus(AppointmentStatus.LATE);
        appointment.setPriority(PatientPriority.LATE);
        
        updateQueuePositions(appointment.getDoctorId(), appointment.getAppointmentDate());
        
        return appointment;
    }
    
    public Appointment markMissed(String appointmentId) {
        Appointment appointment = dataStorage.appointments.get(appointmentId);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        
        appointment.setStatus(AppointmentStatus.MISSED);
        appointment.setPriority(PatientPriority.LATE);
        
        dataStorage.removePatientDepartmentRecord(
            appointment.getPatientId(),
            appointment.getDepartmentId(),
            appointment.getAppointmentDate()
        );
        
        updateQueuePositions(appointment.getDoctorId(), appointment.getAppointmentDate());
        
        return appointment;
    }

    public void handleDoctorUnavailable(String doctorId, LocalDate date) {
        Doctor doctor = dataStorage.doctors.get(doctorId);
        if (doctor == null) {
            throw new RuntimeException("医生不存在");
        }
        
        doctor.setAvailable(false);
        doctor.setUnavailableDate(date);
    }
    
    public void handleDoctorAvailable(String doctorId) {
        Doctor doctor = dataStorage.doctors.get(doctorId);
        if (doctor == null) {
            throw new RuntimeException("医生不存在");
        }
        
        doctor.setAvailable(true);
        doctor.setUnavailableDate(null);
    }
    
    public int transferPatients(String fromDoctorId, LocalDate date) {
        Doctor fromDoctor = dataStorage.doctors.get(fromDoctorId);
        if (fromDoctor == null) {
            throw new RuntimeException("医生不存在");
        }
        
        String alternativeDoctorId = findAlternativeDoctor(fromDoctor);
        if (alternativeDoctorId == null) {
            throw new RuntimeException("该科室暂无其他可用医生");
        }
        
        Doctor toDoctor = dataStorage.doctors.get(alternativeDoctorId);
        List<Appointment> appointments = dataStorage.getDoctorAppointments(fromDoctorId, date);
        int transferredCount = 0;
        
        for (Appointment appointment : appointments) {
            if (appointment.getStatus() == AppointmentStatus.WAITING || 
                appointment.getStatus() == AppointmentStatus.LATE ||
                appointment.getStatus() == AppointmentStatus.CHECKED_IN) {
                
                appointment.setDoctorId(alternativeDoctorId);
                appointment.setDoctorName(toDoctor.getName());
                
                String newQueueNumber = dataStorage.generateQueueNumber(alternativeDoctorId, date);
                appointment.setQueueNumber(newQueueNumber);
                
                transferredCount++;
            }
        }
        
        if (transferredCount > 0) {
            updateQueuePositions(alternativeDoctorId, date);
        }
        
        return transferredCount;
    }

    public void refreshQueues() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        for (Appointment appointment : dataStorage.appointments.values()) {
            if (appointment.getAppointmentDate().equals(today)
                && appointment.getStatus() == AppointmentStatus.WAITING) {
                if (appointment.getExpectedTime() != null
                    && now.isAfter(appointment.getExpectedTime().plusMinutes(lateThresholdMinutes * 2))) {
                    appointment.setStatus(AppointmentStatus.MISSED);
                    dataStorage.removePatientDepartmentRecord(
                        appointment.getPatientId(),
                        appointment.getDepartmentId(),
                        today
                    );
                }
            }
        }
    }

    public List<Appointment> getQueueByDoctor(String doctorId) {
        return dataStorage.getWaitingAppointments(doctorId);
    }

    public List<Appointment> getMissedAppointments() {
        return dataStorage.getMissedAppointments();
    }

    public List<Department> getAllDepartments() {
        return new ArrayList<>(dataStorage.departments.values());
    }

    public List<Doctor> getDoctorsByDepartment(String departmentId) {
        List<Doctor> result = new ArrayList<>();
        for (Doctor doctor : dataStorage.doctors.values()) {
            if (doctor.getDepartmentId().equals(departmentId)) {
                result.add(doctor);
            }
        }
        return result;
    }

    public Appointment getAppointmentById(String id) {
        return dataStorage.appointments.get(id);
    }

    public List<Appointment> getAllAppointments() {
        return new ArrayList<>(dataStorage.appointments.values());
    }

    public Appointment callNext(String doctorId) {
        List<Appointment> queue = dataStorage.getWaitingAppointments(doctorId);
        for (Appointment appointment : queue) {
            if (appointment.getStatus() == AppointmentStatus.CHECKED_IN
                || appointment.getStatus() == AppointmentStatus.WAITING) {
                appointment.setStatus(AppointmentStatus.IN_CONSULTATION);
                updateQueuePositions(doctorId, LocalDate.now());
                return appointment;
            }
        }
        return null;
    }

    public Appointment completeAppointment(String appointmentId) {
        Appointment appointment = dataStorage.appointments.get(appointmentId);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        updateQueuePositions(appointment.getDoctorId(), LocalDate.now());
        return appointment;
    }
    
    public void recalculateAllQueues() {
        LocalDate today = LocalDate.now();
        for (Doctor doctor : dataStorage.doctors.values()) {
            updateQueuePositions(doctor.getId(), today);
        }
    }
    
    public void recalculateDoctorQueue(String doctorId) {
        updateQueuePositions(doctorId, LocalDate.now());
    }
    
    public Appointment requeuePatient(String appointmentId) {
        Appointment appointment = dataStorage.appointments.get(appointmentId);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        
        if (appointment.getStatus() != AppointmentStatus.MISSED) {
            throw new RuntimeException("只有过号患者可以重新排队");
        }
        
        appointment.setStatus(AppointmentStatus.LATE);
        appointment.setPriority(PatientPriority.LATE);
        appointment.setCheckInTime(LocalDateTime.now());
        
        dataStorage.recordPatientDepartmentRecord(
            appointment.getPatientId(),
            appointment.getDepartmentId(),
            appointment.getAppointmentDate()
        );
        
        updateQueuePositions(appointment.getDoctorId(), appointment.getAppointmentDate());
        
        return appointment;
    }
}
