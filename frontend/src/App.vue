<template>
  <div id="app">
    <el-container style="height: 100vh">
      <el-header style="background: #409EFF; color: white; font-size: 24px; font-weight: bold; display: flex; align-items: center;">
        🏥 医院预约分诊系统
      </el-header>
      <el-container>
        <el-aside width="200px" style="background: #f5f5f5; padding: 20px 0;">
          <el-menu
            :default-active="activeMenu"
            @select="handleMenuSelect"
            background-color="#f5f5f5"
            text-color="#333"
            active-text-color="#409EFF"
          >
            <el-menu-item index="appointment">📋 预约挂号</el-menu-item>
            <el-menu-item index="queue">📊 叫号队列</el-menu-item>
            <el-menu-item index="doctor">👨‍⚕️ 医生管理</el-menu-item>
            <el-menu-item index="history">📜 预约记录</el-menu-item>
          </el-menu>
        </el-aside>
        <el-main style="padding: 20px; background: #fff;">
          <div v-if="activeMenu === 'appointment'">
            <h2>📋 预约挂号</h2>
            <el-form :model="appointmentForm" label-width="100px" style="max-width: 500px;">
              <el-form-item label="患者姓名">
                <el-input v-model="appointmentForm.patientName" />
              </el-form-item>
              <el-form-item label="身份证号">
                <el-input v-model="appointmentForm.idCard" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="appointmentForm.phone" />
              </el-form-item>
              <el-form-item label="年龄">
                <el-input-number v-model="appointmentForm.age" :min="0" :max="150" />
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="appointmentForm.gender">
                  <el-radio label="男">男</el-radio>
                  <el-radio label="女">女</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="选择科室">
                <el-select v-model="appointmentForm.departmentId" placeholder="请选择科室" @change="onDeptChange" style="width: 100%;">
                  <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="选择医生">
                <el-select v-model="appointmentForm.doctorId" placeholder="请选择医生" style="width: 100%;">
                  <el-option v-for="doc in doctors" :key="doc.id" :label="doc.name + ' - ' + doc.title" :value="doc.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="预约日期">
                <el-date-picker v-model="appointmentForm.appointmentDate" type="date" placeholder="选择日期" style="width: 100%;" />
              </el-form-item>
              <el-form-item label="症状描述">
                <el-input type="textarea" v-model="appointmentForm.symptoms" rows="3" />
              </el-form-item>
              <el-form-item label="是否急诊">
                <el-switch v-model="appointmentForm.isEmergency" />
                <span style="color: red; margin-left: 10px;" v-if="appointmentForm.isEmergency">急诊患者可优先就诊</span>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitAppointment">提交预约</el-button>
                <el-button @click="resetForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-if="activeMenu === 'queue'">
            <h2>📊 叫号队列管理</h2>
            <el-select v-model="selectedDoctor" placeholder="选择医生查看队列" @change="loadQueue" style="width: 300px; margin-bottom: 20px;">
              <el-option v-for="doc in allDoctors" :key="doc.id" :label="doc.name + ' - ' + doc.departmentName" :value="doc.id" />
            </el-select>
            <el-button type="primary" @click="callNextPatient" style="margin-left: 20px;">呼叫下一位</el-button>
            <el-button type="success" @click="recalculateQueue">重新计算等待时间</el-button>
            <el-button @click="loadQueue">刷新队列</el-button>
            
            <el-alert
              title="急诊优先规则"
              type="warning"
              :closable="false"
              style="margin: 15px 0;"
              show-icon>
              <template #default>
                急诊患者自动插队到队列最前面，会影响后续普通患者的预计等待时间。点击「重新计算等待时间」可更新所有患者的排队位置和预计候诊时间。
              </template>
            </el-alert>
            
            <el-row :gutter="20">
              <el-col :span="16">
                <h3 style="margin-bottom: 10px;">📋 当前候诊队列</h3>
                <el-table :data="currentQueue" border height="400">
                  <el-table-column prop="queueNumber" label="候诊号" width="100" />
                  <el-table-column prop="patientName" label="患者姓名" width="100" />
                  <el-table-column prop="priority" label="优先级" width="80">
                    <template #default="{ row }">
                      <el-tag size="small" :type="row.priority === 'EMERGENCY' ? 'danger' : row.priority === 'LATE' ? 'warning' : 'info'">
                        {{ row.priority === 'EMERGENCY' ? '急诊' : row.priority === 'LATE' ? '迟到' : '普通' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="queuePosition" label="位置" width="60" />
                  <el-table-column prop="estimatedWaitMinutes" label="等待(分)" width="80" />
                  <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                      <el-tag size="small" :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="260">
                    <template #default="{ row }">
                      <el-button size="small" type="success" @click="doCheckIn(row.id)" v-if="row.status === 'WAITING' || row.status === 'LATE'">签到</el-button>
                      <el-button size="small" type="warning" @click="doMarkLate(row.id)" v-if="row.status === 'WAITING'">迟到</el-button>
                      <el-button size="small" type="danger" @click="doMarkMissed(row.id)" v-if="row.status === 'WAITING' || row.status === 'LATE'">过号</el-button>
                      <el-button size="small" type="primary" @click="doComplete(row.id)" v-if="row.status === 'IN_CONSULTATION'">完成</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-col>
              
              <el-col :span="8">
                <h3 style="margin-bottom: 10px;">⚠️ 过号列表</h3>
                <el-table :data="missedAppointments" border height="400" empty-text="暂无过号患者">
                  <el-table-column prop="queueNumber" label="候诊号" width="100" />
                  <el-table-column prop="patientName" label="患者姓名" width="100" />
                  <el-table-column prop="doctorName" label="医生" width="100" />
                  <el-table-column prop="priority" label="优先级" width="80">
                    <template #default="{ row }">
                      <el-tag size="small" :type="row.priority === 'EMERGENCY' ? 'danger' : 'warning'">
                        {{ row.priority === 'EMERGENCY' ? '急诊' : '普通' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="120">
                    <template #default="{ row }">
                      <el-button size="small" type="success" @click="requeuePatient(row.id)">重新排队</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-col>
            </el-row>
          </div>

          <div v-if="activeMenu === 'doctor'">
            <h2>👨‍⚕️ 医生管理</h2>
            <el-table :data="allDoctors" border>
              <el-table-column prop="name" label="医生姓名" width="120" />
              <el-table-column prop="departmentName" label="所属科室" width="120" />
              <el-table-column prop="title" label="职称" width="120" />
              <el-table-column prop="maxDailyPatients" label="每日最大号源" width="150" />
              <el-table-column prop="isAvailable" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.isAvailable ? 'success' : 'danger'">{{ row.isAvailable ? '出诊中' : '停诊' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="待就诊患者" width="130">
                <template #default="{ row }">
                  <el-tag type="info">{{ getDoctorWaitingCount(row.id) }} 人</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280">
                <template #default="{ row }">
                  <el-button size="small" type="danger" @click="setUnavailable(row.id)" v-if="row.isAvailable">设置停诊</el-button>
                  <el-button size="small" type="success" @click="setAvailable(row.id)" v-if="!row.isAvailable">恢复出诊</el-button>
                  <el-button size="small" type="warning" @click="transferPatients(row.id)" v-if="!row.isAvailable && getDoctorWaitingCount(row.id) > 0">转移患者</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="activeMenu === 'history'">
            <h2>📜 预约记录</h2>
            <el-button type="primary" @click="loadAppointments" style="margin-bottom: 20px;">刷新记录</el-button>
            <el-table :data="allAppointments" border>
              <el-table-column prop="queueNumber" label="候诊号" width="120" />
              <el-table-column prop="patientName" label="患者姓名" width="120" />
              <el-table-column prop="doctorName" label="医生姓名" width="120" />
              <el-table-column prop="departmentName" label="科室" width="120" />
              <el-table-column prop="appointmentDate" label="预约日期" width="120" />
              <el-table-column prop="priority" label="优先级" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.priority === 'EMERGENCY' ? 'danger' : row.priority === 'LATE' ? 'warning' : 'info'">
                    {{ row.priority === 'EMERGENCY' ? '急诊' : row.priority === 'LATE' ? '迟到' : '普通' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280">
                <template #default="{ row }">
                  <el-button size="small" type="success" @click="doCheckIn(row.id)" v-if="row.status === 'WAITING' || row.status === 'LATE'">签到</el-button>
                  <el-button size="small" type="warning" @click="doMarkLate(row.id)" v-if="row.status === 'WAITING'">标记迟到</el-button>
                  <el-button size="small" type="danger" @click="doMarkMissed(row.id)" v-if="row.status === 'WAITING' || row.status === 'LATE'">过号处理</el-button>
                  <el-button size="small" type="info" @click="doCancel(row.id)" v-if="row.status !== 'COMPLETED' && row.status !== 'CANCELLED' && row.status !== 'MISSED'">取消</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from './api/triage.js'

const activeMenu = ref('appointment')
const departments = ref([])
const doctors = ref([])
const allDoctors = ref([])
const currentQueue = ref([])
const allAppointments = ref([])
const missedAppointments = ref([])
const selectedDoctor = ref('')

const appointmentForm = ref({
  patientName: '',
  idCard: '',
  phone: '',
  age: 30,
  gender: '男',
  departmentId: '',
  doctorId: '',
  symptoms: '',
  appointmentDate: new Date(),
  isEmergency: false
})

const handleMenuSelect = (index) => {
  activeMenu.value = index
  if (index === 'queue') loadQueue()
  if (index === 'history') loadAppointments()
  if (index === 'doctor') loadAllDoctors()
}

const loadDepartments = async () => {
  const res = await api.getDepartments()
  departments.value = res.data.data
}

const onDeptChange = async (deptId) => {
  const res = await api.getDoctors(deptId)
  doctors.value = res.data.data
}

const loadAllDoctors = async () => {
  const depts = await api.getDepartments()
  let allDocs = []
  for (const dept of depts.data.data) {
    const res = await api.getDoctors(dept.id)
    allDocs = allDocs.concat(res.data.data)
  }
  allDoctors.value = allDocs
}

const submitAppointment = async () => {
  try {
    const res = await api.createAppointment(appointmentForm.value)
    ElMessage.success(res.data.message)
    resetForm()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '预约失败')
  }
}

const resetForm = () => {
  appointmentForm.value = {
    patientName: '',
    idCard: '',
    phone: '',
    age: 30,
    gender: '男',
    departmentId: '',
    doctorId: '',
    symptoms: '',
    appointmentDate: new Date(),
    isEmergency: false
  }
  doctors.value = []
}

const loadQueue = async () => {
  if (!selectedDoctor.value) return
  const res = await api.getQueue(selectedDoctor.value)
  currentQueue.value = res.data.data
  loadMissedAppointments()
}

const loadMissedAppointments = async () => {
  const res = await api.getMissed()
  missedAppointments.value = res.data.data
}

const requeuePatient = async (id) => {
  try {
    const res = await api.requeuePatient(id)
    ElMessage.success(res.data.message)
    loadMissedAppointments()
    loadQueue()
    loadAppointments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const recalculateQueue = async () => {
  if (!selectedDoctor.value) {
    try {
      const res = await api.recalculateQueues()
      ElMessage.success(res.data.message)
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  } else {
    try {
      const res = await api.recalculateDoctorQueue(selectedDoctor.value)
      ElMessage.success(res.data.message)
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
  loadQueue()
  loadAppointments()
}

const callNextPatient = async () => {
  if (!selectedDoctor.value) {
    ElMessage.warning('请先选择医生')
    return
  }
  try {
    const res = await api.callNext(selectedDoctor.value)
    ElMessage.success(`正在呼叫: ${res.data.data.patientName} - ${res.data.data.queueNumber}`)
    loadQueue()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '呼叫失败')
  }
}

const doCheckIn = async (id) => {
  try {
    const res = await api.checkIn(id)
    ElMessage.success(res.data.message)
    loadQueue()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '签到失败')
  }
}

const doComplete = async (id) => {
  try {
    const res = await api.completeAppointment(id)
    ElMessage.success(res.data.message)
    loadQueue()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const doMarkLate = async (id) => {
  try {
    const res = await api.markLate(id)
    ElMessage.success(res.data.message)
    loadQueue()
    loadAppointments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const doMarkMissed = async (id) => {
  try {
    const res = await api.markMissed(id)
    ElMessage.success(res.data.message)
    loadQueue()
    loadMissedAppointments()
    loadAppointments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const doCancel = async (id) => {
  try {
    const res = await api.cancelAppointment(id)
    ElMessage.success(res.data.message)
    loadQueue()
    loadAppointments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '取消失败')
  }
}

const setUnavailable = async (doctorId) => {
  try {
    const today = new Date().toISOString().split('T')[0]
    const res = await api.setDoctorUnavailable(doctorId, today)
    ElMessage.success(res.data.message)
    loadAllDoctors()
    loadAppointments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '设置失败')
  }
}

const setAvailable = async (doctorId) => {
  try {
    const res = await api.setDoctorAvailable(doctorId)
    ElMessage.success(res.data.message)
    loadAllDoctors()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '设置失败')
  }
}

const transferPatients = async (doctorId) => {
  try {
    const today = new Date().toISOString().split('T')[0]
    const res = await api.transferPatients(doctorId, today)
    ElMessage.success(res.data.message)
    loadAllDoctors()
    loadAppointments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '转移失败')
  }
}

const getDoctorWaitingCount = (doctorId) => {
  return allAppointments.value.filter(a => 
    a.doctorId === doctorId && 
    (a.status === 'WAITING' || a.status === 'LATE' || a.status === 'CHECKED_IN')
  ).length
}

const loadAppointments = async () => {
  const res = await api.getAllAppointments()
  allAppointments.value = res.data.data
}

const getStatusType = (status) => {
  const map = {
    WAITING: 'info',
    CHECKED_IN: 'success',
    IN_CONSULTATION: 'warning',
    COMPLETED: 'success',
    CANCELLED: 'info',
    LATE: 'warning',
    MISSED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    WAITING: '等待中',
    CHECKED_IN: '已签到',
    IN_CONSULTATION: '诊疗中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    LATE: '迟到',
    MISSED: '过号'
  }
  return map[status] || status
}

onMounted(() => {
  loadDepartments()
  loadAllDoctors()
  loadAppointments()
  loadMissedAppointments()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
#app {
  font-family: 'Microsoft YaHei', sans-serif;
}
h2 {
  margin-bottom: 20px;
  color: #333;
  border-bottom: 2px solid #409EFF;
  padding-bottom: 10px;
}
</style>
