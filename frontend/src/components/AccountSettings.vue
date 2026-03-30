<template>
    <div class="account-settings">
        <el-form :model="userForm" ref="formRef" label-width="120px" class="settings-form">
            <!-- 头像上传区域 -->
            <div class="avatar-container">
                <el-avatar :size="180" :src="avatarPreview">
                    <img src="http://47.111.155.70/oss/R-C.png" alt="默认头像" />
                </el-avatar>
                <el-upload class="avatar-uploader" action="#" :auto-upload="false" :show-file-list="false"
                    :on-change="handleAvatarChange" :before-upload="beforeAvatarUpload">
                    <el-button type="primary" size="default" class="upload-button">
                        更换头像
                    </el-button>
                </el-upload>
            </div>

            <!-- 基本信息 -->
            <el-form-item label="用户名" prop="username">
                <el-input v-model="userForm.username" disabled placeholder="用户名不可修改" maxlength="50"
                    show-word-limit></el-input>
            </el-form-item>

            <el-form-item label="真实姓名" prop="name">
                <el-input v-model="userForm.name" placeholder="请输入您的昵称" maxlength="50" show-word-limit></el-input>
            </el-form-item>

            <el-form-item label="手机号码" prop="telephone" :error="telephoneError">
                <el-input v-model="userForm.telephone" placeholder="请输入您的手机号码"></el-input>
            </el-form-item>

            <el-form-item label="电子邮箱" prop="email" :error="emailError">
                <el-input v-model="userForm.email" placeholder="请输入您的电子邮箱" maxlength="100"></el-input>
            </el-form-item>

            <el-form-item label="收货地址" prop="location">
                <el-input v-model="userForm.location" placeholder="请输入您的收货地址" maxlength="255"></el-input>
            </el-form-item>

            <!-- 修改密码区域 -->
            <div class="password-section">
                <h3>修改密码</h3>
                <el-form-item label="原密码" prop="oldPassword" :error="oldPasswordError">
                    <el-input v-model="userForm.oldPassword" type="password" placeholder="请输入原密码" show-password
                        @blur="handleOldPasswordBlur" maxlength="100"></el-input>
                </el-form-item>

                <el-form-item label="新密码" prop="newPassword" :error="newPasswordError">
                    <el-input v-model="userForm.newPassword" type="password" placeholder="请输入新密码" show-password
                        maxlength="100"></el-input>
                </el-form-item>

                <el-form-item label="确认新密码" prop="confirmPassword" :error="confirmPasswordError">
                    <el-input v-model="userForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password
                        maxlength="100"></el-input>
                </el-form-item>
            </div>

            <!-- 操作按钮 -->
            <el-form-item class="last-button">
                <el-button type="primary" @click="saveChanges" :loading="loading"
                    :disabled="!formValid || !formChanged">保存</el-button>
                <el-button @click="resetForm" :disabled="!formChanged">重置</el-button>
            </el-form-item>
        </el-form>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { uploadImage } from '../api/image'
import { userInfoUpdate, checkPassword } from '../api/user'

// 表单引用
const formRef = ref<FormInstance>()

