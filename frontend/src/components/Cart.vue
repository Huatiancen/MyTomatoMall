<template>
    <div class="product-wrapper">

        <div v-if="loading">
            <div class="loading-animation">
                <el-skeleton :rows="3" animated />
            </div>
            <p class="loading-tip">正在加载您的购物车信息...</p>
        </div>

        <el-table v-else :data="cartItems" style="width: 100%" :height="630" :row-class-name="tableRowClassName">
            <!-- 选中按钮 -->
            <el-table-column label="选中" width="80">
                <template #default="scope">
                    <el-checkbox :model-value="isItemSelected(scope.row.cartItemId)"
                        @change="toggleItemSelection(scope.row.cartItemId)" :disabled="isAdmin"></el-checkbox>
                </template>
            </el-table-column>

            <!-- 商品封面展示 -->
            <el-table-column prop="cover" label="封面" width="120">
                <template #default="scope">
                    <div class="image-container">
                        <img v-if="scope.row.cover" :src="scope.row.cover" alt="封面" class="product-image" />
                        <img v-else src="http://47.111.155.70/oss/default-cover.jpg"
                            class="product-image">
                        <!-- 添加下架商品遮罩 -->
                        <div v-if="scope.row.productStatus === 'INACTIVE'" class="sold-out-overlay">
                            <div class="sold-out-text">
                                <div>下架</div>
                                <div>sold out</div>
                            </div>
                        </div>
                    </div>
                </template>
            </el-table-column>

            <!-- 书名展示 -->
            <el-table-column label="书名" width="200">
                <template #default="scope">
                    <p class="product-name">{{ scope.row.title || '未知' }}</p>
                </template>
            </el-table-column>

            <!-- 价格展示 -->
            <el-table-column label="价格" width="120">
                <template #default="scope">
                    <p :class="['product-price', { 'inactive-price': scope.row.productStatus === 'INACTIVE' }]">
                        ¥{{ scope.row.price.toFixed(2) || 0 }}
                    </p>
                </template>
            </el-table-column>

            <!-- 数量展示 -->
            <el-table-column label="数量" width="130">
                <template #default="scope">
                    <div class="quantity-selector">
                        <button @click="decreaseQuantity(scope.row)" :disabled="scope.row.productStatus === 'INACTIVE'"
                            class="quantity-btn">-</button>
                        <input type="number" class="quantity-display" :value="scope.row.quantity || 0"
                            @input="(event) => handleQuantityInput(event, scope.row)"
                            @blur="(event) => validateQuantity(event, scope.row)"
                            :disabled="scope.row.productStatus === 'INACTIVE'" min="1" />
                        <button @click="increaseQuantity(scope.row)" :disabled="scope.row.productStatus === 'INACTIVE'"
                            class="quantity-btn">+</button>
                    </div>
                </template>
            </el-table-column>

            <!-- 每行的总价展示 -->
            <el-table-column label="总价" width="120">
                <template #default="scope">
                    <p :class="['quantity-price', { 'inactive-price': scope.row.productStatus === 'INACTIVE' }]">
                        ¥{{ ((scope.row.price || 0) * (scope.row.quantity || 0)).toFixed(2) }}
                    </p>
                </template>
            </el-table-column>

            <!-- 操作按钮 -->
            <el-table-column label="操作" width="150">
                <template #default="scope">
                    <div class="button-container">
                        <el-button type="primary" size="small" @click="goToBookDetail(scope.row.productId)"
                            :disabled="scope.row.productStatus === 'INACTIVE'">详情</el-button>
                    </div>
                </template>
            </el-table-column>
        </el-table>

        <!-- 添加分割线 -->
        <div class="divider"></div>

        <!-- 结算条 -->
        <div class="cart-summary">
            <div class="summary-content">
                <div class="selection-bar">
                    <el-checkbox :model-value="isAllSelected" @change="toggleAllSelection"
                        :disabled="isAdmin">全选</el-checkbox>
                    <el-button class="batch-btn" type="danger" @click="batchDelete"
                        :disabled="selectedItems.length === 0 || isAdmin">批量删除</el-button>
                    <span class="selection-count">已选择{{ selectedItems.length }}件商品</span>
                </div>
                <div class="total-price-container">
                    <p>总计(不含运费)：<span class="total-price">¥{{ selectedTotalAmount }}</span></p>
                </div>
            </div>
            <el-button type="primary" @click="checkout" :disabled="!hasValidSelectedItems">结 算</el-button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter} from 'vue-router';
