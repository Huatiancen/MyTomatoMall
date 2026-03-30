<template>
  <div class="rank-container">
    <div class="rank-grid">
      <!-- 实际数据显示 -->
      <div v-for="item in rankList" :key="item.id" class="rank-card" @click="goToDetail(item.id)">
        <!-- 修改排名标签样式 -->
        <div :class="['rank-number', getTopClass(getIndexNumber(item))]">
          {{ getIndexNumber(item) <= 3 ? 'top' + getIndexNumber(item) : getIndexNumber(item) }} </div>
            <div class="image-wrapper">
              <!-- 始终使用默认封面作为备选 -->
              <img v-if="item.cover" :src="item.cover" @error="handleImageError($event)" alt="商品封面"
                class="product-image" />
              <img v-else src="http://47.111.155.70/oss/default-cover.jpg" class="product-image" />
            </div>
            <div class="product-title">{{ item.title }}</div>
        </div>

        <!-- 空位占位卡片 -->
        <div v-for="i in emptyRows" :key="`empty-${i}`" class="rank-card empty-card">
          <div :class="['rank-number', getTopClass(rankList.length + i)]">
            {{ rankList.length + i <= 3 ? 'top' + (rankList.length + i) : rankList.length + i }} </div>
              <div class="image-wrapper">
                <img src="http://47.111.155.70/oss/default-cover.jpg" class="product-image"
                  alt="暂无数据" />
              </div>
              <div class="product-title">暂无数据</div>
          </div>
        </div>
      </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Product } from "../api/products"

const props = defineProps<{
  title: string
  fetchFunction: () => Promise<any>
  maxRows?: number
}>()

const rankList = ref<Product[]>([])
const router = useRouter()

// 获取序号
const getIndexNumber = (item: Product) => {
  return rankList.value.findIndex(p => p.id === item.id) + 1
}

// 处理图片加载错误
const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.src = 'http://47.111.155.70/oss/default-cover.jpg'
}

// 获取前三名的特殊样式类
const getTopClass = (index: number) => {
  if (index <= 3) {
    return `top-${index}`
  }
  return ''
}

// 计算需要显示的空行数量
const emptyRows = computed(() => {
  // 计算显示的最大数量
  const totalItems = props.maxRows || 10
  // 计算剩余空位数量
  return Math.max(0, totalItems - rankList.value.length)
})

// 跳转到详情页
const goToDetail = (id: number) => {
  router.push(`/BookDetail/${id}`)
}

// 获取榜单数据
onMounted(async () => {
  try {
    const res = await props.fetchFunction()
    // console.log("!!!456")
    // console.log(res)
    // console.log("!!!123")
    if (res?.data?.code === "200") {
      rankList.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取榜单数据失败', error)
  }
})
</script>

<style scoped>
.rank-container {
  width: 100%;
  margin-bottom: 30px;
}

.rank-grid {
  display: grid;
  margin-top: 20px;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  min-width: 1200px;
  width: 100%;
}

.rank-card {
  position: relative;
  height: 260px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  border: 1px solid transparent;
  padding: 0;
}

.rank-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
  border: 1px solid #e60012;
}

.rank-number {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 40px;
  height: 24px;
  background-color: rgba(230, 0, 18, 0.6); /* 普通红色，稍微透明一点 */
  color: white;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  z-index: 2;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 前三名特殊样式 */
.top-1, .top-2, .top-3 {
  width: 50px;
  background-color: #e60012; /* 统一使用最显眼的红色 */
  font-size: 16px;
  font-weight: 800;
  text-transform: uppercase;
  box-shadow: 0 2px 5px rgba(230, 0, 18, 0.4);
}



.image-wrapper {
  width: 100%;
  height: 200px;
  padding: 10px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
  transition: transform 0.3s ease;
}

.rank-card:hover .product-image {
  transform: scale(1.05);
}

.product-title {
  padding: 10px;
  font-size: 15px;
  font-weight: 500;
  color: #333;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  /* margin-top: ; */
}

.rank-card:hover .product-title {
  color: #fa8f68;
}

.empty-card {
  opacity: 0.6;
  cursor: default;
}

.empty-card:hover {
  transform: none;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  border: none;
}
</style>