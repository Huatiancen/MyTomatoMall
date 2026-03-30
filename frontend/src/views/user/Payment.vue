<template>
    <Header />
    <BackgroundLayout2 />
    <div class="container">
        <!-- 支付成功界面 -->
        <div v-if="paymentStatus === 'success'" class="payment-success">
            <div class="success-icon">✓</div>
            <h2>支付成功</h2>
            <p>您的订单已支付成功！我们将尽快为您发货</p>
            <div class="action-buttons">
                <button class="btn primary" @click="goToOrderList">查看我的订单</button>
                <button class="btn secondary" @click="goToMyHomePage">返回首页</button>
            </div>
        </div>

        <!-- 新的支付表单界面，与图片样式匹配 -->
        <div v-else-if="showFormData" class="payment-form">
            <!-- 商品信息区域 -->
            <div class="order-items" v-if="cartItems.length > 0">
                <div class="order-item" v-for="(item, index) in cartItems" :key="index">
                    <div class="item-image">
                        <img v-if="item.cover" :src="item.cover" alt="商品图片" />
                        <img v-else src="https://se-lab.oss-cn-nanjing.aliyuncs.com/default-cover.jpg" alt="商品图片" />
                    </div>
                    <div class="item-info">
                        <div class="item-name">{{ item.title }}</div>
                    </div>
                    <div class="item-price">
                        <div>
                            <span class="price">¥{{ item.price }}</span>
                        </div>
                        <div>
                            <span class="quantity">×{{ item.quantity }}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 应付款和合计放在同一行 -->
            <div class="payment-header">
                <h2 class="payment-title">应付款</h2>
                <div class="payment-summary">
                    <span class="total">合计 ¥{{ paymentData.totalAmount }}</span>
                    <i class="el-icon-arrow-right"></i>
                </div>
            </div>

            <div class="payment-info">
                <div class="info-row">
                    <div class="info-label">订单编号</div>
                    <div class="info-value">
                        {{ paymentData.orderId }}
                    </div>
                </div>

                <div class="info-row">
                    <div class="info-label">支付方式</div>
                    <div class="info-value">{{ paymentData.paymentMethod }}</div>
                </div>

                <div class="info-row">
                    <div class="info-label">下单时间</div>
                    <div class="info-value">{{ formatDate(paymentData.orderTime) }}</div>
                </div>

                <div class="info-row payment-countdown">
                    <div class="info-label">支付提示</div>
                    <div class="info-value countdown">
                        请在 <span class="time-remaining">{{ formatTimeDigit(remainingMinutes) }}:{{
                            formatTimeDigit(remainingSeconds) }}</span> 内完成支付，超时订单将自动取消
                    </div>
                </div>
            </div>

            <div class="action-buttons payment-actions">
                <button class="pay-btn" @click="goToPay">立即支付</button>
                <button class="cancel-btn" @click="goToOrderList">取消支付</button>
            </div>
        </div>

        <!-- 加载和错误状态 -->
        <div v-else>
            <h2>订单支付</h2>

            <div class="loading" v-if="loading">
                <div class="spinner"></div>
                <p>正在获取支付信息...</p>
            </div>

            <div class="error" v-if="errorMessage">
                {{ errorMessage }}
                <button class="retry-btn" @click="getPaymentForm" v-if="canRetry">重新获取支付信息</button>
            </div>
        </div>
    </div>
    <Footer />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElLoading } from 'element-plus';
import { useRouter, useRoute } from 'vue-router';
import { createPayment } from '../../api/order';
import { getCart } from '../../api/cart';
import { getOrderById, getCartItemsById } from '../../api/order'
import { type CartItem } from '../../api/cart';

// 路由和状态
const router = useRouter();
const route = useRoute();
const orderId = ref('');
const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const responseData = ref<Record<string, any> | null>(null);
const showFormData = ref(false);
const canRetry = ref(false);
const paymentStatus = ref('pending'); // 默认为pending状态

// 添加商品列表和折扣信息
const cartItems = ref<CartItem[]>([]);
const paymentData = ref({
    orderId: '',
    totalAmount: '',
    paymentMethod: '支付宝支付',
    orderTime: '', // 增加下单时间
});