import { type CartItem, getCart, removeFromCart as removeCartItemAPI, updateCartItem } from '../api/cart';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getStockByProductId } from "../api/products";

const router = useRouter();
// 会话存储键名“cart”
const cartItems = ref<CartItem[]>([]); // 购物车商品数据
const selectedItems = ref<number[]>([]); // 选中的购物车商品ID数组
const loading = ref(false);

const isAdmin = sessionStorage.getItem('role') === 'ADMIN'

const tableRowClassName = ({ row }: { row: any }) => {
    if (row.productStatus === 'INACTIVE') {
        return 'inactive-row';
    }
    return '';
};
// 判断商品是否被选中
const isItemSelected = (cartItemId: number): boolean => {
    return selectedItems.value.includes(cartItemId);
};

// 切换商品选中状态
const toggleItemSelection = (cartItemId: number) => {
    const index = selectedItems.value.indexOf(cartItemId);
    if (index === -1) {
        // 如果不在数组中，添加它
        selectedItems.value.push(cartItemId);
    } else {
        // 如果已在数组中，移除它
        selectedItems.value.splice(index, 1);
    }
};

// 全选状态
const isAllSelected = computed(() => {
    if (cartItems.value.length === 0) return false;
    return selectedItems.value.length === cartItems.value.length;
});

// 切换全选状态
const toggleAllSelection = (value: boolean) => {
    if (value) {
        selectedItems.value = cartItems.value.map(item => item.cartItemId!);
    } else {
        selectedItems.value = [];
    }
};

// 添加一个计算属性，判断是否有可结算的选中商品（排除下架商品）
const hasValidSelectedItems = computed(() => {
    return cartItems.value.some(item =>
        selectedItems.value.includes(item.cartItemId!) &&
        item.productStatus !== 'INACTIVE'
    );
});

// 批量删除选中的商品
const batchDelete = () => {
    if (selectedItems.value.length === 0) {
        ElMessage.warning('请先选择要删除的商品');
        return;
    }

    ElMessageBox.confirm(
        `确定要删除${selectedItems.value.length}件商品吗？`,
        '提示',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }
    ).then(async () => {
        // 批量删除
        for (const cartItemId of selectedItems.value) {
            const response = await removeCartItemAPI(cartItemId);
            if (response.code.toString() === '200') {
                // 成功删除，更新本地数据
                const updatedItems = cartItems.value.filter(item => item.cartItemId !== cartItemId);
                cartItems.value = updatedItems;

                // 更新本地存储
                saveCartToSessionStorage(updatedItems);
            }
        }
        // 更新选中状态
        selectedItems.value = [];
    }).catch(() => {
        // 取消删除
    });
};

const goToBookDetail = (id : number) => {
    router.push(`/BookDetail/${id}`);
}

// 保存购物车数据到本地存储
const saveCartToSessionStorage = (data: CartItem[]) => {
    sessionStorage.setItem("cart", JSON.stringify(data));
};

