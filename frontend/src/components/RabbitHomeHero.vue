<script setup>
import { computed } from 'vue'
import ProductImage from './ProductImage.vue'

const props = defineProps({
  categories: { type: Array, default: () => [] },
  products: { type: Array, default: () => [] },
  banners: { type: Array, default: () => [] },
  selectedCategory: { type: [Number, String], default: undefined }
})

const emit = defineEmits(['select-category', 'browse'])
const featured = computed(() => props.products[0] || null)
</script>

<template>
  <section class="rabbit-home-hero">
    <aside class="rabbit-category-menu">
      <div class="category-heading"><strong>全部商品分类</strong><small>按分类挑选品质好物</small></div>
      <button class="category-item" :class="{ active: selectedCategory === undefined }" type="button" @click="emit('select-category', undefined)">
        <span>全部好物</span><small>精选商品 · 商家直供</small><b>›</b>
      </button>
      <button v-for="category in categories" :key="category.id" class="category-item" :class="{ active: selectedCategory === category.id }" type="button" @click="emit('select-category', category.id)">
        <span>{{ category.name }}</span><small>严选品质 · 放心购买</small><b>›</b>
      </button>
    </aside>
    <div class="rabbit-hero-main">
      <div class="rabbit-hero-copy">
        <small>QUALITY PICKS · 今日上新</small>
        <h1>精选好物<br><em>轻松购，放心选</em></h1>
        <p>精选品牌与优质商家，让每一次购物都简单愉快。</p>
        <button class="rabbit-primary" type="button" @click="emit('browse')">立即选购 <span>→</span></button>
      </div>
      <div v-if="featured" class="rabbit-featured-product">
        <div class="featured-image"><ProductImage :src="featured.cover" :alt="featured.name"/></div>
        <div class="featured-copy"><small>本周热卖</small><strong>{{ featured.name }}</strong><b>¥{{ Number(featured.price).toFixed(2) }}</b></div>
      </div>
      <div v-else class="rabbit-hero-art"><strong>优选</strong><span>SALE</span><i>+</i></div>
      <div v-if="banners.length" class="rabbit-banner-thumbs"><img v-for="banner in banners.slice(0, 3)" :key="banner.id" :src="banner.imageUrl" :alt="banner.productName || '平台活动'"></div>
    </div>
  </section>
</template>

<style scoped>
.rabbit-home-hero{display:grid;grid-template-columns:250px 1fr;min-height:390px;overflow:hidden;border:1px solid #c7ddf4;background:#fff;box-shadow:0 12px 30px #2a6fa514}.rabbit-category-menu{background:#123d70;padding:20px 0;color:#fff}.category-heading{padding:0 24px 14px;border-bottom:1px solid #3a6290}.category-heading strong,.category-heading small{display:block}.category-heading strong{font-size:18px}.category-heading small{margin-top:4px;color:#b9d3ed;font-size:11px}.category-item{position:relative;width:100%;display:grid;grid-template-columns:1fr auto;gap:2px;border:0;border-left:3px solid transparent;background:transparent;color:#fff;text-align:left;padding:14px 24px;cursor:pointer}.category-item span{font-size:15px}.category-item small{grid-column:1;color:#b9d3ed;font-size:10px}.category-item b{position:absolute;right:22px;top:20px;color:#90b9df;font-size:20px;font-weight:normal}.category-item:hover,.category-item.active{border-left-color:#79baff;background:#1d5792}.category-item:hover b,.category-item.active b{color:#fff}.rabbit-hero-main{position:relative;display:flex;align-items:center;justify-content:space-between;gap:20px;min-width:0;padding:44px 58px;background:linear-gradient(125deg,#e7f2ff 0%,#f7fbff 62%,#d7eaff 100%);overflow:hidden}.rabbit-hero-main:after{content:'';position:absolute;right:-100px;bottom:-180px;width:430px;height:430px;border:80px solid #c2defa;border-radius:50%;opacity:.6}.rabbit-hero-copy{position:relative;z-index:1;max-width:430px}.rabbit-hero-copy small{color:#4c81b5;font-size:10px;font-weight:bold;letter-spacing:2px}.rabbit-hero-copy h1{margin:16px 0 12px;color:#123d70;font-size:42px;line-height:1.16}.rabbit-hero-copy h1 em{font-style:normal;color:#1769d1}.rabbit-hero-copy p{color:#66839f;font-size:14px}.rabbit-primary{margin-top:24px;border:0;border-radius:5px;background:#1769d1;color:#fff;padding:11px 18px;cursor:pointer;font-weight:bold;box-shadow:0 8px 15px #1769d133}.rabbit-primary:hover{background:#0f56b4}.rabbit-primary span{margin-left:8px}.rabbit-featured-product{position:relative;z-index:1;width:230px;padding:12px;border:1px solid #d2e4f5;border-radius:8px;background:#fff;box-shadow:0 18px 30px #2c6da51f;transform:rotate(2deg)}.featured-image{height:180px;border-radius:5px;overflow:hidden;background:#eff6fd}.featured-copy{display:flex;flex-direction:column;padding:10px 4px 2px}.featured-copy small{color:#5d82a4;font-size:10px}.featured-copy strong{margin:4px 0;color:#2b4d6c;font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.featured-copy b{color:#e35d62;font-size:18px}.rabbit-hero-art{position:relative;z-index:1;width:230px;height:190px;display:flex;flex-direction:column;align-items:center;justify-content:center;border:1px solid #c9e0f4;border-radius:14px;background:#fff;color:#1769d1;box-shadow:0 18px 30px #2c6da51f;transform:rotate(3deg)}.rabbit-hero-art strong{font-size:43px;letter-spacing:5px}.rabbit-hero-art span{color:#e35d62;font-size:16px;letter-spacing:5px}.rabbit-hero-art i{font-size:32px;font-style:normal;color:#6ca4db}.rabbit-banner-thumbs{position:absolute;z-index:2;left:58px;bottom:18px;display:flex;gap:7px}.rabbit-banner-thumbs img{width:62px;height:34px;object-fit:cover;border:2px solid #fff;border-radius:4px;box-shadow:0 2px 8px #1c598933}@media(max-width:900px){.rabbit-home-hero{grid-template-columns:190px 1fr}.rabbit-hero-main{padding:30px}.rabbit-hero-copy h1{font-size:33px}.rabbit-featured-product,.rabbit-hero-art{width:170px}.featured-image{height:130px}}@media(max-width:640px){.rabbit-home-hero{display:block}.rabbit-category-menu{display:flex;overflow:auto;padding:0}.category-heading{display:none}.category-item{min-width:150px;padding:13px 16px}.category-item b{right:12px;top:15px}.rabbit-hero-main{min-height:320px;padding:28px 20px}.rabbit-featured-product,.rabbit-hero-art{width:138px;flex:0 0 138px}.featured-image{height:100px}.rabbit-hero-copy h1{font-size:27px}.rabbit-hero-copy p{font-size:12px}.rabbit-hero-main:after{display:none}.rabbit-banner-thumbs{left:20px}}
</style>
