<template>
  <BackgroundLayout>
    <div class="login-wrapper">
      <div class="card promo-section">
        <img src="http://47.111.155.70/oss/advertisement.png" alt="广告图" class="ad-image-full" />
      </div>

      <div class="card register-card">
        <div class="register-header">
          <el-icon class="back-button" @click="handleBack">
            <ArrowLeft />
          </el-icon>
          <h2>{{ step === 1 ? '用户注册' : '完善信息' }}</h2>
        </div>

        <!-- 第一步：必填项 -->
        <template v-if="step === 1">
          <el-form class="register-form">
            <el-form-item>
              <el-input v-model="username" placeholder="请输入用户名(无法修改)" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-input v-model="name" placeholder="请输入真实姓名" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-input v-model="password" type="password" placeholder="请输入密码" show-password maxlength="100" />
            </el-form-item>
            <el-form-item :error="passwordError">
              <el-input v-model="confirmPassword" type="password" placeholder="请再次输入密码" show-password maxlength="100" />
            </el-form-item>
            <el-form-item>
              <el-button :disabled="registerDisabled" type="primary" class="register-button" @click="step = 2">
                下一步
              </el-button>
            </el-form-item>
          </el-form>
        </template>

        <!-- 第二步：选填项 -->
        <template v-else>
          <!-- 上传头像区域 -->
          <div class="avatar-container">
            <el-upload class="avatar-uploader" action="#" :show-file-list="false" :auto-upload="false"
              :on-change="handleAvatarChange" :before-upload="beforeAvatarUpload">
              <div class="avatar-upload-box">
                <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview" />
                <div v-else class="avatar-placeholder">
                  <el-icon>
                    <Plus />
                  </el-icon>
                </div>
              </div>
            </el-upload>
            <p class="avatar-tip">请上传用户头像（选做）</p>
            <el-icon v-if="avatarPreview" class="avatar-remove-icon" @click="removeAvatar">
              <Close />
            </el-icon>
          </div>

          <!-- 表单部分 -->
          <el-form class="register-form">
            <el-form-item :error="telephoneError">
              <el-input v-model="telephone" placeholder="请输入手机号（选填）" :prefix-icon="Iphone" />
            </el-form-item>
            <el-form-item :error="emailError">
              <el-input v-model="email" placeholder="请输入邮箱（选填）" :prefix-icon="Message" maxlength="100" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="location" placeholder="请输入地址（选填）" maxlength="255" />
            </el-form-item>
          </el-form>

          <el-button type="primary" class="register-button step2-button" @click="handleRegister">
            {{ isOneOptionalFilled ? '注册' : '跳过并注册' }}
          </el-button>
        </template>

        <div class="register-footer">
          <p>© 2025 蕃茄读书 - 您的专属阅读伙伴</p>
        </div>
      </div>
    </div>
  </BackgroundLayout>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { userRegister } from '../../api/user'
import BackgroundLayout from '../../components/BackgroundLayout.vue'
import { ArrowLeft, Plus, Close, Iphone, Message } from '@element-plus/icons-vue'
import { uploadImage } from '../../api/image'

const router = useRouter()

const step = ref(1)

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const name = ref('')
const avatar = ref()
const avatarPreview = ref('')
const telephone = ref('')
const email = ref('')
const location = ref('')

const passwordError = ref('')
const telephoneError = ref('')
const emailError = ref('')

watch([password, confirmPassword], () => {
  if (confirmPassword.value && password.value !== confirmPassword.value) {
    passwordError.value = '两次输入的密码不一致'
  } else {
    passwordError.value = ''
  }
})

const chinaMobileRegex = /^1(3[0-9]|4[579]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[189])\d{8}$/
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

watch(telephone, (val) => {
  if (val && !chinaMobileRegex.test(val)) {
    telephoneError.value = '手机号格式不正确'
  } else {
    telephoneError.value = ''
  }
})

watch(email, (val) => {
  if (val && !emailRegex.test(val)) {
    emailError.value = '邮箱格式不正确'
  } else {
    emailError.value = ''
  }
})

const registerDisabled = computed(() => {
  return !username.value || !password.value || !confirmPassword.value || !name.value ||
      password.value !== confirmPassword.value
})

