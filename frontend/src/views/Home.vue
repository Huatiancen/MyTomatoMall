<template>
  <Header />
  <BackgroundLayout2 />
  <div class="home-container">
    <!-- 广告轮播 -->
    <div class="promo-banner-container">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="promobanners.length === 0" class="empty-state">
        <el-icon>
          <Picture />
        </el-icon>
        <span>暂无广告信息</span>
        <el-button v-if="isAdmin" type="primary" @click="showAddDialog">添加广告</el-button>
      </div>

      <el-carousel v-else class="promo-carousel" height="400px" :interval="5000" arrow="hover"
        indicator-position="outside" :autoplay="true">
        <el-carousel-item v-for="banner in promobanners" :key="banner.id">
          <div class="banner-content" @click="handleBannerClick(banner)">
            <!-- 管理员操作按钮组 -->
            <div v-if="isAdmin" class="admin-buttons">
              <el-button type="primary" circle size="small" title="编辑广告" @click.stop="handleEditBanner(banner)">
                <el-icon>
                  <Edit />
                </el-icon>
              </el-button>
              <el-button type="danger" circle size="small" title="删除广告" @click.stop="handleDeleteBanner(banner.id)">
                <el-icon>
                  <Delete />
                </el-icon>
              </el-button>
            </div>
            <div class="banner-text">
              <h1 class="banner-title">{{ banner.title }}</h1>
              <p class="banner-content-text">{{ banner.content }}</p>
            </div>
            <div class="banner-image">
              <img :src="banner.imgUrl" :alt="banner.title" @error="handleImageError">
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>

      <!-- 管理员添加广告按钮 -->
      <div v-if="isAdmin" class="add-banner-button">
        <el-button type="primary" circle @click="showAddDialog" title="添加广告">
          <el-icon>
            <Plus />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- 榜单区域 - 改为走马灯下方 -->
    <div class="ranks-section">
      <div class="rank-section">
        <div class="section-title-container">
          <div class="section-title-icon">
            <el-icon>
              <Star />
            </el-icon>
            <!-- <i class="el-icon-hot-water"></i> -->
          </div>
          <div class="section-title-content">
            <h2 class="section-title">为您精选</h2>
            <div class="section-subtitle">用心推荐 · 爱心之选</div>
          </div>
        </div>
        <Rank title="热销榜" :fetchFunction="getRecommendData" :maxRows="10" class="rank-grid" />
      </div>

      <div class="rank-section">
        <div class="section-title-container">
          <div class="section-title-icon">
            <el-icon>
              <Trophy />
            </el-icon>
            <!-- <i class="el-icon-hot-water"></i> -->
          </div>
          <div class="section-title-content">
            <h2 class="section-title">热销榜</h2>
            <div class="section-subtitle">实时更新 · 人气之选</div>
          </div>
        </div>
        <Rank title="热销榜" :fetchFunction="getHotRankData" :maxRows="10" class="rank-grid" />
      </div>

      <!-- 上新榜 -->
      <div class="rank-section">
        <div class="section-title-container">
          <div class="section-title-icon">
            <el-icon>
              <Calendar />
            </el-icon>
          </div>
          <div class="section-title-content">
            <h2 class="section-title">上新榜</h2>
            <div class="section-subtitle">新品速递 · 优先体验</div>
          </div>
        </div>
        <Rank title="上新榜" :fetchFunction="getNewRankData" :maxRows="10" class="rank-grid" />
      </div>
    </div>
  </div>

  <Footer />

  <!-- 添加广告弹窗组件 -->
  <AddAdvertisement v-model:visible="addDialogVisible" @added="handleAdvertisementAdded"
    @close="addDialogVisible = false" />

  <!-- 编辑广告弹窗组件 -->
  <EditAdvertisementModal v-model:visible="editDialogVisible" :advertisement="currentEditingBanner"
    @updated="handleAdvertisementUpdated" @close="editDialogVisible = false" />
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit, Picture, Plus } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import Header from "../components/Header.vue";
import Footer from "../components/Footer.vue";
import BackgroundLayout2 from "../components/BackgroundLayout2.vue";
import AddAdvertisement from "../components/AddAdvertisement.vue";
import EditAdvertisementModal from "../components/EditAdvertisementModal.vue";
// import defaultImage from "../assets/default-cover.jpg";
import { getAdvertisements, deleteAdvertisement } from "../api/advertisement";
import Rank from '../components/Rank.vue'
import { getTopProductsByRecommend, getTopProductsByCreatedTime, getTopProductsBySales } from '../api/products'
import { Calendar, Star, Trophy } from '@element-plus/icons-vue';

