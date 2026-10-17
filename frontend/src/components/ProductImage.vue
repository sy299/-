<script setup>
import { ref, watch } from 'vue'
import { assetUrl } from '../api'

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '商品图片' }
})
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })
</script>

<template>
  <div class="product-image-component" :class="{ failed }">
    <img v-if="props.src && !failed" :src="assetUrl(props.src)" :alt="props.alt" loading="lazy" decoding="async" @error="failed = true">
    <span v-else>暂无图片</span>
  </div>
</template>

<style scoped>
.product-image-component{width:100%;height:100%;display:grid;place-items:center;overflow:hidden;background:#edf5fc;color:#8aa3ba;font-size:12px}
.product-image-component img{display:block;width:100%;height:100%;object-fit:cover}
.product-image-component.failed{background:#edf5fc}
</style>
