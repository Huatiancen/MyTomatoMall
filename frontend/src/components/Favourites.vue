<template>
  <div class="product-wrapper">

    <div v-if="loading">
      <div class="loading-animation">
        <el-skeleton :rows="3" animated />
      </div>
      <p class="loading-tip">正在加载您的收藏信息...</p>
    </div>

    <el-table v-else :data="favouriteItems" style="width: 100%" :height="630" :row-class-name="tableRowClassName">
      <!-- 选中按钮 -->
      <el-table-column label="选中" width="80">
        <template #default="scope">
          <el-checkbox :model-value="isItemSelected(scope.row.id)"
            @change="toggleItemSelection(scope.row.id)"></el-checkbox>
        </template>
      </el-table-column>

      <!-- 商品封面展示 -->
      <el-table-column prop="cover" label="封面" width="120">
        <template #default="scope">
          <div class="image-container">
            <img v-if="scope.row.cover" :src="scope.row.cover" alt="封面" class="product-image" />
            <img v-else src="http://47.111.155.70/oss/default-cover.jpg" class="product-image">
            <!-- 添加下架商品遮罩 -->
            <div v-if="scope.row.status === 'INACTIVE'" class="sold-out-overlay">
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
          <p :class="['product-price', { 'inactive-price': scope.row.status === 'INACTIVE' }]">
            ¥{{ scope.row.price.toFixed(2) || 0 }}
          </p>
        </template>
      </el-table-column>

      <!-- 描述展示 -->
      <el-table-column label="描述">
        <template #default="scope">
          <p :class="['product-description', { 'inactive-description': scope.row.status === 'INACTIVE' }]">
            {{ scope.row.description || '暂无描述' }}
          </p>
        </template>
      </el-table-column>

      <!-- 操作按钮 -->
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <div class="button-container">
            <el-button type="primary" size="small" @click="viewProductDetail(scope.row.id)"
              :disabled="scope.row.status === 'INACTIVE'">详情</el-button>
            <el-button type="danger" size="small" @click="removeFromFavouriteHandler(scope.row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加分割线 -->
    <div class="divider"></div>

    <!-- 底部操作栏 -->
    <div class="cart-summary">
      <div class="summary-content">
        <div class="selection-bar">
          <el-checkbox :model-value="isAllSelected" @change="toggleAllSelection">全选</el-checkbox>
          <el-button class="batch-btn" type="danger" @click="batchDelete"
            :disabled="selectedItems.length === 0">批量删除</el-button>
          <span class="selection-count">已选择{{ selectedItems.length }}件商品</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { type FavouriteItem, getFavourites, removeFromFavourites } from '../api/favourites';

const router = useRouter();
// 收藏商品数据
const favouriteItems = ref<FavouriteItem[]>([]);
// 选中的收藏商品ID数组
const selectedItems = ref<number[]>([]);
const loading = ref(false);

// 判断商品是否被选中
const isItemSelected = (productId: number): boolean => {
  return selectedItems.value.includes(productId);
};

const tableRowClassName = ({ row }: { row: any }) => {
  if (row.status === 'INACTIVE') {
    return 'inactive-row';
  }
  return '';
};

// 切换商品选中状态
const toggleItemSelection = (productId: number) => {
  const index = selectedItems.value.indexOf(productId);
  if (index === -1) {
    // 如果不在数组中，添加它
    selectedItems.value.push(productId);
  } else {
    // 如果已在数组中，移除它
    selectedItems.value.splice(index, 1);
  }
};

// 全选状态
const isAllSelected = computed(() => {
  if (favouriteItems.value.length === 0) return false;
  return selectedItems.value.length === favouriteItems.value.length;
});

// 切换全选状态
const toggleAllSelection = (value: boolean) => {
  if (value) {
    selectedItems.value = favouriteItems.value.map(item => item.id);
  } else {
    selectedItems.value = [];
  }
};

// 保存收藏数据到本地存储
const saveFavouritesToSessionStorage = (data: FavouriteItem[]) => {
  sessionStorage.setItem("favourites", JSON.stringify(data));
};

