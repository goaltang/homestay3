import axios from "axios";
import { API_BASE_URL } from "@/config/constants";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// 请求拦截器添加token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 房源类型API
export const getAllTypesApi = () => {
  return api.get("/api/v1/homestay-types/all");
};

export const getTypeByIdApi = (id: number) => {
  return api.get(`/api/v1/homestay-types/${id}`);
};

export const createTypeApi = (data: any) => {
  return api.post("/api/v1/homestay-types", data);
};

export const updateTypeApi = (id: number, data: any) => {
  return api.put(`/api/v1/homestay-types/${id}`, data);
};

export const deleteTypeApi = (id: number) => {
  return api.delete(`/api/v1/homestay-types/${id}`);
};

// 类型分类API
export const getAllCategoriesApi = () => {
  return api.get("/api/v1/homestay-types/categories");
};

export const getCategoryByIdApi = (id: number) => {
  return api.get(`/api/v1/homestay-types/categories/${id}`);
};

export const createCategoryApi = (data: any) => {
  return api.post("/api/v1/homestay-types/categories", data);
};

export const updateCategoryApi = (id: number, data: any) => {
  return api.put(`/api/v1/homestay-types/categories/${id}`, data);
};

export const deleteCategoryApi = (id: number) => {
  return api.delete(`/api/v1/homestay-types/categories/${id}`);
};

// 获取按分类分组的房源类型
export const getTypesByCategoryApi = () => {
  return api.get("/api/v1/homestay-types/by-category");
};

// 获取指定分类下的房源类型
export const getTypesByCategoryIdApi = (categoryId: number) => {
  return api.get(`/api/v1/homestay-types/category/${categoryId}`);
};

export default {
  getAllTypesApi,
  getTypeByIdApi,
  createTypeApi,
  updateTypeApi,
  deleteTypeApi,
  getAllCategoriesApi,
  getCategoryByIdApi,
  createCategoryApi,
  updateCategoryApi,
  deleteCategoryApi,
  getTypesByCategoryApi,
  getTypesByCategoryIdApi,
};
