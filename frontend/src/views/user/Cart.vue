<template>
  <Header />
  <BackgroundLayout2 />
  <Cart class="container"/>
  <Footer />
</template>

<script setup lang="ts">
import Cart from '../../components/Cart.vue'
</script>

<style scoped>
.container {
  margin-top: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  /* height: 80vh; */
  position: relative;
}
</style>