const router = useRouter();
const loading = ref(true);
const promobanners = ref([]);
const addDialogVisible = ref(false);
const editDialogVisible = ref(false);
const currentEditingBanner = ref({});

// 判断当前用户是否是管理员
const role = sessionStorage.getItem('role')
const isAdmin = role === 'ADMIN'

// 创建包装函数用于榜单
const getRecommendData = () => {
  return getTopProductsByRecommend(10)
}

const getHotRankData = () => {
  return getTopProductsBySales(10)
}

const getNewRankData = () => {
  return getTopProductsByCreatedTime(10)
}

// 获取广告数据
const fetchPromoBanners = async () => {
  loading.value = true;
  try {
    const res = await getAdvertisements();

    if (res && res.code === '200') {
      // console.log('获取广告数据成功:', res.data);

      promobanners.value = res.data.map(ad => ({
        id: ad.id,
        title: ad.title || '精选活动',
        content: ad.content || '暂无内容',
        imgUrl: ad.imgUrl,
        productId: ad.productId
      }));

      sessionStorage.setItem('promobanners', JSON.stringify(promobanners.value));
    } else {
      console.error('获取广告数据失败:', res?.msg || '未知错误');

      const cachedBanners = sessionStorage.getItem('promobanners');
      if (cachedBanners) {
        promobanners.value = JSON.parse(cachedBanners);
        ElMessage.info('使用缓存的广告数据');
      }
    }
  } catch (error) {
    console.error('请求广告接口错误:', error);

    const cachedBanners = sessionStorage.getItem('promobanners');
    if (cachedBanners) {
      promobanners.value = JSON.parse(cachedBanners);
      ElMessage.info('使用缓存的广告数据');
    }
  } finally {
    loading.value = false;
  }
};

// 图片错误处理
const handleImageError = (event) => {
  event.target.src = defaultImage;
  // console.log('图片加载失败，已替换为默认图片');
};

// 点击广告处理
const handleBannerClick = (banner) => {
  if (banner.productId) {
    router.push(`/BookDetail/${banner.productId}`);
  } else {
    // 可以跳转到一个活动页面或其他目标
    // console.log('点击了广告:', banner.title);
  }
};

// 管理员功能：删除广告
const handleDeleteBanner = async (id) => {
  if (!isAdmin) return;

  try {
    await ElMessageBox.confirm('确定要删除此广告吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    const res = await deleteAdvertisement(id);

    if (res && res.code === '200') {
      ElMessage.success('删除广告成功');
      promobanners.value = promobanners.value.filter(item => item.id !== id);
      sessionStorage.setItem('promobanners', JSON.stringify(promobanners.value));
    } else {
      ElMessage.error('删除广告失败: ' + (res?.msg || '未知错误'));
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除广告出错:', error);
      ElMessage.error('操作失败，请重试');
    }
  }
};

// 管理员功能：编辑广告
const handleEditBanner = (banner) => {
  if (!isAdmin) return;
  currentEditingBanner.value = { ...banner };
  editDialogVisible.value = true;
};

// 广告更新成功后的处理
const handleAdvertisementUpdated = (updatedBanner) => {
  const index = promobanners.value.findIndex(item => item.id === updatedBanner.id);
  if (index !== -1) {
    promobanners.value[index] = { ...updatedBanner };
    sessionStorage.setItem('promobanners', JSON.stringify(promobanners.value));
    ElMessage.success('广告更新成功');
  }
};

// 显示添加广告弹窗
const showAddDialog = () => {
  if (!isAdmin) return;
  addDialogVisible.value = true;
};

// 添加广告成功后的回调
const handleAdvertisementAdded = (newBanner) => {
  promobanners.value.push(newBanner);
  sessionStorage.setItem('promobanners', JSON.stringify(promobanners.value));
  ElMessage.success('广告添加成功');
};

// 组件挂载时获取数据
onMounted(() => {
  fetchPromoBanners();
});
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.promo-banner-container {
  margin-top: 100px;
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  position: relative;
}

.loading-container {
  padding: 40px;
  background-color: white;
}

.empty-state {
  height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  background-color: white;
}

.empty-state .el-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.promo-carousel :deep(.el-carousel__indicators) {
  bottom: 10px;
  z-index: 10;
}

.promo-carousel :deep(.el-carousel__indicator) {
  padding: 0 4px;
}

.promo-carousel :deep(.el-carousel__button) {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.2);
}

.promo-carousel :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  background-color: #333;
}

.promo-carousel :deep(.el-carousel__container) {
  background-color: #f9f9f9;
  border-radius: 16px 16px 0 0;
}

.promo-carousel :deep(.el-carousel__indicators--outside) {
  background-color: #f9f9f9;
  width: 100%;
  padding-bottom: 10px;
  margin: 0;
  border-radius: 0 0 16px 16px;
}