// 获取购物车信息
const fetchCartItems = async () => {
    loading.value = true
    try {
        // 尝试从 sessionStorage 获取购物车数据
        const cartData = sessionStorage.getItem('cart');
        if (cartData) {
            // 如果本地存储有购物车数据，直接使用
            cartItems.value = JSON.parse(cartData);
            // console.log('从本地存储获取购物车信息:', cartItems.value);
        } else {
            // 如果没有数据，调用后端 API 获取
            const response = await getCart();
            if (response.code.toString() === '200') {
                cartItems.value = response.data.items || [];
                // console.log('从后端获取购物车信息:', cartItems.value);
                // 将获取的购物车信息存储到 sessionStorage
                sessionStorage.setItem('cart', JSON.stringify(cartItems.value));
            } else {
                console.error('获取购物车信息失败:', response.message);
            }
        }

        // 将 `sessionStorage` 中的 `products` 数据填充到购物车项目中
        const products = JSON.parse(sessionStorage.getItem('products') || '[]');
        cartItems.value.forEach(item => {
            const product = products.find((p: any) => p.id === item.productId);
            if (product) {
                item.title = product.title;
                item.cover = product.cover;
                item.productStatus = product.status;
            }
        });
        // 移除已经下架商品的选中状态
        selectedItems.value = selectedItems.value.filter(id => {
            const item = cartItems.value.find(i => i.cartItemId === id);
            return item && item.productStatus !== 'INACTIVE';
        });
    } catch (error) {
        console.error('获取购物车请求失败:', error);
    } finally {
        loading.value = false
    }
};

// 获取商品库存的统一方法
const getStockByProductIdFromSessionOrAPI = async (productId: number): Promise<number> => {
    // 从后端接口获取
    try {
        // console.log("从后端接口获取")
        const response = await getStockByProductId(productId);
        if (response.data.code === '200' && response.data.data) {
            return response.data.data.amount;
        } else {
            console.error('获取库存失败:', response.data.message);
            return 0; // 库存获取失败时返回 0
        }
    } catch (error) {
        console.error('获取库存时发生错误:', error);
        return 0;
    }
};


// 增加商品数量，不超过库存
const increaseQuantity = async (item: CartItem) => {
    const stockAmount = await getStockByProductIdFromSessionOrAPI(item.productId);
    // console.log(stockAmount)
    if (item.quantity < stockAmount) {
        item.quantity++;
        // 更新后端数量
        await updateCartItem(item.cartItemId!, item.quantity);
        // 更新本地存储
        saveCartToSessionStorage(cartItems.value);
    } else {
        ElMessage.warning('超出库存');
    }
};

// 减少商品数量，如果数量为 0，则弹窗确认删除
const decreaseQuantity = async (item: CartItem) => {
    if (item.quantity > 1) {
        item.quantity--;
        // 更新后端数量
        await updateCartItem(item.cartItemId!, item.quantity);
        // 更新本地存储
        saveCartToSessionStorage(cartItems.value);
    } else {
        // 弹窗确认删除
        // ElMessageBox.confirm("数量为0，是否确认删除该商品？", "提示", {
        //     confirmButtonText: '确定',
        //     cancelButtonText: '取消',
        //     type: 'warning'
        // }).then(async () => {
        //     // 用户点击确认按钮
        //     await removeFromCart(item.cartItemId!);
        //     saveCartToSessionStorage(cartItems.value);
        // }).catch(() => {
        //     // 用户点击取消按钮，不做任何操作
        // });
        ElMessage.info('已达到最小数量，如需移除请点击删除按钮');
    }
};

// 处理输入框数量变化
const handleQuantityInput = async (event: any, item: any) => {
    let inputValue = parseInt(event.target.value);

    // 确保输入是有效的数字
    if (isNaN(inputValue)) {
        event.target.value = item.quantity;
        return;
    }

    const stockAmount = await getStockByProductIdFromSessionOrAPI(item.productId);

    // 边界检查：小于1则设为1，大于库存则设为最大库存量
    if (inputValue < 1) {
        inputValue = 1;
    } else if (inputValue > stockAmount) {
        inputValue = stockAmount;
        ElMessage.warning('超出库存量，已设为最大可购买数量');
    }

    // 更新数量
    item.quantity = inputValue;
    event.target.value = inputValue;

    // 更新后端数量
    await updateCartItem(item.cartItemId!, inputValue);

    // 更新本地存储
    saveCartToSessionStorage(cartItems.value);
};

