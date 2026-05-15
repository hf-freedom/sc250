package com.hospital.triage.controller;

import com.hospital.triage.dto.AppointmentRequest;
import com.hospital.triage.dto.Result;
import com.hospital.triage.model.Appointment;
import com.hospital.triage.model.Department;
import com.hospital.triage.model.Doctor;
import com.hospital.triage.service.TriageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/triage")
@CrossOrigin(origins = "*")
public class TriageController {
    @Autowired
    private TriageService triageService;

    @PostMapping("/appointment")
    public Result<Appointment> createAppointment(@RequestBody AppointmentRequest request) {
        try {
            Appointment appointment = triageService.createAppointment(request);
            return Result.success("预约成功", appointment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/appointment/{id}/cancel")
    public Result<Appointment> cancelAppointment(@PathVariable String id) {
        try {
            Appointment appointment = triageService.cancelAppointment(id);
            return Result.success("取消成功", appointment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/appointment/{id}/checkin")
    public Result<Appointment> checkIn(@PathVariable String id) {
        try {
            Appointment appointment = triageService.checkIn(id);
            return Result.success("签到成功", appointment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/appointments")
    public Result<List<Appointment>> getAllAppointments() {
        return Result.success(triageService.getAllAppointments());
    }

    @GetMapping("/appointment/{id}")
    public Result<Appointment> getAppointmentById(@PathVariable String id) {
        Appointment appointment = triageService.getAppointmentById(id);
        if (appointment == null) {
            return Result.error("预约不存在");
        }
        return Result.success(appointment);
    }

    @GetMapping("/queue/{doctorId}")
    public Result<List<Appointment>> getQueueByDoctor(@PathVariable String doctorId) {
        return Result.success(triageService.getQueueByDoctor(doctorId));
    }

    @PostMapping("/queue/{doctorId}/next")
    public Result<Appointment> callNext(@PathVariable String doctorId) {
        Appointment next = triageService.callNext(doctorId);
        if (next == null) {
            return Result.error("当前没有等待的患者");
        }
        return Result.success("呼叫下一位患者", next);
    }

    @PostMapping("/appointment/{id}/complete")
    public Result<Appointment> completeAppointment(@PathVariable String id) {
        try {
            Appointment appointment = triageService.completeAppointment(id);
            return Result.success("完成诊疗", appointment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/appointment/{id}/mark-late")
    public Result<Appointment> markLate(@PathVariable String id) {
        try {
            Appointment appointment = triageService.markLate(id);
            return Result.success("已标记为迟到", appointment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/appointment/{id}/mark-missed")
    public Result<Appointment> markMissed(@PathVariable String id) {
        try {
            Appointment appointment = triageService.markMissed(id);
            return Result.success("已标记为过号", appointment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/doctor/{doctorId}/unavailable")
    public Result<Void> handleDoctorUnavailable(
            @PathVariable String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            triageService.handleDoctorUnavailable(doctorId, date);
            return Result.success("医生已设置为停诊状态", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/doctor/{doctorId}/available")
    public Result<Void> handleDoctorAvailable(@PathVariable String doctorId) {
        try {
            triageService.handleDoctorAvailable(doctorId);
            return Result.success("医生已恢复出诊", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/doctor/{doctorId}/transfer")
    public Result<String> transferPatients(
            @PathVariable String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            int count = triageService.transferPatients(doctorId, date);
            return Result.success("成功转移 " + count + " 名患者", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/departments")
    public Result<List<Department>> getAllDepartments() {
        return Result.success(triageService.getAllDepartments());
    }

    @GetMapping("/doctors/department/{departmentId}")
    public Result<List<Doctor>> getDoctorsByDepartment(@PathVariable String departmentId) {
        return Result.success(triageService.getDoctorsByDepartment(departmentId));
    }

    @GetMapping("/missed")
    public Result<List<Appointment>> getMissedAppointments() {
        return Result.success(triageService.getMissedAppointments());
    }

    @PostMapping("/refresh")
    public Result<Void> refreshQueues() {
        triageService.refreshQueues();
        return Result.success("队列已刷新", null);
    }
    
    @PostMapping("/recalculate")
    public Result<Void> recalculateQueues() {
        triageService.recalculateAllQueues();
        return Result.success("已重新计算所有队列的等待时间", null);
    }
    
    @PostMapping("/recalculate/{doctorId}")
    public Result<Void> recalculateDoctorQueue(@PathVariable String doctorId) {
        triageService.recalculateDoctorQueue(doctorId);
        return Result.success("已重新计算该医生队列的等待时间", null);
    }
    
    @PostMapping("/appointment/{id}/requeue")
    public Result<Appointment> requeuePatient(@PathVariable String id) {
        try {
            Appointment appointment = triageService.requeuePatient(id);
            return Result.success("患者已重新排队", appointment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