// 解析表单的辅助函数
const parseFormElement = (htmlString: string) => {
    const parser = new DOMParser();
    const formDoc = parser.parseFromString(htmlString, 'text/html');
    const form = formDoc.querySelector('form');

    if (form) {
        return {
            action: form.getAttribute('action'),
            bizContent: (form.querySelector('input[name="biz_content"]') as HTMLInputElement)?.value
        };
    }

    return null;
};

// 表单和支付相关数据
const formAction = ref('');
const bizContent = ref('');

// 获取购物车商品信息
const fetchCartItems = async () => {
    if (!orderId.value) return;

    try {
        const response = await getCartItemsById(parseInt(orderId.value, 10));
        if (response && response.data.code === '200') {
            cartItems.value = response.data.data || [];
            // console.log('获取到的商品信息:', cartItems.value);
        } else {
            console.error('获取商品信息失败:', response);
        }
    } catch (error) {
        console.error('获取商品信息出错:', error);
    }
};

// 获取支付表单
const getPaymentForm = async () => {
    if (!orderId.value.trim()) {
        errorMessage.value = '未找到有效的订单ID，请先创建订单';
        canRetry.value = true;
        return;
    }

    // 重置状态
    errorMessage.value = '';
    successMessage.value = '';
    showFormData.value = false;
    responseData.value = null;
    loading.value = true;

    try {
        // 首先通过getOrderById获取订单详细信息
        const orderResponse = await getOrderById(parseInt(orderId.value, 10));

        if (!orderResponse || orderResponse.data.code !== '200') {
            errorMessage.value = '获取订单信息失败，请检查订单是否有效';
            canRetry.value = true;
            loading.value = false;
            return;
        }

        const orderData = orderResponse.data;
        // console.log(orderData)

        // 更新支付信息
        paymentData.value = {
            orderId: orderData.data.orderId || orderId.value,
            totalAmount: orderData.data.totalAmount || '0.00',
            paymentMethod: '支付宝支付',
            orderTime: orderData.data.createTime || formatCurrentTime()
        };

        // 启动倒计时
        startCountdown();

        // 获取订单商品信息
        await fetchCartItems();

        // 然后调用createPayment获取支付表单
        const paymentResponse = await createPayment(parseInt(orderId.value, 10));
        responseData.value = paymentResponse;

        if (paymentResponse && paymentResponse.code === '200' && paymentResponse.data && paymentResponse.data.paymentForm) {
            // 解析支付表单
            const formData = parseFormElement(paymentResponse.data.paymentForm);
            if (formData) {
                formAction.value = formData.action || "";
                bizContent.value = formData.bizContent;

                // 显示支付表单
                showFormData.value = true;
                // console.log('最终支付数据:', paymentData.value);
            } else {
                errorMessage.value = '解析支付表单失败';
                canRetry.value = true;
            }
        } else {
            // 处理错误情况
            errorMessage.value = paymentResponse.msg || '获取支付表单失败';
            canRetry.value = true;
        }
    } catch (error: any) {
        console.error('获取支付信息错误:', error);
        errorMessage.value = `获取支付信息失败: ${error.message || '未知错误'}`;
        canRetry.value = true;
    } finally {
        loading.value = false;
    }
};

// 添加支付倒计时相关状态
const remainingMinutes = ref(30);
const remainingSeconds = ref(0);
const countdownTimer = ref<number | null>(null);

// 计算剩余支付时间
const calculateRemainingTime = () => {
    if (!paymentData.value.orderTime) return;

    const orderTime = new Date(paymentData.value.orderTime).getTime();
    const currentTime = new Date().getTime();
    const elapsedTimeMs = currentTime - orderTime;

    // 计算剩余总秒数（30分钟 - 已过去的时间）
    const totalRemainingSeconds = Math.max(0, 30 * 60 - Math.floor(elapsedTimeMs / 1000));
    remainingMinutes.value = Math.floor(totalRemainingSeconds / 60);
    remainingSeconds.value = totalRemainingSeconds % 60;

    // 如果时间已到，取消倒计时并提示用户
    if (totalRemainingSeconds <= 0) {
        if (countdownTimer.value) {
            clearInterval(countdownTimer.value);
            countdownTimer.value = null;
        }
        ElMessage.warning('支付时间已超时，请重新下单');
        // 可以选择导航回订单列表
        // goToOrderList();
    }
};

