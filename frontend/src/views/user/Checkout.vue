<template>
    <Header />
    <BackgroundLayout2 />
    <div class="main-container">
        <!-- 收货人信息 -->
        <div class="reciever-area">
            <div class="receiver-infos">
                <div v-for="(info, index) in recieverInfos" :key="index" class="receiver-info"
                    :class="{ 'active': selectedAddress === index }" @click="selectAddress(index)">
                    <div class="info-item">
                        <span class="name">{{ info.name }}</span>
                        <span class="phone">{{ info.phone }}</span>
                    </div>
                    <div class="address">
                        <p>{{ info.address }}</p>
                    </div>
                </div>
            </div>
            <button class="add-address" @click="() => addNewAddress()">新增收货地址 +</button>
        </div>

        <!-- 送货清单 -->
        <div class="delivery-list">
            <h3>送货清单</h3>
            <div class="store-info">
                <div class="store-logo">
                    <img src="../../../public/logo.png" alt="蕃茄商城" />
                </div>
                <span class="store-name">蕃茄商城</span>
                <a href="#" class="back-link" @click="backCart">返回购物车</a>
            </div>

            <div class="delivery-container">
                <div class="delivery-options">
                    <div class="option-item">
                        <span class="option-label">配送方式</span>
                        <div class="delivery-buttons">
                            <button v-for="item in deliveryOptions" :key="item.value"
                                @click="deliveryMethod = item.value"
                                :class="['delivery-button', { 'active': deliveryMethod === item.value }]">
                                {{ item.label }}
                            </button>
                        </div>
                    </div>
                    <div class="option-item">
                        <span class="option-label">配送时间</span>
                        <el-select v-model="deliveryTime" placeholder="请选择">
                            <el-option v-for="item in timeOptions" :key="item.value" :label="item.label"
                                :value="item.value">
                            </el-option>
                        </el-select>
                    </div>
                </div>

                <div class="product-list">
                    <table class="product-table">
                        <thead>
                            <tr>
                                <th>封面</th>
                                <th>商品名称</th>
                                <th>单价</th>
                                <th>数量</th>
                                <th>总价</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="(item, index) in selectedCartItems" :key="index">
                                <td class="product-info">
                                    <img v-if="item.cover" :src="item.cover" alt="商品图片" class="product-cover" />
                                    <img v-else src="http://47.111.155.70/oss/default-cover.jpg"
                                        class="product-cover" />
                                </td>
                                <td>
                                    <div class="product-details">
                                        <div class="product-title">{{ item.title }}</div>
                                        <div class="return-policy">支持7天无理由退换货</div>
                                    </div>
                                </td>
                                <td class="price">¥{{ item.price.toFixed(2) }}</td>
                                <td class="quantity">{{ item.quantity }}</td>
                                <td class="total">¥{{ (item.price * item.quantity).toFixed(2) }}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- 支付方式 -->
        <div class="payment-methods">
            <h3>支付方式</h3>
            <div class="payment-options">
                <button class="payment-option active">网上支付</button>
                <button class="payment-option" :disabled="true">货到付款</button>
                <button class="payment-option" :disabled="true">银行转账</button>
            </div>
        </div>

        <!-- 底部支付栏 -->
        <div class="payment-bar">
            <div class="shipping-info">
                <p>寄送至: {{ recieverInfos[selectedAddress]?.address || ""}}</p>
                <p>{{ recieverInfos[selectedAddress]?.name || "" }} {{ recieverInfos[selectedAddress]?.phone || ""}}</p>
            </div>
            <div class="order-summary">
                <div class="item-count">共{{ totalItems }}件商品</div>
                <div class="total-amount">
                    <span>实付:</span>
                    <span class="amount">¥{{ totalAmount }}</span>
                    <span class="shipping-cost">(含运费¥0.00元)</span>
                </div>
            </div>
            <button class="pay-button" @click="commitOrder">提交订单</button>
        </div>
    </div>

    <!-- 修改地址模态框组件 -->
    <AddressModal :visible="isAddressModalVisible" :address="addressToEdit"
        @update:visible="isAddressModalVisible = $event" @address-updated="handleAddressUpdated" />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElLoading } from 'element-plus';
import { useRouter, useRoute } from 'vue-router';
import { checkout } from '../../api/cart'
import { createAddress, getAddresses } from '../../api/address';

// Define the CartItem interface
interface CartItem {
    cartItemId: number;
    price: number;
    quantity: number;
    title: string;
    cover?: string;
}

interface CommitInfo {
    id?: number;
    name: string;
    phone: string;
    zipcode: string;
    address: string;
    isDefault?: boolean;  //需要添加这个字段判断是否是默认地址
}

