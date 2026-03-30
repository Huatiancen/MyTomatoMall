<template>
    <Header />
    <BackgroundLayout2 />
    <div class="dashboard-container">
        <el-container class="main-container">
            <!-- 侧边栏导航 -->
            <el-aside width="300px" class="sidebar">
                <div class="user-info">
                    <el-avatar :size="100" :src="userAvatar">
                        <img src="http://47.111.155.70/oss/R-C.png" alt="默认头像" />
                    </el-avatar>
                    <h3>{{ userName }}</h3>
                </div>

                <el-menu :default-active="activeMenu" class="dashboard-menu" @select="handleMenuSelect">
                    <el-menu-item index="account">
                        <el-icon>
                            <UserFilled />
                        </el-icon>
                        <span>账号设置</span>
                    </el-menu-item>

                    <el-menu-item index="orders">
                        <el-icon>
                            <Tickets />
                        </el-icon>
                        <span>我的订单</span>
                    </el-menu-item>

                    <el-menu-item index="cart">
                        <el-icon>
                            <ShoppingCart />
                        </el-icon>
                        <span>我的购物车</span>
                    </el-menu-item>

                    <el-menu-item index="favorites">
                        <el-icon>
                            <Star />
                        </el-icon>
                        <span>我的收藏</span>
                    </el-menu-item>

                    <el-menu-item index="addresses">
                        <el-icon>
                            <Location />
                        </el-icon>
                        <span>地址管理</span>
                    </el-menu-item>
                </el-menu>
            </el-aside>

            <!-- 主体内容区域 -->
            <el-main class="content-area">
                <div class="content-header">
                    <h2>{{ contentTitle }}</h2>
                </div>

                <!-- 账号设置 -->
                <div v-if="activeMenu === 'account'" class="content-section">
                    <AccountSettings />
                </div>

                <!-- 我的订单 -->
                <div v-else-if="activeMenu === 'orders'" class="content-section">
                    <Orders class="container" />
                </div>

                <!-- 我的购物车 -->
                <div v-else-if="activeMenu === 'cart'" class="content-section">
                    <Cart class="container" />
                </div>

                <!-- 我的收藏 -->
                <div v-else-if="activeMenu === 'favorites'" class="content-section">
                    <Favourites class="container" />
                </div>

                <div v-else-if="activeMenu === 'addresses'" class="content-section">
                    <UserAddresses />
                </div>
            </el-main>
        </el-container>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { UserFilled, Tickets, ShoppingCart, Star, Location } from '@element-plus/icons-vue'

// 引入子组件（需要创建这些组件）
// 这里假设这些组件已经存在或将会创建
// import AccountSettings from '../../components/AccountSettings.vue'
// import Header from '../../components/Header.vue'
// import UserOrders from './components/UserOrders.vue'
// import UserCart from './components/UserCart.vue'
// import UserFavorites from './components/UserFavorites.vue'

// 用户信息
const userName = ref('')
const userAvatar = ref('')

// 当前选中的菜单项
const activeMenu = ref('account')

// 根据当前菜单设置内容标题
const contentTitle = computed(() => {
    switch (activeMenu.value) {
        case 'account':
            return '账号设置'
        case 'orders':
            return '我的订单'
        case 'cart':
            return '我的购物车'
        case 'favorites':
            return '我的收藏'
        case 'addresses':
            return '地址管理'
        default:
            return '个人中心'
    }
})

// 处理菜单选择
function handleMenuSelect(index: string) {
    activeMenu.value = index
}

// 页面加载时获取用户信息
onMounted(() => {
    // 从会话存储中获取用户信息
    userName.value = sessionStorage.getItem('username') || ''
    userAvatar.value = sessionStorage.getItem('avatar') || ''

    // 获取URL中的参数，可以通过URL直接跳转到特定页面
    const params = new URLSearchParams(window.location.search)
    const section = params.get('section')
    if (section && ['account', 'orders', 'cart', 'favorites', 'addresses'].includes(section)) {
        activeMenu.value = section
    }

    // 监听头像更新事件
    const handleAvatarUpdate = (e: CustomEvent) => {
        userAvatar.value = e.detail.url
    }

    window.addEventListener('avatar-updated', handleAvatarUpdate as EventListener)

    onUnmounted(() => {
        window.removeEventListener('avatar-updated', handleAvatarUpdate as EventListener)
    })
})
</script>

<style scoped>
.dashboard-container {
    min-height: 100vh;
    background-color: #f5f7fa;
    display: flex;
    flex-direction: column;
    align-items: center;
}

.main-container {
    width: 1300px;
    /* 设置最大宽度 */
    max-width: 1500px;
    margin: 20px auto;
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    overflow: hidden;
    display: flex;
}

.sidebar {
    /* width: 300px; */
    background-color: white;
    border-right: 1px solid #e6e6e6;
    min-height: calc(100vh - 180px);
    flex-shrink: 0;
}

.user-info {
    margin-top: 30px;
    padding: 30px 0;
    text-align: center;
    border-bottom: 1px solid #e6e6e6;
}

.user-info h3 {
    margin-top: 20px;
    font-size: 18px;
    color: #333;
}

.dashboard-menu {
    border-right: none;
}

.content-area {
    /* padding: 25px 30px; */
    flex-grow: 1;
    /* width: 1000px; */
    max-width: 1200px;
    /* 设置主体区域最小宽度 */
    display: flex;
    flex-direction: column;
    margin: 0 auto;
    /* 居中主体区域 */
}

.content-header {
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid #e6e6e6;
    width: 100%;
}

.content-header h2 {
    font-size: 22px;
    color: #333;
    font-weight: 500;
    display: flex;
    flex-direction: column;
    align-content: center;
}

.content-section {
    background-color: white;
    border-radius: 4px;
    width: 100%;
    flex-grow: 1;
    /* display: flex; */
    /* flex-direction: column; */
    /* align-content: center; */
}

.container {
    /* margin-top: 80px; */
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-start;
    /* height: 80vh; */
    position: relative;
}
</style>
