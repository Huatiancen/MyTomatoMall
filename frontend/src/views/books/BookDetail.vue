<template>
  <Header />
  <BackgroundLayout2 />
  <div class="book-detail-container">
    <div class="book-detail-content">
      <img v-if="bookDetail.cover" :src="bookDetail.cover" alt="加载失败" class="book-cover" />
      <img v-else src="http://47.111.155.70/oss/default-cover.jpg" class="book-cover" />
      <div class="product-info">
        <h1>{{ bookDetail.title || '加载中...' }}</h1>
        <div class="book-meta">
          <p class="author">作者：{{ bookDetail.specifications[0]?.value || '暂无信息' }}</p>
          <p class="publisher">出版社：{{ bookDetail.specifications[5]?.value || '暂无信息' }}</p>
          <p class="publish-date">出版时间：{{ bookDetail.specifications[6]?.value || '暂无信息' }}</p>
          <!-- 添加标签信息 -->
          <p class="book-tags">
            标签：
            <span v-if="bookDetail.tags && bookDetail.tags[0]" class="tag-item">
              {{ bookDetail.tags[0] }}{{ bookDetail.tags[1] ? ' · ' + bookDetail.tags[1] : '' }}
            </span>
            <span v-else>暂无标签</span>
          </p>
        </div>

        <div class="price-section">
          <p>书价：<span class="current-price">¥{{ bookDetail.price.toFixed(2) || 0 }}</span></p>
        </div>

        <div class="rating-stock-container">
          <div class="rating-section">
            <p class="rating">评分：<img v-for="(star, index) in stars" :key="index" :src="star" class="star-image"></p>
          </div>

          <div class="stock-info" v-if="stockAmount > 0 && stockAmount < 100">
            <span class="stock-warning">⚠️ 仅剩 {{ stockAmount }} 件</span>
          </div>
        </div>

        <div class="action-buttons">
          <div class="quantity-selector">
            <button @click="decreaseQuantity" class="quantity-btn" :disabled="isAdmin || quantity <= 1">-</button>
            <!-- 将 span 替换为 input -->
            <input type="number" class="quantity-display" :value="quantity" @input="handleQuantityInput"
              @blur="validateQuantity" :disabled="isAdmin" min="1" :max="stockAmount" />
            <button @click="increaseQuantity" class="quantity-btn"
              :disabled="isAdmin || quantity >= stockAmount">+</button>
          </div>
          <button @click="addToCart" :disabled="isAdmin || stockAmount <= 0" class="add-to-cart-button">
            <span class="cart-icon">🛒</span> <!-- 购物车图标 -->
            {{ stockAmount <= 0 ? '暂无库存' : '加入购物车' }} </button>
              <button class="favorite-button" @click="toggleFavorite" :disabled="isAdmin">
                <span class="star-icon" :class="{ 'active': isFavorited }">★</span>
                {{ isFavorited ? '已收藏' : '收藏商品' }}</button>
        </div>
        <div class="action-buttons">
          <!-- ADMIN 角色显示的更改商品信息按钮 -->
          <el-button type='primary' v-if="isAdmin" @click="openEditModal" class="edit-product-button">更改商品信息</el-button>
          <!-- ADMIN 角色显示的修改商品库存按钮 -->
          <el-button type='primary' v-if="isAdmin" @click="openStockModal" class="edit-stock-button">修改商品库存</el-button>
          <!-- 添加修改标签按钮 -->
          <el-button type='primary' v-if="isAdmin" @click="openTagModal" class="edit-tag-button">修改标签</el-button>
        </div>
      </div>
    </div>

    <div class="product-tabs">
      <div class="tab-header">
        <div class="tab active">商品介绍</div>
      </div>

      <div class="tab-content">
        <div class="description-section">
          <h3>内容简介</h3>
          <p>{{ bookDetail.description || '暂无内容简介' }}</p>
          <h3>详细介绍</h3>
          <p>{{ bookDetail.detail || '暂无详细介绍' }}</p>
        </div>

        <div class="specs-content">
          <h3>规格说明</h3>
          <ul>
            <li v-for="(spec, index) in bookDetail.specifications || []" :key="index">
              <strong>{{ spec.item || '未知' }}：</strong>{{ spec.value || '暂无信息' }}
            </li>
          </ul>
        </div>
      </div>
    </div>


    <EditProductModal :visible="isEditModalVisible" :product="bookDetail" @update:visible="isEditModalVisible = $event"
      @product-updated="handleProductUpdated" />

    <StockModal :visible="isStockModalVisible" :product="bookDetail" @update:visible="isStockModalVisible = $event"
      @stock-updated="handleStockUpdated" />
  </div>
  <Footer />
  <!-- 在文件底部添加标签编辑弹窗 -->
  <el-dialog v-model="isTagModalVisible" title="修改商品标签" width="400px">
    <div class="tag-selection">
      <div class="tag-selection-row">
        <label>主类标签:</label>
        <el-select v-model="selectedMainTag" placeholder="请选择主类标签" @change="updateSubTagOptions">
          <el-option v-for="mainTag in mainTags" :key="mainTag" :label="mainTag" :value="mainTag">
          </el-option>
        </el-select>
      </div>

      <div class="tag-selection-row" v-if="selectedMainTag">
        <label>子类标签:</label>
        <el-select v-model="selectedSubTag" placeholder="请选择子类标签">
          <el-option v-for="subTag in availableSubTags" :key="subTag" :label="subTag" :value="subTag">
          </el-option>
        </el-select>
      </div>

      <div class="current-tag-info" v-if="bookDetail.tags && bookDetail.tags.length">
        <div class="current-tag-label">当前标签:</div>
        <div class="current-tag-value">
          <span class="main-tag-value">{{ bookDetail.tags[0] }}</span>
          <span v-if="bookDetail.tags[1]" class="sub-tag-value"> / {{ bookDetail.tags[1] }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="isTagModalVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProductTags" :disabled="!selectedMainTag">保存标签</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getProductById, getStockByProductId,updateProductTags} from '../../api/products'