// 更新 RecieverInfo 接口
interface RecieverInfo {
    name: string;
    phone: string;
    zipcode: string;
    region: string[]; // 用于存储省市区的编码数组
    detailAddress: string; // 详细街道地址
}

const route = useRoute();
const router = useRouter();

const selectedCartItems = ref<CartItem[]>([])
const recieverInfos = ref<CommitInfo[]>([])
const totalAmount = ref('0.00')
const totalItems = ref(0)
// 选中的地址索引
const selectedAddress = ref(0);

// 获取查询参数中的selectedIds
const selectedIds = route.query.selectedIds
    ? String(route.query.selectedIds).split(',').map(id => Number(id))
    : [];

// 选择地址的方法
const selectAddress = (index: number) => {
    selectedAddress.value = index;
};

// 地址编辑模态框状态
const isAddressModalVisible = ref(false);
const addressToEdit = ref<RecieverInfo | null>(null);

// 修改为只添加新地址
const addNewAddress = () => {
    // console.log("打开添加地址模态框");
    // 创建空白地址对象
    addressToEdit.value = { name: '', phone: '', zipcode: '', region:[], detailAddress: '' };
    isAddressModalVisible.value = true;
};

// 地址添加后的处理
const handleAddressUpdated = async (newAddress: RecieverInfo) => {
    try {
        // console.log('新地址已添加:', newAddress);
        // 显示加载提示
        const loading = ElLoading.service({
            lock: true,
            text: '保存地址中...',
            background: 'rgba(255, 255, 255, 0.7)'
        });
        const newCommitAddress: CommitInfo = {
            name: newAddress.name,
            phone: newAddress.phone,
            zipcode: newAddress.zipcode,
            address: newAddress.region.join(' ') + ' ' + newAddress.detailAddress
        }
        const response = await createAddress(newCommitAddress);
        loading.close();
        // 处理响应
        if (response.data.code === '200') {
            // console.log(response.data.data)
            newCommitAddress.id = response.data.data.shoppingAddressId;
            recieverInfos.value = [...recieverInfos.value, newCommitAddress];
            selectedAddress.value = recieverInfos.value.length - 1;
            // 更新本地存储
            sessionStorage.setItem('userAddresses', JSON.stringify(recieverInfos.value));
            ElMessage.success('地址保存成功');
        } else {
            ElMessage.error('地址保存失败，请稍后重试');
        }
    } catch (error) {
        console.error('保存地址时出错:', error);
        ElMessage.error('保存地址时出错，请稍后重试');
    }
};

// 移除编辑索引相关代码
// const editingAddressIndex = ref(-1); // 删除此行

onMounted(async () => {
    try {
        // 获取完整的购物车数据
        const storedCartItems = sessionStorage.getItem('cart');
        if (storedCartItems) {
            const allCartItems = JSON.parse(storedCartItems);
            // 根据selectedIds过滤出选中的购物车项目
            selectedCartItems.value = allCartItems.filter((item: CartItem) =>
                selectedIds.includes(item.cartItemId)
            );
            // 计算选中商品的总金额
            let total = 0;
            selectedCartItems.value.forEach(item => {
                total += (item.price || 0) * item.quantity;
            });
            totalAmount.value = total.toFixed(2);
            totalItems.value = selectedCartItems.value.reduce((sum, item) => sum + item.quantity, 0);
            // 如果没有找到任何选中商品，可能需要返回购物车页面
            if (selectedCartItems.value.length === 0) {
                ElMessage.warning('未找到选中的商品，请返回购物车重新选择');
                // 可以添加自动跳转回购物车的逻辑
                // router.push('/cart');
            }
        } else {
            ElMessage.warning('未找到购物车数据，请返回购物车重新选择');
            selectedCartItems.value = [];
            totalAmount.value = '0.00';
        }

        // 加载用户地址 - 修改为优先从后端获取
        const loading = ElLoading.service({
            lock: true,
            text: '加载地址信息...',
            background: 'rgba(255, 255, 255, 0.7)'
        });

        try {
            // 从后端API获取地址列表
            const response = await getAddresses();

            if (response.data && response.data.code === '200' && response.data.data) {
                // 处理API返回的地址数据，转换为前端需要的格式
                const addresses = response.data.data.map((addr: any) => ({
                    id: addr.shoppingAddressId,
                    name: addr.name,
                    phone: addr.phone,
                    zipcode: addr.zipcode || '',
                    address: addr.address || '',
                    isDefault: addr.isDefault
                }));

                if (addresses.length > 0) {
                    recieverInfos.value = sortAddressesByDefault(addresses);
                    selectedAddress.value = 0;

                    // 更新本地存储以便下次使用
                    sessionStorage.setItem('userAddresses', JSON.stringify(recieverInfos.value));
                    // console.log('已从后端加载地址数据:', recieverInfos.value);
                } else {
                    // 没有地址，回退到检查本地存储
                    fallbackToLocalAddresses();
                }
            } else {
                // API返回错误，回退到检查本地存储
                console.warn('从后端获取地址失败，回退到本地存储', response);
                fallbackToLocalAddresses();
            }
        } catch (error) {
            // API调用错误，回退到检查本地存储
            console.error('获取地址时出错，回退到本地存储:', error);
            fallbackToLocalAddresses();
        } finally {
            loading.close();
        }
    } catch (error) {
        console.error('初始化结算页面时出错:', error);
        ElMessage.error('加载页面时出现错误，请稍后再试');
    }
});

