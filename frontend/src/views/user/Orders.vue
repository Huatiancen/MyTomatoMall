<template>
    <Header />
    <BackgroundLayout2 />
    <Orders class="container" />
    <Footer />
</template>

<script setup lang="ts">
import Orders from '../../components/Orders.vue'
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