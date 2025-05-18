import request from "../utils/request";

/**
 * 获取所有可用的设施和服务列表
 * @param params 查询参数
 */
export const getAmenitiesApi = (params?: any) => {
  return request({
    url: "/api/amenities",
    method: "get",
    params,
  });
};

/**
 * 获取所有设施列表(按分类)
 */
export const getAmenitiesByCategoryApi = () => {
  return request({
    url: "/api/amenities/by-categories",
    method: "get",
  });
};

/**
 * 初始化默认设施
 * 一键添加系统预设的所有设施
 */
export const initDefaultAmenitiesApi = () => {
  return request({
    url: "/api/amenities/init-defaults",
    method: "post",
  });
};

/**
 * 创建设施
 * @param data 设施数据
 */
export const createAmenityApi = (data: any) => {
  return request({
    url: "/api/amenities",
    method: "post",
    data,
  });
};

/**
 * 更新设施
 * @param id 设施ID
 * @param data 设施数据
 */
export const updateAmenityApi = (id: string | number, data: any) => {
  return request({
    url: `/api/amenities/${id}`,
    method: "put",
    data,
  });
};

/**
 * 删除设施
 * @param id 设施ID
 */
export const deleteAmenityApi = (id: string | number) => {
  return request({
    url: `/api/amenities/${id}`,
    method: "delete",
  });
};

/**
 * 获取所有设施分类
 */
export const getCategoriesApi = () => {
  return request({
    url: "/api/amenities/categories",
    method: "get",
  });
};

/**
 * 创建设施分类
 * @param data 分类数据
 */
export const createCategoryApi = (data: any) => {
  return request({
    url: "/api/amenities/categories",
    method: "post",
    data,
  });
};

/**
 * 更新设施分类
 * @param code 分类编码
 * @param data 分类数据
 */
export const updateCategoryApi = (code: string, data: any) => {
  return request({
    url: `/api/amenities/categories/${code}`,
    method: "put",
    data,
  });
};

/**
 * 删除设施分类
 * @param code 分类编码
 */
export const deleteCategoryApi = (code: string) => {
  return request({
    url: `/api/amenities/categories/${code}`,
    method: "delete",
  });
};

/**
 * 批量删除设施分类
 * @param codes 分类编码数组
 */
export const batchDeleteCategoriesApi = (codes: string[]) => {
  return request({
    url: "/api/amenities/categories/batch",
    method: "delete",
    data: { codes },
  });
};

/**
 * 一键激活所有设施
 */
export const activateAllAmenitiesApi = () => {
  return request({
    url: "/api/amenities/activate-all",
    method: "put",
  });
};