// 用户表单数据
const userForm = reactive({
    username: '',
    name: '',
    telephone: '',
    email: '',
    location: '',
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

// 添加原始表单数据变量用于比较
const originalForm = reactive({
    username: '',
    name: '',
    telephone: '',
    email: '',
    location: ''
})

// 头像相关
const avatar = ref()
const avatarPreview = ref('')
const loading = ref()
// const password = sessionStorage.getItem('password')

const oldPasswordError = ref('')
const confirmPasswordError = ref('')
const telephoneError = ref('')
const emailError = ref('')
const newPasswordError = ref('')
const newPasswordErrorTemp = ref('')

// 修改 watch 函数，添加"请先输入原密码"的逻辑
watch(() => [userForm.oldPassword, userForm.newPassword, userForm.confirmPassword],
    ([oldPassword, newPassword, confirmPassword]) => {
        // 两次密码不一致
        if (confirmPassword && newPassword !== confirmPassword) {
            confirmPasswordError.value = '两次输入的密码不一致'
        } else {
            confirmPasswordError.value = ''
        }

        // 添加新逻辑：检查是否有新密码输入但没有原密码
        if ((newPassword || confirmPassword) && !oldPassword) {
            oldPasswordError.value = '请先输入原密码'
        } else if (oldPasswordError.value === '请先输入原密码' && (oldPassword || !confirmPassword && !newPassword)) {
            // 如果用户后来输入了原密码，清除此错误提示
            oldPasswordError.value = ''
        }
    }
)

// 添加专门的watch监听newPassword的变化
watch(() => userForm.newPassword, (newVal) => {
    // 只在原密码已验证通过的情况下进行检查
    if (newPasswordErrorTemp.value === 'validated') {
        if (!newVal) {
            // 如果新密码为空
            newPasswordError.value = '新密码不能为空';
        } else if (newVal === userForm.oldPassword) {
            // 如果新密码与原密码相同
            newPasswordError.value = '新密码不能与原密码相同';
        } else {
            // 如果以上两种错误情况都不存在，清除这两种特定的错误信息
            if (newPasswordError.value === '新密码不能为空' ||
                newPasswordError.value === '新密码不能与原密码相同') {
                newPasswordError.value = '';
            }
        }
    } else {
        // 如果原密码尚未验证通过，且用户已开始输入新密码，检查是否存在原密码
        if (newVal && !userForm.oldPassword) {
            oldPasswordError.value = '请先输入原密码';
        }
    }
});

// 添加单独的watch监听oldPassword的变化
watch(() => userForm.oldPassword, (newVal, oldVal) => {
    // 当原密码内容发生变化时，清除验证相关的错误信息
    if (newVal !== oldVal) {
        // 只清除API验证产生的错误，保留"请先输入原密码"类型的错误
        if (oldPasswordError.value === '原密码不正确' ||
            oldPasswordError.value === '密码验证失败，请稍后重试' ||
            oldPasswordError.value === '验证中...') {
            oldPasswordError.value = ''
        }

        // 重置新密码为空的错误标记，等待下次验证
        newPasswordErrorTemp.value = ''
    }
    if (!userForm.oldPassword || oldPasswordError.value === '') {
        if (newPasswordError.value === '新密码不能为空') {
            newPasswordError.value = ''
        } 
    }
})

// 处理旧密码失焦事件
const handleOldPasswordBlur = async () => {
    // 只有当有输入内容时才验证
    if (userForm.oldPassword) {
        try {
            // 显示验证中状态
            oldPasswordError.value = '验证中...'

            // 调用API验证旧密码
            const response = await checkPassword({
                username: userForm.username,
                password: userForm.oldPassword
            })

            // 根据API返回的布尔值设置错误信息
            if (response.data.data === true) {
                // 密码正确
                oldPasswordError.value = ''

                // 设置验证通过标记，确保后续新密码为空时能正确提示
                newPasswordErrorTemp.value = 'validated'

                // 密码验证成功且新密码为空，显示提示
                if (!userForm.newPassword) {
                    newPasswordError.value = '新密码不能为空'
                } else if (userForm.newPassword === userForm.oldPassword) {
                    // 验证通过但新旧密码相同
                    newPasswordError.value = '新密码不能与原密码相同'
                }
            } else {
                // 密码错误
                oldPasswordError.value = '原密码不正确'
                newPasswordErrorTemp.value = '' // 密码错误时清除标记
                newPasswordError.value = '' // 清除新密码错误，避免同时显示两个错误
            }
        } catch (error) {
            console.error('验证密码失败', error)
            oldPasswordError.value = '密码验证失败，请稍后重试'
        }
    }
}

const chinaMobileRegex = /^1(3[0-9]|4[579]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[189])\d{8}$/
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

watch(() => userForm.telephone, (val) => {
    if (val && !chinaMobileRegex.test(val)) {
        telephoneError.value = '手机号格式不正确'
    } else {
        telephoneError.value = ''
    }
})

watch(() => userForm.email, (val) => {
    if (val && !emailRegex.test(val)) {
        emailError.value = '邮箱格式不正确'
    } else {
        emailError.value = ''
    }
})

const formValid = computed(() => {
    // 检查是否存在任何错误信息
    if (telephoneError.value || emailError.value || oldPasswordError.value || confirmPasswordError.value || newPasswordError.value) {
        return false
    }

    // 检查必填字段
    if (!userForm.name) {
        return false
    }

    // 检查如果用户正在修改密码，确保两个密码字段都正确
    if (userForm.oldPassword || userForm.newPassword || userForm.confirmPassword) {
        if (!userForm.oldPassword || !userForm.newPassword || !userForm.confirmPassword) {
            return false
        }
        if (userForm.newPassword !== userForm.confirmPassword) {
            return false
        }
        if (oldPasswordError.value) {
            return false
        }
    }

    // 如果修改了电话，确保格式正确
    if (userForm.telephone && !chinaMobileRegex.test(userForm.telephone)) {
        return false
    }

    // 如果修改了邮箱，确保格式正确
    if (userForm.email && !emailRegex.test(userForm.email)) {
        return false
    }

    return true
})

// 头像上传前的验证
const beforeAvatarUpload = (file: File) => {
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
    return true
}

// 处理头像变更
const handleAvatarChange = (file: any) => {
    if (!file) return

    const reader = new FileReader()
    reader.onload = (e) => {
        avatarPreview.value = e.target?.result as string
        avatar.value = file.raw
    }
    reader.readAsDataURL(file.raw)
}

// 检测表单是否有变化
const formChanged = computed(() => {
    // 检查基本信息是否有变化
    if (userForm.name !== originalForm.name ||
        userForm.telephone !== originalForm.telephone ||
        userForm.email !== originalForm.email ||
        userForm.location !== originalForm.location) {
        return true
    }

    // 检查头像是否有变化
    if (avatar.value) {
        return true
    }

    // 检查是否正在修改密码
    if (userForm.oldPassword || userForm.newPassword || userForm.confirmPassword) {
        return true
    }

    // 没有变化
    return false
})

// 保存修改
const saveChanges = async () => {
    if (!formRef.value) return

    await formRef.value.validate(async (valid) => {
        if (valid) {
            loading.value = true

            try {
                // 上传头像（如果有新头像）
                let avatarUrl = ''
                if (avatar.value) {
                    const imageResponse = await uploadImage(avatar.value)
                    if (imageResponse.data.code === '200') {
                        avatarUrl = imageResponse.data.data
                    } else {
                        ElMessage.error('头像上传失败')
                        loading.value = false
                        return
                    }
                }

                // 构建更新用户信息的数据对象
                const userData: { username: string; name?: string; telephone?: string; role?: string; email?: string; location?: string; avatar?: string; password?: string } = {
                    username: userForm.username,
                    name: userForm.name,
                    role: sessionStorage.getItem('role') || '',
                    telephone: userForm.telephone,
                    email: userForm.email,
                    location: userForm.location,
                    avatar: avatarUrl || avatarPreview.value
                }

                // 如果输入了密码字段，将密码信息一并更新
                if (userForm.oldPassword && userForm.newPassword) {
                    userData['password'] = userForm.newPassword
                }

                // 调用统一的updateUserInfo API更新所有信息
                const response = await userInfoUpdate(userData)

                if (response.data.code === '200') {
                    // 更新成功后清空密码字段
                    userForm.oldPassword = ''
                    userForm.newPassword = ''
                    userForm.confirmPassword = ''

                    // 更新sessionStorage中的用户信息
                    if (userData.name) sessionStorage.setItem('name', userData.name)
                    if (avatarUrl) sessionStorage.setItem('avatar', avatarUrl)
                    if (userData.telephone) sessionStorage.setItem('telephone', userData.telephone)
                    if (userData.email) sessionStorage.setItem('email', userData.email)
                    if (userData.location) sessionStorage.setItem('location', userData.location)

                    // 更新原始表单数据，使其与当前表单保持一致
                    originalForm.name = userForm.name
                    originalForm.telephone = userForm.telephone
                    originalForm.email = userForm.email
                    originalForm.location = userForm.location

                    // 清除头像修改状态
                    avatar.value = null

                    const event = new CustomEvent('avatar-updated', {
                        detail: { url: avatarUrl || avatarPreview.value }
                    })
                    window.dispatchEvent(event)

                    ElMessage.success('个人信息更新成功')
                } else {
                    ElMessage.error(response.data.msg || '更新失败')
                }
            } catch (error) {
                console.error('更新用户信息失败', error)
                ElMessage.error('更新失败，请稍后重试')
            } finally {
                loading.value = false
            }
        }
    })
}

// 重置表单
const resetForm = () => {
    if (!formRef.value) return
    formRef.value.resetFields()

    // 重新获取用户信息
    loadUserInfo()
}

// 加载用户信息
const loadUserInfo = () => {
    // 从sessionStorage获取基本信息
    const username = sessionStorage.getItem('username') || ''
    const name = sessionStorage.getItem('name') || ''
    const telephone = sessionStorage.getItem('telephone') || ''
    const email = sessionStorage.getItem('email') || ''
    const location = sessionStorage.getItem('location') || ''

    // 更新当前表单
    userForm.username = username
    userForm.name = name
    userForm.telephone = telephone
    userForm.email = email
    userForm.location = location

    // 同时更新原始数据，用于检测变化
    originalForm.username = username
    originalForm.name = name
    originalForm.telephone = telephone
    originalForm.email = email
    originalForm.location = location

    avatarPreview.value = sessionStorage.getItem('avatar') || ''

    avatarPreview.value = sessionStorage.getItem('avatar') || ''
    // 清空密码字段
    userForm.oldPassword = ''
    userForm.newPassword = ''
    userForm.confirmPassword = ''
}

onMounted(() => {
    loadUserInfo()
})
</script>

<style scoped>
.account-settings {
    /* padding: 20px; */
    width: 100%;
    /* 添加100%宽度占满父容器 */
}

.settings-form {
    width: 70%;
    /* 从固定600px改为100%填充 */
    max-width: 1200px;
    /* 设置最大宽度防止在特大屏幕上过度拉伸 */
    margin: 0 auto;
}

.avatar-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 30px;
}

.avatar-uploader {
    margin-top: 15px;
}

.upload-button {
    margin-top: 10px;
}

.password-section {
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
}

.password-section h3 {
    margin-top: 10px;
    margin-bottom: 30px;
    font-size: 18px;
    font-weight: 500;
    color: #333;
}

/* 调整表单内容的宽度 */
/* :deep(.el-form-item__content) {
    flex: 1;
    max-width: 100%;
} */

/* 调整表单项目的布局 */
:deep(.el-form-item) {
    margin-left: -30px;
    margin-right: 30px;
    /* 确保标签和输入框垂直居中对齐 */
}

:deep(.el-form-item__label) {
    line-height: 45px;
    /* 与输入框高度匹配 */
    padding-bottom: 0;
    text-align: right;
    /* padding-right: 12px; */
}

:deep(.el-input) {
    height: 45px;
}

.last-button {
    margin-top: 70px;
    margin-left: 380px;
    margin-right: -30px;
}

</style>