// 从本地存储加载地址的辅助函数
const fallbackToLocalAddresses = () => {
    const storedAddresses = sessionStorage.getItem('userAddresses');
    if (storedAddresses) {
        // 如果存在保存的地址，加载它们
        recieverInfos.value = JSON.parse(storedAddresses);
        // console.log('已加载本地存储的地址:', recieverInfos.value);
        // 如果有地址，默认选中第一个
        if (recieverInfos.value.length > 0) {
            selectedAddress.value = 0;
        }
    } else {
        // console.log('未找到保存的地址，打开添加地址表单');
        // 如果不存在保存的地址，直接打开添加地址模态框
        recieverInfos.value = [];
        // 延迟一点执行，确保组件已完全挂载
        setTimeout(() => {
            addNewAddress();
        }, 200);
    }
};

// 添加一个辅助函数用于将默认地址移到数组首位
const sortAddressesByDefault = (addresses: CommitInfo[]) => {
    if (!addresses || addresses.length <= 1) return addresses;

    // 复制一个新数组，避免直接修改原数组
    const sortedAddresses = [...addresses];

    // 找出默认地址，如果有的话
    const defaultIndex = sortedAddresses.findIndex(addr => addr.isDefault === true);

    // 如果找到默认地址且不在首位，将其移至首位
    if (defaultIndex > 0) {
        const defaultAddress = sortedAddresses.splice(defaultIndex, 1)[0];
        sortedAddresses.unshift(defaultAddress);
    }

    return sortedAddresses;
};

// 配送方式选项
const deliveryOptions = [
    { value: '1', label: '送货上门' },
    { value: '2', label: '到店自提' },
    { value: '3', label: '定时配送' }
];

// 配送时间选项
const timeOptions = [
    { value: 'unlimited', label: '时间不限' },
    { value: 'work', label: '只工作日送货' },
    { value: 'rest', label: '只双休日、假日送货' }
];

const deliveryMethod = ref('1');
const deliveryTime = ref('unlimited');

const backCart = () => {
    router.back();
}

const commitOrder = async () => {
    // 定义loading变量，便于在finally中访问
    let loading = null;
    try {
        // 检查是否选择了地址
        if (recieverInfos.value.length === 0) {
            ElMessage.warning('请添加收货地址');
            return;
        }
        // 构建提交数据
        const commitData = {
            cartItemIds: Array.isArray(selectedIds) ? selectedIds : [selectedIds], // 确保是数组形式
            shoppingAddress: recieverInfos.value[selectedAddress.value],
            paymentMethod: "ALIPAY"
        };
        loading = ElLoading.service({
            lock: true,
            text: '订单提交中...',
            background: 'rgba(255, 255, 255, 0.7)'
        });
        // 调用后端API
        const response = await checkout(commitData);
        // 处理响应
        if (response && String(response.code) === '200') {
            ElMessage.success('订单提交成功，即将跳转到支付页面');
            if (response.data.orderId) {
                // 将购物车项ID与订单ID关联存储到sessionStorage
                const encodedOrderId = btoa(response.data.orderId.toString());
                router.push({ path: '/payment', query: { order_id: encodedOrderId, payment_status: "pending" } });
            }
        } else {
            ElMessage.error('创建订单失败，请稍后再试');
        }
    } catch (error) {
        console.error('提交订单时出错:', error);
        ElMessage.error('提交订单时出错，请稍后再试');
    } finally {
        if (loading) {
            // 关闭加载状态
            loading.close();
        }
    }
};

</script>

<style scoped>
.main-container {
    width: 1100px;
    margin: 0 auto;
    padding: 20px;
    background-color: #f5f5f5;
    min-height: 100vh;
    margin-top: 80px;
}

/* 收货人信息 */
.reciever-area {
    background-color: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;
    /* display: flex;
    flex-direction: column; */
    /* align-items: left; */
}

.receiver-infos {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: left;
    /* justify-content: space-between; */
    gap: 15px;
}

