<template>
  <div class="background-container">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
// 这里可以保留其他背景组件的逻辑
</script>

<style scoped>
.background-container {
  z-index: -1000;
  width: 100%;
  height: 100vh;
  background: linear-gradient(to bottom, #f5f5dc 0%, #fff 100%);
  position: absolute;
  top: 0;
  left: 0;
}
</style>