// 启动倒计时
const startCountdown = () => {
    // 先计算一次初始值
    calculateRemainingTime();

    // 清除可能存在的旧定时器
    if (countdownTimer.value) {
        clearInterval(countdownTimer.value);
    }

    // 设置新的定时器，每秒更新一次
    countdownTimer.value = setInterval(() => {
        if (remainingSeconds.value > 0) {
            remainingSeconds.value -= 1;
        } else if (remainingMinutes.value > 0) {
            remainingMinutes.value -= 1;
            remainingSeconds.value = 59;
        } else {
            // 时间到，清除定时器
            if (countdownTimer.value) {
                clearInterval(countdownTimer.value);
                countdownTimer.value = null;
            }
        }
    }, 1000) as unknown as number;
};

// 格式化时间显示，确保个位数前面有0
const formatTimeDigit = (num: number): string => {
    return num < 10 ? `0${num}` : `${num}`;
};

// 组件卸载时清除定时器
onUnmounted(() => {
    if (countdownTimer.value) {
        clearInterval(countdownTimer.value);
        countdownTimer.value = null;
    }
});

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

// 添加格式化当前时间的辅助函数
const formatCurrentTime = () => {
    const now = new Date();
    return now.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    }).replace(/\//g, '-');
};

// 导航到订单列表
const goToOrderList = () => {
    router.push('/orders');
};

// 导航到首页
const goToMyHomePage = () => {
    router.push('/home');
};

// 跳转到支付宝支付
const goToPay = () => {
    if (!formAction.value || !bizContent.value) {
        errorMessage.value = '支付表单信息不完整，无法跳转支付';
        canRetry.value = true;
        return;
    }

    // 创建表单并提交
    const form = document.createElement('form');
    form.method = 'post';
    form.action = formAction.value;
    form.target = '_self'; // 在当前窗口打开

    // 添加支付参数
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = 'biz_content';
    input.value = bizContent.value;

    form.appendChild(input);
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);

    ElMessage.success('正在跳转到支付页面，请在本窗口完成支付');
};

// 在组件加载时检查URL参数
onMounted(async () => {
    // 从URL参数获取订单信息
    const encodedOrderId = route.query.order_id; // 注意参数名应为order_id而非orderId
    const urlPaymentStatus = route.query.payment_status;

    if (encodedOrderId) {
        try {
            // 对Base64编码的订单ID进行解码
            const decodedOrderId = atob(encodedOrderId.toString());
            orderId.value = decodedOrderId;
            // console.log("从URL获取并解密订单ID:", orderId.value);
            // 获取支付表单
            getPaymentForm();
        } catch (error) {
            console.error("订单ID解密失败:", error);
            errorMessage.value = "订单信息格式错误";
            canRetry.value = true;
        }
    } else {
        // 没有订单ID参数
        errorMessage.value = "未找到有效的订单信息";
        canRetry.value = true;
    }

    // 处理支付状态
    if (urlPaymentStatus === 'success') {
        // 设置支付状态
        paymentStatus.value = 'success';

        // 显示加载指示器
        const loading = ElLoading.service({
            lock: true,
            text: '正在更新购物车...',
            background: 'rgba(255, 255, 255, 0.7)'
        });

        try {
            // 支付成功后，重新从后端获取购物车数据
            const response = await getCart();

            if (response.code.toString() === '200') {
                // 用后端返回的最新数据更新本地购物车
                sessionStorage.setItem('cart', JSON.stringify(response.data.items || []));
                // console.log("已从后端重新获取购物车数据");
            } else {
                console.error("获取购物车数据失败:", response);
                ElMessage.warning('更新购物车信息失败，请刷新页面');
            }

        } catch (error) {
            console.error("更新购物车数据失败:", error);
            ElMessage.error('更新购物车信息出错，请稍后手动刷新');
        } finally {
            // 关闭加载指示器
            loading.close();
        }
    } else {
        paymentStatus.value = 'pending';
    }
});

</script>

<style scoped>
/* 基础样式 - 缩小主体框 */
.container {
    width: 800px;
    /* 从1100px减小到800px */
    margin: 20px auto;
    margin-top: 60px;
    /* 减小顶部边距 */
    padding: 20px;
    /* 减小内边距 */
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

h2 {
    color: #2e353e;
    font-size: 24px;
    /* 减小字体大小 */
    text-align: center;
    margin-bottom: 20px;
    border-bottom: 1px solid #eee;
    padding-bottom: 12px;
}

/* 支付形式样式 */
.payment-form {
    padding: 15px 0;
}

/* 应付款和合计在一行显示 */
.payment-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 10px;
}

