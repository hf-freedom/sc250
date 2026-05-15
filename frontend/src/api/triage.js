import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8003/api/triage',
  timeout: 10000
})

export const getDepartments = () => api.get('/departments')
export const getDoctors = (deptId) => api.get(`/doctors/department/${deptId}`)
export const createAppointment = (data) => api.post('/appointment', data)
export const cancelAppointment = (id) => api.post(`/appointment/${id}/cancel`)
export const checkIn = (id) => api.post(`/appointment/${id}/checkin`)
export const getAllAppointments = () => api.get('/appointments')
export const getQueue = (doctorId) => api.get(`/queue/${doctorId}`)
export const callNext = (doctorId) => api.post(`/queue/${doctorId}/next`)
export const completeAppointment = (id) => api.post(`/appointment/${id}/complete`)
export const setDoctorUnavailable = (doctorId, date) => 
  api.post(`/doctor/${doctorId}/unavailable?date=${date}`)
export const getMissed = () => api.get('/missed')
export const refreshQueues = () => api.post('/refresh')
export const setDoctorAvailable = (doctorId) => api.post(`/doctor/${doctorId}/available`)
export const transferPatients = (doctorId, date) => api.post(`/doctor/${doctorId}/transfer?date=${date}`)
export const markLate = (id) => api.post(`/appointment/${id}/mark-late`)
export const markMissed = (id) => api.post(`/appointment/${id}/mark-missed`)
export const recalculateQueues = () => api.post('/recalculate')
export const recalculateDoctorQueue = (doctorId) => api.post(`/recalculate/${doctorId}`)
export const requeuePatient = (id) => api.post(`/appointment/${id}/requeue`)

export default api