.banner-content {
  display: flex;
  height: 100%;
  padding: 0 5%;
  color: #333;
  position: relative;
  background-color: #f9f9f9;
  overflow: hidden;
}

.banner-content::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1440 320'%3E%3Cpath fill='%23ff6b6b' fill-opacity='0.05' d='M0,224L60,213.3C120,203,240,181,360,181.3C480,181,600,203,720,192C840,181,960,139,1080,138.7C1200,139,1320,181,1380,202.7L1440,224L1440,320L1380,320C1320,320,1200,320,1080,320C960,320,840,320,720,320C600,320,480,320,360,320C240,320,120,320,60,320L0,320Z'%3E%3C/path%3E%3Cpath fill='%23556270' fill-opacity='0.05' d='M0,160L60,149.3C120,139,240,117,360,128C480,139,600,181,720,186.7C840,192,960,160,1080,133.3C1200,107,1320,85,1380,74.7L1440,64L1440,320L1380,320C1320,320,1200,320,1080,320C960,320,840,320,720,320C600,320,480,320,360,320C240,320,120,320,60,320L0,320Z'%3E%3C/path%3E%3C/svg%3E");
  background-size: 100% 100%;
  background-position: center;
  z-index: 1;
  opacity: 0.7;
}

.banner-text {
  flex: 1;
  padding: 40px 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  z-index: 2;
}

.banner-title {
  font-size: 42px;
  font-weight: 800;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #ff6b6b, #556270);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  -webkit-font-smoothing: antialiased;
  font-family: 'Montserrat', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  letter-spacing: -0.5px;
  text-shadow:
      2px 2px 0 rgba(85, 98, 112, 0.2),
      -1px -1px 0 rgba(255, 107, 107, 0.4),
      0 4px 6px rgba(0, 0, 0, 0.1);
  display: inline-block;
  transform: perspective(500px) rotateX(5deg);
  transform-origin: bottom left;
  transition: transform 0.3s ease;
  padding: 0 10px;
  margin-left: -10px;
}

.banner-content-text {
  font-size: 18px;
  background: linear-gradient(to right, #444, #666);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-family: 'Source Sans Pro', 'Noto Sans SC', sans-serif;
  font-weight: 400;
  letter-spacing: 0.3px;
  word-spacing: 1px;
  line-height: 1.7;
  margin-bottom: 22px;
  padding-left: 12px;
  border-left: 3px solid rgba(255, 107, 107, 0.3);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  overflow: hidden;
  text-overflow: ellipsis;
  max-height: 122px;
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.banner-image {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.banner-image img {
  max-width: 100%;
  max-height: 90%;
  object-fit: contain;
  transform: perspective(1000px) rotateY(-5deg);
  transition: transform 0.5s ease;
  box-shadow: 5px 5px 15px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
}

.banner-image img:hover {
  transform: perspective(1000px) rotateY(0deg);
  box-shadow: 8px 8px 20px rgba(0, 0, 0, 0.15);
}

.add-banner-button {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 10;
}

.admin-buttons {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 3;
  display: flex;
  gap: 8px;
}

.admin-buttons .el-button {
  opacity: 0.8;
  transition: all 0.2s;
}

.admin-buttons .el-button:hover {
  transform: scale(1.1);
  opacity: 1;
}

/* 榜单区域样式优化 */
.ranks-section {
  margin-top: 40px;
}

.rank-section {
  margin-bottom: 40px;
  position: relative;
}

.section-title-container {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #e60012 0%, #ff3c4e 70%, #ff6b6b 100%);
  padding: 14px 20px;
  border-radius: 10px 10px 0 0;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(230, 0, 18, 0.15);
  margin-bottom: 2px;
}

.section-title-container:before {
  content: '';
  position: absolute;
  top: -20px;
  right: -20px;
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.section-title-container:after {
  content: '';
  position: absolute;
  bottom: -15px;
  left: 30px;
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.section-title-icon {
  width: 36px;
  height: 36px;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-title-icon i {
  font-size: 22px;
  color: #e60012;
}

.section-title-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
  margin-left: -50px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  color: white;
  margin: 0;
  letter-spacing: 0.5px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}

.section-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 2px;
  font-weight: 400;
}

/* 为了兼容Element Plus icons */
.el-icon-hot-water:before {
  content: "\e908";
  /* 使用合适的图标代码 */
}

.el-icon-new:before {
  content: "\e7d8";
  /* 使用合适的图标代码 */
}

/* 修改排行榜组件样式以适应新标题 */
.rank-grid {
  /* margin-top: 20px; */
  border-radius: 0 0 10px 10px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}
</style>