const isOneOptionalFilled = computed(() => {
  return telephone.value || email.value || location.value || avatar.value
})

function handleAvatarChange(file: any) {
  const reader = new FileReader()
  reader.onload = e => {
    avatarPreview.value = e.target?.result as string
    avatar.value = file.raw
  }
  reader.readAsDataURL(file.raw)
}

function beforeAvatarUpload(file: File) {
  // 检查文件类型
  const isImage = file.type.startsWith('image/')
  const isLt1M = file.size / 1024 / 1024 < 1

  if (!isImage) {
    ElMessage.error('上传头像只能是图片格式!')
    return false
  }
  if (!isLt1M) {
    ElMessage.error('上传头像大小不能超过 1MB!')
    return false
  }
  return true // 允许上传
}

function removeAvatar() {
  avatarPreview.value = ''
  avatar.value = ''
}

async function handleRegister() {
  try {
    // 如果用户上传了头像，先上传图片
    let avatarUrl = ''
    if (avatar.value && avatar.value instanceof File) {
      const loading = ElMessage({
        message: '头像上传中...',
        type: 'info',
        duration: 0
      })

      try {
        // 调用图片上传API
        const response = await uploadImage(avatar.value)
        loading.close()

        if (response.data && response.data.code === '200') {
          avatarUrl = response.data.data
          avatarUrl = avatarUrl.replace("https://se2-project.oss-cn-hangzhou-internal.aliyuncs.com", "http://47.111.155.70/oss")
          // console.log('修改后的头像URL:', avatarUrl)
        } else {
          ElMessage.error(response.data?.msg || '头像上传失败')
          return
        }
      } catch (error) {
        loading.close()
        ElMessage.error('网络错误，头像上传失败')
        console.error('头像上传错误:', error)
        return
      }
    }

    // 进行用户注册
    const registerResponse = await userRegister({
      username: username.value,
      password: password.value,
      name: name.value,
      avatar: avatarUrl, // 使用上传后的URL
      role: 'CUSTOMER',
      telephone: telephone.value,
      email: email.value,
      location: location.value
    })

    if (registerResponse.data.code === '200') {
      ElMessage({ message: '注册成功！请登录账号', type: 'success', center: true })
      router.replace({ path: '/login' })
    } else {
      ElMessage({ message: registerResponse.data.msg || '注册失败', type: 'error', center: true })
    }
  } catch (error) {
    ElMessage({ message: '网络错误，请稍后重试', type: 'error', center: true })
  }
}

function handleBack() {
  if (step.value === 2) {
    step.value = 1
  } else {
    router.push({ path: '/login' })
  }
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
  border-radius: 16px;
  overflow: hidden;
}

.register-card {
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

.register-header {
  position: absolute;
  top: 24px;
  left: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-button {
  cursor: pointer;
  font-size: 20px;
  color: #409EFF;
}

.avatar-container {
  position: relative;
  width: 180px;
  height: auto;
  /* margin: 100px auto 20px; */
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-upload-box {
  margin-top: 20px;
  width: 180px;
  height: 180px;
  border: 2px dashed #dcdfe6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-color: #f9f9f9;
}

.avatar-placeholder {
  color: #999;
  font-size: 32px;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-tip {
  text-align: center;
  font-size: 14px;
  color: #999;
  margin-top: 10px;
}

.avatar-remove-icon {
  position: absolute;
  top: 0;
  right: 0;
  width: 24px;
  height: 24px;
  background: #fff;
  border-radius: 50%;
  color: #f56c6c;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.register-footer {
  text-align: center;
  font-size: 12px;
  color: #999;
  position: absolute;
  bottom: 40px;
}

.register-button {
  font-size: 18px;
  padding: 12px 32px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 48px auto 0;
  width: 200px;
  height: 45px;
}

.step2-button {
  margin-top: 10px;
  /* 更小的上边距 */
}

.register-form {
  margin-top: 10px;
  width: 90%;
  margin-left: auto;
  margin-right: auto;
}

.register-form :deep(.el-input__inner) {
  height: 40px;
  line-height: 45px;
}

</style>
