<script setup>
import ProductImage from './ProductImage.vue'

const props = defineProps({
  product: { type: Object, required: true },
  liked: { type: Boolean, default: false }
})

const emit = defineEmits(['favorite', 'buy', 'cart'])
</script>

<template>
  <article class="rabbit-product-card">
    <div class="rabbit-product-cover">
      <ProductImage :src="props.product.cover" :alt="props.product.name"/>
      <button class="rabbit-favorite" :class="{ liked }" type="button" :aria-label="liked ? '取消收藏' : '收藏商品'" @click="emit('favorite', props.product)">{{ liked ? '♥' : '♡' }}</button>
      <span class="rabbit-product-badge">品质严选</span>
    </div>
    <div class="rabbit-product-info">
      <small>{{ props.product.merchantName || '优质商家' }}</small>
      <h3>{{ props.product.name }}</h3>
      <p>{{ props.product.detail || '精选好物，品质保障，放心购买' }}</p>
      <footer>
        <strong>¥{{ Number(props.product.price).toFixed(2) }}</strong>
        <span><button type="button" class="rabbit-buy" @click="emit('buy', props.product)">立即购买</button><button type="button" class="rabbit-add" @click="emit('cart', props.product)">加入购物车</button></span>
      </footer>
    </div>
  </article>
</template>

<style scoped>
.rabbit-product-card{background:#fff;border:1px solid #e1ebf5;transition:transform .2s,box-shadow .2s}.rabbit-product-card:hover{transform:translateY(-4px);box-shadow:0 12px 24px #2b6ea51c}.rabbit-product-cover{position:relative;height:220px;background:#eff6fd;overflow:hidden}.rabbit-product-cover :deep(.product-image-component){transition:transform .3s}.rabbit-product-card:hover .rabbit-product-cover :deep(.product-image-component){transform:scale(1.04)}.rabbit-favorite{position:absolute;z-index:1;right:10px;top:10px;width:31px;height:31px;border:0;border-radius:50%;background:#ffffffeb;color:#7d99b2;font-size:18px;cursor:pointer}.rabbit-favorite.liked{color:#e35d62}.rabbit-product-badge{position:absolute;left:10px;top:10px;border-radius:3px;background:#1769d1;color:#fff;padding:4px 7px;font-size:10px}.rabbit-product-info{padding:14px 15px 16px}.rabbit-product-info small{color:#7894ad;font-size:11px}.rabbit-product-info h3{height:21px;margin:5px 0;color:#294d6d;font-size:15px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.rabbit-product-info p{height:34px;margin:0;color:#8ba0b3;font-size:11px;line-height:17px;overflow:hidden}.rabbit-product-info footer{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:12px}.rabbit-product-info footer strong{color:#e35d62;font-size:19px;white-space:nowrap}.rabbit-product-info footer span{display:flex;gap:4px}.rabbit-product-info footer button{border-radius:4px;padding:7px 8px;cursor:pointer;font-size:10px;white-space:nowrap}.rabbit-buy{border:1px solid #1769d1;background:#1769d1;color:#fff}.rabbit-add{border:1px solid #a9c9e7;background:#eff7ff;color:#1769d1}.rabbit-buy:hover,.rabbit-add:hover{filter:brightness(.95)}
</style>
