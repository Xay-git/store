package com.sweet.modular.stockDetail.service;import com.sweet.modular.stockDetail.entity.StockDetail;import com.baomidou.mybatisplus.extension.service.IService;import com.sweet.core.model.system.LayuiPageInfo;import com.baomidou.mybatisplus.extension.service.IService;/** * <p> * 入库单详情 服务类 * </p> * * @author admin * @since 2020-06-20 */public interface StockDetailService extends IService<StockDetail> {    public LayuiPageInfo findPageBySpec(StockDetail stockDetail);}