// 在失焦时验证数量
const validateQuantity = async (event: any, item: any) => {
    let inputValue = parseInt(event.target.value);

    // 确保输入是有效的数字
    if (isNaN(inputValue)) {
        event.target.value = item.quantity;
        return;
    }

    const stockAmount = await getStockByProductIdFromSessionOrAPI(item.productId);

    // 边界检查：小于1则设为1，大于库存则设为最大库存量
    if (inputValue < 1) {
        item.quantity = 1;
        event.target.value = 1;

        // 更新后端数量
        await updateCartItem(item.cartItemId!, 1);

        // 更新本地存储
        saveCartToSessionStorage(cartItems.value);
    } else if (inputValue > stockAmount) {
        item.quantity = stockAmount;
        event.target.value = stockAmount;
        ElMessage.warning('超出库存量，已设为最大可购买数量');

        // 更新后端数量
        await updateCartItem(item.cartItemId!, stockAmount);

        // 更新本地存储
        saveCartToSessionStorage(cartItems.value);
    }
};

// 计算选中的商品总金额，排除下架商品
const selectedTotalAmount = computed(() => {
    let total = 0;
    cartItems.value.forEach(item => {
        // 只计算未下架的选中商品
        if (selectedItems.value.includes(item.cartItemId!) && item.productStatus !== 'INACTIVE') {
            total += (item.price || 0) * item.quantity;
        }
    });
    return total.toFixed(2);
});

const checkout = () => {
    // 获取选中的购物车项目
    const selectedCartItems = cartItems.value.filter(item =>
        selectedItems.value.includes(item.cartItemId!)
    );

    // 如果没有选中任何商品，显示提示并返回
    if (selectedCartItems.length === 0) {
        ElMessage.warning('请先选择要结算的商品');
        return;
    }

    // 过滤出可结算的商品（状态不为 INACTIVE）
    const checkoutCartItems = selectedCartItems.filter(item => item.productStatus !== 'INACTIVE');
    // console.log(`checkoutCartItems + ${JSON.stringify(checkoutCartItems)}`)

    // 如果没有可结算的商品，提示用户并返回
    if (checkoutCartItems.length === 0) {
        ElMessage.warning('选中的商品都已下架，无法结算');
        return;
    }

    // 如果有下架商品，告知用户
    if (checkoutCartItems.length < selectedCartItems.length) {
        ElMessage.info(`已选中${selectedCartItems.length}件商品，但有${selectedCartItems.length - checkoutCartItems.length}件已下架，将只结算${checkoutCartItems.length}件上架商品`);
    }

    // 通过路由导航将选中的商品传递给结账页面
    router.push({
        path: '/checkout',
        query: {
            // 将选中商品ID作为查询参数传递
            selectedIds: checkoutCartItems.map(item => item.cartItemId).join(',')
        },
    });
};

onMounted(() => {
    fetchCartItems();
});
</script>

<style scoped>

