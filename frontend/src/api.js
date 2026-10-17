import axios from 'axios'
const apiBase=import.meta.env.VITE_API_BASE||'http://localhost:8080/api'
const apiOrigin=apiBase.replace(/\/api\/?$/, '')
const api=axios.create({baseURL:apiBase})
api.interceptors.request.use(c=>{const t=localStorage.getItem('mall_token');if(t)c.headers.Authorization='Bearer '+t;return c})
export const call=async(config)=>{
  try {
    const r=await api(config)
    if(r.data?.success===false)throw new Error(r.data.message)
    return r.data.data
  } catch(error) {
    const message=error.response?.data?.message||error.response?.data?.error||error.message||'请求失败'
    throw new Error(message)
  }
}
export const assetUrl=(value)=>{
  if(!value)return ''
  if(/^(https?:|data:|blob:)/i.test(value))return value
  return `${apiOrigin}${value.startsWith('/')?'':'/'}${value}`
}
export const uploadImage=(file)=>{
  const form=new FormData()
  form.append('file',file)
  return call({url:'/uploads/images',method:'post',data:form})
}
export default api
