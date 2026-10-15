<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  categories: { type: Array, default: () => [] },
  cartCount: { type: Number, default: 0 },
  logged: { type: Boolean, default: false },
  user: { type: Object, default: null },
  isAdmin: { type: Boolean, default: false },
  isMerchant: { type: Boolean, default: false },
  keyword: { type: String, default: '' },
  selectedCategory: { type: [Number, String], default: undefined },
  currentView: { type: String, default: 'home' }
})

const emit = defineEmits(['go', 'search', 'category', 'login', 'logout'])
const searchText = ref(props.keyword)
watch(() => props.keyword, value => { searchText.value = value || '' })

function submitSearch() {
  emit('search', searchText.value.trim())
}
</script>

<template>
  <div class="rabbit-header">
    <div class="rabbit-topnav">
      <div class="rabbit-container topnav-inner">
        <span>优选商城 · 小兔鲜式品质生活</span>
        <nav>
          <button v-if="logged && user?.role === 'USER'" type="button" @click="emit('go', 'orders')">我的订单</button>
          <button v-if="logged && user?.role === 'USER'" type="button" @click="emit('go', 'addresses')">收货地址</button>
          <button v-if="logged && (isAdmin || isMerchant)" type="button" @click="emit('go', 'manage')">商家工作台</button>
          <button v-if="logged" type="button" @click="emit('logout')">{{ user?.nickname || '会员' }} · 退出</button>
          <button v-else type="button" @click="emit('login')">请先登录</button>
        </nav>
      </div>
    </div>

    <header class="rabbit-main-header">
      <div class="rabbit-container main-header-inner">
        <button class="rabbit-brand" type="button" @click="emit('go', 'home')">
          <span class="rabbit-brand-mark">优</span>
          <span><strong>优选商城</strong><small>品质好物，一站购齐</small></span>
        </button>
        <nav class="rabbit-nav" aria-label="商品导航">
          <button :class="{ active: currentView === 'home' && selectedCategory === undefined }" type="button" @click="emit('go', 'home')">首页</button>
          <button v-for="category in categories.slice(0, 4)" :key="category.id" :class="{ active: currentView === 'home' && selectedCategory === category.id }" type="button" @click="emit('category', category.id)">{{ category.name }}</button>
          <button :class="{ active: currentView === 'seckill' }" type="button" @click="emit('go', 'seckill')">限时秒杀</button>
        </nav>
        <form class="rabbit-search" @submit.prevent="submitSearch">
          <span aria-hidden="true">⌕</span>
          <input v-model="searchText" type="search" placeholder="搜一搜优质好物">
          <button type="submit">搜索</button>
        </form>
        <button class="rabbit-cart" type="button" @click="emit('go', 'cart')">
          <span aria-hidden="true">购物袋</span>
          <i v-if="cartCount">{{ cartCount }}</i>
        </button>
      </div>
    </header>
  </div>
</template>

<style scoped>
.rabbit-header{position:sticky;top:0;z-index:1000}.rabbit-container{width:min(1240px,calc(100% - 40px));margin:0 auto}.rabbit-topnav{background:#123d70;color:#dbeaff;font-size:12px}.topnav-inner{height:38px;display:flex;align-items:center;justify-content:space-between}.topnav-inner nav{display:flex;align-items:center}.topnav-inner button{border:0;border-left:1px solid #35618f;background:transparent;color:#dbeaff;padding:0 14px;cursor:pointer;font-size:12px}.topnav-inner button:hover{color:#fff}.rabbit-main-header{background:#fff;border-bottom:1px solid #dfebf5}.main-header-inner{min-height:88px;display:flex;align-items:center;gap:26px}.rabbit-brand{display:flex;align-items:center;gap:10px;min-width:190px;border:0;background:transparent;color:#123d70;text-align:left;cursor:pointer}.rabbit-brand-mark{display:grid;place-items:center;width:42px;height:42px;border-radius:12px;background:#1769d1;color:#fff;font-size:22px;font-weight:800;box-shadow:0 6px 15px #1769d133}.rabbit-brand strong,.rabbit-brand small{display:block}.rabbit-brand strong{font-size:18px}.rabbit-brand small{margin-top:3px;color:#91a6bb;font-size:10px}.rabbit-nav{display:flex;align-items:center;gap:20px;flex:1}.rabbit-nav button{border:0;background:transparent;color:#49667f;cursor:pointer;font-size:14px;padding:31px 0 27px;border-bottom:2px solid transparent;white-space:nowrap}.rabbit-nav button:hover,.rabbit-nav button.active{color:#1769d1;border-color:#1769d1}.rabbit-search{height:38px;width:260px;display:flex;align-items:center;background:#f5f9fd;border-bottom:1px solid #cbddec}.rabbit-search>span{color:#6e8ba9;font-size:23px;line-height:1;margin:0 7px}.rabbit-search input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:#294765;font-size:13px}.rabbit-search input::placeholder{color:#9fb2c4}.rabbit-search button{border:0;border-radius:5px;background:#1769d1;color:#fff;padding:7px 12px;cursor:pointer;font-size:12px}.rabbit-cart{position:relative;min-width:72px;border:0;background:transparent;color:#45617c;cursor:pointer;font-size:12px;padding:12px 2px}.rabbit-cart:hover{color:#1769d1}.rabbit-cart i{position:absolute;right:-2px;top:5px;min-width:18px;padding:2px 5px;border-radius:10px;background:#ec6a71;color:#fff;font-size:10px;font-style:normal}@media(max-width:900px){.rabbit-container{width:calc(100% - 28px)}.main-header-inner{gap:12px}.rabbit-brand{min-width:auto}.rabbit-nav{gap:10px;overflow:auto}.rabbit-nav button{font-size:12px}.rabbit-search{width:180px}.rabbit-brand small{display:none}}@media(max-width:620px){.topnav-inner>span{display:none}.topnav-inner{justify-content:flex-end}.main-header-inner{flex-wrap:wrap;padding:12px 0}.rabbit-nav{order:3;flex-basis:100%;justify-content:space-between}.rabbit-nav button{padding:8px 0}.rabbit-search{flex:1}.rabbit-cart{min-width:58px}}
</style>
