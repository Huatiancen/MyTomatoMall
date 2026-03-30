<template>
  <Header />
  <background-layout2 />
  <TagSidebar @filter="filterByTag" class="fixed-sidebar" />

  <div class="search-and-control-area">
    <Search v-model:value="searchQuery" @search="handleSearch" />
  </div>

  <div class="main-container">
    <div class="search-feedback" v-if="searchQuery">
      <p>搜索 "{{ searchQuery }}" 的结果: {{ filteredProducts.length }} 件商品</p>
      <el-button type="text" @click="clearSearch">清除</el-button>
    </div>

    <div class="content-wrapper">
      <div class="product-wrapper">
        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="5" animated />
        </div>
        <div v-if="paginatedProducts.length > 0" class="product-grid">
          <el-card v-for="product in paginatedProducts" :key="product.id" class="product-card"
            @click="goToBookDetail(product.id)">
            <div class="product-checkbox" @click.stop>
              <el-checkbox v-if="isAdmin" :model-value="product.selected"
                @change="(val: boolean) => toggleItemSelection(product.id, val)" />
            </div>
            <div class="image-container">
              <img v-if="product.cover" :src="product.cover" alt="加载失败" class="product-image" />
              <img v-else src="http://47.111.155.70/oss/default-cover.jpg" class="product-image" />
              <div v-if="product.status === 'INACTIVE'" class="sold-out-overlay">
                <div class="sold-out-text">
                  <div>下架</div>
                  <div>sold out</div>
                </div>
              </div>
            </div>
            <div class="product-price">
              <span>¥{{ product.price.toFixed(2) }}</span>
            </div>
            <div class="product-info">
              <h3 class="book-title">{{ product.title }}</h3>
              <p class="book-author">作者：{{ product.author }}</p>
            </div>
          </el-card>
        </div>

        <div v-else-if="!loading && paginatedProducts.length === 0" class="empty-state">
          <el-empty :image-size="200" description="暂无符合条件的商品"></el-empty>
        </div>
        <div class="pagination-wrapper">
          <el-pagination background layout="prev, pager, next, jumper" :page-size="pageSize" :current-page="currentPage"
            :total="filteredProducts.length" @current-change="goToPage" />
        </div>

      </div>
    </div>

    <div class="products-toolbar" v-if="isAdmin">
      <div class="toolbar-left">
        <el-checkbox :model-value="isAllSelected" @change="toggleAllSelection">全选</el-checkbox>
        <span class="selection-count">已选择 {{ selectedProducts.length }} 件商品</span>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="addProduct">添加商品</el-button>
        <el-button type="success" @click="batchPublish" :disabled="selectedProducts.length === 0">上架</el-button>
        <el-button type="warning" @click="batchUnpublish" :disabled="selectedProducts.length === 0">下架</el-button>
        <el-button type="danger" @click="batchDelete" :disabled="selectedProducts.length === 0">删除</el-button>
      </div>
    </div>
    <Footer />
  </div>
</template>



<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAllProducts, deleteProductById, publishProduct} from '../../api/products' // 引入products.ts中的方法
import { ElMessage, ElMessageBox } from 'element-plus'
// import BackgroundLayout2 from "../../components/BackgroundLayout2.vue";
// import TagSidebar from "../../components/TagSidebar.vue";
const currentPage = ref(1);
const loading = ref(true)
const pageSize = 15; // 每页显示的商品数量

const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredProducts.value.slice(start, start + pageSize);
});

const totalPages = computed(() => Math.ceil(filteredProducts.value.length / pageSize));

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
};
// 商品信息
interface Product {
  id: number
  title: string
  price: number
  cover: string
  author: string
  tags: string[] // 添加tags属性
  status: string
  selected: boolean
}

const products = ref<Product[]>([]) // 商品数据

const router = useRouter()
const isAdmin = ref(false) // 是否为管理员
const allProducts = ref<Product[]>([]); // 保存所有商品的原始列表
const selectedProducts = ref<number[]>([]); // 存储选中的商品ID
const searchQuery = ref('') // 当前搜索词
const tagFilteredProducts = ref<Product[]>([])

// 判断是否全选
const isAllSelected = computed(() => {
  if (products.value.length === 0) return false;
  return selectedProducts.value.length === products.value.length;
});

// 切换商品选中状态
const toggleItemSelection = (productId: number, isSelected: boolean) => {
  // 查找商品
  const product = products.value.find(p => p.id === productId);
  if (!product) return;

  // 更新商品选中状态
  product.selected = isSelected;

  // 更新选中商品ID数组
  if (isSelected) {
    if (!selectedProducts.value.includes(productId)) {
      selectedProducts.value.push(productId);
    }
  } else {
    const index = selectedProducts.value.indexOf(productId);
    if (index !== -1) {
      selectedProducts.value.splice(index, 1);
    }
  }
};