import { addToCart as addToCartAPI, updateCartItem, getCart, type CartItem } from '../../api/cart'
import {
  isFavourite,
  addToFavourites,
  removeFromFavourites,
  type FavouriteItem,
  getFavourites
} from '../../api/favourites'
import { ElMessage } from 'element-plus'
import fullStar from '../../assets/full-star.png'
import halfStar from '../../assets/half-star.png'
import emptyStar from '../../assets/empty-star.png'
import tagsData from "../../assets/tags.json";
const stars = computed(() => {
  const rating = bookDetail.value.rate / 2
  const list = []

  for (let i = 0; i < 5; i++) {
    if (i < Math.floor(rating)) {
      list.push(fullStar)
    } else if (i === Math.floor(rating) && rating % 1 >= 0.5) {
      list.push(halfStar)
    } else {
      list.push(emptyStar)
    }
  }

  return list
})

// 收藏狀態
const isFavorited = ref(false)

// 檢查商品是否已收藏
const checkFavoriteStatus = async (productId: number) => {
  try {
    // 先檢查本地存儲
    const favouritesData = sessionStorage.getItem('favourites')
    if (favouritesData) {
      const favourites: FavouriteItem[] = JSON.parse(favouritesData)
      const found = favourites.some(item => item.id === productId)
      if (found) {
        isFavorited.value = true
        return
      }
    }

    // 本地未找到，調用API檢查
    const response = await isFavourite(productId)
    if (response.code.toString() === "200") {  // 保持字符串比较，因为后端返回的确实是"200"
      isFavorited.value = !!response.data  // 确保转换为布尔值
      // console.log('收藏状态检查结果:', isFavorited.value)
    }
  } catch (error) {
    console.error('检查状态失败', error)
  }
}

// 更新本地收藏數據
const updateLocalFavourites = (isAdd: boolean) => {
  if (!bookDetail.value) return

  // 獲取當前本地存儲的收藏數據
  const favouritesData = sessionStorage.getItem('favourites')
  let favourites: FavouriteItem[] = favouritesData ? JSON.parse(favouritesData) : []

  if (isAdd) {
    // 添加到收藏
    const newItem: FavouriteItem = {
      id: bookDetail.value.id,
      title: bookDetail.value.title,
      price: bookDetail.value.price,
      description: bookDetail.value.description || '',
      cover: bookDetail.value.cover || '',
      detail: bookDetail.value.detail || ''
    }

    // 檢查是否已存在
    const existingIndex = favourites.findIndex(item => item.id === newItem.id)
    if (existingIndex === -1) {
      favourites.push(newItem)
    }
  } else {
    // 從收藏中移除
    favourites = favourites.filter(item => item.id !== bookDetail.value.id)
  }

  // 保存回本地存儲
  sessionStorage.setItem('favourites', JSON.stringify(favourites))
}

