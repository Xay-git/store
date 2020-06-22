package com.sweet.modular.stockDetail.service.impl;import com.sweet.modular.stockDetail.entity.StockDetail;import com.sweet.modular.stockDetail.mapper.StockDetailMapper;import com.sweet.modular.stockDetail.service.StockDetailService;import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;import org.springframework.stereotype.Service;import com.baomidou.mybatisplus.core.metadata.IPage;import com.baomidou.mybatisplus.extension.plugins.pagination.Page;import com.sweet.core.model.system.LayuiPageFactory;import com.sweet.core.model.system.LayuiPageInfo;/** * <p> * 入库单详情 服务实现类 * </p> * * @author admin * @since 2020-06-20 */@Servicepublic class StockDetailServiceImpl extends ServiceImpl<StockDetailMapper, StockDetail> implements StockDetailService {    @Override    public LayuiPageInfo findPageBySpec(StockDetail stockDetail) {        Page pageContext = LayuiPageFactory.defaultPage();        IPage page = this.baseMapper.customPageList(pageContext, stockDetail);        return LayuiPageFactory.createPageInfo(page);    }}