// 全选/取消全选
const toggleAllSelection = (value: boolean) => {
  if (value) {
    selectedProducts.value = products.value.map(product => product.id);
    // 更新所有商品的选中状态
    products.value.forEach(product => product.selected = true);
  } else {
    selectedProducts.value = [];
    // 取消所有商品的选中状态
    products.value.forEach(product => product.selected = false);
  }
};

// 批量上架商品
const batchPublish = async () => {
  if (selectedProducts.value.length === 0) {
    ElMessage.warning('请先选择要上架的商品');
    return;
  }

  // 筛选出状态为 INACTIVE 的商品
  const inactiveProducts = products.value.filter(
    product => product.selected && product.status === 'INACTIVE'
  );

  if (inactiveProducts.length === 0) {
    ElMessage.warning('选中的商品中没有可上架的商品（已下架的商品才能上架）');
    return;
  }

  try {
    ElMessageBox.confirm(`确定要上架所选的${inactiveProducts.length}件商品吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      let successCount = 0;

      // 对每个处于下架状态的商品调用上架API
      for (const product of inactiveProducts) {
        try {
          const response = await publishProduct(product.id);
          if (response && response.data && response.data.code === '200') {
            successCount++;
            // 更新本地状态
            const targetProduct = products.value.find(p => p.id === product.id);
            if (targetProduct) {
              targetProduct.status = 'ACTIVE';
            }
          }
        } catch (err) {
          console.error(`上架商品 ${product.id} 失败:`, err);
        }
      }

      if (successCount > 0) {
        ElMessage.success(`成功上架${successCount}件商品`);

        // 更新 sessionStorage 中的数据
        const storageProducts = JSON.parse(sessionStorage.getItem("products") || "[]");
        inactiveProducts.forEach(product => {
          const storageProduct = storageProducts.find((item: any) => item.id === product.id);
          if (storageProduct) {
            storageProduct.status = 'ACTIVE';
          }
        });
        sessionStorage.setItem("products", JSON.stringify(storageProducts));
      } else {
        ElMessage.error('没有商品成功上架，请重试');
      }

      // 清空选中商品
      selectedProducts.value = [];
      products.value.forEach(product => product.selected = false);
    });
  } catch (error) {
    console.error('上架商品时出错:', error);
    ElMessage.error('上架操作失败，请重试');
  }
};

// 批量下架商品
const batchUnpublish = async () => {
  if (selectedProducts.value.length === 0) {
    ElMessage.warning('请先选择要下架的商品');
    return;
  }

  // 筛选出状态为 ACTIVE 的商品
  const inactiveProducts = products.value.filter(
    product => product.selected && product.status === 'ACTIVE'
  );

  if (inactiveProducts.length === 0) {
    ElMessage.warning('选中的商品中没有可下架的商品（已上架的商品才能下架）');
    return;
  }

  try {
    ElMessageBox.confirm(`确定要下架所选的${inactiveProducts.length}件商品吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      let successCount = 0;

      // 对每个处于下架状态的商品调用上架API
      for (const product of inactiveProducts) {
        try {
          const response = await publishProduct(product.id);
          if (response && response.data && response.data.code === '200') {
            successCount++;
            // 更新本地状态
            const targetProduct = products.value.find(p => p.id === product.id);
            if (targetProduct) {
              targetProduct.status = 'INACTIVE';
            }
          }
        } catch (err) {
          console.error(`下架商品 ${product.id} 失败:`, err);
        }
      }

      if (successCount > 0) {
        ElMessage.success(`成功下架${successCount}件商品`);

        // 更新 sessionStorage 中的数据
        const storageProducts = JSON.parse(sessionStorage.getItem("products") || "[]");
        inactiveProducts.forEach(product => {
          const storageProduct = storageProducts.find((item: any) => item.id === product.id);
          if (storageProduct) {
            storageProduct.status = 'INACTIVE';
          }
        });
        sessionStorage.setItem("products", JSON.stringify(storageProducts));
      } else {
        ElMessage.error('没有商品成功下架，请重试');
      }

      // 清空选中商品
      selectedProducts.value = [];
      products.value.forEach(product => product.selected = false);
    });
  } catch (error) {
    console.error('下架商品时出错:', error);
    ElMessage.error('下架操作失败，请重试');
  }
};