// 切換收藏狀態
const toggleFavorite = async () => {
  if (!bookDetail.value.id) return

  try {
    const favouritesData = sessionStorage.getItem('favourites')
    if (!favouritesData && !isFavorited.value) {
      // 先获取完整的收藏列表
      // console.log('本地无收藏数据，从后端获取')
      const response = await getFavourites()
      if (response.code.toString() === "200") {
        // 保存到本地存储
        sessionStorage.setItem('favourites', JSON.stringify(response.data || []))
        // console.log('成功从后端获取收藏列表')
      }
    }
    if (isFavorited.value) {
      // 取消收藏
      const response = await removeFromFavourites(bookDetail.value.id)
      if (response.code.toString() === "200") {
        isFavorited.value = false
        // 更新本地存儲
        updateLocalFavourites(false)
        ElMessage.success('已取消收藏')
      } else {
        ElMessage.error('取消收藏失败: ' + response.message)
      }
    } else {
      // 添加收藏
      const response = await addToFavourites(bookDetail.value.id)
      // console.log(response)
      if (response.code.toString() === "200") {
        isFavorited.value = true
        // 更新本地存儲
        updateLocalFavourites(true)
        ElMessage.success('已加入收藏')
      } else {
        ElMessage.error('收藏失败: ' + response.message)
      }
    }
  } catch (error) {
    console.error('操作收藏失败', error)
    ElMessage.error('请求失败')
  }
}

// 从标签数据中提取主标签
const mainTags = computed(() => {
  const mainTagSet = new Set();
  tagsData.forEach(([main]) => {
    mainTagSet.add(main);
  });
  return Array.from(mainTagSet) as string[];
});

// 打开标签编辑弹窗
const openTagModal = () => {
  isTagModalVisible.value = true;

  // 如果有现有标签，预先选中
  if (bookDetail.value.tags && bookDetail.value.tags.length > 0) {
    selectedMainTag.value = bookDetail.value.tags[0];
    updateSubTagOptions();

    if (bookDetail.value.tags.length > 1) {
      selectedSubTag.value = bookDetail.value.tags[1];
    }
  }
};

// 更新子标签选项
const updateSubTagOptions = () => {
  if (!selectedMainTag.value) {
    availableSubTags.value = [];
    selectedSubTag.value = '';
    return;
  }

  const subTags: string[] = [];
  tagsData.forEach(([main, sub]) => {
    if (main === selectedMainTag.value) {
      subTags.push(sub);
    }
  });
  availableSubTags.value = subTags;

  // 如果之前选择的子标签不在新的可选列表中，清空选择
  if (selectedSubTag.value && !subTags.includes(selectedSubTag.value)) {
    selectedSubTag.value = '';
  }
};

// 保存商品标签
const saveProductTags = async () => {
  if (!selectedMainTag.value) {
    ElMessage.warning('请至少选择一个主类标签');
    return;
  }

  try {
    const tagArray: [string, string] = [
      selectedMainTag.value,
      selectedSubTag.value || ''
    ];

    const response = await updateProductTags(bookDetail.value.id, tagArray);

    if (response.data.code === '200') {
      ElMessage.success('标签更新成功');
      // 更新本地数据
      bookDetail.value.tags = tagArray;
      isTagModalVisible.value = false;
    } else {
      ElMessage.error(`标签更新失败: ${response.data.msg}`);
    }
  } catch (error) {
    console.error('更新标签请求失败', error);
    ElMessage.error('标签更新失败，请重试');
  }
};

const isEditModalVisible = ref(false)  // 弹窗显示状态
const openEditModal = () => {
  isEditModalVisible.value = true
  // console.log(isEditModalVisible.value)
}
const isTagModalVisible = ref(false);  // 标签修改弹窗状态
const selectedMainTag = ref('');        // 选中的主标签
const selectedSubTag = ref('');         // 选中的子标签
const availableSubTags = ref<string[]>([]); // 可选的子标签列表
// 库存修改弹窗状态
const isStockModalVisible = ref(false)
const openStockModal = () => {
  // console.log("打开库存修改弹窗")
  isStockModalVisible.value = true
}

