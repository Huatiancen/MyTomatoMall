<template>
  <div class="background-container">
    <div class="background-layer"></div>
    <div class="content-layer">
      <img src="http://47.111.155.70/oss/logo.png" alt="Logo" class="page-logo" />
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.background-container {
  position: fixed;
  inset: 0;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

.background-layer {
  position: absolute;
  inset: 0;
  background-image: url('http://47.111.155.70/oss/login-bg.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  filter: brightness(1.1) blur(3px);
  z-index: 0;
}

.content-layer {
  position: relative;
  width: 100%;
  height: 100%;
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}

.page-logo {
  position: absolute;
  top: 48px;
  left: 48px;
  width: 240px;
}
</style>