// 批量删除商品
const batchDelete = async () => {
  if (selectedProducts.value.length === 0) {
    ElMessage.warning('请先选择要删除的商品');
    return;
  }

  try {
    ElMessageBox.confirm(`确定要删除所选的${selectedProducts.value.length}件商品吗？此操作不可撤销。`, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      // 批量删除选中的商品
      for (const productId of selectedProducts.value) {
        await deleteProductById(productId);

        // 直接在本地更新数据
        // 从三个数据源中移除已删除的商品
        products.value = products.value.filter(product => product.id !== productId);
        allProducts.value = allProducts.value.filter(product => product.id !== productId);
        tagFilteredProducts.value = tagFilteredProducts.value.filter(product => product.id !== productId);

        // 更新sessionStorage中的数据
        const storageProducts = JSON.parse(sessionStorage.getItem("products") || "[]");
        const updatedStorageProducts = storageProducts.filter((item: any) => item.id !== productId);
        sessionStorage.setItem("products", JSON.stringify(updatedStorageProducts));
      }

      ElMessage.success(`成功删除${selectedProducts.value.length}件商品`);

      // 不再重新获取所有商品数据
      // await fetchProducts(); // 删除这行

      // 刷新可能需要的计算属性
      currentPage.value = 1; // 回到第一页，避免分页问题
      selectedProducts.value = []; // 清空选中列表

      // 应用当前过滤条件
      applyFilters();
    });
  } catch (error) {
    console.error('删除商品时出错:', error);
    ElMessage.error('删除操作失败，请重试');
  }
};

const handleSearch = (query: string) => {
  searchQuery.value = query
  applyFilters() // 应用过滤条件
}

// 修改标签筛选函数
const filterByTag = (tags: string[]) => {
  // console.log('筛选标签:', tags)

  if (!tags || tags.length === 0) {
    // 如果没有选择标签，显示所有商品
    tagFilteredProducts.value = [...allProducts.value]
  } else {
    // 根据标签筛选
    const [mainTag, subTag] = tags

    tagFilteredProducts.value = allProducts.value.filter(product => {
      if (!product.tags) return false

      if (subTag) {
        // 如果有子标签，则主标签和子标签都需要匹配
        return product.tags[0] === mainTag && product.tags[1] === subTag
      } else {
        // 否则只需要匹配主标签
        return product.tags[0] === mainTag
      }
    })
  }

  applyFilters() // 应用搜索和标签的组合过滤
}

// 应用所有过滤条件（标签和搜索）
const applyFilters = () => {
  if (!searchQuery.value) {
    // 如果没有搜索词，只应用标签过滤
    products.value = [...tagFilteredProducts.value]
    return
  }

  // 将搜索词和标签过滤组合使用
  const query = searchQuery.value.toLowerCase()
  products.value = tagFilteredProducts.value.filter(product =>
    product.title.toLowerCase().includes(query) ||
    product.author.toLowerCase().includes(query)
  )
}

// 清除搜索
const clearSearch = () => {
  searchQuery.value = '';
  applyFilters();
}

// 修改 fetchProducts 方法，优先从sessionStorage读取数据
const fetchProducts = async () => {
  loading.value = true
  try {
    // 首先尝试从 sessionStorage 获取数据
    const cachedData = sessionStorage.getItem("products")

    let productsData = []
    let fromCache = false

    if (cachedData) {
      try {
        productsData = JSON.parse(cachedData)
        fromCache = true
        // console.log('从缓存加载了', productsData.length, '件商品')
      } catch (e) {
        console.error('解析缓存数据失败:', e)
        // 如果缓存解析失败，则继续从API获取
        fromCache = false
      }
    }

    // 如果没有缓存或解析失败，则从API获取
    if (!fromCache) {
      const res = await getAllProducts()
      // console.log('从API获取商品数据')

      if (res.data.code === '200') {
        productsData = res.data.data
        // 更新缓存
        sessionStorage.setItem("products", JSON.stringify(productsData))
      } else {
        console.error('商品加载失败', res.data.msg)
        return
      }
    }

    // 创建产品对象列表
    const productsList: Product[] = []

    productsData.forEach((item: any) => {
      productsList.push({
        id: item.id,
        author: item.specifications[0]?.value || '未知',
        cover: item.cover,
        title: item.title,
        price: item.price,
        tags: item.tags || ['', ''], // 确保tags属性存在
        status: item.status,
        selected: false
      })
    })

    // 保存所有商品和当前显示商品
    allProducts.value = productsList
    products.value = productsList
    tagFilteredProducts.value = productsList // 初始化标签过滤后的商品列表

    // 如果是从API获取的，已经在前面更新了sessionStorage
  } catch (error) {
    console.error('请求失败', error)
  } finally {
    loading.value = false // 无论成功失败都结束加载状态
  }
}

const filteredProducts = computed(() => {
  // 管理员可以看到所有商品，普通用户只能看到上架商品
  if (isAdmin.value) {
    return products.value;
  } else {
    return products.value.filter(product => product.status === 'ACTIVE');
  }
});

// 判断用户是否为管理员
onMounted(() => {
  const role = sessionStorage.getItem('role')
  isAdmin.value = role === 'ADMIN'
  fetchProducts()
})

// 跳转到商品详情页面
const goToBookDetail = (id: number) => {
  router.push((`/BookDetail/${id}`)) // 使用命名路由和 params
}