.payment-title {
    color: #333;
    font-size: 18px;
    margin: 0;
    padding: 0;
    border: none;
    text-align: left;
}

.payment-summary {
    display: flex;
    align-items: center;
}

.total {
    font-size: 16px;
    font-weight: bold;
    color: #e74c3c;
}

/* 商品信息区域 */
.order-items {
    margin-bottom: 20px;
    background-color: #fff;
    border-radius: 8px;
}

.order-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;
}

.item-image {
    width: 120px;
    height: 120px;
    margin-right: 12px;
    border-radius: 4px;
    overflow: hidden;
    background-color: #f5f5f5;
}

.item-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.item-info {
    flex: 1;
}

.item-name {
    font-size: 22px;
    font-weight: bold;
    color: #333;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 400px;
}

.item-price {
    text-align: right;
    min-width: 80px;
}

.price {
    font-size: 16px;
    font-weight: bold;
    color: #e74c3c;
}

.quantity {
    font-size: 14px;
    color: #999;
}

.payment-info {
    border-top: 1px solid #eee;
    padding-top: 15px;
}

.info-row {
    display: flex;
    padding: 12px 0;
    font-size: 14px;
    border-bottom: 1px solid #f5f5f5;
}

.info-label {
    width: 100px;
    color: #888;
}

.info-value {
    flex: 1;
    color: #333;
    display: flex;
    align-items: center;
}

/* 倒计时样式 */
.payment-countdown {
    background-color: #fff7e6;
    padding: 10px 12px;
    margin: 12px 0;
    border-radius: 4px;
    border: 1px solid #ffe7ba;
}

.time-remaining {
    color: #ff4d4f;
    font-weight: bold;
    font-size: 16px;
    margin: 0 4px;
    font-family: monospace;
}

.countdown {
    color: #d46b08;
}

.payment-actions {
    display: flex;
    justify-content: center;
    gap: 15px;
    margin-top: 25px;
}

.pay-btn {
    padding: 10px 25px;
    background-color: #1677ff;
    color: white;
    border: none;
    border-radius: 4px;
    font-size: 15px;
    cursor: pointer;
}

.cancel-btn {
    padding: 10px 25px;
    background-color: #f5f5f5;
    color: #333;
    border: none;
    border-radius: 4px;
    font-size: 15px;
    cursor: pointer;
}

.pay-btn:hover {
    background-color: #0e5edb;
}

.cancel-btn:hover {
    background-color: #e0e0e0;
}

/* 其它样式保持不变 */
.loading {
    text-align: center;
    margin: 20px 0;
}

.spinner {
    border: 4px solid rgba(0, 0, 0, 0.1);
    border-left-color: #1677ff;
    border-radius: 50%;
    width: 30px;
    height: 30px;
    animation: spin 1s linear infinite;
    display: inline-block;
}

@keyframes spin {
    0% {
        transform: rotate(0deg);
    }

    100% {
        transform: rotate(360deg);
    }
}

.error {
    color: #d9534f;
    background-color: #fdf7f7;
    padding: 10px;
    border-radius: 4px;
    margin-top: 10px;
}

.retry-btn {
    background-color: #ff9800;
    color: white;
    border: none;
    padding: 8px 16px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    margin-top: 10px;
    margin-left: 10px;
}

.retry-btn:hover {
    background-color: #e68a00;
}

.payment-success {
    text-align: center;
    padding: 30px 20px;
}

.success-icon {
    font-size: 50px;
    height: 90px;
    width: 90px;
    line-height: 90px;
    background-color: #52c41a;
    color: white;
    border-radius: 50%;
    margin: 0 auto 25px;
}

.action-buttons {
    margin-top: 30px;
    display: flex;
    justify-content: center;
    gap: 15px;
}

.btn.primary {
    background-color: #1677ff;
    color: white;
    border: none;
    padding: 10px 22px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 15px;
}

.btn.secondary {
    background-color: #f0f0f0;
    color: #333;
    border: none;
    padding: 10px 22px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 15px;
}

.btn:hover {
    opacity: 0.9;
}
</style>