<template>
    <div class="orders-container">
        <!-- <h2 class="page-title">我的订单</h2> -->

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
            <div class="loading-animation">
                <el-skeleton :rows="3" animated />
            </div>
            <p class="loading-tip">正在加载您的订单信息...</p>
        </div>

        <!-- 错误信息 -->
        <div v-else-if="error" class="error-message">
            <p>{{ error }}</p>
            <el-button type="primary" @click="loadOrders">重新加载</el-button>
        </div>

        <!-- 订单为空时 -->
        <div v-else-if="orders.length === 0" class="empty-orders">
            <el-empty description="暂无订单" />
        </div>

        <!-- 订单列表 -->
        <div v-else class="orders-list">
            <div v-for="order in orders" :key="order.orderId" class="order-card">
                <!-- 订单头部信息 -->
                <div class="order-header">
                    <div class="order-info">
                        <span class="order-date">{{ formatDate(order.createTime) }}</span>
                        <span class="order-number">订单号: {{ order.orderId }}</span>
                    </div>
                    <div class="order-status" :class="getStatusClass(order.status)">
                        {{ getStatusText(order.status) }}
                    </div>
                </div>

                <!-- 订单商品信息 -->
                <div class="order-content">
                    <div v-if="order.loading" class="loading-items">
                        <el-skeleton :rows="1" animated />
                    </div>
                    <div v-else-if="order.items && order.items.length > 0" class="order-items">
                        <!-- 修改商品图片区域，为每个商品添加标题 -->
                        <div class="items-preview">
                            <!-- 显示所有商品图片，最多显示4个 -->
                            <div class="item-thumbnails">
                                <div v-for="(item, index) in order.items.slice(0, 4)" :key="index"
                                    class="item-thumbnail-container"
                                    @click="item.productStatus !== 'INACTIVE' ? navigateToBookDetail(item.productId) : $event.stopPropagation()">
                                    <div class="item-thumbnail">
                                        <img v-if="item.cover" :src="item.cover" alt="商品图片">
                                        <img v-else src="http://47.111.155.70/oss/default-cover.jpg"
                                            alt="商品图片" />
                                        <div v-if="item.productStatus === 'INACTIVE'" class="sold-out-overlay">
                                            <div class="sold-out-text">
                                                <div>下架</div>
                                                <div>sold out</div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="item-title">
                                        {{ item.name || item.title }}
                                    </div>
                                </div>

                                <div v-if="order.items.length > 4" class="more-items-container">
                                    <div class="more-items-badge">
                                        +{{ order.items.length - 4 }}
                                    </div>
                                    <div class="more-items-text">
                                        还有更多商品
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- 订单价格信息 -->
                        <div class="order-price">
                            <span class="price-value">¥{{ order.totalAmount.toFixed(2) }}</span>
                            <span class="item-count">共{{ getTotalQuantity(order.items) }}件</span>
                        </div>
                    </div>
                </div>

                <!-- 订单操作按钮 -->
                <div class="order-actions">
                    <!-- 所有状态都有删除按钮 -->
                    <el-button v-if="order.status !== 'PENDING'" size="small"
                        @click="confirmDeleteOrder(order.orderId)">
                        删除订单
                    </el-button>

                    <!-- PENDING状态特有按钮 -->
                    <template v-if="order.status === 'PENDING'">
                        <el-button size="small" @click="confirmCancelOrder(order.orderId)">
                            取消订单
                        </el-button>
                        <el-button type="primary" size="small" @click="goToPayment(order.orderId)">
                            立即支付
                        </el-button>
                    </template>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { getOrders, getCartItemsById, deleteOrder, cancelOrder } from '../api/order';
import { type CartItem } from '../api/cart';

const router = useRouter();
const orders = ref<any[]>([]);
const loading = ref(false);
const error = ref('');

// 加载所有订单
const loadOrders = async () => {
    loading.value = true;
    error.value = '';

    try {
        const response = await getOrders();

        if (response.data && response.data.code === '200') {
            orders.value = response.data.data || [];

            // 获取每个订单的商品信息
            for (const order of orders.value) {
                order.loading = true;
                await fetchOrderItems(order);
                order.loading = false;
            }
            
            // 按orderId从大到小排序（最新的订单在前面）
            orders.value.sort((a, b) => b.orderId - a.orderId);

        } else {
            error.value = response.data?.msg || '获取订单失败';
        }
    } catch (err) {
        console.error('加载订单时出错:', err);
        error.value = '加载订单时出错，请稍后再试';
    } finally {
        loading.value = false;
    }
};

