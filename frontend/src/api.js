import axios from 'axios'
const api=axios.create({baseURL:import.meta.env.VITE_API_BASE||'http://localhost:8080/api'})
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
export default api
