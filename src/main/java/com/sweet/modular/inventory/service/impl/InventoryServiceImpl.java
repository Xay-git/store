package com.sweet.modular.inventory.service.impl;import com.sweet.modular.inventory.entity.Inventory;import com.sweet.modular.inventory.mapper.InventoryMapper;import com.sweet.modular.inventory.service.InventoryService;import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;import org.springframework.stereotype.Service;import com.baomidou.mybatisplus.core.metadata.IPage;import com.baomidou.mybatisplus.extension.plugins.pagination.Page;import com.sweet.core.model.system.LayuiPageFactory;import com.sweet.core.model.system.LayuiPageInfo;import java.math.BigDecimal;/** * <p> * 库存表 服务实现类 * </p> * * @author admin * @since 2020-06-22 */@Servicepublic class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements InventoryService {    @Override    public LayuiPageInfo findPageBySpec(Inventory inventory) {        Page pageContext = LayuiPageFactory.defaultPage();        IPage page = this.baseMapper.customPageList(pageContext, inventory);        return LayuiPageFactory.createPageInfo(page);    }    @Override    public Inventory getInventoryByDeptId(String deptId, Long productId) {        Inventory inventory = baseMapper.getInventoryByDeptId(deptId,productId);        if(inventory==null){            inventory = new Inventory();            inventory.setDeptId(deptId);            inventory.setProductId(productId);            inventory.setCount(0L);            this.save(inventory);        }        return inventory;    }    @Override    public Inventory updateInventory(Inventory inventory, Long count) {        Long oldCount = inventory.getCount();        inventory.setCount(inventory.getCount()+count);        updateById(inventory);        return inventory;    }}