// 获取订单的商品信息
const fetchOrderItems = async (order: any) => {
    try {
        const response = await getCartItemsById(order.orderId);

        if (response.data && response.data.code === '200') {
            let items = response.data.data || [];
            items.sort((a: CartItem, b: CartItem) => a.productId - b.productId); // 按产品ID排序
            order.items = items;
        } else {
            order.items = [];
            console.warn(`获取订单 ${order.orderId} 的商品信息失败`);
        }
    } catch (err) {
        order.items = [];
        console.error(`获取订单 ${order.orderId} 的商品信息时出错:`, err);
    }
};

// 计算订单中商品总数量
const getTotalQuantity = (items: CartItem[]) => {
    return items.reduce((sum, item) => sum + (item.quantity || 1), 0);
};

// 格式化日期
const formatDate = (dateString: string) => {
    if (!dateString) return '';

    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        }).replace(/\//g, '-');
    } catch (e) {
        return dateString;
    }
};

// 根据状态获取显示文本
const getStatusText = (status: string) => {
    switch (status) {
        case 'SUCCESS':
            return '已支付';
        case 'PENDING':
            return '待支付';
        case 'EXPIRED':
            return '已失效';
        default:
            return status;
    }
};

// 根据状态获取样式类
const getStatusClass = (status: string) => {
    switch (status) {
        case 'SUCCESS':
            return 'status-success';
        case 'PENDING':
            return 'status-pending';
        case 'EXPIRED':
            return 'status-expired';
        default:
            return '';
    }
};

// 删除订单确认
const confirmDeleteOrder = (orderId: number | string) => {
    ElMessageBox.confirm(
        '确定要删除此订单吗？删除后无法恢复。',
        '删除订单',
        {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning'
        }
    ).then(() => {
        handleDeleteOrder(orderId);
    }).catch(() => {
        // 取消删除，不做任何操作
    });
};

// 处理删除订单
const handleDeleteOrder = async (orderId: number | string) => {
    try {
        const loading = ElLoading.service({
            lock: true,
            text: '删除中...',
            background: 'rgba(255, 255, 255, 0.7)'
        });

        const response = await deleteOrder(orderId);

        loading.close();

        if (response.data && response.data.code === '200') {
            ElMessage.success('订单已成功删除');
            // 从列表中移除已删除的订单
            orders.value = orders.value.filter(order => order.orderId !== orderId);
        } else {
            ElMessage.error(response.data?.msg || '删除订单失败');
        }
    } catch (err) {
        console.error('删除订单时出错:', err);
        ElMessage.error('删除订单时出错，请稍后再试');
    }
};

// 取消订单确认
const confirmCancelOrder = (orderId: number | string) => {
    ElMessageBox.confirm(
        '确定要取消此订单吗？',
        '取消订单',
        {
            confirmButtonText: '确定取消',
            cancelButtonText: '不取消',
            type: 'warning'
        }
    ).then(() => {
        handleCancelOrder(orderId);
    }).catch(() => {
        // 用户选择不取消，不做任何操作
    });
};

// 处理取消订单
const handleCancelOrder = async (orderId: number | string) => {
    try {
        const loading = ElLoading.service({
            lock: true,
            text: '取消中...',
            background: 'rgba(255, 255, 255, 0.7)'
        });

        const response = await cancelOrder(orderId);

        loading.close();

        if (response.data && response.data.code === '200') {
            ElMessage.success('订单已成功取消');
            // 重新加载订单列表
            await loadOrders();
        } else {
            ElMessage.error(response.data?.msg || '取消订单失败');
        }
    } catch (err) {
        console.error('取消订单时出错:', err);
        ElMessage.error('取消订单时出错，请稍后再试');
    }
};

// 跳转到支付页面
const goToPayment = (orderId: number | string) => {
    // Base64编码订单ID
    const encodedOrderId = btoa(orderId.toString());

    // 导航到支付页面，并传递编码后的订单ID
    router.push({
        path: '/payment',
        query: {
            order_id: encodedOrderId
        }
    });
};

