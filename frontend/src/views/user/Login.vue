<template>
  <BackgroundLayout>
    <div class="login-wrapper">
      <!-- 广告卡片 -->
      <div class="card promo-section">
        <img src="http://47.111.155.70/oss/advertisement.png" alt="广告图" class="ad-image-full" />
      </div>

      <!-- 登录卡片 -->
      <div class="card login-card">
        <div class="login-header">
          <h2>用户登录</h2>
        </div>
        <el-form class="login-form">
          <el-form-item>
            <el-input v-model="username" placeholder="请输入用户名" prefix-icon="el-icon-user" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="password" type="password" placeholder="请输入密码" prefix-icon="el-icon-lock"
              @keyup.enter="handleLogin" show-password />
          </el-form-item>
        </el-form>
        <el-button :disabled="loginDisabled" type="primary" class="login-button" @click="handleLogin">登录</el-button>
        <el-button type="text" class="register-button" @click="goToRegister">注册新账号</el-button>
        <div class="login-footer">
          <p>© 2025 蕃茄读书 - 您的专属阅读伙伴</p>
        </div>
      </div>
    </div>
  </BackgroundLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { userLogin, userInfo } from '../../api/user'
import BackgroundLayout from '../../components/BackgroundLayout.vue'

const router = useRouter()
const username = ref('')
const password = ref('')

const loginDisabled = computed(() => !username.value || !password.value)

function handleLogin() {
  userLogin({ username: username.value, password: password.value }).then(res => {
    if (res.data.code === '200') {
      ElMessage({ message: '登录成功！', type: 'success', center: true })
      sessionStorage.setItem('token', res.data.data)
      userInfo(username.value).then(res => {
        sessionStorage.setItem('username', res.data.data.username)
        sessionStorage.setItem('avatar', res.data.data.avatar)
        sessionStorage.setItem('name', res.data.data.name)
        sessionStorage.setItem('telephone', res.data.data.telephone)
        sessionStorage.setItem('email', res.data.data.email)
        sessionStorage.setItem('location', res.data.data.location)
        sessionStorage.setItem('role', res.data.data.role)
        router.push({ path: '/home' })
      })
    } else {
      ElMessage({ message: res.data.msg || '登录失败', type: 'error', center: true })
      password.value = ''
    }
  }).catch(() => {
    ElMessage({ message: '网络错误，请稍后重试', type: 'error', center: true })
  })
}
// function handleLogin() {
//     // 模拟登录成功
//     ElMessage({ message: '登录成功！', type: 'success', center: true })
//
//     // 模拟设置会话存储中的用户数据
//     sessionStorage.setItem('token', 'mock-token')
//     sessionStorage.setItem('username', username.value)
//     sessionStorage.setItem('avatar', 'mock-avatar')
//     sessionStorage.setItem('name', 'Mock Name')
//     sessionStorage.setItem('telephone', '1234567890')
//     sessionStorage.setItem('email', 'mock@example.com')
//     sessionStorage.setItem('location', 'Mock Location')
//     sessionStorage.setItem('role', 'USER')
//
//     // 模拟登录成功后跳转到首页
//     router.push({ path: '/dashboard' })
// }


function goToRegister() {
  router.push({ path: '/register' })
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: stretch;
  gap: 40px;
  width: 90%;
  max-width: 1200px;
  height: 660px;
  margin-top: 80px;
}

.card {
  border-radius: 16px;
  padding: 40px;
  background-color: rgba(255, 255, 255, 0.95);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.promo-section {
  flex: 3;
  padding: 0;
  overflow: hidden;
}

.login-card {
  flex: 2;
  align-items: center;
  position: relative;
}

.ad-image-full {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.login-header {
  text-align: center;
  margin-bottom: 50px;
  position: absolute;
  top: 80px;
}

.login-form {
  margin-bottom: 20px;
  width: 90%;
}

.login-form :deep(.el-input__inner) {
  height: 40px;
  line-height: 45px;
}

.login-button {
  width: 50%;
  padding: 24px 0;
  margin-top: 40px;
  font-size: 20px;
  font-weight: bold;
  color: white;
  background-color: #409EFF;
  border: none;
  border-radius: 8px;
  transition: background-color 0.3s;
}

.login-button:hover {
  background-color: #337ecc;
}

.register-button {
  width: 50%;
  padding: 12px 0;
  font-size: 16px;
  margin-top: 12px;
  border: none;
  margin-left: -0.5px;
}

.login-footer {
  text-align: center;
  font-size: 12px;
  color: #999;
  position: absolute;
  bottom: 40px;
}
</style>