const handleProductUpdated = (updatedProduct: any) => {
  // 创建一个ID映射，用于合并规格
  interface SpecificationWithId extends Specification {
    id?: number | string;
  }
  const mergedSpecifications: SpecificationWithId[] = [];

  // 创建一个规格ID到规格对象的映射
  const specMap = new Map();

  // 首先添加现有的规格项
  if (bookDetail.value.specifications && bookDetail.value.specifications.length > 0) {
    bookDetail.value.specifications.forEach(spec => {
      if (spec.id) {
        specMap.set(spec.id, { ...spec });
      } else {
        // 没有ID的规格项直接添加
        mergedSpecifications.push({ ...spec });
      }
    });
  }

  // 然后添加更新的规格项，覆盖相同ID的旧规格
  if (updatedProduct.specifications && updatedProduct.specifications.length > 0) {
    updatedProduct.specifications.forEach((spec: Specification) => {
      if (spec.id) {
        specMap.set(spec.id, { ...spec });
      } else {
        // 没有ID的规格项直接添加
        mergedSpecifications.push({ ...spec });
      }
    });
  }

  // 将映射转换为数组
  const mappedSpecs = Array.from(specMap.values());

  // 更新商品信息，包括合并后的规格
  bookDetail.value = {
    ...updatedProduct,
    specifications: [...mappedSpecs, ...mergedSpecifications]
  };

  // console.log('更新后的商品信息:', bookDetail.value);
}

// 保存实际库存数量
const stockAmount = ref(0);


// 从API或本地存储获取库存信息
const getProductStock = async (productId: number) => {
  try {
    // 从后端获取
    const res = await getStockByProductId(productId);
    let stockData;
    if (res.data.code === '200') {
      stockData = res.data.data;
      // console.log('从API获取库存信息:', stockData);
      // 保存到本地存储
    } else {
      console.error('获取商品库存信息失败：', res.data.msg);
      return 0;
    }

    // 返回库存数量
    return stockData ? stockData.amount : 0;
  } catch (error) {
    console.error('获取商品库存信息失败', error);
    return 0;
  }
}

// 处理库存更新后的回调
const handleStockUpdated = (updatedStock: any) => {
  // console.log('库存已更新:', updatedStock);
  // 更新当前商品的库存信息
  stockAmount.value = updatedStock.stock;

  // 如果当前选择的数量超过了更新后的库存，调整数量
  if (quantity.value > stockAmount.value) {
    quantity.value = stockAmount.value > 0 ? stockAmount.value : 1;
  }
}

const route = useRoute()  // 获取路由信息

// 定义规格类型
interface Specification {
  item: string;
  value: string;
  id?: number | string; // 添加可选的id属性
}

// 商品详情数据
const bookDetail = ref({
  id: 0,
  title: '',
  price: 0,
  rate: 0,
  description: '',
  cover: '',
  detail: '',
  stock: 0, // 添加库存字段
  specifications: [] as Specification[],
  tags: ['', ''] as [string, string] // 添加 tags 字段
})

// 判断当前用户是否是管理员
const role = sessionStorage.getItem('role')
const isAdmin = role === 'ADMIN'

// 购物车中的商品项
const cartItems = ref<CartItem[]>([]);

// 获取购物车信息
const fetchCartItems = async () => {
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
  } catch (error) {
    console.error('获取购物车请求失败:', error);
  }
};


// 获取商品详情
onMounted(async () => {
  window.scrollTo(0, 0);
  const bookId = Number(route.params.id) // 获取传递的商品 ID
  if (bookId) {
    await checkFavoriteStatus(bookId)
    // 尝试从sessionStorage获取products列表
    const productsJson = sessionStorage.getItem('products')

    if (productsJson) {
      try {
        // 解析存储的商品列表
        const products = JSON.parse(productsJson)

        // 查找ID匹配的商品
        const product = products.find((p: any) => p.id === bookId)

        if (product) {
          // 如果在本地存储中找到了商品数据，直接使用
          bookDetail.value = product
          // 获取最新库存信息
          stockAmount.value = await getProductStock(bookId);
          // 获取购物车信息
          await fetchCartItems();
          return
        }
      } catch (error) {
        console.error('解析sessionStorage中的products失败', error)
        // 解析失败时继续使用API获取
      }
    }

    // 如果从sessionStorage中没有找到数据，则回退到API请求
    await getProductFromAPI(bookId)
    await checkFavoriteStatus(bookId)
    // 获取最新库存信息
    stockAmount.value = await getProductStock(bookId);
    // 获取购物车信息
    await fetchCartItems();
  }
})