.receiver-info {
    border: 1px solid #333;
    padding: 15px;
    margin-bottom: 15px;
    border-radius: 5px;
    width: calc(25% - 15px);
    box-sizing: border-box;
    height: 165px;
    cursor: pointer;
    transition: all 0.1s ease;
    position: relative;
    /* 添加相对定位 */
}

/* 选中状态 - 使用outline而不是改变border */
.receiver-info.active {
    border-color: #ff0000;
    outline: 2px solid #ff0000;
    /* 使用outline，不会改变元素尺寸 */
    /* 移除padding调整 */
}

.info-item {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;
}

.name {
    font-weight: bold;
}

.phone {
    color: #666;
}

.address p {
    color: #666;
    text-align: left;
    line-height: 1.5;
}

.add-address {
    background-color: #f5f5f5;
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 8px 15px;
    cursor: pointer;
    font-size: 14px;
    margin-top: 10px;
    width: 160px;
}

.add-address:hover {
    border-color: #666;
}

/* 送货清单 */
.delivery-list {
    background-color: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;
}

.delivery-list h3 {
    font-size: 18px;
    margin-bottom: 15px;
    color: #333;
}

.store-info {
    display: flex;
    align-items: center;
    margin-bottom: 15px;
}

.store-logo img {
    width: 30px;
    height: 30px;
    margin-right: 10px;
}

.store-name {
    font-weight: bold;
    color: #ff0000;
}

.back-link {
    margin-left: auto;
    color: #0066cc;
    text-decoration: none;
}

/* 配送选项和商品列表容器 */
.delivery-container {
    display: flex;
    gap: 20px;
}

.delivery-options {
    flex: 1;
}

.option-item {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 15px;
    margin-top: 30px;
}

/* 调整标签样式 */
.option-label {
    width: 100%;
    /* 占满整行 */
    color: #666;
    font-weight: bold;
    margin-bottom: 10px;
    text-align: left;
}

/* 调整按钮组样式 */
.delivery-buttons {
    display: flex;
    gap: 10px;
    width: 70%;
    /* 确保按钮组占满宽度 */
}

.delivery-button {
    padding: 8px 15px;
    border: 1px solid #ddd;
    border-radius: 4px;
    background-color: #f5f5f5;
    cursor: pointer;
    transition: all 0.3s;
    flex: 1;
    text-align: center;
}

.delivery-button.active {
    background-color: #ff0000;
    color: white;
    border-color: #ff0000;
    outline: none;
}

.delivery-button:hover:not(.active) {
    border-color: #ff0000;
}

.el-select {
    width: 70%;
}

.product-list {
    flex: 2;
}

.product-table {
    width: 100%;
    border-collapse: collapse;
}

.product-table th {
    text-align: left;
    padding: 10px;
    border-bottom: 1px solid #eee;
    color: #666;
}

.product-table td {
    padding: 15px 10px;
    border-bottom: 1px solid #eee;
}

.product-info {
    display: flex;
    align-items: center;
}

.product-cover {
    width: 50px;
    height: 80px;
    object-fit: cover;
    margin-right: 15px;
}

.product-details {
    flex: 1;
}

.product-title {
    font-weight: bold;
    margin-bottom: 5px;
    text-align: left;
}

.return-policy {
    color: #999;
    font-size: 12px;
    text-align: left;
}

.price,
.total {
    text-align: left;
    font-weight: bold;
    color: #e60012;
}

.quantity {
    text-align: left;
}

/* 支付方式 */
.payment-methods {
    background-color: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;
}

.payment-methods h3 {
    font-size: 18px;
    margin-bottom: 15px;
    color: #333;
}

.payment-options {
    display: flex;
    gap: 15px;
}

.payment-option {
    flex: 1;
    padding: 15px;
    border: 1px solid #ddd;
    border-radius: 4px;
    cursor: pointer;
    text-align: center;
    background-color: #f5f5f5;
}

.payment-option.active {
    background-color: #ff0000;
    color: white;
}

/* 底部支付栏 */
.payment-bar {
    background-color: #fff;
    padding: 15px 20px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    position: sticky;
    bottom: 0;
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
}

.shipping-info {
    flex: 1;
}

.shipping-info p {
    margin: 5px 0;
    color: #666;
    font-size: 14px;
}

.shipping-cost {
    color: #666;
    font-size: 14px;
}

.order-summary {
    display: flex;
    align-items: center;
    margin-right: 20px;
}

.item-count {
    margin-right: 20px;
    color: #666;
}

.total-amount {
    font-size: 18px;
    font-weight: bold;
}

.amount {
    color: #ff0000;
}

.pay-button {
    background-color: #ff0000;
    color: white;
    border: none;
    padding: 10px 30px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 16px;
}

.pay-button:hover {
    background-color: #e60012;
}
</style>