// 获取收藏夹信息
const fetchFavouriteItems = async () => {
  loading.value = true
  try {
    // 尝试从 sessionStorage 获取收藏夹数据
    const favouritesData = sessionStorage.getItem('favourites');
    if (favouritesData) {
      // 如果本地存储有收藏夹数据，直接使用
      favouriteItems.value = JSON.parse(favouritesData);
      // console.log('从本地存储获取收藏夹信息:', favouriteItems.value);
    } else {
      // 如果没有数据，调用后端 API 获取
      const response = await getFavourites();
      // console.log('从后端获取收藏夹信息response:', response);
      if (response.code.toString() === "200") {
        favouriteItems.value = response.data || [];
        // console.log('从后端获取收藏夹信息:', favouriteItems.value);
        // 将获取的收藏夹信息存储到 sessionStorage
        sessionStorage.setItem('favourites', JSON.stringify(favouriteItems.value));
      } else {
        console.error('获取收藏夹信息失败:', response.message);
      }
    }
  } catch (error) {
    console.error('获取收藏夹请求失败:', error);
  } finally {
    loading.value = false
  }
};

// 删除收藏商品
const removeFromFavouriteHandler = async (productId: number) => {
  try {
    // 弹窗确认删除
    await ElMessageBox.confirm("确定要删除该收藏商品吗？", "提示", {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    // 用户点击确认按钮后执行删除逻辑
    const response = await removeFromFavourites(productId);
    if (response.code.toString() === "200") {
      // 成功删除，更新本地数据
      const updatedItems = favouriteItems.value.filter(item => item.id !== productId);
      favouriteItems.value = updatedItems;

      // 更新本地存储
      saveFavouritesToSessionStorage(updatedItems);

      // 如果该商品在选中列表中，也需要移除
      const selectedIndex = selectedItems.value.indexOf(productId);
      if (selectedIndex !== -1) {
        selectedItems.value.splice(selectedIndex, 1);
      }

      ElMessage.success('已取消收藏');
    } else {
      ElMessage.error('取消收藏失败: ' + response.message);
    }
  } catch (error) {
    // 用户点击取消按钮或发生错误时
    if (error !== 'cancel') {
      console.error('删除收藏商品失败', error);
    }
  }
};

// 批量删除选中的商品
const batchDelete = () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要删除的商品');
    return;
  }

  ElMessageBox.confirm(
      `确定要删除${selectedItems.value.length}件收藏商品吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    // 批量删除
    for (const productId of selectedItems.value) {
      const response = await removeFromFavourites(productId);
      if (response.code.toString() === "200") {
        // 成功删除，更新本地数据
        const updatedItems = favouriteItems.value.filter(item => item.id !== productId);
        favouriteItems.value = updatedItems;

        // 更新本地存储
        saveFavouritesToSessionStorage(updatedItems);
      }
    }
    // 更新选中状态
    selectedItems.value = [];
    ElMessage.success('已批量取消收藏');
  }).catch(() => {
    // 取消删除
  });
};

// 查看商品详情
const viewProductDetail = (productId: number) => {
  router.push({
    path: `/BookDetail/${productId}`
  });
};

onMounted(() => {
  fetchFavouriteItems();
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

.product-image {
  width: 80px;
  height: 100px;
  object-fit: cover;
  display: block;
  margin: 0 auto;
}

/* 添加到 <style scoped> 部分 */
.image-container {
  position: relative;
  width: 80px;
  height: 100px;
  margin: 0 auto;
}

/* 下架商品遮罩样式 */
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
:deep(.inactive-row) {
  background-color: #f9f9f9;
  color: #999;
}

.product-name, .product-price {
  font-size: 18px;
  font-weight: bold;
}

.product-price {
  color: #e74c3c;
}

.product-description {
  font-size: 14px;
  color: #666;
  text-overflow: ellipsis;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
}

.button-container {
  display: flex;
  align-items: center;
  gap: 10px;
  /* 添加按钮之间的间距 */
  justify-content: flex-start;
  /* 居中按钮 */
}

.divider {
  height: 1px;
  background-color: #e0e0e0;
  margin: 10px 0;
}

/* 底部操作栏 */
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

/* 添加下架商品价格样式 - 暗淡效果 */
.inactive-price {
  color: #999 !important;
  text-decoration: line-through;
  opacity: 0.7;
  /* 降低不透明度 */
}

/* 添加下架商品价格样式 - 暗淡效果 */
.inactive-description {
  color: #999 !important;
  opacity: 0.8;
  /* 降低不透明度 */
}
</style>