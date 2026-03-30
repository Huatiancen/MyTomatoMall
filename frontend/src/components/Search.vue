<template>
  <div class="search-container">
    <el-input v-model="localSearchQuery" placeholder="请输入搜索内容" class="search-input" @input="debounceSearch"
      @keyup.enter="performSearch" clearable @clear="clearSearch">
      <template #suffix>
        <el-icon class="el-input__icon" @click="performSearch">
          <el-icon-search />
        </el-icon>
      </template>
    </el-input>
    <el-button @click="performSearch" class="search-button">搜索</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElInput, ElButton, ElIcon } from 'element-plus'

// 添加props接收外部传入的值
const props = defineProps({
  value: {
    type: String,
    default: ''
  }
})

// 本地状态
const localSearchQuery = ref(props.value)

// 监听外部值变化
watch(() => props.value, (newValue) => {
  if (newValue !== localSearchQuery.value) {
    localSearchQuery.value = newValue
  }
})

const emit = defineEmits(['search', 'update:value']) // 添加update:value事件

// 监听本地值变化并通知父组件
watch(localSearchQuery, (newValue) => {
  emit('update:value', newValue)
})

// 使用防抖函数来处理用户输入
let debounceTimer: number | null = null;
const debounceSearch = () => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }

  // 设置300ms的延迟，避免频繁触发搜索
  debounceTimer = setTimeout(() => {
    performSearch()
  }, 300) as unknown as number
}

// 执行搜索，并将搜索词发送给父组件
const performSearch = () => {
  // console.log('搜索内容:', localSearchQuery.value)
  emit('search', localSearchQuery.value.trim())
}

// 清除搜索
const clearSearch = () => {
  localSearchQuery.value = ''
  emit('search', '') // 发送空字符串表示清除搜索条件
  emit('update:value', '') // 同步更新到父组件
}
</script>

<style scoped>
.search-container {
  display: flex;
  position: relative;
  gap: 5px;                   /* 搜索框和按钮之间的间距 */
  width: 500px;                /* 容器宽度 */
  border: 2px solid #409EFF;  /* 为整个容器加边框 */
  border-radius: 25px;         /* 圆角效果 */
  padding: 5px 15px;           /* 内边距 */
  background-color: #ffffff;  /* 背景颜色 */
  margin: 0 auto;             /* 水平居中 */
  position: absolute;         /* 绝对定位 */
  left: 50%;                  /* 左边缘在屏幕中心 */
  top: 15%;                   /* 上边缘在屏幕中心 */
  transform: translate(-50%); /* 向左上偏移自身尺寸的一半，实现完全居中 */
}
.search-button {
  background-color: #409EFF;  /* 自定义按钮背景色 */
  color: white;               /* 自定义文字颜色 */
  border-radius: 20px;        /* 按钮圆角 */
  padding: 5px 20px;          /* 按钮内边距 */
}
</style>
