<template>
  <div class="advantage-banner">
    <div
        class="advantage-item"
        v-for="(item, index) in advantages"
        :key="index"
    >
      <span class="text">
        <strong class="keyword">{{ item.keyword }}</strong>{{ item.desc }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const advantages = ref([
  {
    keyword: '精选  ',
    desc: '品质好书，精心挑选'
  },
  {
    keyword: '便捷  ',
    desc: '一键下单，轻松购书'
  },
  {
    keyword: '专业  ',
    desc: '正版图书，权威推荐'
  },
  {
    keyword: '优惠  ',
    desc: '每日特惠，省钱省心'
  }
])
</script>

<style>
.advantage-banner {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 30px 0;
  background-color: transparent; /* 设置背景为透明 */
  flex-wrap: wrap;
}

.advantage-item {
  display: flex;
  align-items: center;
  margin: 10px 20px;
  white-space: nowrap;
}

.text {
  color: #333;
  font-size: 16px;
}

.keyword {
  font-size: 24px; /* 设置大字号 */
  color: red; /* 设置为红色 */
  font-weight: bold; /* 设置为粗体 */
}
</style>