// 添加商品的方法
const addProduct = () => {
  // 这里添加跳转到添加商品页面的逻辑
  // console.log('添加商品')
  router.push('/addProduct')
}
</script>

<style scoped>

.main-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: relative;
  padding-right: 20px;
  margin-left: 160px; /* 为侧边栏留出空间 */
  max-width: calc(100% - 160px); /* 确保不会超出屏幕 */
}

/* 搜索栏和控制区域 */
.search-and-control-area {
  margin-top: 5%;
  margin-left: 80px;
  width: 100%;
  height: 40px;
  position: relative;
  margin-bottom: 20px;
  /* 增加底部边距，为搜索结果留出空间 */
}

/* 搜索结果反馈信息 */
.search-feedback {
  margin-top: 20px;
  /* 增加顶部边距，避免与搜索框重叠 */
  padding: 8px 15px;
  background-color: #f6f8fa;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  /* 添加相对定位 */
  z-index: 1;
  /* 确保层级正确 */
}

/* 内容区包裹侧边栏和商品展示区 */
.content-wrapper {
  display: flex;
  width: 100%;
  margin-top: 20px;
  gap: 20px;
}

/* 标签侧边栏容器 */
.tag-sidebar-container {
  flex-shrink: 0;
}

/* 商品展示区域 */
.product-wrapper {
  flex-grow: 1;
  background-color: white;
  min-width: 1060px;
  border-radius: 10px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  padding: 20px;
  overflow-x: hidden; /* 防止水平滚动 */
  min-height: 650px;
}

/* 添加加载状态的样式 */
.loading-state {
  padding: 20px;
  min-height: 650px;
}

/* 添加商品按钮 */
.add-product-button {
  position: absolute;
  right: 0;
  top: 70%;
  transform: translateY(-50%);
  z-index: 10;
  background-color: #e74c3c;
  color: white;
  border: none;
}

/* 商品网格布局 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);  /* 固定5列布局 */
  grid-template-rows: auto;
  gap: 20px;
  align-items: start;
}

/* 商品卡片样式 */
.product-card {
  width: 100%;
  height: 320px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-sizing: border-box;
  cursor: pointer;
  position: relative;
  padding: 10px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  /* 初始边框透明 */
}

/* 添加选中效果 */
.product-card:hover {
  border: 1px solid #e74c3c;
  /* 边框颜色变化 */
  box-shadow: 0 0 10px rgba(231, 76, 60, 0.2);
  /* 轻微阴影 */
}

/* 图片容器设置相对定位，作为遮罩的参考点 */
.image-container {
  position: relative;
  width: 100%;
  height: 180px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 5px;
  overflow: hidden;
}

/* 下架商品遮罩层样式 */
.sold-out-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  /* 半透明灰黑色背景 */
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 5px;
  z-index: 5;
}

/* 下架文本样式 */
.sold-out-text {
  text-align: center;
  color: white;
  font-weight: bold;
  font-size: 24px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  /* 文字阴影效果 */
  letter-spacing: 2px;
}

/* 英文文本稍小 */
.sold-out-text div:nth-child(2) {
  font-size: 20px;
  margin-top: 5px;
  font-weight: normal;
}

/* 书籍封面 */
.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 价格信息 */
.product-price {
  text-align: center;
  margin-top: 10px;
  color: #e74c3c;
  font-size: 18px;
  font-weight: bold;
}

.product-info {
  margin-top: -15px;
}

.book-title {
  font-size: 16px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-author {
  font-size: 14px;
  margin-top: -10px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 操作按钮 */
.product-actions {
  display: flex;
  justify-content: space-between;
  margin-top: -5px;
}

/* 商品选中复选框样式 */
.product-checkbox {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 20;
}

/* 工具栏样式 */
.products-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: rgba(240, 240, 240, 0.95);
  padding: 15px 25px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
  margin-top: 20px;
  margin-bottom: 20px;
  width: 1050px;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

.selection-count {
  margin-left: 20px;
  color: #666;
}

/* 调整删除按钮的位置，防止与复选框重叠 */
.delete-button {
  position: absolute;
  top: 5px;
  right: 5px;
  padding: 2px 6px;
  background-color: transparent;
  border: none;
  color: #ff4d4f;
  font-size: 12px;
  font-weight: bold;
  z-index: 10;
  cursor: pointer;
}

/* 空状态样式 */
.empty-state {
  min-height: 650px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: white;
  /* border-radius: 10px; */
  /* box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); */
  margin: 0;
  box-sizing: border-box;
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

.pagination-wrapper {
  position: absolute;
  bottom: 20px;
  left: 0;
  width: 100%;
  display: flex;
  justify-content: center;
}

.product-wrapper {
  position: relative;
  padding-bottom: 90px; /* 留出底部空间给分页器 */
}
</style>