// 从API获取商品数据的方法（作为备选方案）
const getProductFromAPI = async (bookId: number) => {
  try {
    const res = await getProductById(bookId)
    if (res.data.code === '200') {
      bookDetail.value = res.data.data
      // 确保 tags 属性存在，如果后端没有返回则初始化为空数组
      if (!bookDetail.value.tags) {
        bookDetail.value.tags = ['', ''];
      }
    } else {
      console.error('商品详情加载失败', res.data.msg)
    }
  } catch (error) {
    console.error('请求失败', error)
  }
}

// 购物数量控制
const quantity = ref(1);

// These functions will be replaced by the enhanced versions with animation below

const addToCart = async () => {
  if (stockAmount.value <= 0) {
    ElMessage.warning('商品暂无库存');
    return;
  }

  if (quantity.value > stockAmount.value) {
    ElMessage.warning(`超出库存数量`);
    quantity.value = stockAmount.value;
    return;
  }

  try {
    // 在购物车中查找当前商品
    const existingCartItem = cartItems.value.find(item => item.productId === bookDetail.value.id);
    // console.log('购物车中存在商品？:', existingCartItem);
    if (existingCartItem) {
      // 如果商品已在购物车中，更新数量
      const newQuantity = existingCartItem.quantity + quantity.value;
      // console.log(newQuantity);
      if (newQuantity <= stockAmount.value) {
        const response = await updateCartItem(existingCartItem.cartItemId!, newQuantity);
        // console.log(response.code);
        if (response.code.toString() === '200') {
          // 后端返回成功，更新本地购物车信息
          existingCartItem.quantity = newQuantity;
          sessionStorage.setItem('cart', JSON.stringify(cartItems.value));
          ElMessage.success(`成功更新购物车商品数量`);
          quantity.value = 1;  // 重置数量
        } else {
          ElMessage.error(`更新购物车失败: ${response.message}`);
        }
      } else {
        ElMessage.warning(`您购物车中已经加入的该商品数量以及您当前加入购物车数量总和已经超过库存限制，添加失败`);
      }
    } else {
      // 如果商品不在购物车中，添加新商品
      const cartData: CartItem = {
        productId: bookDetail.value.id,
        quantity: quantity.value
      };

      const response = await addToCartAPI(cartData);
      // console.log(response);
      if (response && response.code.toString() === '200') {
        ElMessage.success(`成功添加到购物车`);
        // 将新商品添加到本地购物车中
        cartItems.value.push(response.data);
        // 同步更新到 sessionStorage
        sessionStorage.setItem('cart', JSON.stringify(cartItems.value));
        quantity.value = 1;  // 重置数量
      } else {
        ElMessage.error(`添加购物车失败: ${response.message}`);
      }
    }
  } catch (error) {
    console.error('购物车操作请求失败:', error);
    ElMessage.error('网络错误，购物车操作失败');
  }
}

// 修改 increaseQuantity 和 decreaseQuantity 方法，添加动画效果
const decreaseQuantity = () => {
  if (quantity.value > 1) {
    quantity.value--;
  }
}

const increaseQuantity = () => {
  if (quantity.value < stockAmount.value) {
    quantity.value++;
  }
}


// 添加到 script setup 部分

// 处理输入框数值变化
const handleQuantityInput = (event: any) => {
  let inputValue = parseInt(event.target.value);

  // 确保输入是有效的数字
  if (isNaN(inputValue)) {
    event.target.value = quantity.value;
    return;
  }

  // 边界检查：小于1则设为1，大于库存则设为最大库存量
  if (inputValue < 1) {
    inputValue = 1;
  } else if (inputValue > stockAmount.value) {
    inputValue = stockAmount.value;
    ElMessage.warning('超出库存量，已设为最大可购买数量');
  }

  // 更新数值并触发动画效果
  quantity.value = inputValue;
  event.target.value = inputValue;
};

