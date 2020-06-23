package com.sweet.modular.stock.controller;import cn.hutool.json.JSONUtil;import com.sweet.core.exception.ServiceException;import com.sweet.core.shiro.ShiroKit;import com.sweet.core.translationDict.DictParam;import com.sweet.core.translationDict.TranslationDict;import com.sweet.modular.inventory.entity.Inventory;import com.sweet.modular.inventory.service.InventoryService;import com.sweet.modular.stock.StockDto;import com.sweet.modular.stockDetail.entity.StockDetail;import com.sweet.modular.stockDetail.service.StockDetailService;import com.sweet.system.entity.User;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.bind.annotation.RequestMapping;import com.sweet.core.model.ResultBean;import com.sweet.core.model.system.LayuiPageInfo;import com.sweet.modular.stock.entity.Stock;import com.sweet.modular.stock.service.StockService;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.web.bind.annotation.RequestMapping;import com.sweet.core.util.StringUtil;import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.ResponseBody;import org.springframework.stereotype.Controller;import java.util.Date;import java.util.List;/** * <p> * 入库单 前端控制器 * </p> * * @author admin * @since 2020-06-18 */@Controller@RequestMapping("/admin/stock")public class StockController {    @Autowired    StockService stockService;    @Autowired    StockDetailService stockDetailService;    @Autowired    InventoryService inventoryService;    /**     * 列表页     */    @RequestMapping("/stock_list")    public String list(){        return "/modular/stock/stock_list";    }    @RequestMapping("/choose_product")    public String choose_product(){        return "/modular/stock/choose_product";    }    /**     * 编辑页     */    @RequestMapping("/stock_edit")    public String edit(){        return "/modular/stock/stock_edit";    }    /**     * 添加/编辑     */    @RequestMapping("/addStock")    @ResponseBody    @Transactional    public ResultBean addStock(Stock stock){        String stockNo = "SHEYI"+StringUtil.getNumberForPK();        stock.setStockStatus(1);        stock.setStockNo(stockNo);        stockService.saveOrUpdate(stock);        String productList = stock.getProductList();        List<StockDto> list = JSONUtil.toList(JSONUtil.parseArray(productList), StockDto.class);        list.stream().forEach(stockDto -> {            StockDetail stockDetail = new StockDetail();            stockDetail.setProductId(stockDto.getId());            stockDetail.setProductName(stockDto.getName());            stockDetail.setCount(stockDto.getCount());            stockDetail.setStockId(stock.getId());            stockDetailService.save(stockDetail);        });        return ResultBean.success(stock);    }    /**     * 添加/编辑     */    @RequestMapping("/checkStock")    @ResponseBody    @Transactional    public ResultBean checkStock(Stock stock){        if(stock.getStockStatus()==2){            throw new ServiceException("该入库单已经审核过~");        }        User loginUser = ShiroKit.getUser();        stock.setCheckId(loginUser.getUserId());        stock.setCheckName(loginUser.getRealName());        stock.setCheckTime(new Date());        stock.setStockStatus(2);        stockService.updateById(stock);        if(stock.getStockStatus()==2){            List<StockDetail> list = stockDetailService.getDetailByStockId(stock.getId());            list.stream().forEach(stockDetail -> {                String deptId = stockDetail.getDeptId();                Long productId = stockDetail.getProductId();                Long count  = stockDetail.getCount();                Inventory inventory = inventoryService.getInventoryByDeptId(deptId,productId);                inventoryService.updateInventory(inventory,count);            });        }        return ResultBean.success(stock);    }    /**     * 删除     */    @RequestMapping("/delStock")    @ResponseBody    public ResultBean delStock(Stock stock){        stockService.removeById(stock);        return ResultBean.success(stock);    }    /**     * 添加修改菜单     * @param menu     * @return     */    @RequestMapping("/getStockDetail")    @ResponseBody    public ResultBean getStockDetail(String id){        Stock stock = stockService.getById(id);        return ResultBean.success(stock);    }    /**     * 列表数据     */    @RequestMapping("/getStockList")    @ResponseBody    public LayuiPageInfo getStockList(Stock Stock){        LayuiPageInfo pageInfo = stockService.findPageBySpec(Stock);        return pageInfo;    }}