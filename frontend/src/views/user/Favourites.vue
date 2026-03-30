<template>
  <Header />
  <BackgroundLayout2 />
  <Favourites class="container"/>
  <Footer />
</template>

<script setup lang="ts">
import Favourites from '../../components/Favourites.vue'
// import BackgroundLayout2 from "../../components/BackgroundLayout2.vue";
</script>

<style scoped>
.container {
  margin-top: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  position: relative;
}
</style>