// 在失焦时验证数量
const validateQuantity = (event: any) => {
  let inputValue = parseInt(event.target.value);

  // 确保输入是有效的数字
  if (isNaN(inputValue)) {
    event.target.value = quantity.value;
    return;
  }

  // 边界检查：小于1则设为1，大于库存则设为最大库存量
  if (inputValue < 1) {
    quantity.value = 1;
    event.target.value = 1;
  } else if (inputValue > stockAmount.value) {
    quantity.value = stockAmount.value;
    event.target.value = stockAmount.value;
    ElMessage.warning('超出库存量，已设为最大可购买数量');
  }
};

</script>

<style scoped>
/* 收藏按钮样式优化 */
.favorite-button {
  padding: 10px 20px;
  background-color: #fff;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 160px;
  /* 固定长度 */
  transition: all 0.3s ease;
  /* 添加过渡效果 */
  outline: none;
  /* 去除点击时的外边框 */
}

/* 鼠标悬停效果 */
.favorite-button:hover {
  background-color: #f9f3e0;
  /* 使用淡黄色背景，与星星颜色相协调 */
  /* transform: translateY(-2px); */
  border-color: #f39c12;
  /* 边框颜色变为星星颜色 */
}

/* 去除点击后的黑边 */
.favorite-button:focus,
.favorite-button:active {
  outline: none !important;
  box-shadow: none !important;
  border-color: #f39c12;
}

/* 星星图标样式 */
.star-icon {
  color: #f39c12;
  margin-right: 5px;
  font-size: 16px;
  transition: transform 0.2s ease;
}

/* 未收藏状态下的星星样式 */
.favorite-button:hover .star-icon:not(.active) {
  transform: scale(1.1);
}

/* 保持已有的动画效果 */
.favorite-button .star-icon.active {
  color: #f39c12;
  animation: bounce 0.5s;
}

@keyframes bounce {

  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.3);
  }
}

