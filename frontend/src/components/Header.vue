<template>
    <header class="header-container">
        <div class="header-wrapper">
            <!-- 左侧logo和网站名称 -->
            <div class="header-left">
                <img src="http://47.111.155.70/oss/logo.png" alt="蕃茄读书" class="logo-image" />
            </div>

            <!-- 中间的导航菜单 -->
            <div class="header-center">
                <el-menu mode="horizontal" :router="true" :ellipsis="false" class="nav-menu">
                    <el-menu-item index="/home">首页</el-menu-item>
                    <el-menu-item index="/displayBoard">图书馆</el-menu-item>
                </el-menu>
            </div>

            <!-- 右侧的用户信息和地址 -->
            <div class="header-right">
                <div class="location-info">
                    <i class="el-icon-location"></i>
                    <span>送至：{{ deliveryAddress }}</span>
                </div>

                <div class="nav-links">
                    <router-link to="/cart" class="nav-link">
                        <el-icon>
                            <ShoppingCart />
                        </el-icon>
                        <i class="el-icon-shopping-cart-full"></i>
                        <span>购物车</span>
                    </router-link>
                    <router-link to="/orders" class="nav-link">
                        <el-icon>
                            <Tickets />
                        </el-icon>
                        <i class="el-icon-tickets"></i>
                        <span>我的订单</span>
                    </router-link>
                    <router-link to="/favourites" class="nav-link">
                        <el-icon>
                            <Star />
                        </el-icon>
                        <i class="el-icon-tickets"></i>
                        <span>我的收藏</span>
                    </router-link>
                </div>

                <el-dropdown trigger="click" @command="handleCommand">
                    <div class="user-avatar">
                        <el-avatar :size="40" :src="userAvatar">
                            <img src="http://47.111.155.70/oss/R-C.png" alt="默认头像" />
                        </el-avatar>
                    </div>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="dashboard">个人中心</el-dropdown-item>
                            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>
    </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Tickets, ShoppingCart, Star } from '@element-plus/icons-vue'

const router = useRouter()

const userAvatar = ref('')
const userAddress = ref('')

const deliveryAddress = computed(() => {
    return userAddress.value || '北京'
})

// 加载用户信息
onMounted(() => {
    userAvatar.value = sessionStorage.getItem('avatar') || ''
    userAddress.value = sessionStorage.getItem('address') || ''
    // 监听头像更新事件
    const handleAvatarUpdate = (e: CustomEvent) => {
        userAvatar.value = e.detail.url
    }

    window.addEventListener('avatar-updated', handleAvatarUpdate as EventListener)

    onUnmounted(() => {
        window.removeEventListener('avatar-updated', handleAvatarUpdate as EventListener)
    })
})

// 获取用户详细信息（包括头像和地址）
    

// 下拉菜单命令处理
function handleCommand(command: string) {
    // console.log(command)
    switch (command) {
        case 'dashboard':
            router.push('/dashboard')
            break
        case 'logout':
            logout()
            break
    }
}

// 退出登录
function logout() {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        // 清除会话存储的用户信息
        // sessionStorage.removeItem('token')
        // sessionStorage.removeItem('name')
        // sessionStorage.removeItem('address')
        // sessionStorage.removeItem('favourites')
        // sessionStorage.removeItem('cart')
        sessionStorage.clear()
        router.replace('/login')
        ElMessage({
            type: 'success',
            message: '已成功退出登录'
        })
    }).catch(() => {
        // 用户取消操作
    })
}
</script>

<style scoped>
.header-container {
    width: 100%;
    background-color: white;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    position: fixed;
    top: 0;
    left: 0;
    z-index: 1000;
}

.header-wrapper {
    max-width: 1280px;
    height: 64px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
}

.header-left {
    display: flex;
    align-items: center;
}

.logo-image {
    height: 40px;
    margin-right: 12px;
}

.header-center {
    flex: 1;
    display: flex;
  justify-content: flex-start; /* 改为靠左对齐，原来是center */
  margin-left: 20px;
}

.nav-menu {
    border-bottom: none;
}

:deep(.el-menu--horizontal > .el-menu-item) {
    height: 64px;
    line-height: 64px;
    font-size: 16px;
}

:deep(.el-menu--horizontal > .el-menu-item.is-active) {
    border-bottom: 2px solid #409EFF;
    color: #409EFF;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 24px;
}

.location-info {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    color: #606266;
}

.nav-links {
    display: flex;
    gap: 20px;
    margin-right: 5px;
}

.nav-link {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #606266;
    text-decoration: none;
    font-size: 14px;
    transition: color 0.3s;
}

.nav-link:hover {
    color: #409EFF;
}

.user-avatar {
    cursor: pointer;
}

.user-avatar:hover {
    opacity: 0.8;
}
</style>