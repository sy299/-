<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { call } from './api'
import ProductImage from './components/ProductImage.vue'
import RabbitHeader from './components/RabbitHeader.vue'
import RabbitHomeHero from './components/RabbitHomeHero.vue'
import RabbitProductCard from './components/RabbitProductCard.vue'

const view = ref('home')
const home = ref({ categories: [], products: [], announcements: [], banners: [] })
const products = ref([])
const productSentinel = ref(null)
const loadingProducts = ref(false)
const hasMoreProducts = ref(true)
const productPage = ref(0)
const productPageSize = 8
const legacyProductCache = ref(null)
let productQueryId = 0
let productObserver
const cart = ref([])
const orders = ref([])
const addresses = ref([])
const selectedProduct = ref(null)
const seckillActivities = ref([])
const seckillError = ref('')
const seckillLoading = ref(false)
const selectedSeckillActivity = ref(null)
const productDetailLoading = ref(false)
const productOrderSubmitting = ref(false)
const productQuantity = ref(1)
const selectedAddressId = ref(null)
const returnToProductAfterAddress = ref(false)
const favorites = ref([])
const manageOrders = ref([])
const manageProducts = ref([])
const dashboard = ref(null)
const adminTab = ref('categories')
const adminCategories = ref([])
const adminAnnouncements = ref([])
const adminBanners = ref([])
const adminTags = ref([])
const categoryForm = ref({ name: '', logo: '', showOnHome: true })
const announcementForm = ref({ title: '', content: '' })
const bannerForm = ref({ imageUrl: '', productId: '', sortNo: 0, enabled: true })
const tagForm = ref({ merchantId: 2, tag: '', description: '' })
const productForm = ref({ name: '', cover: '', categoryId: 1, price: '', stock: 0, detail: '' })
const keyword = ref('')
const categoryId = ref()
const loginOpen = ref(false)
const login = ref({ username: 'user', password: 'user123' })
const registerMode = ref(false)
const registerForm = ref({ username: '', password: '', confirmPassword: '', nickname: '', phone: '' })
const user = ref(JSON.parse(localStorage.getItem('mall_user') || 'null'))
const addressForm = ref({ receiverName: '', receiverPhone: '', detailAddress: '', isDefault: false })
const addressEditing = ref(null)
const selectedCart = ref([])
const checkoutSubmitting = ref(false)
const paymentOpen = ref(false)
const paymentSubmitting = ref(false)
const paymentProvider = ref('ALIPAY')
const paymentOrder = ref(null)
const paymentOptions = [
  { code: 'ALIPAY', name: '支付宝', mark: '支' },
  { code: 'WECHAT', name: '微信支付', mark: '微' }
]
const logged = ref(!!localStorage.getItem('mall_token'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const isMerchant = computed(() => user.value?.role === 'MERCHANT')
const cartCount = computed(() => cart.value.reduce((sum, item) => sum + item.quantity, 0))
const selectedTotal = computed(() => cart.value.filter(item => selectedCart.value.includes(item.id)).reduce((sum, item) => sum + Number(item.price) * item.quantity, 0))
const defaultAddress = computed(() => addresses.value.find(item => item.is_default) || addresses.value[0])
const selectedAddress = computed(() => addresses.value.find(item => String(item.id) === String(selectedAddressId.value)) || defaultAddress.value)

async function loadHome() {
  home.value = await call({ url: '/home' })
  await loadMoreProducts(true)
}
async function loadSeckillActivities(notify = false) {
  seckillLoading.value = true
  seckillError.value = ''
  try { seckillActivities.value = await call({ url: '/seckill/activities' }) }
  catch (error) {
    seckillError.value = error.message
    if (notify) ElMessage.error(error.message)
  } finally {
    seckillLoading.value = false
  }
}
async function loadMoreProducts(reset = false) {
  if (reset) {
    productQueryId += 1
    products.value = []
    productPage.value = 0
    hasMoreProducts.value = true
    loadingProducts.value = false
    legacyProductCache.value = null
  }
  if (loadingProducts.value || !hasMoreProducts.value) return

  const queryId = productQueryId
  const nextPage = productPage.value + 1
  loadingProducts.value = true
  try {
    let response = legacyProductCache.value
    if (!response) {
      response = await call({
        url: '/products',
        params: {
          keyword: keyword.value || undefined,
          categoryId: categoryId.value || undefined,
          page: nextPage,
          pageSize: productPageSize
        }
      })
      if (response.length > productPageSize) legacyProductCache.value = response
    }
    if (queryId !== productQueryId) return
    const batch = legacyProductCache.value
      ? response.slice((nextPage - 1) * productPageSize, nextPage * productPageSize)
      : response
    products.value = [...products.value, ...batch]
    productPage.value = nextPage
    hasMoreProducts.value = legacyProductCache.value
      ? batch.length === productPageSize
      : response.length === productPageSize
  } catch (error) {
    if (queryId === productQueryId) ElMessage.error(error.message)
  } finally {
    if (queryId === productQueryId) loadingProducts.value = false
  }
}
async function search() {
  await loadMoreProducts(true)
}
function headerSearch(value) {
  view.value = 'home'
  keyword.value = value
  categoryId.value = undefined
  search()
}
function selectCategory(id) {
  view.value = 'home'
  categoryId.value = id
  search()
}
function browseProducts() {
  document.querySelector('.catalog')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
async function loadUserData() {
  if (!logged.value || user.value?.role !== 'USER') return
  await Promise.all([loadCart(), loadOrders(), loadAddresses(), loadFavorites()])
}
async function signIn() {
  try {
    const result = await call({ url: '/auth/login', method: 'post', data: login.value })
    localStorage.setItem('mall_token', result.token)
    localStorage.setItem('mall_user', JSON.stringify(result.user))
    user.value = result.user
    logged.value = true
    loginOpen.value = false
    await loadUserData()
  } catch (error) { ElMessage.error(error.message) }
}
async function signUp() {
  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.nickname) { ElMessage.warning('请填写用户名、密码和昵称'); return }
  if (registerForm.value.password.length < 6) { ElMessage.warning('密码至少需要6位'); return }
  if (registerForm.value.password !== registerForm.value.confirmPassword) { ElMessage.warning('两次输入的密码不一致'); return }
  try {
    await call({ url: '/auth/register', method: 'post', data: { username: registerForm.value.username, password: registerForm.value.password, nickname: registerForm.value.nickname, phone: registerForm.value.phone } })
    login.value = { username: registerForm.value.username, password: registerForm.value.password }
    registerMode.value = false
    await signIn()
    ElMessage.success('注册成功，欢迎加入优选商城')
  } catch (error) { ElMessage.error(error.message) }
}
function openRegister() {
  registerMode.value = true
  registerForm.value = { username: '', password: '', confirmPassword: '', nickname: '', phone: '' }
}
function signOut() {
  localStorage.clear()
  logged.value = false
  user.value = null
  cart.value = []
  orders.value = []
  addresses.value = []
  favorites.value = []
  view.value = 'home'
  window.location.reload()
}
async function addToCart(product) {
  if (!logged.value) { loginOpen.value = true; return }
  if (user.value.role !== 'USER') { ElMessage.warning('请使用普通用户账号购买商品'); return }
  try {
    await call({ url: '/cart', method: 'post', data: { productId: product.id, quantity: 1 } })
    await loadCart()
    ElMessage.success('已加入购物车')
  } catch (error) { ElMessage.error(error.message) }
}
async function buyNow(product) {
  selectedSeckillActivity.value = null
  await openProductDetail(product)
}
async function buySeckill(activity) {
  selectedSeckillActivity.value = activity
  await openProductDetail({ id: activity.productId, name: activity.productName, cover: activity.cover, price: activity.originalPrice })
}
async function openProductDetail(product) {
  selectedProduct.value = { ...product }
  productQuantity.value = 1
  selectedAddressId.value = defaultAddress.value?.id ?? null
  view.value = 'product'
  productDetailLoading.value = true
  try {
    const detail = await call({ url: '/products/' + product.id })
    selectedProduct.value = { ...selectedProduct.value, ...detail }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    productDetailLoading.value = false
  }
}
async function confirmProductOrder() {
  if (!selectedProduct.value || productOrderSubmitting.value) return
  if (!logged.value) { loginOpen.value = true; return }
  if (user.value?.role !== 'USER') { ElMessage.warning('请使用普通用户账号购买商品'); return }
  if (!selectedAddress.value) {
    returnToProductAfterAddress.value = true
    view.value = 'addresses'
    await loadAddresses()
    if (defaultAddress.value) {
      returnToProductAfterAddress.value = false
      view.value = 'product'
    }
    else ElMessage.warning('请先添加收货地址')
    return
  }
  productOrderSubmitting.value = true
  try {
    const address = selectedAddress.value
    const orderRequest = { receiverName: address.receiver_name, receiverPhone: address.receiver_phone, receiverAddress: address.detail_address }
    let createdOrder
    if (selectedSeckillActivity.value) {
      createdOrder = await call({ url: `/seckill/activities/${selectedSeckillActivity.value.id}/purchase`, method: 'post', data: orderRequest })
      selectedSeckillActivity.value.availableStock = Math.max(0, Number(selectedSeckillActivity.value.availableStock) - 1)
      seckillActivities.value = seckillActivities.value.map(activity => activity.id === selectedSeckillActivity.value.id ? { ...activity, availableStock: selectedSeckillActivity.value.availableStock } : activity)
    } else {
      createdOrder = await call({ url: '/orders', method: 'post', data: { ...orderRequest, productId: selectedProduct.value.id, quantity: productQuantity.value } })
    }
    ElMessage.success('订单创建成功，请选择支付方式')
    await loadOrders()
    view.value = 'orders'
    openPayment(createdOrder)
  } catch (error) { ElMessage.error(error.message) }
  finally { productOrderSubmitting.value = false }
}
function openPayment(order) {
  if (!order?.orderId && !order?.id) {
    ElMessage.error('订单编号缺失，暂时无法发起支付')
    return
  }
  paymentOrder.value = {
    id: order.orderId ?? order.id,
    orderNo: order.orderNo ?? order.order_no,
    totalAmount: order.totalAmount ?? order.total_amount
  }
  paymentProvider.value = 'ALIPAY'
  paymentOpen.value = true
}
async function submitPayment() {
  if (!paymentOrder.value || paymentSubmitting.value) return
  paymentSubmitting.value = true
  try {
    const result = await call({ url: `/payments/orders/${paymentOrder.value.id}`, method: 'post', data: { provider: paymentProvider.value } })
    paymentOpen.value = false
    if (result.paymentUrl) window.open(result.paymentUrl, '_blank', 'noopener,noreferrer')
    if (result.paymentForm) {
      const paymentWindow = window.open('', '_blank')
      if (paymentWindow) {
        paymentWindow.document.write(result.paymentForm)
        paymentWindow.document.close()
      }
    }
    ElMessage.success(result.message || '支付请求已创建，请在支付页面完成付款')
    await loadOrders()
  } catch (error) { ElMessage.error(error.message) }
  finally { paymentSubmitting.value = false }
}
function payOrder(order) {
  openPayment(order)
}
async function loadCart() {
  cart.value = await call({ url: '/cart' })
  selectedCart.value = selectedCart.value.filter(id => cart.value.some(item => item.id === id && !cartUnavailableReason(item)))
}
function cartUnavailableReason(item) {
  if (item.status && item.status !== 'ON_SALE') return '已下架，不可结算'
  if (Number(item.stock) < 1) return '暂时无库存'
  return ''
}
async function loadOrders() { orders.value = await call({ url: '/orders' }) }
async function loadAddresses() {
  addresses.value = await call({ url: '/addresses' })
  if (!addresses.value.some(item => String(item.id) === String(selectedAddressId.value))) selectedAddressId.value = defaultAddress.value?.id ?? null
}
async function loadFavorites() { favorites.value = await call({ url: '/favorites' }) }
function isFavorite(id) { return favorites.value.some(item => item.productId === id) }
async function toggleFavorite(product) {
  if (!logged.value) { loginOpen.value = true; return }
  try {
    if (isFavorite(product.id)) await call({ url: '/favorites/' + product.id, method: 'delete' })
    else await call({ url: '/favorites/' + product.id, method: 'post' })
    await loadFavorites()
  } catch (error) { ElMessage.error(error.message) }
}
async function updateCart(item, quantity) {
  if (quantity < 1) return removeCart(item)
  try { await call({ url: '/cart/' + item.id, method: 'put', data: { productId: item.productId, quantity } }); await loadCart() } catch (error) { ElMessage.error(error.message) }
}
async function removeCart(item) {
  await call({ url: '/cart/' + item.id, method: 'delete' })
  await loadCart()
}
async function checkoutSelected() {
  if (checkoutSubmitting.value) return
  if (!selectedCart.value.length) { ElMessage.warning('请先选择要结算的商品'); return }
  if (!defaultAddress.value) { view.value = 'addresses'; ElMessage.warning('请先添加收货地址'); return }
  const selectedItems = cart.value.filter(item => selectedCart.value.includes(item.id))
  const unavailable = selectedItems.find(cartUnavailableReason)
  if (unavailable) { ElMessage.error(`商品「${unavailable.name}」${cartUnavailableReason(unavailable)}`); return }
  const a = defaultAddress.value
  checkoutSubmitting.value = true
  try {
    await call({ url: '/orders/from-cart', method: 'post', data: { cartIds: selectedCart.value, receiverName: a.receiver_name, receiverPhone: a.receiver_phone, receiverAddress: a.detail_address } })
    selectedCart.value = []
    await loadCart()
    await loadOrders()
    view.value = 'orders'
    ElMessage.success('批量下单成功，请在订单中心选择支付方式')
  } catch (error) { ElMessage.error(error.message) }
  finally { checkoutSubmitting.value = false }
}
async function saveAddress() {
  try {
    if (addressEditing.value) await call({ url: '/addresses/' + addressEditing.value, method: 'put', data: addressForm.value })
    else await call({ url: '/addresses', method: 'post', data: addressForm.value })
    resetAddressForm()
    await loadAddresses()
    if (returnToProductAfterAddress.value && defaultAddress.value) {
      returnToProductAfterAddress.value = false
      view.value = 'product'
    }
    ElMessage.success('地址已保存')
  } catch (error) { ElMessage.error(error.message) }
}
function editAddress(address) {
  addressEditing.value = address.id
  addressForm.value = { receiverName: address.receiver_name, receiverPhone: address.receiver_phone, detailAddress: address.detail_address, isDefault: !!address.is_default }
}
function resetAddressForm() {
  addressEditing.value = null
  addressForm.value = { receiverName: '', receiverPhone: '', detailAddress: '', isDefault: false }
}
async function deleteAddress(address) {
  await call({ url: '/addresses/' + address.id, method: 'delete' })
  await loadAddresses()
}
async function setDefault(address) {
  await call({ url: '/addresses/' + address.id + '/default', method: 'put' })
  await loadAddresses()
}
async function manageProductAddress() {
  if (!logged.value) { loginOpen.value = true; return }
  if (user.value?.role !== 'USER') { ElMessage.warning('该功能仅对普通用户开放'); return }
  returnToProductAfterAddress.value = true
  await go('addresses')
}
async function confirmOrder(order) {
  try { await call({ url: '/orders/' + order.id + '/confirm', method: 'put' }); await loadOrders(); ElMessage.success('已确认收货') } catch (error) { ElMessage.error(error.message) }
}
async function refundOrder(order) {
  try {
    const reason = await ElMessageBox.prompt('请输入退款原因', '申请退款', { confirmButtonText: '提交', cancelButtonText: '取消' })
    await call({ url: '/orders/' + order.id + '/refund', method: 'post', data: { reason: reason.value } })
    await loadOrders()
    ElMessage.success('退款申请已提交')
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message) }
}
async function reviewOrder(order) {
  try {
    const result = await ElMessageBox.prompt('请输入评分（1-5）和评价内容，用逗号分隔', '发表评价', { confirmButtonText: '提交', cancelButtonText: '取消' })
    const parts = result.value.split(',')
    await call({ url: '/reviews', method: 'post', data: { orderId: order.id, rating: Number(parts[0]), content: parts.slice(1).join(',') } })
    ElMessage.success('评价成功')
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message) }
}
async function loadManage() {
  if (isAdmin.value) dashboard.value = await call({ url: '/admin/dashboard' })
  if (isAdmin.value || isMerchant.value) manageOrders.value = await call({ url: '/manage/orders' })
  if (isAdmin.value || isMerchant.value) manageProducts.value = await call({ url: '/manage/products' })
  if (isAdmin.value) {
    const result = await Promise.all([call({ url: '/admin/categories' }), call({ url: '/admin/content/announcements' }), call({ url: '/admin/content/banners' }), call({ url: '/admin/tags' })])
    adminCategories.value = result[0]
    adminAnnouncements.value = result[1]
    adminBanners.value = result[2]
    adminTags.value = result[3]
  }
}
async function addCategory() { try { await call({ url: '/admin/categories', method: 'post', data: categoryForm.value }); categoryForm.value = { name: '', logo: '', showOnHome: true }; await loadManage(); ElMessage.success('分类已添加') } catch (error) { ElMessage.error(error.message) } }
async function deleteCategory(id) { try { await call({ url: '/categories/' + id, method: 'delete' }); await loadManage(); ElMessage.success('分类已删除') } catch (error) { ElMessage.error(error.message) } }
async function addAnnouncement() { try { await call({ url: '/admin/content/announcements', method: 'post', data: announcementForm.value }); announcementForm.value = { title: '', content: '' }; await loadManage(); ElMessage.success('公告已发布') } catch (error) { ElMessage.error(error.message) } }
async function deleteAnnouncement(id) { await call({ url: '/admin/content/announcements/' + id, method: 'delete' }); await loadManage() }
async function addBanner() { try { await call({ url: '/admin/content/banners', method: 'post', data: bannerForm.value }); bannerForm.value = { imageUrl: '', productId: '', sortNo: 0, enabled: true }; await loadManage(); ElMessage.success('轮播图已添加') } catch (error) { ElMessage.error(error.message) } }
async function addTag() { try { await call({ url: '/admin/tags', method: 'post', data: tagForm.value }); tagForm.value = { merchantId: 2, tag: '', description: '' }; await loadManage(); ElMessage.success('商家标签已添加') } catch (error) { ElMessage.error(error.message) } }
async function deleteTag(id) { await call({ url: '/admin/tags/' + id, method: 'delete' }); await loadManage() }
async function updateManageOrder(order, status) {
  try { await call({ url: '/manage/orders/' + order.id + '/status', method: 'put', data: { status } }); await loadManage(); ElMessage.success('订单状态已更新') } catch (error) { ElMessage.error(error.message) }
}
async function addProduct() { try { await call({ url: '/products', method: 'post', data: { ...productForm.value, price: Number(productForm.value.price), stock: Number(productForm.value.stock) } }); productForm.value = { name: '', cover: '', categoryId: 1, price: '', stock: 0, detail: '' }; await loadManage(); ElMessage.success('商品已发布') } catch (error) { ElMessage.error(error.message) } }
async function removeProduct(product) { try { await call({ url: '/products/' + product.id, method: 'delete' }); await loadManage(); ElMessage.success('商品已下架') } catch (error) { ElMessage.error(error.message) } }
async function toggleProductStatus(product) { try { const status = product.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'; await call({ url: '/manage/products/' + product.id + '/status', method: 'put', data: { status } }); await loadManage(); ElMessage.success(status === 'ON_SALE' ? '商品已上架' : '商品已下架') } catch (error) { ElMessage.error(error.message) } }
async function go(next) {
  if (next !== 'addresses') returnToProductAfterAddress.value = false
  view.value = next
  if (!logged.value && !['home', 'seckill'].includes(next)) { loginOpen.value = true; view.value = 'home'; return }
  if (['cart', 'orders', 'addresses'].includes(next) && user.value?.role !== 'USER') {
    ElMessage.warning('该功能仅对普通用户开放')
    view.value = 'home'
    return
  }
  if (next === 'home') {
    categoryId.value = undefined
    keyword.value = ''
    await loadMoreProducts(true)
  }
  if (next === 'cart') await loadCart()
  if (next === 'orders') await loadOrders()
  if (next === 'addresses') await loadAddresses()
  if (next === 'manage') await loadManage()
}
onMounted(async () => {
  await loadHome()
  await loadSeckillActivities()
  if (logged.value) await loadUserData()
  await nextTick()
  productObserver = new IntersectionObserver(entries => {
    if (entries.some(entry => entry.isIntersecting)) loadMoreProducts()
  }, { rootMargin: '500px 0px' })
  if (productSentinel.value) productObserver.observe(productSentinel.value)
})
onBeforeUnmount(() => productObserver?.disconnect())
</script>

<template>
  <div class="shell">
    <RabbitHeader :categories="home.categories" :cart-count="cartCount" :logged="logged" :user="user" :is-admin="isAdmin" :is-merchant="isMerchant" :keyword="keyword" :selected-category="categoryId" :current-view="view" @go="go" @search="headerSearch" @category="selectCategory" @login="loginOpen = true" @logout="signOut"/>
    <main>
      <section v-if="view === 'home'" class="content">
        <RabbitHomeHero :categories="home.categories" :products="products" :banners="home.banners" :selected-category="categoryId" @select-category="selectCategory" @browse="browseProducts"/>
        <section v-if="seckillActivities.length" class="seckill-section">
          <div class="section-title"><div><small>LIMITED TIME</small><h2>限时秒杀</h2></div><button class="link-button" type="button" @click="go('seckill')">查看全部 →</button></div>
          <div class="seckill-list">
            <article v-for="activity in seckillActivities" :key="activity.id" class="seckill-item">
              <ProductImage class="seckill-image" :src="activity.cover" :alt="activity.productName"/>
              <div class="seckill-copy"><small>{{ activity.active ? '正在进行' : '即将开始' }} · 剩余 {{ activity.availableStock }}</small><h3>{{ activity.productName }}</h3><span><b>¥{{ Number(activity.seckillPrice).toFixed(2) }}</b><del>¥{{ Number(activity.originalPrice).toFixed(2) }}</del></span></div>
              <button class="primary" type="button" :disabled="!activity.active || Number(activity.availableStock) < 1" @click="buySeckill(activity)">{{ Number(activity.availableStock) < 1 ? '已售罄' : activity.active ? '查看并抢购' : '未开始' }}</button>
            </article>
          </div>
        </section>
        <div class="section-title catalog"><div><small>PLATFORM FAVORITES</small><h2>平台热卖</h2></div><span>{{ hasMoreProducts ? `已加载 ${products.length} 件商品` : `共 ${products.length} 件精选商品` }}</span></div>
        <div class="product-grid"><RabbitProductCard v-for="product in products" :key="product.id" :product="product" :liked="isFavorite(product.id)" @favorite="toggleFavorite" @buy="buyNow" @cart="addToCart"/></div>
        <div ref="productSentinel" class="product-load-state" aria-live="polite">
          <span v-if="loadingProducts">正在加载更多商品…</span>
          <button v-else-if="hasMoreProducts" type="button" @click="loadMoreProducts()">继续加载</button>
          <span v-else-if="products.length">已经到底了</span>
          <span v-else>没有找到相关商品</span>
        </div>
        <div v-if="home.announcements && home.announcements.length" class="notice"><b>{{ home.announcements[0].title }}</b><span>{{ home.announcements[0].content }}</span></div>
      </section>
      <section v-else-if="view === 'seckill'" class="content seckill-page">
        <div class="page-title seckill-page-title"><small>LIMITED TIME SALE</small><h1>限时秒杀</h1><p>精选好物限时降价，库存有限，售完即止。</p><button class="primary" type="button" :disabled="seckillLoading" @click="loadSeckillActivities(true)">{{ seckillLoading ? '正在刷新…' : '刷新活动' }}</button></div>
        <div v-if="seckillActivities.length" class="seckill-page-grid">
          <article v-for="activity in seckillActivities" :key="activity.id" class="seckill-card">
            <ProductImage class="seckill-card-image" :src="activity.cover" :alt="activity.productName"/>
            <div class="seckill-card-body">
              <div class="seckill-card-status" :class="{ ended: !activity.active || Number(activity.availableStock) < 1 }">
                <span>{{ Number(activity.availableStock) < 1 ? '已售罄' : activity.active ? '抢购进行中' : '暂未开始' }}</span>
                <small>剩余 {{ activity.availableStock }} 件</small>
              </div>
              <h2>{{ activity.productName }}</h2>
              <p>限时特惠，先到先得</p>
              <div class="seckill-card-price"><strong>¥{{ Number(activity.seckillPrice).toFixed(2) }}</strong><del>¥{{ Number(activity.originalPrice).toFixed(2) }}</del></div>
              <button class="primary seckill-card-action" type="button" :disabled="!activity.active || Number(activity.availableStock) < 1" @click="buySeckill(activity)">{{ Number(activity.availableStock) < 1 ? '已售罄' : activity.active ? '查看详情并抢购' : '即将开始' }}</button>
            </div>
          </article>
        </div>
        <div v-else-if="seckillError" class="empty seckill-empty">秒杀活动暂时无法加载，请点击“刷新活动”重试。</div>
        <div v-else-if="seckillLoading" class="empty seckill-empty">正在加载秒杀活动…</div>
        <div v-else class="empty seckill-empty">当前没有进行中的秒杀活动</div>
      </section>
      <section v-else-if="view === 'product'" class="content narrow">
        <button class="link-button detail-back" type="button" @click="go('home')">← 返回商品列表</button>
        <div v-if="productDetailLoading" class="empty detail-loading">正在加载商品详情…</div>
        <div v-else-if="selectedProduct" class="product-detail">
          <ProductImage class="detail-image" :src="selectedProduct.cover" :alt="selectedProduct.name"/>
          <div class="detail-info">
            <small>{{ selectedProduct.merchantName || '优质商家' }}</small>
            <h1>{{ selectedProduct.name }}</h1>
            <p class="detail-description">{{ selectedProduct.detail || '精选好物，品质保障，放心购买。' }}</p>
            <div class="detail-price">¥{{ Number(selectedSeckillActivity?.seckillPrice ?? selectedProduct.price).toFixed(2) }}</div>
            <div class="detail-stock" :class="{ soldout: Number(selectedProduct.stock) < 1 || (selectedSeckillActivity && Number(selectedSeckillActivity.availableStock) < 1) }">
              {{ selectedSeckillActivity ? `秒杀剩余 ${selectedSeckillActivity.availableStock} 件 · 商品库存 ${selectedProduct.stock} 件` : Number(selectedProduct.stock) > 0 ? `库存 ${selectedProduct.stock} 件` : '当前商品已售罄' }}
            </div>
            <div v-if="!selectedSeckillActivity" class="detail-quantity">
              <span>购买数量</span>
              <div class="quantity"><button type="button" :disabled="productQuantity <= 1" @click="productQuantity = Math.max(1, productQuantity - 1)">−</button><input v-model.number="productQuantity" type="number" min="1" :max="Math.max(Number(selectedProduct.stock) || 1, 1)"/><button type="button" :disabled="productQuantity >= Number(selectedProduct.stock)" @click="productQuantity = Math.min(Number(selectedProduct.stock), productQuantity + 1)">＋</button></div>
            </div>
            <div v-if="addresses.length" class="detail-address">
              <label for="order-address">收货地址</label>
              <el-select id="order-address" v-model="selectedAddressId" placeholder="请选择收货地址">
                <el-option v-for="address in addresses" :key="address.id" :value="address.id" :label="`${address.receiver_name} · ${address.receiver_phone} · ${address.detail_address}`"/>
              </el-select>
              <button class="link-button" type="button" @click="manageProductAddress">管理地址</button>
            </div>
            <button v-else class="link-button detail-add-address" type="button" @click="manageProductAddress">添加收货地址</button>
            <button class="primary detail-submit" type="button" :disabled="productOrderSubmitting || Number(selectedProduct.stock) < 1 || (selectedSeckillActivity && (!selectedSeckillActivity.active || Number(selectedSeckillActivity.availableStock) < 1)) || (!selectedSeckillActivity && (productQuantity < 1 || productQuantity > Number(selectedProduct.stock)))" @click="confirmProductOrder">
              {{ productOrderSubmitting ? '正在提交…' : selectedSeckillActivity ? '确认秒杀' : Number(selectedProduct.stock) < 1 ? '暂时售罄' : '确认下单' }}
            </button>
          </div>
        </div>
      </section>
      <section v-else-if="view === 'cart'" class="content narrow">
        <div class="page-title"><small>YOUR CART</small><h1>购物车</h1><p>选择商品，一次结算。</p></div>
        <div v-for="item in cart" :key="item.id" class="row cart-row" :class="{ unavailable: cartUnavailableReason(item) }"><input v-model="selectedCart" type="checkbox" :value="item.id" :disabled="!!cartUnavailableReason(item)"><ProductImage class="cart-product-image" :src="item.cover" :alt="item.name"/><div><b>{{ item.name }}</b><small v-if="cartUnavailableReason(item)" class="cart-unavailable">{{ cartUnavailableReason(item) }}</small><small>¥{{ Number(item.price).toFixed(2) }}</small></div><span class="quantity"><button @click="updateCart(item,item.quantity-1)">−</button>{{ item.quantity }}<button @click="updateCart(item,item.quantity+1)">＋</button></span><strong>¥{{ (item.price * item.quantity).toFixed(2) }}</strong><button class="link-button" @click="removeCart(item)">删除</button></div>
        <div v-if="cart.length" class="cart-summary"><span>已选 {{ selectedCart.length }} 件，合计 <b>¥{{ selectedTotal.toFixed(2) }}</b></span><button class="primary" :disabled="checkoutSubmitting || !selectedCart.length" @click="checkoutSelected">{{ checkoutSubmitting ? '正在结算…' : '合并结算' }}</button></div><div v-if="!cart.length" class="empty">购物车还是空的，去挑选一些好物吧。</div>
      </section>
      <section v-else-if="view === 'orders'" class="content narrow">
        <div class="page-title"><small>ORDER CENTER</small><h1>我的订单</h1><p>查看订单状态、收货和售后进度。</p></div>
        <div v-for="order in orders" :key="order.id" class="row order-row"><div><b>{{ order.product_name_snapshot }}</b><small>订单号 {{ order.order_no }} · {{ order.receiver_address }}</small><small>下单时间 {{ order.created_at }}</small></div><strong>¥{{ order.total_amount }}</strong><span class="status">{{ order.status }}</span><span class="order-actions"><button v-if="order.status === 'PENDING'" class="link-button" @click="payOrder(order)">去支付</button><button v-if="order.status === 'SHIPPED'" class="link-button" @click="confirmOrder(order)">确认收货</button><button v-if="['PENDING','PAID','SHIPPED','COMPLETED'].includes(order.status)" class="link-button" @click="refundOrder(order)">申请退款</button><button v-if="order.status === 'COMPLETED'" class="link-button" @click="reviewOrder(order)">评价</button></span></div>
        <div v-if="!orders.length" class="empty">还没有订单。</div>
      </section>
      <section v-else-if="view === 'addresses'" class="content narrow">
        <div class="page-title"><small>DELIVERY ADDRESS</small><h1>收货地址</h1><p>下单时将使用默认地址，也可以随时修改。</p></div>
        <div class="address-form"><el-input v-model="addressForm.receiverName" placeholder="收货人姓名"/><el-input v-model="addressForm.receiverPhone" placeholder="联系电话"/><el-input v-model="addressForm.detailAddress" placeholder="详细地址"/><label><input v-model="addressForm.isDefault" type="checkbox"> 设为默认地址</label><button class="primary" @click="saveAddress">{{ addressEditing ? '保存修改' : '新增地址' }}</button><button v-if="addressEditing" class="link-button" @click="resetAddressForm">取消编辑</button></div>
        <div v-for="address in addresses" :key="address.id" class="row address-row"><div><b>{{ address.receiver_name }} · {{ address.receiver_phone }}</b><small>{{ address.detail_address }}</small></div><span v-if="address.is_default" class="status">默认地址</span><button v-else class="link-button" @click="setDefault(address)">设为默认</button><button class="link-button" @click="editAddress(address)">编辑</button><button class="link-button" @click="deleteAddress(address)">删除</button></div>
        <div v-if="!addresses.length" class="empty">还没有收货地址。</div>
      </section>
      <section v-else-if="view === 'manage'" class="content narrow">
        <div class="page-title"><small>CONTROL CENTER</small><h1>工作台</h1><p>{{ isAdmin ? '平台数据与订单管理' : '商家订单处理' }}</p></div>
        <div v-if="dashboard" class="dashboard"><div><b>{{ dashboard.users }}</b><span>用户</span></div><div><b>{{ dashboard.merchants }}</b><span>商家</span></div><div><b>{{ dashboard.products }}</b><span>商品</span></div><div><b>{{ dashboard.orders }}</b><span>订单</span></div></div>
        <div class="section-title"><div><small>ORDER MANAGEMENT</small><h2>待处理订单</h2></div></div>
        <div v-for="order in manageOrders" :key="order.id" class="row order-row"><div><b>{{ order.product_name_snapshot }}</b><small>{{ order.order_no }} · {{ order.receiver_name }} · {{ order.receiver_address }}</small></div><strong>¥{{ order.total_amount }}</strong><span class="status">{{ order.status }}</span><span class="order-actions"><button v-if="order.status === 'PENDING'" class="link-button" @click="updateManageOrder(order,'SHIPPED')">发货</button><button v-if="order.status === 'REFUND_REQUESTED'" class="link-button" @click="updateManageOrder(order,'REFUND_APPROVED')">同意退款</button><button v-if="order.status === 'REFUND_REQUESTED'" class="link-button" @click="updateManageOrder(order,'REFUND_REJECTED')">拒绝退款</button></span></div>
        <div v-if="!manageOrders.length" class="empty">暂无待处理订单。</div>
        <div class="product-manager">
          <div class="section-title"><div><small>PRODUCT MANAGEMENT</small><h2>商品发布与库存</h2></div></div>
          <div class="admin-form product-form"><el-input v-model="productForm.name" placeholder="商品名称"/><el-input v-model="productForm.cover" placeholder="商品封面 URL"/><el-input v-model="productForm.categoryId" placeholder="分类 ID"/><el-input v-model="productForm.price" placeholder="价格"/><el-input v-model="productForm.stock" placeholder="库存"/><el-input v-model="productForm.detail" placeholder="商品详情"/><button class="primary" @click="addProduct">发布商品</button></div>
          <div v-for="product in manageProducts" :key="product.id" class="admin-line product-line"><ProductImage :src="product.cover" :alt="product.name"/><span>{{ product.name }}</span><small>¥{{ product.price }} · 库存 {{ product.stock }} · <b :class="product.status === 'ON_SALE' ? 'on-sale' : 'off-sale'">{{ product.status === 'ON_SALE' ? '已上架' : '已下架' }}</b></small><button class="link-button" @click="toggleProductStatus(product)">{{ product.status === 'ON_SALE' ? '下架' : '上架' }}</button></div>
          <div v-if="!manageProducts.length" class="empty">暂无商品。</div>
        </div>
        <div v-if="isAdmin" class="admin-panel">
          <div class="admin-tabs"><button :class="{active: adminTab === 'categories'}" @click="adminTab = 'categories'">分类</button><button :class="{active: adminTab === 'announcements'}" @click="adminTab = 'announcements'">公告</button><button :class="{active: adminTab === 'banners'}" @click="adminTab = 'banners'">轮播图</button><button :class="{active: adminTab === 'tags'}" @click="adminTab = 'tags'">商家标签</button></div>
          <div v-if="adminTab === 'categories'" class="admin-section"><div class="admin-form"><el-input v-model="categoryForm.name" placeholder="分类名称"/><el-input v-model="categoryForm.logo" placeholder="分类图标 URL（可选）"/><label><input v-model="categoryForm.showOnHome" type="checkbox"> 首页展示</label><button class="primary" @click="addCategory">新增分类</button></div><div v-for="item in adminCategories" :key="item.id" class="admin-line"><span>{{ item.name }}</span><small>{{ item.show_on_home ? '首页展示' : '隐藏' }}</small><button class="link-button" @click="deleteCategory(item.id)">删除</button></div></div>
          <div v-else-if="adminTab === 'announcements'" class="admin-section"><div class="admin-form"><el-input v-model="announcementForm.title" placeholder="公告标题"/><el-input v-model="announcementForm.content" placeholder="公告内容"/><button class="primary" @click="addAnnouncement">发布公告</button></div><div v-for="item in adminAnnouncements" :key="item.id" class="admin-line"><span>{{ item.title }}</span><small>{{ item.content }}</small><button class="link-button" @click="deleteAnnouncement(item.id)">删除</button></div></div>
          <div v-else-if="adminTab === 'banners'" class="admin-section"><div class="admin-form"><el-input v-model="bannerForm.imageUrl" placeholder="图片 URL"/><el-input v-model="bannerForm.productId" placeholder="关联商品 ID（可选）"/><button class="primary" @click="addBanner">添加轮播图</button></div><div v-for="item in adminBanners" :key="item.id" class="admin-line"><span>轮播图 #{{ item.id }}</span><small>{{ item.image_url }}</small></div></div>
          <div v-else class="admin-section"><div class="admin-form"><el-input v-model="tagForm.merchantId" placeholder="商家 ID"/><el-input v-model="tagForm.tag" placeholder="标签名称"/><el-input v-model="tagForm.description" placeholder="标签说明"/><button class="primary" @click="addTag">设置标签</button></div><div v-for="item in adminTags" :key="item.id" class="admin-line"><span>{{ item.merchantName }} · {{ item.tag }}</span><small>{{ item.description }}</small><button class="link-button" @click="deleteTag(item.id)">删除</button></div></div>
        </div>
      </section>
    </main>
    <footer>优选商城 · 精选电商平台 <span>品质商品，安心服务，轻松购物</span></footer>
    <el-dialog v-model="paymentOpen" title="选择支付方式" width="400px">
      <div v-if="paymentOrder" class="payment-dialog">
        <p class="payment-order-summary">订单 {{ paymentOrder.orderNo }} · 应付 <strong>¥{{ Number(paymentOrder.totalAmount).toFixed(2) }}</strong></p>
        <div class="payment-options" role="radiogroup" aria-label="支付方式">
          <button v-for="option in paymentOptions" :key="option.code" type="button" class="payment-option" :class="{ selected: paymentProvider === option.code }" @click="paymentProvider = option.code">
            <span class="payment-mark" :class="option.code.toLowerCase()">{{ option.mark }}</span><span>{{ option.name }}</span><b v-if="paymentProvider === option.code">已选择</b>
          </button>
        </div>
        <p class="payment-note">支付将跳转到对应的第三方收银台，订单会在支付成功后更新为已支付。</p>
        <button class="primary full" :disabled="paymentSubmitting" @click="submitPayment">{{ paymentSubmitting ? '正在发起支付…' : '立即支付' }}</button>
      </div>
    </el-dialog>
    <el-dialog v-model="loginOpen" :title="registerMode ? '创建优选商城账号' : '欢迎回来'" width="380px"><div class="login-form" v-if="!registerMode"><p>登录后即可收藏、加购和下单</p><el-input v-model="login.username" placeholder="用户名"/><el-input v-model="login.password" type="password" show-password placeholder="密码" @keyup.enter="signIn"/><button class="primary full" @click="signIn">登录优选商城</button><div class="auth-switch"><span>还没有账号？</span><button @click="openRegister">立即注册</button></div><small>演示账号：user/user123、merchant/merchant123、admin/admin123</small></div><div class="login-form" v-else><p>注册后即可收藏、加购和下单</p><el-input v-model="registerForm.username" placeholder="用户名"/><el-input v-model="registerForm.nickname" placeholder="昵称"/><el-input v-model="registerForm.phone" placeholder="手机号（可选）"/><el-input v-model="registerForm.password" type="password" show-password placeholder="密码（至少6位）"/><el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="确认密码" @keyup.enter="signUp"/><button class="primary full" @click="signUp">注册并登录</button><div class="auth-switch"><span>已有账号？</span><button @click="registerMode = false">返回登录</button></div></div></el-dialog>
  </div>
</template>

<style scoped>
.auth-switch{display:flex;justify-content:center;align-items:center;gap:5px;color:#8299ad;font-size:12px}
.auth-switch button{border:0;background:transparent;color:#1769d1;cursor:pointer;padding:0;font-size:12px}
.product-load-state{min-height:58px;display:flex;align-items:center;justify-content:center;color:#7f98af;font-size:13px}
.product-load-state button{border:1px solid #b8d2eb;border-radius:4px;background:#fff;color:#1769d1;padding:8px 18px;cursor:pointer}
.product-load-state button:hover{background:#eff7ff}
.product-line{align-items:center}
.cart-product-image{width:65px;height:65px;flex:0 0 65px;border-radius:6px;overflow:hidden}
.cart-row.unavailable{background:#fbfcfd}
.cart-unavailable{color:#c84c50!important}
.cart-row input:disabled{cursor:not-allowed;opacity:.45}
.product-line .product-image-component{width:58px;height:58px;flex:0 0 58px;border-radius:7px;overflow:hidden}
.product-line .product-image-component img{width:100%;height:100%;object-fit:cover}
.product-line .product-image-component span{font-size:10px}
.product-line .on-sale{color:#1d9b68;font-weight:600}
.product-line .off-sale{color:#e45e61;font-weight:600}
.detail-back{margin:0 0 16px -5px}
.product-detail{display:grid;grid-template-columns:minmax(0,1fr) minmax(300px,.9fr);gap:34px;align-items:start;background:#fff;border:1px solid #deebf5;padding:24px}
.detail-image{display:block;width:100%;aspect-ratio:1/1;overflow:hidden;background:#eff6fd;border-radius:6px}
.detail-image :deep(img){width:100%;height:100%;object-fit:cover}
.detail-info{padding:10px 4px}
.detail-info>small{color:#718ba3;font-size:12px}
.detail-info h1{margin:9px 0 12px;color:#234969;font-size:27px;line-height:1.3}
.detail-description{min-height:46px;color:#7b91a5;font-size:13px;line-height:1.7}
.detail-price{margin-top:17px;color:#dc565c;font-size:30px;font-weight:700}
.detail-stock{margin:8px 0 20px;color:#46826d;font-size:13px}
.detail-stock.soldout{color:#c84c50}
.detail-quantity{display:flex;align-items:center;justify-content:space-between;max-width:270px;margin:18px 0;color:#536f88;font-size:13px}
.detail-quantity .quantity{border:1px solid #d6e3ed;border-radius:4px;gap:0}
.detail-quantity .quantity button{border:0;border-radius:0}
.detail-quantity input{width:46px;border:0;text-align:center;color:#3d5871;outline:0}
.detail-address{display:grid;grid-template-columns:1fr auto;gap:7px 10px;align-items:center;margin:20px 0 16px}
.detail-address label{grid-column:1/-1;color:#536f88;font-size:13px}
.detail-address .el-select{width:100%;min-width:0}
.detail-address .link-button{white-space:nowrap}
.detail-add-address{margin:4px 0 16px}
.detail-submit{width:100%;max-width:300px}
.detail-submit:disabled{background:#a7b4c0;cursor:not-allowed}
.detail-loading{padding:80px 15px}
.seckill-section{margin-top:32px}
.seckill-section .section-title{margin:0 0 14px}
.seckill-list{display:grid;gap:10px}
.seckill-item{display:grid;grid-template-columns:94px minmax(0,1fr) auto;align-items:center;gap:15px;padding:10px 14px;background:#fff;border:1px solid #e0ebf5;border-radius:6px}
.seckill-image{width:94px;height:78px;overflow:hidden;background:#eff6fd;border-radius:4px}
.seckill-image :deep(img){width:100%;height:100%;object-fit:cover}
.seckill-copy{min-width:0}
.seckill-copy small{color:#ca565b;font-size:11px}
.seckill-copy h3{margin:5px 0;color:#31516c;font-size:14px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.seckill-copy span{display:flex;align-items:center;gap:9px}
.seckill-copy b{color:#d64f57;font-size:17px}
.seckill-copy del{color:#8ea0b0;font-size:11px}
.seckill-item>.primary{white-space:nowrap}
.seckill-item>.primary:disabled{background:#a7b4c0;cursor:not-allowed}
.seckill-page{max-width:1120px}
.seckill-page-title{position:relative}
.seckill-page-title .primary{position:absolute;right:0;bottom:22px;padding:8px 14px;font-size:12px}
.seckill-page-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px;margin-top:24px}
.seckill-card{overflow:hidden;background:#fff;border:1px solid #deebf5;border-radius:8px;transition:transform .18s,box-shadow .18s}
.seckill-card:hover{transform:translateY(-3px);box-shadow:0 12px 25px #2d6fa516}
.seckill-card-image{width:100%;height:220px;background:#eff6fd}
.seckill-card-image :deep(img){width:100%;height:100%;object-fit:cover}
.seckill-card-body{padding:16px}
.seckill-card-status{display:flex;align-items:center;justify-content:space-between;color:#ca565b;font-size:12px}
.seckill-card-status.ended{color:#8d9dac}
.seckill-card-status small{color:#889dac;font-size:11px}
.seckill-card h2{margin:9px 0 5px;color:#294d6d;font-size:18px}
.seckill-card p{margin:0;color:#8ba0b3;font-size:12px}
.seckill-card-price{display:flex;align-items:baseline;gap:9px;margin-top:16px}
.seckill-card-price strong{color:#d64f57;font-size:25px}
.seckill-card-price del{color:#8ea0b0;font-size:12px}
.seckill-card-action{width:100%;margin-top:16px}
.seckill-card-action:disabled{background:#a7b4c0;cursor:not-allowed}
.seckill-empty{padding:120px 15px}
.payment-dialog{display:flex;flex-direction:column;gap:14px}.payment-order-summary{margin:0;color:#536f88;font-size:13px}.payment-order-summary strong{color:#d64f57;font-size:22px}.payment-options{display:grid;gap:9px}.payment-option{display:flex;align-items:center;gap:10px;width:100%;padding:11px 12px;border:1px solid #d9e7f2;border-radius:6px;background:#fff;color:#345875;text-align:left;cursor:pointer}.payment-option.selected{border-color:#1769d1;background:#f2f8ff;box-shadow:0 0 0 1px #1769d1 inset}.payment-option b{margin-left:auto;color:#1769d1;font-size:11px;font-weight:normal}.payment-mark{display:grid;place-items:center;width:28px;height:28px;border-radius:6px;color:#fff;font-size:15px;font-weight:bold}.payment-mark.alipay{background:#1677ff}.payment-mark.wechat{background:#20b767}.payment-note{margin:0;color:#8a9eaf;font-size:11px;line-height:1.6}
@media(max-width:700px){.product-detail{grid-template-columns:1fr;gap:16px;padding:15px}.detail-image{max-height:420px}.detail-info{padding:2px}.detail-info h1{font-size:22px}.detail-submit{max-width:none}}
@media(max-width:520px){.seckill-item{grid-template-columns:68px minmax(0,1fr);gap:9px;padding:9px}.seckill-image{width:68px;height:68px}.seckill-item>.primary{grid-column:1/-1;width:100%}}
@media(max-width:820px){.seckill-page-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:560px){.seckill-page-title .primary{position:static;margin-top:8px}.seckill-page-grid{grid-template-columns:1fr}.seckill-card-image{height:200px}}
</style>