/* 禁用状态样式 */
.favorite-button:disabled {
  background-color: #dcdcdc;
  border-color: #dcdcdc;
  color: #999;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 禁用状态下的图标 */
.a.favorite-button:disabled .star-icon {
  opacity: 0.5;
}

/* 修改购物车按钮样式 */
.add-to-cart-button {
  padding: 10px 20px;
  background-color: #e74c3c;
  color: white;
  border: 1px solid #e74c3c;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  /* 使用flex布局 */
  align-items: center;
  /* 垂直居中 */
  justify-content: center;
  /* 水平居中 */
  width: 160px;
  /* 固定宽度，与收藏按钮一致 */
  transition: all 0.3s ease;
  /* 添加过渡效果 */
  outline: none;
  /* 去除点击时的外边框 */
}

/* 购物车图标样式 */
.cart-icon {
  margin-right: 5px;
  font-size: 16px;
  transition: transform 0.2s ease;
}

/* 鼠标悬停效果 */
.add-to-cart-button:hover {
  background-color: #c0392b;
  /* 深红色 */
  border-color: #c0392b;
  /* transform: translateY(-2px); */
  /* 轻微上移效果 */
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  /* 添加阴影 */
}

/* 鼠标悬停时图标效果 */
.add-to-cart-button:hover .cart-icon {
  transform: scale(1.1);
  /* 轻微放大效果 */
}

/* 去除点击后的黑边 */
.add-to-cart-button:focus,
.add-to-cart-button:active {
  outline: none !important;
  box-shadow: none !important;
  border-color: #c0392b;
}

/* 禁用状态样式 */
.add-to-cart-button:disabled {
  background-color: #dcdcdc;
  border-color: #dcdcdc;
  color: #999;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 禁用状态下的图标 */
.add-to-cart-button:disabled .cart-icon {
  opacity: 0.5;
}

/* 管理员按钮基础样式 */
.edit-product-button,
.edit-stock-button,
.edit-tag-button {
  width: 120px;
  padding: 10px 0;
  color: white;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s ease;
  border: none;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 更改商品信息按钮（绿色） */
.edit-product-button {
  background-color: #27ae60;
}

.edit-product-button:hover {
  background-color: #219952;
  /* transform: translateY(-2px); */
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15);
}

.edit-product-button:active {
  /* transform: translateY(0); */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 修改商品库存按钮（蓝色） */
.edit-stock-button {
  background-color: #3498db;
}

.edit-stock-button:hover {
  background-color: #2980b9;
  /* transform: translateY(-2px); */
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15);
}

.edit-stock-button:active {
  /* transform: translateY(0); */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 修改标签按钮（蓝色） */
.edit-tag-button {
  background-color: #3498db;
}

.edit-tag-button:hover {
  background-color: #2980b9;
  /* transform: translateY(-2px); */
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15);
}

.edit-tag-button:active {
  /* transform: translateY(0); */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.book-detail-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  margin-top: 100px;
  width: 1100px;
  background-color: #f5f5f5;
}

.book-detail-content {
  display: flex;
  justify-content: flex-start;
  flex-wrap: wrap;
  padding: 20px;
  width: 100%;
  background-color: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.book-cover {
  width: 320px;
  height: 420px;
  object-fit: cover;
  margin-right: 30px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.product-info {
  flex-grow: 1;
  max-width: 600px;
  text-align: left;
}

.product-info h1 {
  font-size: 24px;
  margin-bottom: 15px;
  color: #333;
}

/* 在<style scoped>部分添加 */
.book-tags {
  margin: 5px 0;
  color: #666;
}

.tag-item {
  display: inline-block;
  background-color: rgba(231, 76, 60, 0.1);
  /* 改为红色系，与网站主色调一致 */
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 14px;
  color: #e74c3c;
  /* 改为红色系，与加入购物车按钮一致 */
}

/* 书籍元数据项的统一样式 */
.book-meta p {
  margin: 8px 0;
  padding: 6px 0;
  font-size: 15px;
  color: #555;
  display: flex;
  align-items: center;
}

/* 元数据项加入轻微分隔 */
.book-meta p::before {
  content: "•";
  margin-right: 8px;
  color: #e74c3c;
  font-weight: bold;
}

/* 元数据值部分样式 */
.book-meta p span {
  font-weight: 500;
  color: #333;
  margin-left: 5px;
}

/* 确保现有的标签样式不受影响 */
.book-meta .book-tags::before {
  content: "•";
  margin-right: 8px;
  color: #e74c3c;
  font-weight: bold;
}

.price-section {
  margin-bottom: 20px;
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 5px;
}

.price-label {
  font-size: 14px;
  color: #999;
}

.original-price {
  text-decoration: line-through;
  color: #999;
  margin-right: 10px;
}

.current-price {
  color: #e74c3c;
  font-size: 24px;
  font-weight: bold;
}

.rating-section {
  margin-bottom: 20px;
}

.rating {
  font-size: 16px;
  /* color: #f39c12;*/
}

.star-image {
  width: 16px;
  height: 16px;
  display: inline-block;
  vertical-align: middle;
}

.action-buttons {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  align-items: center;
}

.add-to-cart-button {
  padding: 10px 25px;
  background-color: #e74c3c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.add-to-cart-button:disabled {
  background-color: #dcdcdc;
  cursor: not-allowed;
}

.favorite-button {
  padding: 10px 25px;
  background-color: #fff;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.star-icon {
  color: #f39c12;
  margin-right: 5px;
}

.product-tabs {
  width: 100%;
  background-color: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.tab-header {
  display: flex;
  border-bottom: 1px solid #eee;
}

.tab {
  padding: 15px 30px;
  cursor: pointer;
  font-size: 16px;
  color: #666;
}

.tab.active {
  color: #e74c3c;
  border-bottom: 2px solid #e74c3c;
}

.tab-content {
  padding: 20px;
  text-align: left;
}

.description-section {
  margin-bottom: 30px;
}

.description-section h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.description-section p {
  line-height: 1.6;
  color: #666;
}

.specs-content {
  margin-top: 20px;
}

.specs-content h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.specs-content ul {
  list-style-type: none;
  padding: 0;
}

.specs-content li {
  padding: 10px 0;
  border-bottom: 1px solid #eee;
  display: flex;
}

.specs-content li strong {
  width: 100px;
  color: #666;
}

.edit-product-button {
  /* padding: 10px 25px; */
  width: 120px;
  background-color: #27ae60;
  color: white;
  border-radius: 4px;
  font-size: 14px;
}

.edit-stock-button {
  background-color: #3498db;
  width: 120px;
  color: white;
  border-radius: 4px;
  font-size: 14px;
}

/* 评分和库存信息容器 */
.rating-stock-container {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 67px;
  margin-bottom: 4px;
  margin-top: -13px;
}

/* 修改评分部分样式 */
.rating-section {
  margin-bottom: 0;
}

/* 修改库存信息样式 */
.stock-info {
  margin: 0;
  /* margin-left: auto; */
}

/* 优化库存警告样式 */
.stock-warning {
  color: #e74c3c;
  font-weight: 500;
  background-color: rgba(231, 76, 60, 0.1);
  padding: 5px 10px;
  border-radius: 20px;
  display: inline-block;
  animation: pulse 2s infinite;
  font-size: 14px;
  box-shadow: 0 1px 3px rgba(231, 76, 60, 0.2);
}

/* 闪烁效果动画 */
@keyframes pulse {
  0% {
    opacity: 1;
  }

  50% {
    opacity: 0.7;
  }

  100% {
    opacity: 1;
  }
}

/* 美化数量选择器样式 */
.quantity-selector {
  display: flex;
  align-items: center;
  margin-right: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  background: white;
  height: 40px;
  min-width: 120px;
  transition: all 0.2s ease;
}

.quantity-selector:hover {
  border-color: #d0d0d0;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
}

.quantity-btn {
  width: 40px;
  height: 40px;
  background: white;
  border: none;
  font-size: 18px;
  font-weight: 500;
  color: #e74c3c;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  position: relative;
}

.quantity-btn::after {
  content: '';
  position: absolute;
  top: 10px;
  bottom: 10px;
  width: 1px;
  background-color: #eee;
}

.quantity-btn:first-child::after {
  right: 0;
}

.quantity-btn:last-child::after {
  left: 0;
}

.quantity-btn:disabled {
  color: #ddd;
  cursor: not-allowed;
  background: #fafafa;
}

.quantity-btn:hover:not(:disabled) {
  background-color: #fef8f8;
  color: #c0392b;
}

.quantity-btn:active:not(:disabled) {
  background-color: #fde7e7;
  transform: scale(0.97);
}

.quantity-btn:focus {
  outline: none;
}

/* 更新数量输入框样式 */
.quantity-display {
  min-width: 40px;
  flex-grow: 1;
  height: 38px;
  /* 略微减小以适应边框 */
  text-align: center;
  line-height: 38px;
  background: white;
  font-size: 16px;
  font-weight: 500;
  color: #333;
  border: none;
  outline: none;
  appearance: textfield;
  -webkit-appearance: textfield;
  -moz-appearance: textfield;
  /* 移除 Firefox 默认的上下箭头 */
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
  box-shadow: inset 0 0 0 1px #e74c3c20;
}

/* 禁用状态的输入框 */
.quantity-display:disabled {
  background-color: #fafafa;
  color: #999;
  cursor: not-allowed;
}

/* 添加微动画效果 */
/* @keyframes pulse2 {
  0% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.1);
  }

  100% {
    transform: scale(1);
  }
}

.quantity-display.changed {
  animation: pulse2 0.3s ease;
  color: #e74c3c;
} */

/* 修改标签修改按钮样式 */
.edit-tag-button {
  background-color: #3498db;
  /* 改为蓝色，与编辑库存按钮一致 */
  width: 120px;
  color: white;
  border-radius: 4px;
  font-size: 14px;
}

/* 标签选择区域样式 */
.tag-selection {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 10px 0;
}

.tag-selection-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tag-selection-row label {
  width: 80px;
  text-align: right;
  font-weight: bold;
  color: #606266;
}

.tag-selection-row .el-select {
  width: 100%;
}

/* 修改当前标签信息样式 */
.current-tag-info {
  margin-top: 20px;
  padding: 10px;
  background-color: #f8f8f8;
  border-radius: 4px;
  border-left: 3px solid #e74c3c;
  /* 改为红色，与网站主色调一致 */
}

.current-tag-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

.current-tag-value {
  font-size: 16px;
}

.main-tag-value {
  font-weight: bold;
  color: #333;
}

.sub-tag-value {
  color: #666;
}
</style>