// 跳转到书籍详情页面前检查书籍是否存在
const navigateToBookDetail = (productId: number | string) => {
    router.push(`/BookDetail/${productId}`);
};

// 组件加载时获取订单数据
onMounted(() => {
    loadOrders();
});
</script>

<style scoped>
.orders-container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 20px;
    min-height: calc(100vh - 200px);
    /* 确保最小高度，为footer留出空间 */
}

.loading-container,
.error-message,
.empty-orders {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 30px 0;
}

.error-message {
    color: #f56c6c;
    text-align: center;
}

.orders-list {
    /* margin-top: -40px; */
    display: flex;
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
    /* 添加：让订单卡片左对齐 */
    justify-content: flex-start;
    /* 添加：从顶部开始排列 */
}

.order-card {
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    overflow: hidden;
    width: 800px;
    max-width: 100%;
    /* 添加：响应式支持 */
}

.order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background-color: #f8f8f8;
    border-bottom: 1px solid #eee;
}

.order-info {
    display: flex;
    gap: 20px;
}

.order-date {
    color: #606266;
    font-size: 14px;
}

.order-number {
    color: #606266;
    font-size: 14px;
}

.order-status {
    font-size: 14px;
    font-weight: 500;
}

.status-success {
    color: #67c23a;
}

.status-pending {
    color: #e6a23c;
}

.status-expired {
    color: #909399;
}

.order-content {
    padding: 16px 20px;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.loading-items {
    width: 100%;
}

.loading-animation {
    width: 100%;
    max-width: 600px;
    margin: 0 auto;
}

.loading-tip {
    margin-top: 15px;
    color: #909399;
    font-size: 14px;
}

.order-items {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
}

.items-preview {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

/* 调整缩略图列表布局 */
.item-thumbnails {
    display: flex;
    flex-wrap: wrap;
    /* 允许商品换行显示 */
    gap: 6px;
}

/* 商品图片容器样式，包含图片和标题 */
.item-thumbnail-container {
    display: flex;
    flex-direction: column;
    width: 100px;
    /* 增加宽度 */
    margin-right: 10px;
    margin-bottom: -5px;
}

/* 图片容器相对定位，为蒙板提供参考点 */
.item-thumbnail {
    width: 100px;
    height: 100px;
    border-radius: 4px;
    overflow: hidden;
    background-color: #f5f5f5;
    border: 1px solid #eaeaea;
    position: relative;
    /* 添加相对定位 */
}

/* 下架商品蒙板样式 */
.sold-out-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.6);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 5;
}

/* 下架文本样式 */
.sold-out-text {
    text-align: center;
    color: white;
    font-weight: bold;
    font-size: 14px;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.sold-out-text div:nth-child(2) {
    font-size: 12px;
    margin-top: 2px;
    font-weight: normal;
}

.item-thumbnail img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

/* 添加商品标题样式 */
.item-title {
    font-size: 12px;
    color: #333;
    margin-top: 2px;
    width: 100px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: center;
}

/* 添加"更多商品"提示的样式 */
.more-items-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100px;
    height: 100px;
    margin-right: 10px;
    cursor: pointer;
}

.more-items-badge {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 50px;
    height: 50px;
    background-color: rgba(0, 0, 0, 0.1);
    border-radius: 50%;
    font-size: 16px;
    font-weight: bold;
    color: #606266;
    margin-bottom: 8px;
}

.more-items-text {
    font-size: 12px;
    color: #909399;
    text-align: center;
}

.multi-items-hint {
    font-size: 12px;
    color: #909399;
}

.order-price {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
}

.price-value {
    font-size: 18px;
    font-weight: 600;
    color: #e74c3c;
}

.item-count {
    font-size: 12px;
    color: #909399;
}

.order-actions {
    padding: 12px 16px;
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    border-top: 1px solid #eee;
}

/* 响应式调整 */
@media (max-width: 768px) {
    .order-info {
        flex-direction: column;
        gap: 4px;
    }

    .item-thumbnails {
        flex-wrap: wrap;
    }

    .item-thumbnail {
        width: 60px;
        height: 60px;
    }

    .single-item-title {
        max-width: 200px;
    }
}
</style>