/* 商品展示区域 */
.product-wrapper {
    margin-top: 3%;
    display: flex;
    justify-content: start;
    flex-wrap: wrap;
    padding: 20px;
    background-color: white;
    border-radius: 10px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    flex-grow: 1;
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

.quantity-price {
    font-size: 18px;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: left;
    color: #e74c3c;
}

/* 数量选择器整体容器样式 */
.quantity-selector {
    display: flex;
    align-items: center;
    width: 80px;
    height: 30px;
    border-radius: 8px;
    overflow: hidden;
    background-color: #f7f7f7;
    border: 1px solid #ebebeb;
}

/* 增减按钮样式 */
.quantity-btn {
    width: 30px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    font-size: 18px;
    font-weight: 400;
    color: #606266;
    cursor: pointer;
    transition: background-color 0.2s;
    padding: 0;
    margin: 0;
}

/* 增加按钮特殊颜色 */
.quantity-btn:last-child {
    color: #e74c3c;
}

/* 按钮悬停效果 */
.quantity-btn:hover:not(:disabled) {
    background-color: #eaeaea;
}

/* 按钮点击效果 */
.quantity-btn:active:not(:disabled) {
    background-color: #e0e0e0;
}

.quantity-btn:focus {
    outline: none;
}

/* 禁用状态 */
.quantity-btn:disabled {
    color: #c0c4cc;
    cursor: not-allowed;
}

/* 输入框样式 */
.quantity-display {
    flex: 1;
    width: 40px;
    height: 100%;
    border: none;
    background: transparent;
    text-align: center;
    font-size: 16px;
    color: #333;
    margin: 0;
    padding: 0;
    appearance: textfield;
}

/* 移除浏览器默认的上下箭头 */
.quantity-display::-webkit-outer-spin-button,
.quantity-display::-webkit-inner-spin-button {
    -webkit-appearance: none;
    margin: 0;
}

/* 保持其他样式效果一致 */
.quantity-display:focus {
    outline: none;
}

/* 禁用状态的输入框 */
.quantity-display:disabled {
    color: #999;
    cursor: not-allowed;
    background-color: transparent;
}

.el-table {
    width: 100%;
    table-layout: fixed;
    /* 确保列宽固定 */
}

/* 价格和数量展示样式 */
.product-price,
.product-name {
    font-size: 18px;
    font-weight: bold;
}

.product-price {
    color: #e74c3c;
}

.product-image {
    width: 80px;
    height: 100px;
    object-fit: cover;
    display: block;
    margin: 0 auto;
    /* 居中图片 */
}

/* 商品图片容器相对定位，用于放置遮罩 */
.image-container {
    position: relative;
    width: 80px;
    height: 100px;
    margin: 0 auto;
}

/* 下架商品遮罩 */
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
    border-radius: 3px;
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

/* 下架商品行样式 */
.el-table .inactive-row {
    background-color: #f9f9f9;
    opacity: 0.85;
}

/* 下架商品行样式 */
:deep(.inactive-row) {
    background-color: #f9f9f9;
    color: #999;
}

/* 添加下架商品价格样式 - 暗淡效果 */
.inactive-price {
    color: #999 !important;
    /* 使用灰色替代鲜红色 */
    text-decoration: line-through;
    /* 添加删除线 */
    opacity: 0.7;
    /* 降低不透明度 */
}

.button-container {
    display: flex;
    align-items: center;
    justify-content: flex-start;
}

/* 结算条样式 */
.cart-summary {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: rgba(240, 240, 240, 0.95);
    padding: 15px 25px;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
    margin-bottom: 20px;
}

.summary-content {
    display: flex;
    align-items: center;
}

.selection-bar {
    display: flex;
    align-items: center;
    margin-right: 20px;
}

.batch-btn {
    margin-left: 20px;
}

.selection-count {
    margin-left: 20px;
    color: #666;
}

.total-price-container {
    font-size: 18px;
    font-weight: bold;
    margin: 0;
    color: #333;
}

.total-price {
    color: #e60012;
    font-size: 20px;
}

.divider {
    height: 1px;
    background-color: #e0e0e0;
    margin: 10px 0;
}

/* 调整结算条样式，确保所有元素在一行中 */
.summary-content {
    display: flex;
    align-items: center;
}

.selection-bar {
    display: flex;
    align-items: center;
    margin-right: 20px;
}

.total-price-container {
    margin-left: 20px;
    margin-right: 20px;
}

/* 未选中状态的复选框边框加粗 */
:deep(.el-checkbox__input .el-checkbox__inner) {
    border-width: 2px !important;
    /* 将边框从默认的 1px 增加到 2px */
    transition: border-color 0.3s;
}

/* 鼠标悬停时的边框效果增强 */
:deep(.el-checkbox__input:hover .el-checkbox__inner) {
    border-color: #409eff;
    /* 使用 Element 的主色调 */
}

/* 选中状态下保持较粗的边框 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    border-width: 2px !important;
}

/* 可选：稍微增大复选框尺寸，使粗边框更加明显 */
:deep(.el-checkbox__inner) {
    width: 18px;
    /* 默认是 16px */
    height: 18px;
    /* 默认是 16px */
}

/* 可选：调整选中标记的位置，以适应更大的尺寸 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner::after) {
    height: 9px;
    right: 3px;
    /* left: 1px; */
    /* top: 2px; */
    width: 4px;
}

</style>

